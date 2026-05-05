package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.PlaceType;
import com.worldremembers.livinglegends.map.MapDestinationDescriptor;
import com.worldremembers.livinglegends.map.MapIntegrationSettings;
import com.worldremembers.livinglegends.map.MapPlaceDescriptor;
import com.worldremembers.livinglegends.map.PlaceMapIntegration;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointVisibilityType;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.world.MinimapWorldManager;
import xaero.hud.path.XaeroPath;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;

final class XaeroFabricIntegration implements PlaceMapIntegration {
    private static final String PROVIDER_ID = "xaero";
    private static final String DESTINATION_SET_NAME = "World Remembers Destinations";
    private static final String DEFAULT_SET_NAME = "gui.xaero_default";
    private static final int[] XAERO_COLORS = {
            0x000000,
            0x0000AA,
            0x00AA00,
            0x00AAAA,
            0xAA0000,
            0xAA00AA,
            0xFFAA00,
            0xAAAAAA,
            0x555555,
            0x5555FF,
            0x55FF55,
            0x55FFFF,
            0xFF5555,
            0xFF55FF,
            0xFFFF55,
            0xFFFFFF
    };

    private final boolean minimapInstalled;
    private final Logger logger;
    private final Map<String, TrackedWaypoint> destinations = new LinkedHashMap<>();
    private final Set<String> loggedMessages = new LinkedHashSet<>();

    XaeroFabricIntegration(Logger logger) {
        this.logger = logger;
        this.minimapInstalled = detectXaeroMinimap();
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean available() {
        return minimapInstalled;
    }

    @Override
    public boolean supportsPlaceLabels() {
        return false;
    }

    @Override
    public boolean supportsDestinations() {
        return true;
    }

    @Override
    public boolean destinationRuntimeReady() {
        return currentSession() != null;
    }

    boolean sessionReady() {
        return currentSession() != null;
    }

    @Override
    public void replacePlaceLabels(Collection<MapPlaceDescriptor> places, MapIntegrationSettings settings) {
        // Xaero compatibility is destination-only. JourneyMap owns permanent fantasy labels.
    }

    @Override
    public void clearPlaceLabels() {
        // No-op: do not create, rebuild, or delete generated Xaero label waypoint sets.
    }

    @Override
    public void addOrUpdateDestination(MapDestinationDescriptor destination, MapIntegrationSettings settings) {
        addOrUpdateDestinationResult(destination, settings);
    }

    @Override
    public DestinationUpdateResult addOrUpdateDestinationResult(
            MapDestinationDescriptor destination,
            MapIntegrationSettings settings
    ) {
        if (!available()) {
            return DestinationUpdateResult.PROVIDER_UNAVAILABLE;
        }
        if (settings == null || !settings.destinationsEnabledFor(PROVIDER_ID)) {
            clearDestinations();
            return DestinationUpdateResult.DESTINATIONS_DISABLED;
        }
        if (destination == null || destination.placeId().isBlank()) {
            return DestinationUpdateResult.INVALID_DESTINATION;
        }
        removeDestinationResult(destination.placeId());
        MinimapSession session = currentSession();
        if (session == null) {
            warnFailure(destination.placeId(), "session_not_ready");
            return DestinationUpdateResult.PROVIDER_NOT_READY;
        }
        String dimensionId = dimensionId(destination.dimensionId());
        MinimapWorld world = minimapWorld(session, dimensionId);
        if (world == null) {
            warnFailure(destination.placeId(), "world_unavailable");
            return DestinationUpdateResult.PROVIDER_NOT_READY;
        }
        try {
            Waypoint waypoint = createDestinationWaypoint(destination);
            WaypointSet set = getOrCreateSet(world, DESTINATION_SET_NAME);
            if (set == null) {
                warnFailure(destination.placeId(), "waypoint_set_unavailable");
                return DestinationUpdateResult.WAYPOINT_SET_UNAVAILABLE;
            }
            world.setCurrentWaypointSetId(set.getName());
            set.add(waypoint);
            ensureCurrentWaypointSet(world, "after_add");
            destinations.put(destination.placeId(), new TrackedWaypoint(dimensionId, waypoint.getName()));
            saveWorld(session, world);
            return DestinationUpdateResult.SUCCESS;
        } catch (RuntimeException | LinkageError exception) {
            destinations.remove(destination.placeId());
            warnFailure(destination.placeId(), "waypoint_create_failed:" + exception.getClass().getSimpleName());
            return DestinationUpdateResult.WAYPOINT_CREATE_FAILED;
        }
    }

    private static boolean detectXaeroMinimap() {
        try {
            return FabricLoader.getInstance().isModLoaded("xaerominimap");
        } catch (RuntimeException | LinkageError exception) {
            return false;
        }
    }

    private void warnOnce(String key, String message) {
        if (logger != null && rememberLog(key)) {
            logger.warn(message);
        }
    }

    private boolean rememberLog(String key) {
        if (loggedMessages.add(key)) {
            if (loggedMessages.size() > 64) {
                var iterator = loggedMessages.iterator();
                if (iterator.hasNext()) {
                    iterator.next();
                    iterator.remove();
                }
            }
            return true;
        }
        return false;
    }

    private void warnFailure(String placeId, String reason) {
        warnOnce(
                "create_failed|" + placeId + "|" + reason,
                "Could not create Xaero destination for " + placeId
                        + ": " + reason + ". Open Xaero's map once and try again."
        );
    }

    @Override
    public void removeDestination(String placeId) {
        removeDestinationResult(placeId);
    }

    @Override
    public boolean removeDestinationResult(String placeId) {
        if (placeId == null || placeId.isBlank()) {
            return true;
        }
        TrackedWaypoint tracked = destinations.get(placeId);
        if (tracked == null) {
            return true;
        }
        boolean removed = removeTrackedName(tracked.dimensionId(), tracked.name(), DESTINATION_SET_NAME, true);
        if (removed) {
            destinations.remove(placeId);
            return true;
        }
        if (!containsTrackedName(tracked.dimensionId(), tracked.name(), DESTINATION_SET_NAME)) {
            destinations.remove(placeId);
            return true;
        }
        warnOnce("remove_failed|" + placeId,
                "Could not remove Xaero destination for " + placeId + ": Xaero is not ready.");
        return false;
    }

    @Override
    public void clearDestinations() {
        destinations.clear();
        clearSetFromAllLoadedWorlds(DESTINATION_SET_NAME, true);
    }

    @Override
    public boolean hasDestination(String placeId) {
        if (!available() || placeId == null || placeId.isBlank()) {
            return false;
        }
        TrackedWaypoint tracked = destinations.get(placeId);
        if (tracked == null) {
            return false;
        }
        if (!containsTrackedName(tracked.dimensionId(), tracked.name(), DESTINATION_SET_NAME)) {
            destinations.remove(placeId);
            return false;
        }
        return true;
    }

    private static Waypoint createDestinationWaypoint(MapDestinationDescriptor destination) {
        String name = destinationName(destination);
        Waypoint waypoint = new Waypoint(
                destination.centerX(),
                destination.centerY(),
                destination.centerZ(),
                name,
                "LL",
                WaypointColor.fromIndex(colorIndex(typeColor(destination.placeType()))),
                WaypointPurpose.NORMAL,
                false,
                true
        );
        waypoint.setVisibility(WaypointVisibilityType.GLOBAL);
        waypoint.setTemporary(false);
        waypoint.setOneoffDestination(false);
        return waypoint;
    }

    private static MinimapSession currentSession() {
        try {
            return BuiltInHudModules.MINIMAP.getCurrentSession();
        } catch (RuntimeException | LinkageError exception) {
            return null;
        }
    }

    private static MinimapWorld minimapWorld(MinimapSession session, String dimensionId) {
        if (session == null) {
            return null;
        }
        try {
            RegistryKey<World> dimension = dimension(dimensionId);
            String dimensionDirectory = session.getDimensionHelper().getDimensionDirectoryName(dimension);
            String node = session.getWorldStateUpdater().getPotentialWorldNode(dimension, false);
            MinimapWorldManager manager = session.getWorldManager();
            if (manager == null || manager.getAutoRootContainer() == null) {
                return null;
            }
            XaeroPath root = manager.getAutoRootContainer().getPath();
            if (root == null || dimensionDirectory == null || dimensionDirectory.isBlank()
                    || node == null || node.isBlank()) {
                return null;
            }
            XaeroPath fullPath = root.resolve(dimensionDirectory).resolve(node);
            MinimapWorld world = manager.getWorld(fullPath);
            return world == null ? manager.addWorld(fullPath) : world;
        } catch (RuntimeException | LinkageError exception) {
            return null;
        }
    }

    private WaypointSet getOrCreateSet(MinimapWorld world, String setName) {
        if (world == null || setName == null || setName.isBlank()) {
            return null;
        }
        WaypointSet set = world.getWaypointSet(setName);
        if (set != null) {
            return set;
        }
        try {
            world.addWaypointSet(setName);
            set = world.getWaypointSet(setName);
            if (set == null) {
                WaypointSet built = WaypointSet.Builder.begin().setName(setName).build();
                if (built != null) {
                    world.addWaypointSet(built);
                    set = world.getWaypointSet(setName);
                }
            }
        } catch (RuntimeException | LinkageError exception) {
            warnOnce("set_create_failed|" + setName + "|" + exception.getClass().getSimpleName(),
                    "Could not create Xaero waypoint set \"" + setName
                            + "\": " + exception.getClass().getSimpleName());
            return null;
        }
        return set;
    }

    private void removeTrackedNames(Map<String, Set<String>> removed, String setName, boolean save) {
        if (removed.isEmpty()) {
            return;
        }
        MinimapSession session = currentSession();
        if (session == null) {
            return;
        }
        for (Map.Entry<String, Set<String>> entry : removed.entrySet()) {
            removeTrackedNames(session, entry.getKey(), entry.getValue(), setName, save);
        }
    }

    private void clearSetFromAllLoadedWorlds(String setName, boolean save) {
        MinimapSession session = currentSession();
        if (session == null) {
            return;
        }
        try {
            if (session.getWorldManager() == null) {
                return;
            }
            var root = session.getWorldManager().getAutoRootContainer();
            if (root == null) {
                return;
            }
            for (MinimapWorld world : root.getAllWorldsIterable()) {
                if (world == null) {
                    continue;
                }
                WaypointSet set = world.getWaypointSet(setName);
                if (set != null) {
                    set.clear();
                }
                ensureCurrentWaypointSet(world, "after_clear");
                if (set != null && save) {
                    saveWorld(session, world);
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    private boolean removeTrackedName(String dimensionId, String name, String setName, boolean save) {
        if (name == null || name.isBlank()) {
            return true;
        }
        MinimapSession session = currentSession();
        if (session == null) {
            return false;
        }
        return removeTrackedNames(session, dimensionId, Set.of(name), setName, save);
    }

    private boolean removeTrackedNames(
            MinimapSession session,
            String dimensionId,
            Set<String> names,
            String setName,
            boolean save
    ) {
        if (names == null || names.isEmpty()) {
            return true;
        }
        MinimapWorld world = minimapWorld(session, dimensionId);
        if (world == null) {
            return false;
        }
        WaypointSet set = world.getWaypointSet(setName);
        if (set == null) {
            ensureCurrentWaypointSet(world, "after_remove");
            return true;
        }
        List<Waypoint> removed = new ArrayList<>();
        for (Waypoint waypoint : set.getWaypoints()) {
            if (waypoint != null && names.contains(waypoint.getName())) {
                removed.add(waypoint);
            }
        }
        if (removed.isEmpty()) {
            ensureCurrentWaypointSet(world, "after_remove");
            return true;
        }
        set.removeAll(removed);
        ensureCurrentWaypointSet(world, "after_remove");
        if (save) {
            saveWorld(session, world);
        }
        return true;
    }

    private WaypointSet ensureCurrentWaypointSet(MinimapWorld world, String phase) {
        if (world == null) {
            return null;
        }
        try {
            WaypointSet current = world.getCurrentWaypointSet();
            if (current != null) {
                return current;
            }
            String currentId = world.getCurrentWaypointSetId();
            if (currentId != null && !currentId.isBlank()) {
                WaypointSet restored = world.getWaypointSet(currentId);
                if (restored == null) {
                    world.addWaypointSet(currentId);
                    restored = world.getWaypointSet(currentId);
                }
                if (restored != null) {
                    world.setCurrentWaypointSetId(restored.getName());
                    return restored;
                }
            }
            for (WaypointSet set : world.getIterableWaypointSets()) {
                if (set != null) {
                    world.setCurrentWaypointSetId(set.getName());
                    return set;
                }
            }
            world.addWaypointSet(DEFAULT_SET_NAME);
            WaypointSet fallback = world.getWaypointSet(DEFAULT_SET_NAME);
            if (fallback == null) {
                fallback = getOrCreateSet(world, DESTINATION_SET_NAME);
            }
            if (fallback != null) {
                world.setCurrentWaypointSetId(fallback.getName());
            }
            return fallback;
        } catch (RuntimeException | LinkageError exception) {
            warnOnce("current_restore_failed|" + phase + "|" + exception.getClass().getSimpleName(),
                    "Could not restore Xaero current waypoint set after " + phase
                            + ": " + exception.getClass().getSimpleName());
            return null;
        }
    }

    private static boolean containsTrackedName(String dimensionId, String name, String setName) {
        MinimapSession session = currentSession();
        if (session == null) {
            return false;
        }
        MinimapWorld world = minimapWorld(session, dimensionId);
        if (world == null) {
            return false;
        }
        WaypointSet set = world.getWaypointSet(setName);
        if (set == null) {
            return false;
        }
        for (Waypoint waypoint : set.getWaypoints()) {
            if (waypoint != null && name.equals(waypoint.getName())) {
                return true;
            }
        }
        return false;
    }

    private static void saveWorld(MinimapSession session, MinimapWorld world) {
        try {
            session.getWorldManagerIO().saveWorld(world);
        } catch (IOException | RuntimeException | LinkageError ignored) {
        }
    }

    private static RegistryKey<World> dimension(String dimensionId) {
        Identifier id = Identifier.tryParse(dimensionId(dimensionId));
        if (id == null) {
            id = World.OVERWORLD.getValue();
        }
        return RegistryKey.of(RegistryKeys.WORLD, id);
    }

    private static String dimensionId(String dimensionId) {
        return dimensionId == null || dimensionId.isBlank() ? "minecraft:overworld" : dimensionId.trim();
    }

    private static String destinationName(MapDestinationDescriptor destination) {
        String displayName = destination == null ? "" : destination.displayName();
        if (displayName == null || displayName.isBlank()) {
            return Text.translatable("living_legends.map.destination_name_fallback").getString();
        }
        return Text.translatable("living_legends.map.destination_name", displayName).getString();
    }

    private static int typeColor(PlaceType type) {
        PlaceType resolved = type == null ? PlaceType.CUSTOM : type;
        return switch (resolved) {
            case DEATH_SITE -> 0xC76042;
            case BATTLEFIELD, PVP_ARENA, RAID_SITE -> 0xB94B38;
            case SLAUGHTER_FIELD -> 0xA98243;
            case PORTAL_LANDMARK, DIMENSION_THRESHOLD -> 0x777DE2;
            case FIRST_DISCOVERY, BOSS_SITE -> 0xC09338;
            case SETTLEMENT -> 0x5A9C58;
            case GENERAL_LANDMARK -> 0x73894D;
            default -> 0xE7C27A;
        };
    }

    private static int colorIndex(int rgb) {
        int clean = rgb & 0x00FF_FFFF;
        int bestIndex = 15;
        long bestDistance = Long.MAX_VALUE;
        for (int index = 0; index < XAERO_COLORS.length; index++) {
            int color = XAERO_COLORS[index];
            int dr = ((clean >> 16) & 0xFF) - ((color >> 16) & 0xFF);
            int dg = ((clean >> 8) & 0xFF) - ((color >> 8) & 0xFF);
            int db = (clean & 0xFF) - (color & 0xFF);
            long distance = (long) dr * dr + (long) dg * dg + (long) db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    private record TrackedWaypoint(String dimensionId, String name) {
        private TrackedWaypoint {
            dimensionId = XaeroFabricIntegration.dimensionId(dimensionId);
            name = name == null ? "" : name.trim();
        }
    }
}
