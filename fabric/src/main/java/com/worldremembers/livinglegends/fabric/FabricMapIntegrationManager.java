package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.NameRecipe;
import com.worldremembers.livinglegends.NamedPlace;
import com.worldremembers.livinglegends.PlaceType;
import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import com.worldremembers.livinglegends.map.MapIntegrationSettings;
import com.worldremembers.livinglegends.map.MapPlaceDescriptor;
import com.worldremembers.livinglegends.visual.PlaceVisualThemeResolver;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class FabricMapIntegrationManager {
    private static final long PERF_LOG_THRESHOLD_NANOS = 10_000_000L;
    private static volatile boolean registered;
    private static final Map<UUID, String> LAST_SYNC_FINGERPRINTS = new LinkedHashMap<>();

    private FabricMapIntegrationManager() {
    }

    static synchronized void registerNetworking(Logger logger) {
        if (registered) {
            return;
        }
        MapIntegrationS2CPayload.registerType();
        MapDestinationS2CPayload.registerType();
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> syncPlayer(handler.player, logger, "join"))
        );
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                LAST_SYNC_FINGERPRINTS.remove(handler.player.getUuid())
        );
        registered = true;
    }

    static void syncAllFromWorld(Object worldOrServer, Logger logger) {
        MinecraftServer server = serverFrom(worldOrServer);
        if (server == null) {
            return;
        }
        syncAll(server, logger);
    }

    static void syncAll(MinecraftServer server, Logger logger) {
        if (server == null) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            syncPlayer(player, logger, "sync_all");
        }
    }

    static void clearSyncFingerprints() {
        LAST_SYNC_FINGERPRINTS.clear();
    }

    static void syncPlayerById(Object worldOrServer, String playerId, Logger logger) {
        MinecraftServer server = serverFrom(worldOrServer);
        if (server == null || playerId == null || playerId.isBlank()) {
            return;
        }
        ServerPlayerEntity player = playerById(server, playerId);
        if (player != null) {
            syncPlayer(player, logger, "player_dirty");
        }
    }

    static void syncPlayer(ServerPlayerEntity player, Logger logger) {
        syncPlayer(player, logger, "direct");
    }

    static void syncPlayer(ServerPlayerEntity player, Logger logger, String reason) {
        if (player == null) {
            return;
        }
        long startNanos = System.nanoTime();
        MapIntegrationSettings settings = MapIntegrationSettings.fromConfig(WorldRemembersLivingLegends.config());
        List<MapPlaceDescriptor> descriptors = settings.placeLabelsEnabled()
                ? discoveredPlaceDescriptors(player, settings, logger)
                : List.of();
        String fingerprint = fingerprint(settings, descriptors);
        UUID playerId = player.getUuid();
        boolean forceSend = "journal_open".equals(reason);
        if (!forceSend && fingerprint.equals(LAST_SYNC_FINGERPRINTS.get(playerId))) {
            logPerf(logger, "map_label_sync_skip", player, descriptors.size(), startNanos, reason);
            return;
        }
        if (send(player, new MapIntegrationS2CPayload(settings, descriptors), logger)) {
            LAST_SYNC_FINGERPRINTS.put(playerId, fingerprint);
        } else {
            LAST_SYNC_FINGERPRINTS.remove(playerId);
        }
        logPerf(logger, "map_label_sync", player, descriptors.size(), startNanos, reason);
    }

    static void removeDestinationFromWorld(Object worldOrServer, String placeId, Logger logger) {
        if (placeId == null || placeId.isBlank()) {
            return;
        }
        sendDestinationToAll(serverFrom(worldOrServer), MapDestinationS2CPayload.remove(placeId), logger);
    }

    static void refreshDestinationFromWorld(Object worldOrServer, NamedPlace place, Logger logger) {
        if (place == null) {
            return;
        }
        sendDestinationToAll(serverFrom(worldOrServer), MapDestinationS2CPayload.refresh(toDescriptor(place)), logger);
    }

    private static List<MapPlaceDescriptor> discoveredPlaceDescriptors(
            ServerPlayerEntity player,
            MapIntegrationSettings settings,
            Logger logger
    ) {
        Set<String> discovered = WorldRemembersLivingLegendsFabricStorage.discoveredPlaces(
                player.getServerWorld(),
                player.getUuidAsString(),
                logger
        );
        if (discovered.isEmpty()) {
            return List.of();
        }
        List<MapPlaceDescriptor> result = new ArrayList<>();
        for (NamedPlace place : WorldRemembersLivingLegendsFabricStorage.places(player.getServerWorld(), logger)) {
            if (!shouldSendPlace(place, discovered, settings)) {
                continue;
            }
            result.add(toDescriptor(place));
        }
        return result;
    }

    private static boolean shouldSendPlace(
            NamedPlace place,
            Set<String> discovered,
            MapIntegrationSettings settings
    ) {
        if (place == null || !discovered.contains(place.placeIdString())) {
            return false;
        }
        if (place.placeType() == PlaceType.UNKNOWN || place.placeType() == PlaceType.CAMP) {
            return false;
        }
        if (!WorldRemembersLivingLegends.config().placeTypes.shouldDisplayExisting(place.placeType())) {
            return false;
        }
        return place.placeType() != PlaceType.GENERAL_LANDMARK || settings.placeLabels().showGeneralLandmarks();
    }

    private static MapPlaceDescriptor toDescriptor(NamedPlace place) {
        String fallback = place.manuallyRenamed()
                ? place.manualName()
                : WorldRemembersLivingLegendsFabricNameResolver.resolveToString(place.nameRecipe());
        NameRecipe recipe = place.nameRecipe() == null ? NameRecipe.empty() : place.nameRecipe();
        return new MapPlaceDescriptor(
                place.placeIdString(),
                fallback,
                place.placeType(),
                place.dimensionId(),
                place.center().x(),
                place.center().y(),
                place.center().z(),
                place.radius(),
                place.manuallyRenamed(),
                place.manualName(),
                recipe,
                fallback,
                "",
                PlaceVisualThemeResolver.fromPlace(place)
        );
    }

    private static boolean send(ServerPlayerEntity player, MapIntegrationS2CPayload payload, Logger logger) {
        try {
            if (!ServerPlayNetworking.canSend(player, MapIntegrationS2CPayload.ID)) {
                return false;
            }
            ServerPlayNetworking.send(player, payload);
            return true;
        } catch (RuntimeException exception) {
            if (logger != null && WorldRemembersLivingLegends.config().debug.enabled) {
                logger.warn("Could not send map integration payload: " + exception.getMessage());
            }
            return false;
        }
    }

    private static void sendDestinationToAll(MinecraftServer server, MapDestinationS2CPayload payload, Logger logger) {
        if (server == null || payload == null) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            sendDestination(player, payload, logger);
        }
    }

    private static boolean sendDestination(
            ServerPlayerEntity player,
            MapDestinationS2CPayload payload,
            Logger logger
    ) {
        try {
            if (!ServerPlayNetworking.canSend(player, MapDestinationS2CPayload.ID)) {
                return false;
            }
            ServerPlayNetworking.send(player, payload);
            return true;
        } catch (RuntimeException exception) {
            if (logger != null && WorldRemembersLivingLegends.config().debug.enabled) {
                logger.warn("Could not send map destination payload: " + exception.getMessage());
            }
            return false;
        }
    }

    private static String fingerprint(MapIntegrationSettings settings, List<MapPlaceDescriptor> descriptors) {
        StringBuilder builder = new StringBuilder(128 + descriptors.size() * 96);
        builder.append("enabled=").append(settings.enabled())
                .append(";journeyMap=").append(settings.journeyMapEnabled())
                .append(";xaero=").append(settings.xaeroMapEnabled())
                .append(";labels=").append(settings.placeLabels().enabled())
                .append(',').append(settings.placeLabels().showGeneralLandmarks())
                .append(',').append(settings.placeLabels().showTooltips())
                .append(',').append(settings.placeLabels().showCoordinatesInTooltip())
                .append(',').append(settings.placeLabels().showDimensionInTooltip())
                .append(',').append(settings.placeLabels().showPlaceTypeInTooltip())
                .append(";destinations=").append(settings.destinations().enabled())
                .append(',').append(settings.destinations().onlyOneActiveDestination())
                .append(',').append(settings.destinations().clearWhenEnteringPlaceRadius())
                .append(',').append(settings.destinations().fallbackClearDistanceBlocks());
        for (MapPlaceDescriptor place : descriptors) {
            if (place == null) {
                continue;
            }
            NameRecipe recipe = place.nameRecipe() == null ? NameRecipe.empty() : place.nameRecipe();
            builder.append("\nplace=").append(place.placeId())
                    .append('|').append(place.placeType().idString())
                    .append('|').append(place.dimensionId())
                    .append('@').append(place.centerX()).append(',').append(place.centerY()).append(',').append(place.centerZ())
                    .append('|').append(place.radius())
                    .append('|').append(place.manualName())
                    .append('|').append(place.manualNameText())
                    .append('|').append(place.serverResolvedFallbackName())
                    .append('|').append(place.visualTheme().fingerprintKey())
                    .append('|').append(recipe.recipeSignature())
                    .append('|').append(recipe.seed())
                    .append('|').append(recipe.fallbackResolvedName());
        }
        return builder.toString();
    }

    private static void logPerf(
            Logger logger,
            String phase,
            ServerPlayerEntity player,
            int places,
            long startNanos,
            String reason
    ) {
        if (logger == null || !WorldRemembersLivingLegends.config().debug.enabled) {
            return;
        }
        long elapsed = System.nanoTime() - startNanos;
        if (elapsed < PERF_LOG_THRESHOLD_NANOS) {
            return;
        }
        logger.info("living_legends_perf phase=" + phase
                + " player=" + player.getName().getString()
                + " places=" + places
                + " ms=" + String.format(java.util.Locale.ROOT, "%.2f", elapsed / 1_000_000.0)
                + " reason=" + (reason == null || reason.isBlank() ? "unknown" : reason));
    }

    private static MinecraftServer serverFrom(Object value) {
        if (value instanceof MinecraftServer server) {
            return server;
        }
        if (value instanceof ServerWorld world) {
            return world.getServer();
        }
        return null;
    }

    private static ServerPlayerEntity playerById(MinecraftServer server, String playerId) {
        try {
            UUID uuid = UUID.fromString(playerId.trim());
            return server.getPlayerManager().getPlayer(uuid);
        } catch (IllegalArgumentException ignored) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player.getUuidAsString().equals(playerId.trim())) {
                    return player;
                }
            }
            return null;
        }
    }
}
