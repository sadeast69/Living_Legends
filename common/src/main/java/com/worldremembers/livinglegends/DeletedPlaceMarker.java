package com.worldremembers.livinglegends;

import com.worldremembers.livinglegends.config.LivingLegendsConfig;

import java.io.Serializable;
import java.util.Objects;

public record DeletedPlaceMarker(
        String originalPlaceId,
        PlaceType placeType,
        String dimensionId,
        WorldPos center,
        int radius,
        PlaceBounds bounds,
        DeathSiteEnvironment environment,
        String firstDiscoveryKey,
        String structureId,
        PlaceCauseType causeType,
        long deletedGameTime,
        boolean suppressAutoRecreate
) implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final long MINECRAFT_DAY_TICKS = 24_000L;

    public DeletedPlaceMarker {
        originalPlaceId = WorldPos.optionalId(originalPlaceId);
        if (originalPlaceId.isBlank()) {
            throw new IllegalArgumentException("originalPlaceId must not be blank");
        }
        placeType = placeType == null ? PlaceType.UNKNOWN : placeType;
        environment = environment == null ? DeathSiteEnvironment.UNKNOWN : environment;
        dimensionId = WorldPos.requireId(dimensionId, "dimensionId");
        center = center == null ? new WorldPos(dimensionId, 0, 64, 0) : center;
        if (!dimensionId.equals(center.dimensionId())) {
            center = new WorldPos(dimensionId, center.x(), center.y(), center.z());
        }
        radius = Math.max(0, radius);
        bounds = bounds == null ? PlaceBounds.around(center, Math.max(8, radius), Math.max(8, radius)) : bounds;
        firstDiscoveryKey = WorldPos.optionalId(firstDiscoveryKey);
        structureId = WorldPos.optionalId(structureId);
        causeType = causeType == null ? PlaceCauseType.UNKNOWN : causeType;
        deletedGameTime = Math.max(0L, deletedGameTime);
    }

    public static DeletedPlaceMarker fromPlace(NamedPlace place, long deletedGameTime) {
        Objects.requireNonNull(place, "place");
        return new DeletedPlaceMarker(
                place.placeIdString(),
                place.placeType(),
                place.dimensionId(),
                place.center(),
                place.radius(),
                place.bounds(),
                place.environment(),
                place.firstDiscoveryKey(),
                place.structureId(),
                place.cause() == null ? PlaceCauseType.UNKNOWN : place.cause().causeType(),
                deletedGameTime,
                true
        );
    }

    public boolean expired(long currentGameTime, LivingLegendsConfig config) {
        int days = config == null || config.generation == null
                ? -1
                : config.generation.deletedPlaceSuppressionDays;
        if (days < 0) {
            return false;
        }
        if (days == 0) {
            return true;
        }
        long elapsed = Math.max(0L, currentGameTime - deletedGameTime);
        return elapsed >= (long) days * MINECRAFT_DAY_TICKS;
    }

    public boolean suppresses(PlaceCluster cluster, LivingLegendsConfig config, long currentGameTime) {
        if (cluster == null || !suppressAutoRecreate || expired(currentGameTime, config)) {
            return false;
        }
        if (config == null || config.generation == null || !config.generation.deletedPlaceSuppressionEnabled) {
            return false;
        }
        if (config.generation.deletedPlaceSuppressionDays == 0) {
            return false;
        }
        if (placeType != cluster.placeType() || !dimensionId.equals(cluster.dimensionId())) {
            return false;
        }
        if (placeType == PlaceType.DEATH_SITE && environment != cluster.environment()) {
            return false;
        }
        if (!firstDiscoveryKey.isBlank() && !firstDiscoveryKey.equals(cluster.firstDiscoveryKey())) {
            return false;
        }
        if (!structureId.isBlank() && !structureId.equals(cluster.structureId())) {
            return false;
        }
        PlaceCauseType clusterCauseType = cluster.cause() == null ? PlaceCauseType.UNKNOWN : cluster.cause().causeType();
        if (causeType != PlaceCauseType.UNKNOWN && clusterCauseType != PlaceCauseType.UNKNOWN && causeType != clusterCauseType) {
            return false;
        }
        return overlaps(cluster, config);
    }

    public boolean matchesImportedPlace(NamedPlace place, LivingLegendsConfig config) {
        if (place == null || placeType != place.placeType() || !dimensionId.equals(place.dimensionId())) {
            return false;
        }
        if (placeType == PlaceType.DEATH_SITE && environment != place.environment()) {
            return false;
        }
        if (!firstDiscoveryKey.isBlank() && !firstDiscoveryKey.equals(place.firstDiscoveryKey())) {
            return false;
        }
        if (!structureId.isBlank() && !structureId.equals(place.structureId())) {
            return false;
        }
        PlaceCauseType placeCauseType = place.cause() == null ? PlaceCauseType.UNKNOWN : place.cause().causeType();
        if (causeType != PlaceCauseType.UNKNOWN && placeCauseType != PlaceCauseType.UNKNOWN && causeType != placeCauseType) {
            return false;
        }
        int mergeDistance = PlaceGenerationLimits.maxMergeDistanceBlocks(place.placeType(), config);
        int distance = Math.max(Math.max(radius, place.radius()), mergeDistance);
        return center.squaredDistanceTo(place.center()) <= (long) distance * distance
                || bounds.expanded(distance, Math.max(16, distance)).contains(place.center())
                || place.bounds().expanded(distance, Math.max(16, distance)).contains(center);
    }

    private boolean overlaps(PlaceCluster cluster, LivingLegendsConfig config) {
        WorldPos clusterCenter = new WorldPos(cluster.dimensionId(), cluster.centerX(), cluster.centerY(), cluster.centerZ());
        int mergeDistance = PlaceGenerationLimits.maxMergeDistanceBlocks(cluster.placeType(), config);
        int clusterRadius = Math.max(8, Math.max(cluster.radius(), PlaceGenerationLimits.maxClusterRadiusBlocks(cluster.placeType(), config)));
        int distance = Math.max(Math.max(radius, clusterRadius), mergeDistance);
        return center.squaredDistanceTo(clusterCenter) <= (long) distance * distance
                || bounds.expanded(distance, Math.max(16, distance)).contains(clusterCenter);
    }

    public String debugString() {
        return "id=" + originalPlaceId
                + " type=" + placeType.name()
                + " environment=" + environment.name()
                + " dimension=" + dimensionId
                + " center=" + center.blockIdString()
                + " radius=" + radius
                + " firstDiscoveryKey=" + firstDiscoveryKey
                + " structureId=" + structureId
                + " causeType=" + causeType.name()
                + " deletedGameTime=" + deletedGameTime
                + " suppressAutoRecreate=" + suppressAutoRecreate;
    }
}
