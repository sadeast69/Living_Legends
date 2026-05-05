package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.PlaceType;
import com.worldremembers.livinglegends.map.MapDestinationDescriptor;
import com.worldremembers.livinglegends.map.MapIntegrationSettings;
import com.worldremembers.livinglegends.map.MapPlaceDescriptor;
import com.worldremembers.livinglegends.map.PlaceMapIntegration;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.waypoint.Waypoint;
import dev.ftb.mods.ftbchunks.api.client.waypoint.WaypointManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class FtbChunksFabricIntegration implements PlaceMapIntegration {
    private static final String PROVIDER_ID = "ftbchunks";

    private final boolean chunksInstalled;
    private final Logger logger;
    private final Map<String, TrackedWaypoint> destinations = new LinkedHashMap<>();
    private final Set<String> loggedMessages = new LinkedHashSet<>();

    FtbChunksFabricIntegration(Logger logger) {
        this.logger = logger;
        this.chunksInstalled = detectFtbChunks();
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean available() {
        return chunksInstalled;
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
        return currentWaypointManager().isPresent();
    }

    @Override
    public void replacePlaceLabels(Collection<MapPlaceDescriptor> places, MapIntegrationSettings settings) {
        // FTB Chunks compatibility is destination-only. JourneyMap owns permanent fantasy labels.
    }

    @Override
    public void clearPlaceLabels() {
        // No-op: do not create, rebuild, or delete generated FTB Chunks label waypoints.
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
        String dimensionId = dimensionId(destination.dimensionId());
        Optional<WaypointManager> manager = waypointManager(dimensionId);
        if (manager.isEmpty()) {
            warnFailure(destination.placeId(), "waypoint_manager_not_ready");
            return DestinationUpdateResult.PROVIDER_NOT_READY;
        }

        BlockPos pos = new BlockPos(destination.centerX(), destination.centerY(), destination.centerZ());
        String name = destinationName(destination);
        try {
            removeOwnedAt(manager.get(), pos, name);
            if (hasWaypointAt(manager.get(), pos)) {
                warnFailure(destination.placeId(), "waypoint_position_occupied");
                return DestinationUpdateResult.WAYPOINT_CREATE_FAILED;
            }

            Waypoint waypoint = manager.get()
                    .addTransientWaypointAt(pos, name)
                    .setColor(typeColor(destination.placeType()))
                    .setHidden(false)
                    .setTransient(true);
            if (!isOwnedWaypoint(waypoint, pos, name) || findOwnedWaypoint(manager.get(), pos, name).isEmpty()) {
                warnFailure(destination.placeId(), "waypoint_create_failed");
                return DestinationUpdateResult.WAYPOINT_CREATE_FAILED;
            }

            destinations.put(destination.placeId(), new TrackedWaypoint(dimensionId, pos, name));
            return DestinationUpdateResult.SUCCESS;
        } catch (RuntimeException | LinkageError exception) {
            destinations.remove(destination.placeId());
            warnFailure(destination.placeId(), "waypoint_api_failed:" + exception.getClass().getSimpleName());
            return DestinationUpdateResult.WAYPOINT_CREATE_FAILED;
        }
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
        Optional<WaypointManager> manager = waypointManager(tracked.dimensionId());
        if (manager.isEmpty()) {
            warnOnce("remove_not_ready|" + placeId,
                    "Could not remove FTB Chunks destination for " + placeId
                            + ": waypoint manager is not ready.");
            return false;
        }
        try {
            if (removeTracked(manager.get(), tracked)) {
                destinations.remove(placeId);
                return true;
            }
            if (findOwnedWaypoint(manager.get(), tracked.pos(), tracked.name()).isEmpty()) {
                destinations.remove(placeId);
                return true;
            }
        } catch (RuntimeException | LinkageError exception) {
            warnOnce("remove_exception|" + placeId + "|" + exception.getClass().getSimpleName(),
                    "Could not remove FTB Chunks destination for " + placeId
                            + ": " + exception.getClass().getSimpleName() + ".");
            return false;
        }
        warnOnce("remove_failed|" + placeId,
                "Could not remove FTB Chunks destination for " + placeId + ".");
        return false;
    }

    @Override
    public void clearDestinations() {
        Map<String, TrackedWaypoint> removed = new LinkedHashMap<>(destinations);
        destinations.clear();
        for (Map.Entry<String, TrackedWaypoint> entry : removed.entrySet()) {
            waypointManager(entry.getValue().dimensionId()).ifPresent(manager -> {
                try {
                    removeTracked(manager, entry.getValue());
                } catch (RuntimeException | LinkageError ignored) {
                }
            });
        }
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
        Optional<WaypointManager> manager = waypointManager(tracked.dimensionId());
        if (manager.isEmpty()) {
            return true;
        }
        try {
            if (findOwnedWaypoint(manager.get(), tracked.pos(), tracked.name()).isPresent()) {
                return true;
            }
        } catch (RuntimeException | LinkageError exception) {
            warnOnce("has_destination_exception|" + placeId + "|" + exception.getClass().getSimpleName(),
                    "Could not check FTB Chunks destination for " + placeId
                            + ": " + exception.getClass().getSimpleName() + ".");
            return true;
        }
        destinations.remove(placeId);
        return false;
    }

    private static boolean detectFtbChunks() {
        try {
            return FabricLoader.getInstance().isModLoaded(PROVIDER_ID);
        } catch (RuntimeException | LinkageError exception) {
            return false;
        }
    }

    private Optional<WaypointManager> currentWaypointManager() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) {
            return Optional.empty();
        }
        return waypointManager(client.world.getRegistryKey());
    }

    private Optional<WaypointManager> waypointManager(String dimensionId) {
        return waypointManager(dimension(dimensionId));
    }

    private static Optional<WaypointManager> waypointManager(RegistryKey<World> dimension) {
        try {
            return FTBChunksAPI.clientApi().getWaypointManager(dimension);
        } catch (RuntimeException | LinkageError exception) {
            return Optional.empty();
        }
    }

    private static boolean removeTracked(WaypointManager manager, TrackedWaypoint tracked) {
        if (manager == null || tracked == null) {
            return true;
        }
        Optional<Waypoint> waypoint = findOwnedWaypoint(manager, tracked.pos(), tracked.name());
        return waypoint.isEmpty() || manager.removeWaypoint(waypoint.get());
    }

    private static void removeOwnedAt(WaypointManager manager, BlockPos pos, String name) {
        for (Waypoint waypoint : new ArrayList<>(manager.getAllWaypoints())) {
            if (isOwnedWaypoint(waypoint, pos, name)) {
                manager.removeWaypoint(waypoint);
            }
        }
    }

    private static Optional<Waypoint> findOwnedWaypoint(WaypointManager manager, BlockPos pos, String name) {
        if (manager == null) {
            return Optional.empty();
        }
        for (Waypoint waypoint : manager.getAllWaypoints()) {
            if (isOwnedWaypoint(waypoint, pos, name)) {
                return Optional.of(waypoint);
            }
        }
        return Optional.empty();
    }

    private static boolean hasWaypointAt(WaypointManager manager, BlockPos pos) {
        if (manager == null || pos == null) {
            return false;
        }
        for (Waypoint waypoint : manager.getAllWaypoints()) {
            if (waypoint != null && pos.equals(waypoint.getPos())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOwnedWaypoint(Waypoint waypoint, BlockPos pos, String name) {
        if (waypoint == null || pos == null || name == null || name.isBlank()) {
            return false;
        }
        return pos.equals(waypoint.getPos())
                && name.equals(waypoint.getName())
                && waypoint.isTransient()
                && !waypoint.isDeathpoint();
    }

    private void warnFailure(String placeId, String reason) {
        warnOnce(
                "create_failed|" + placeId + "|" + reason,
                "Could not create FTB Chunks destination for " + placeId + ": " + reason
                        + ". Open FTB Chunks' map once and try again."
        );
    }

    private void warnOnce(String key, String message) {
        if (logger != null && loggedMessages.add(key)) {
            if (loggedMessages.size() > 64) {
                var iterator = loggedMessages.iterator();
                if (iterator.hasNext()) {
                    iterator.next();
                    iterator.remove();
                }
            }
            logger.warn(message);
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

    private record TrackedWaypoint(String dimensionId, BlockPos pos, String name) {
        private TrackedWaypoint {
            dimensionId = FtbChunksFabricIntegration.dimensionId(dimensionId);
            pos = pos == null ? BlockPos.ORIGIN : pos.toImmutable();
            name = name == null ? "" : name.trim();
        }
    }
}
