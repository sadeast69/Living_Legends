package com.worldremembers.livinglegends.neoforge;

import com.worldremembers.livinglegends.DeathSiteEnvironment;
import com.worldremembers.livinglegends.NamedPlace;
import com.worldremembers.livinglegends.PlaceCause;
import com.worldremembers.livinglegends.PlaceType;
import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import com.worldremembers.livinglegends.config.LivingLegendsConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class WorldRemembersLivingLegendsNeoForgeNotifications {
    private static final ArrayDeque<Long> PLACE_CREATED_NOTIFICATION_TIMES = new ArrayDeque<>();
    private static final long ONE_MINUTE_MILLIS = 60_000L;

    private WorldRemembersLivingLegendsNeoForgeNotifications() {
    }

    public static void notifyPlaceCreated(Object serverObject, NamedPlace place, Logger logger) {
        LivingLegendsConfig config = WorldRemembersLivingLegends.config();
        LivingLegendsConfig.PlaceCreated settings = config.notifications.placeCreated;
        if (!(serverObject instanceof MinecraftServer server) || place == null) {
            debug(logger, config, "notification skipped reason=server_or_place_unavailable");
            return;
        }
        if (!settings.enabled) {
            debug(logger, config, "notification skipped reason=notifications_disabled placeId=" + place.placeIdString());
            return;
        }
        if (!settings.enabledFor(place.placeType())) {
            debug(logger, config, "notification skipped reason=type_disabled placeId=" + place.placeIdString()
                    + " type=" + place.placeType().name());
            return;
        }
        if (rateLimited(settings.maxNotificationsPerMinute)) {
            debug(logger, config, "notification skipped reason=rate_limited placeId=" + place.placeIdString()
                    + " type=" + place.placeType().name());
            return;
        }

        List<ServerPlayer> recipients = recipients(server, place, settings);
        if (recipients.isEmpty()) {
            debug(logger, config, "notification skipped reason=no_recipients placeId=" + place.placeIdString());
            return;
        }

        String messageKey = messageKey(place);
        MutableComponent message = Component.translatable(messageKey, displayName(place));
        if (settings.includeCoordinates) {
            message.append(Component.literal(" "))
                    .append(Component.translatable(
                            "living_legends.message.place_created.coords",
                            place.center().x(),
                            place.center().y(),
                            place.center().z()
                    ));
        }
        if (settings.includeDimension) {
            message.append(Component.literal(" "))
                    .append(Component.translatable("living_legends.message.place_created.dimension", place.dimensionId()));
        }
        if (settings.includePlaceType) {
            message.append(Component.literal(" "))
                    .append(Component.translatable("living_legends.message.place_created.type", place.placeType().name()));
        }

        for (ServerPlayer player : recipients) {
            player.sendSystemMessage(message);
        }
        debug(logger, config, "notification sent"
                + " placeId=" + place.placeIdString()
                + " type=" + place.placeType().name()
                + " messageKey=" + messageKey
                + " recipients=" + recipients.size());
    }

    private static MutableComponent displayName(NamedPlace place) {
        if (place.manuallyRenamed() && !place.manualName().isBlank()) {
            return Component.literal(place.manualName());
        }
        return WorldRemembersLivingLegendsNeoForgeNameResolver.resolve(place.nameRecipe()).copy();
    }

    private static List<ServerPlayer> recipients(
            MinecraftServer server,
            NamedPlace place,
            LivingLegendsConfig.PlaceCreated settings
    ) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (!settings.sendToNearbyPlayersOnly) {
            return settings.sendToAllPlayers ? new ArrayList<>(players) : List.of();
        }

        List<ServerPlayer> result = new ArrayList<>();
        long radius = Math.max(1L, settings.nearbyRadiusBlocks);
        long radiusSquared = radius * radius;
        for (ServerPlayer player : players) {
            String playerDimension = player.serverLevel().dimension().location().toString();
            if (!place.dimensionId().equals(playerDimension)) {
                continue;
            }
            long dx = player.blockPosition().getX() - place.center().x();
            long dy = player.blockPosition().getY() - place.center().y();
            long dz = player.blockPosition().getZ() - place.center().z();
            if (dx * dx + dy * dy + dz * dz <= radiusSquared) {
                result.add(player);
            }
        }
        return result;
    }

    private static String messageKey(NamedPlace place) {
        PlaceCause cause = place.cause();
        String firstDiscoveryKey = cause == null ? "" : cause.firstDiscoveryKey();
        String blockId = cause == null ? "" : cause.blockId();
        String structureId = cause == null ? "" : cause.structureId();
        String bossId = cause == null ? "" : cause.bossId();
        String portalType = cause == null ? "" : cause.portalType();

        if (place.placeType() == PlaceType.FIRST_DISCOVERY && "minecraft:stronghold".equals(structureId)) {
            return "living_legends.message.place_created.first_discovery.stronghold";
        }
        if (place.placeType() == PlaceType.FIRST_DISCOVERY
                && ("world:first_diamond_ore_mined".equals(firstDiscoveryKey)
                || "minecraft:diamond_ore".equals(blockId)
                || "minecraft:deepslate_diamond_ore".equals(blockId))) {
            return "living_legends.message.place_created.first_discovery.diamond";
        }
        if (place.placeType() == PlaceType.BOSS_SITE && "minecraft:wither".equals(bossId)) {
            return "living_legends.message.place_created.boss_site.wither";
        }
        if (place.placeType() == PlaceType.BOSS_SITE && "minecraft:ender_dragon".equals(bossId)) {
            return "living_legends.message.place_created.boss_site.ender_dragon";
        }
        if (place.placeType() == PlaceType.PORTAL_LANDMARK && "nether".equals(portalType)) {
            return "living_legends.message.place_created.portal_landmark.nether";
        }
        if (place.placeType() == PlaceType.DEATH_SITE && place.environment() == DeathSiteEnvironment.SURFACE) {
            return "living_legends.message.place_created.death_site.surface";
        }
        if (place.placeType() == PlaceType.DEATH_SITE && place.environment() == DeathSiteEnvironment.CAVE) {
            return "living_legends.message.place_created.death_site.cave";
        }
        return "living_legends.message.place_created." + place.placeType().idString();
    }

    private static boolean rateLimited(int maxNotificationsPerMinute) {
        if (maxNotificationsPerMinute <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        while (!PLACE_CREATED_NOTIFICATION_TIMES.isEmpty()
                && now - PLACE_CREATED_NOTIFICATION_TIMES.peekFirst() > ONE_MINUTE_MILLIS) {
            PLACE_CREATED_NOTIFICATION_TIMES.removeFirst();
        }
        if (PLACE_CREATED_NOTIFICATION_TIMES.size() >= maxNotificationsPerMinute) {
            return true;
        }
        PLACE_CREATED_NOTIFICATION_TIMES.addLast(now);
        return false;
    }

    private static void debug(Logger logger, LivingLegendsConfig config, String message) {
        if (logger != null && config != null && config.debug != null && config.debug.enabled) {
            logger.info("World Remembers " + message);
        }
    }
}
