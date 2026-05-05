package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.DeathSiteEnvironment;
import com.worldremembers.livinglegends.NameRecipe;
import com.worldremembers.livinglegends.NamedPlace;
import com.worldremembers.livinglegends.PlaceType;
import com.worldremembers.livinglegends.WorldPos;
import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import com.worldremembers.livinglegends.config.LivingLegendsConfig;
import com.worldremembers.livinglegends.visual.PlaceVisualThemeResolver;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class WorldRemembersLivingLegendsFabricPlaceTitles {
    private static final int CELL_SIZE = 128;
    private static final long PERF_LOG_THRESHOLD_NANOS = 10_000_000L;
    private static final Map<UUID, PlayerTitleState> PLAYER_STATES = new HashMap<>();
    private static final SpatialIndex INDEX = new SpatialIndex();
    private static long tickCounter;
    private static boolean indexDirty = true;
    private static volatile boolean networkingRegistered;

    private WorldRemembersLivingLegendsFabricPlaceTitles() {
    }

    static void registerNetworking(Logger logger) {
        try {
            PlaceTitleS2CPayload.registerType();
            networkingRegistered = true;
        } catch (RuntimeException exception) {
            if (logger != null) {
                logger.warn("Failed to register place title payload: " + exception.getMessage());
            }
        }
    }

    static boolean networkingRegistered() {
        return networkingRegistered;
    }

    static void tick(Object serverObject, Logger logger) {
        if (!(serverObject instanceof MinecraftServer server)) {
            return;
        }
        LivingLegendsConfig.TitleOverlay config = WorldRemembersLivingLegends.config().titleOverlay;
        if (config == null) {
            return;
        }
        boolean shouldSendTitles = config.enabled && config.showOnEnter;
        boolean shouldTrackDiscovery = shouldTrackNaturalDiscovery();
        if (!shouldSendTitles && !shouldTrackDiscovery) {
            return;
        }

        tickCounter++;
        long startNanos = System.nanoTime();
        int interval = Math.max(1, config.checkIntervalTicks);
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        if (players.isEmpty()) {
            return;
        }
        rebuildIndexIfNeeded(server, logger);

        for (ServerPlayerEntity player : players) {
            int offset = Math.floorMod(player.getUuid().hashCode(), interval);
            if (Math.floorMod(tickCounter + offset, interval) != 0) {
                continue;
            }
            checkPlayer(player, config, shouldSendTitles, shouldTrackDiscovery, logger);
        }
        logPerf(logger, "title_natural_discovery_tick", players.size(), startNanos, "server_tick");
    }

    static void invalidateIndex() {
        indexDirty = true;
    }

    static void onPlaceCreated(Object serverObject, NamedPlace place, Logger logger) {
        invalidateIndex();
        LivingLegendsConfig.TitleOverlay config = WorldRemembersLivingLegends.config().titleOverlay;
        if (!(serverObject instanceof MinecraftServer server) || place == null) {
            return;
        }

        rebuildIndexIfNeeded(server, logger);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerWorld world = player.getServerWorld();
            String dimensionId = dimensionId(world);
            if (!place.dimensionId().equals(dimensionId)) {
                continue;
            }
            BlockPos pos = player.getBlockPos();
            if (insideEnter(place, pos.getX(), pos.getY(), pos.getZ(), config)) {
                WorldRemembersLivingLegendsFabricStorage.recordJournalDiscovery(
                        world,
                        player.getUuidAsString(),
                        place.placeIdString(),
                        logger
                );
                if (config != null && config.enabled && config.showOnPlaceCreated) {
                    sendPlaceTitle(player, place, PlaceTitleS2CPayload.Reason.CREATED, true, logger);
                }
            }
        }
    }

    static String sendNearestDebug(ServerPlayerEntity player, Logger logger) {
        if (player == null) {
            return "World Remembers title debug failed: no player";
        }
        NamedPlace nearest = nearestPlace(player, logger);
        if (nearest == null) {
            return "World Remembers title debug: no nearest NamedPlace in this dimension";
        }
        boolean sent = sendPlaceTitle(player, nearest, PlaceTitleS2CPayload.Reason.DEBUG, true, logger);
        return "World Remembers title nearest id=" + nearest.placeIdString()
                + " type=" + nearest.placeType().name()
                + " sent=" + sent;
    }

    static String sendLiteralTest(ServerPlayerEntity player, PlaceType placeType, String styleId, String name, Logger logger) {
        if (player == null) {
            return "World Remembers title test failed: no player";
        }
        String title = name == null || name.isBlank() ? "Test Place" : name.trim();
        BlockPos pos = player.getBlockPos();
        PlaceTitleS2CPayload payload = new PlaceTitleS2CPayload(
                "debug:title_test",
                (placeType == null ? PlaceType.CUSTOM : placeType).idString(),
                styleId == null || styleId.isBlank() ? "vanilla_adventure" : styleId,
                true,
                title,
                NameRecipe.empty(),
                title,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                0,
                PlaceTitleS2CPayload.Reason.DEBUG,
                PlaceVisualThemeResolver.resolve(placeType == null ? PlaceType.CUSTOM : placeType, dimensionId(player.getServerWorld()), "", "")
        );
        boolean sent = sendPayload(player, payload, logger);
        return "World Remembers title test sent=" + sent
                + " style=" + payload.nameStyle()
                + " type=" + payload.placeType()
                + " title=\"" + title + "\"";
    }

    static String clear(ServerPlayerEntity player, Logger logger) {
        if (player == null) {
            return "World Remembers title clear failed: no player";
        }
        boolean sent = sendPayload(player, PlaceTitleS2CPayload.clear(), logger);
        return "World Remembers title clear sent=" + sent;
    }

    static String debugHere(ServerPlayerEntity player, Logger logger) {
        if (player == null) {
            return "World Remembers title debug failed: no player";
        }
        LivingLegendsConfig.TitleOverlay config = WorldRemembersLivingLegends.config().titleOverlay;
        rebuildIndexIfNeeded(player.getServer(), logger);
        ServerWorld world = player.getServerWorld();
        BlockPos pos = player.getBlockPos();
        String dimensionId = dimensionId(world);
        List<NamedPlace> nearby = INDEX.query(dimensionId, pos.getX(), pos.getZ());
        List<SelectionCandidate> containing = containingPlaces(nearby, pos.getX(), pos.getY(), pos.getZ(), false, config);
        SelectionCandidate selected = selectBest(containing, config);
        PlayerTitleState state = PLAYER_STATES.get(player.getUuid());
        StringBuilder message = new StringBuilder("World Remembers title here")
                .append(" pos=").append(pos.getX()).append(',').append(pos.getY()).append(',').append(pos.getZ())
                .append(" dimension=").append(dimensionId)
                .append(" containing=").append(containing.size())
                .append(" selected=").append(selected == null ? "none" : selected.place().placeIdString())
                .append(" priority=").append(selected == null ? "-" : titlePriority(selected.place().placeType()))
                .append(" distance=").append(selected == null ? "-" : formatDistance(Math.sqrt(selected.distanceSquared())));
        if (state != null) {
            long now = world.getTime();
            message.append("\ncurrentInside=").append(state.currentInsidePlaceId == null ? "none" : state.currentInsidePlaceId)
                    .append(" lastShown=").append(state.lastShownPlaceId == null ? "none" : state.lastShownPlaceId)
                    .append(" globalCooldownRemaining=").append(remaining(now, state.lastGlobalTitleGameTime, config.globalCooldownTicks))
                    .append(" teleportDelayRemaining=").append(remaining(now, state.lastTeleportOrDimensionChangeGameTime, config.teleportDelayTicks));
            if (selected != null) {
                long cooldown = selected.place().placeType() == PlaceType.GENERAL_LANDMARK
                        ? config.generalLandmarkCooldownTicks
                        : config.samePlaceCooldownTicks;
                long last = state.lastShownGameTimeByPlaceId.getOrDefault(selected.place().placeIdString(), Long.MIN_VALUE / 4);
                message.append("\nselectedCooldownRemaining=").append(remaining(now, last, cooldown));
            }
        }
        for (int index = 0; index < Math.min(8, containing.size()); index++) {
            SelectionCandidate candidate = containing.get(index);
            message.append("\n- ").append(candidate.place().placeIdString())
                    .append(" type=").append(candidate.place().placeType().name())
                    .append(" distance=").append(formatDistance(Math.sqrt(candidate.distanceSquared())))
                    .append(" score=").append(formatScore(candidate.place().score()));
        }
        return message.toString();
    }

    static String debugState(ServerPlayerEntity player) {
        if (player == null) {
            return "World Remembers title state failed: no player";
        }
        PlayerTitleState state = PLAYER_STATES.get(player.getUuid());
        if (state == null) {
            return "World Remembers title state: empty";
        }
        return "World Remembers title state currentInside="
                + (state.currentInsidePlaceId == null ? "none" : state.currentInsidePlaceId)
                + " lastShown=" + (state.lastShownPlaceId == null ? "none" : state.lastShownPlaceId)
                + " trackedCooldowns=" + state.lastShownGameTimeByPlaceId.size()
                + " dimension=" + (state.dimensionId == null ? "unknown" : state.dimensionId);
    }

    private static void checkPlayer(
            ServerPlayerEntity player,
            LivingLegendsConfig.TitleOverlay config,
            boolean shouldSendTitles,
            boolean shouldTrackDiscovery,
            Logger logger
    ) {
        ServerWorld world = player.getServerWorld();
        long now = world.getTime();
        String dimensionId = dimensionId(world);
        BlockPos pos = player.getBlockPos();
        PlayerTitleState state = PLAYER_STATES.computeIfAbsent(player.getUuid(), ignored -> new PlayerTitleState());
        updateMovementState(state, dimensionId, pos, now);

        List<NamedPlace> nearby = INDEX.query(dimensionId, pos.getX(), pos.getZ());
        List<SelectionCandidate> containing = containingPlaces(nearby, pos.getX(), pos.getY(), pos.getZ(), false, config);
        SelectionCandidate selected = selectBest(containing, config);

        if (selected == null) {
            if (state.currentInsidePlaceId != null) {
                NamedPlace current = findPlace(nearby, state.currentInsidePlaceId);
                if (current == null || !insideExit(current, pos.getX(), pos.getY(), pos.getZ(), config)) {
                    state.currentInsidePlaceId = null;
                    state.currentInsideDiscoverySynced = false;
                }
            }
            return;
        }

        String selectedId = selected.place().placeIdString();
        if (selectedId.equals(state.currentInsidePlaceId)) {
            if (shouldTrackDiscovery && !state.currentInsideDiscoverySynced) {
                markDiscovered(player, selected.place(), logger);
                state.currentInsideDiscoverySynced = true;
            }
            return;
        }
        state.currentInsidePlaceId = selectedId;
        state.currentInsideDiscoverySynced = false;

        if (shouldTrackDiscovery) {
            markDiscovered(player, selected.place(), logger);
            state.currentInsideDiscoverySynced = true;
        }
        if (!shouldSendTitles) {
            return;
        }
        if (now - state.lastTeleportOrDimensionChangeGameTime < config.teleportDelayTicks) {
            return;
        }
        if (now - state.lastGlobalTitleGameTime < config.globalCooldownTicks) {
            return;
        }
        long sameCooldown = selected.place().placeType() == PlaceType.GENERAL_LANDMARK
                ? config.generalLandmarkCooldownTicks
                : config.samePlaceCooldownTicks;
        long lastShown = state.lastShownGameTimeByPlaceId.getOrDefault(selectedId, Long.MIN_VALUE / 4);
        if (now - lastShown < sameCooldown) {
            return;
        }

        sendPlaceTitle(player, selected.place(), PlaceTitleS2CPayload.Reason.ENTERED, true, logger);
    }

    private static void updateMovementState(PlayerTitleState state, String dimensionId, BlockPos pos, long now) {
        if (state.dimensionId == null || !state.dimensionId.equals(dimensionId)) {
            state.dimensionId = dimensionId;
            state.currentInsidePlaceId = null;
            state.currentInsideDiscoverySynced = false;
            state.lastTeleportOrDimensionChangeGameTime = now;
        } else if (state.hasLastPosition) {
            long dx = (long) pos.getX() - state.lastX;
            long dy = (long) pos.getY() - state.lastY;
            long dz = (long) pos.getZ() - state.lastZ;
            if (dx * dx + dy * dy + dz * dz > 128L * 128L) {
                state.currentInsidePlaceId = null;
                state.currentInsideDiscoverySynced = false;
                state.lastTeleportOrDimensionChangeGameTime = now;
            }
        }
        state.lastX = pos.getX();
        state.lastY = pos.getY();
        state.lastZ = pos.getZ();
        state.hasLastPosition = true;
    }

    private static boolean sendPlaceTitle(
            ServerPlayerEntity player,
            NamedPlace place,
            PlaceTitleS2CPayload.Reason reason,
            boolean markCooldown,
            Logger logger
    ) {
        if (player == null || place == null) {
            return false;
        }
        String fallback = place.manuallyRenamed()
                ? place.manualName()
                : WorldRemembersLivingLegendsFabricNameResolver.resolveToString(place.nameRecipe());
        PlaceTitleS2CPayload payload = new PlaceTitleS2CPayload(
                place.placeIdString(),
                place.placeType().idString(),
                place.nameRecipe().styleId(),
                place.manuallyRenamed(),
                place.manualName(),
                place.nameRecipe(),
                fallback,
                place.center().x(),
                place.center().y(),
                place.center().z(),
                place.radius(),
                reason,
                PlaceVisualThemeResolver.fromPlace(place)
        );
        boolean sent = sendPayload(player, payload, logger);
        if (sent && markCooldown) {
            markShown(player, place, player.getServerWorld().getTime(), logger);
        }
        return sent;
    }

    private static boolean sendPayload(ServerPlayerEntity player, PlaceTitleS2CPayload payload, Logger logger) {
        try {
            if (!ServerPlayNetworking.canSend(player, PlaceTitleS2CPayload.ID)) {
                return false;
            }
            ServerPlayNetworking.send(player, payload);
            return true;
        } catch (RuntimeException exception) {
            if (logger != null && WorldRemembersLivingLegends.config().debug.enabled) {
                logger.warn("Could not send place title payload: " + exception.getMessage());
            }
            return false;
        }
    }

    private static void markShown(ServerPlayerEntity player, NamedPlace place, long gameTime, Logger logger) {
        PlayerTitleState state = PLAYER_STATES.computeIfAbsent(player.getUuid(), ignored -> new PlayerTitleState());
        state.currentInsidePlaceId = place.placeIdString();
        state.lastShownPlaceId = place.placeIdString();
        state.lastGlobalTitleGameTime = gameTime;
        state.lastShownGameTimeByPlaceId.put(place.placeIdString(), gameTime);
        WorldRemembersLivingLegendsFabricStorage.recordJournalDiscovery(
                player.getServerWorld(),
                player.getUuidAsString(),
                place.placeIdString(),
                logger
        );
    }

    private static void markDiscovered(ServerPlayerEntity player, NamedPlace place, Logger logger) {
        if (player == null || place == null) {
            return;
        }
        WorldRemembersLivingLegendsFabricStorage.recordJournalDiscovery(
                player.getServerWorld(),
                player.getUuidAsString(),
                place.placeIdString(),
                logger
        );
    }

    private static boolean shouldTrackNaturalDiscovery() {
        LivingLegendsConfig config = WorldRemembersLivingLegends.config();
        if (config == null) {
            return false;
        }
        boolean journalVisited = config.journal != null
                && config.journal.enabled
                && config.journal.visibilityMode() == LivingLegendsConfig.Journal.VisibilityMode.VISITED_BY_PLAYER;
        boolean mapLabels = config.mapIntegration != null
                && config.mapIntegration.enabled
                && config.mapIntegration.journeyMap != null
                && config.mapIntegration.journeyMap.enabled
                && config.mapIntegration.placeLabels != null
                && config.mapIntegration.placeLabels.enabled;
        return journalVisited || mapLabels;
    }

    private static void rebuildIndexIfNeeded(MinecraftServer server, Logger logger) {
        if (server == null || !indexDirty) {
            return;
        }
        Object overworld = server.getOverworld();
        List<NamedPlace> places = WorldRemembersLivingLegendsFabricStorage.places(overworld, logger).stream()
                .filter(place -> place != null && WorldRemembersLivingLegends.config().placeTypes.shouldDisplayExisting(place.placeType()))
                .toList();
        INDEX.rebuild(places, WorldRemembersLivingLegends.config().titleOverlay);
        indexDirty = false;
    }

    private static NamedPlace nearestPlace(ServerPlayerEntity player, Logger logger) {
        ServerWorld world = player.getServerWorld();
        String dimensionId = dimensionId(world);
        BlockPos pos = player.getBlockPos();
        WorldPos playerPos = new WorldPos(dimensionId, pos.getX(), pos.getY(), pos.getZ());
        return WorldRemembersLivingLegendsFabricStorage.places(world, logger).stream()
                .filter(place -> place != null && dimensionId.equals(place.dimensionId()))
                .filter(place -> WorldRemembersLivingLegends.config().placeTypes.shouldDisplayExisting(place.placeType()))
                .min(Comparator.comparingLong(place -> place.center().squaredDistanceTo(playerPos)))
                .orElse(null);
    }

    private static List<SelectionCandidate> containingPlaces(
            List<NamedPlace> places,
            int x,
            int y,
            int z,
            boolean useExitRadius,
            LivingLegendsConfig.TitleOverlay config
    ) {
        List<SelectionCandidate> result = new ArrayList<>();
        for (NamedPlace place : places) {
            if (place == null || place.placeType() == PlaceType.UNKNOWN || place.placeType() == PlaceType.CAMP) {
                continue;
            }
            if (place.placeType() == PlaceType.GENERAL_LANDMARK
                    && config != null
                    && !config.showGeneralLandmarks) {
                continue;
            }
            if (!(useExitRadius ? insideExit(place, x, y, z, config) : insideEnter(place, x, y, z, config))) {
                continue;
            }
            result.add(new SelectionCandidate(place, distanceSquared(place, x, y, z, distanceMode(place))));
        }
        return result;
    }

    private static SelectionCandidate selectBest(List<SelectionCandidate> candidates, LivingLegendsConfig.TitleOverlay config) {
        if (candidates.isEmpty()) {
            return null;
        }
        SelectionCandidate selected = candidates.stream()
                .min(Comparator
                        .comparingInt((SelectionCandidate candidate) -> titlePriority(candidate.place().placeType()))
                        .thenComparingLong(candidate -> candidate.distanceSquared)
                        .thenComparing((SelectionCandidate candidate) -> candidate.place().score(), Comparator.reverseOrder()))
                .orElse(null);
        if (selected != null
                && selected.place().placeType() == PlaceType.GENERAL_LANDMARK
                && config != null
                && config.generalLandmarkOnlyIfNoHigherPriority) {
            boolean higherPriorityPresent = candidates.stream()
                    .anyMatch(candidate -> candidate.place().placeType() != PlaceType.GENERAL_LANDMARK);
            if (higherPriorityPresent) {
                return null;
            }
        }
        return selected;
    }

    private static boolean insideEnter(NamedPlace place, int x, int y, int z, LivingLegendsConfig.TitleOverlay config) {
        return inside(place, x, y, z, titleRadius(place, config), config);
    }

    private static boolean insideExit(NamedPlace place, int x, int y, int z, LivingLegendsConfig.TitleOverlay config) {
        int padding = config == null ? 8 : config.exitPaddingBlocks;
        return inside(place, x, y, z, titleRadius(place, config) + Math.max(0, padding), config);
    }

    private static boolean inside(NamedPlace place, int x, int y, int z, int radius, LivingLegendsConfig.TitleOverlay config) {
        PlaceTitleDistanceMode mode = distanceMode(place);
        if (mode == PlaceTitleDistanceMode.HORIZONTAL_WITH_Y_TOLERANCE) {
            int tolerance = Math.max(0, config == null ? 48 : config.verticalToleranceBlocks);
            if (Math.abs(y - place.center().y()) > tolerance) {
                return false;
            }
            long dx = (long) x - place.center().x();
            long dz = (long) z - place.center().z();
            return dx * dx + dz * dz <= (long) radius * radius;
        }
        return distanceSquared(place, x, y, z, mode) <= (long) radius * radius;
    }

    private static long distanceSquared(NamedPlace place, int x, int y, int z, PlaceTitleDistanceMode mode) {
        long dx = (long) x - place.center().x();
        long dz = (long) z - place.center().z();
        if (mode == PlaceTitleDistanceMode.HORIZONTAL_WITH_Y_TOLERANCE) {
            return dx * dx + dz * dz;
        }
        long dy = (long) y - place.center().y();
        return dx * dx + dy * dy + dz * dz;
    }

    private static int titleRadius(NamedPlace place, LivingLegendsConfig.TitleOverlay config) {
        int minRadius = config == null ? 24 : config.minTitleRadius;
        return Math.max(Math.max(1, place.radius()), Math.max(1, minRadius));
    }

    private static PlaceTitleDistanceMode distanceMode(NamedPlace place) {
        PlaceType type = place.placeType();
        if (type == PlaceType.DEATH_SITE) {
            DeathSiteEnvironment environment = place.environment();
            if (environment == DeathSiteEnvironment.CAVE || environment == DeathSiteEnvironment.WATER) {
                return PlaceTitleDistanceMode.THREE_D;
            }
            return PlaceTitleDistanceMode.HORIZONTAL_WITH_Y_TOLERANCE;
        }
        return switch (type) {
            case MINING_SITE, PORTAL_LANDMARK, DIMENSION_THRESHOLD, FIRST_DISCOVERY, BOSS_SITE,
                    PET_MEMORIAL, NAMED_MOB_MEMORIAL -> PlaceTitleDistanceMode.THREE_D;
            default -> PlaceTitleDistanceMode.HORIZONTAL_WITH_Y_TOLERANCE;
        };
    }

    private static int titlePriority(PlaceType type) {
        return switch (type == null ? PlaceType.UNKNOWN : type) {
            case FIRST_DISCOVERY -> 1;
            case BOSS_SITE -> 2;
            case PET_MEMORIAL -> 3;
            case NAMED_MOB_MEMORIAL -> 4;
            case RAID_SITE -> 5;
            case PORTAL_LANDMARK -> 6;
            case DIMENSION_THRESHOLD -> 7;
            case DEATH_SITE -> 8;
            case BATTLEFIELD -> 9;
            case PVP_ARENA -> 10;
            case SETTLEMENT -> 11;
            case MINING_SITE -> 12;
            case SLAUGHTER_FIELD -> 13;
            case CUSTOM -> 14;
            case GENERAL_LANDMARK -> 15;
            default -> 50;
        };
    }

    private static NamedPlace findPlace(List<NamedPlace> places, String placeId) {
        if (placeId == null || placeId.isBlank()) {
            return null;
        }
        for (NamedPlace place : places) {
            if (place != null && placeId.equals(place.placeIdString())) {
                return place;
            }
        }
        return null;
    }

    private static String dimensionId(ServerWorld world) {
        return world.getRegistryKey().getValue().toString();
    }

    private static long cellKey(int cellX, int cellZ) {
        return ((long) cellX << 32) ^ (cellZ & 0xffff_ffffL);
    }

    private static int cell(int coordinate) {
        return Math.floorDiv(coordinate, CELL_SIZE);
    }

    private static String formatDistance(double value) {
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 0.000_001) {
            return Long.toString((long) rounded);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String formatScore(double value) {
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 0.000_001) {
            return Long.toString((long) rounded);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static long remaining(long now, long last, long cooldown) {
        if (last <= Long.MIN_VALUE / 8) {
            return 0L;
        }
        return Math.max(0L, cooldown - (now - last));
    }

    private static void logPerf(Logger logger, String phase, int players, long startNanos, String reason) {
        if (logger == null || !WorldRemembersLivingLegends.config().debug.enabled) {
            return;
        }
        long elapsed = System.nanoTime() - startNanos;
        if (elapsed < PERF_LOG_THRESHOLD_NANOS) {
            return;
        }
        logger.info("living_legends_perf phase=" + phase
                + " players=" + players
                + " ms=" + String.format(Locale.ROOT, "%.2f", elapsed / 1_000_000.0)
                + " reason=" + (reason == null || reason.isBlank() ? "unknown" : reason));
    }

    private enum PlaceTitleDistanceMode {
        THREE_D,
        HORIZONTAL_WITH_Y_TOLERANCE
    }

    private record SelectionCandidate(NamedPlace place, long distanceSquared) {
    }

    private static final class PlayerTitleState {
        private String currentInsidePlaceId;
        private boolean currentInsideDiscoverySynced;
        private String lastShownPlaceId;
        private String dimensionId;
        private long lastGlobalTitleGameTime = Long.MIN_VALUE / 4;
        private long lastTeleportOrDimensionChangeGameTime = Long.MIN_VALUE / 4;
        private final Map<String, Long> lastShownGameTimeByPlaceId = new LinkedHashMap<>();
        private boolean hasLastPosition;
        private int lastX;
        private int lastY;
        private int lastZ;
    }

    private static final class SpatialIndex {
        private final Map<String, Map<Long, List<NamedPlace>>> byDimension = new HashMap<>();
        private final Map<String, Integer> maxRadiusByDimension = new HashMap<>();

        private void rebuild(List<NamedPlace> places, LivingLegendsConfig.TitleOverlay config) {
            byDimension.clear();
            maxRadiusByDimension.clear();
            for (NamedPlace place : places) {
                int radius = titleRadius(place, config) + Math.max(0, config == null ? 8 : config.exitPaddingBlocks);
                int minCellX = cell(place.center().x() - radius);
                int maxCellX = cell(place.center().x() + radius);
                int minCellZ = cell(place.center().z() - radius);
                int maxCellZ = cell(place.center().z() + radius);
                Map<Long, List<NamedPlace>> cells = byDimension.computeIfAbsent(place.dimensionId(), ignored -> new HashMap<>());
                maxRadiusByDimension.merge(place.dimensionId(), radius, Math::max);
                for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
                    for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                        cells.computeIfAbsent(cellKey(cellX, cellZ), ignored -> new ArrayList<>()).add(place);
                    }
                }
            }
        }

        private List<NamedPlace> query(String dimensionId, int x, int z) {
            Map<Long, List<NamedPlace>> cells = byDimension.get(dimensionId);
            if (cells == null || cells.isEmpty()) {
                return List.of();
            }
            int range = Math.max(1, (int) Math.ceil(maxRadiusByDimension.getOrDefault(dimensionId, CELL_SIZE) / (double) CELL_SIZE) + 1);
            int centerCellX = cell(x);
            int centerCellZ = cell(z);
            Set<String> seen = new HashSet<>();
            List<NamedPlace> result = new ArrayList<>();
            for (int cellX = centerCellX - range; cellX <= centerCellX + range; cellX++) {
                for (int cellZ = centerCellZ - range; cellZ <= centerCellZ + range; cellZ++) {
                    List<NamedPlace> bucket = cells.get(cellKey(cellX, cellZ));
                    if (bucket == null) {
                        continue;
                    }
                    for (NamedPlace place : bucket) {
                        if (seen.add(place.placeIdString())) {
                            result.add(place);
                        }
                    }
                }
            }
            return result;
        }
    }
}
