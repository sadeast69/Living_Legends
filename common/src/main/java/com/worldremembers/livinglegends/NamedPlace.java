package com.worldremembers.livinglegends;

import com.worldremembers.livinglegends.config.LivingLegendsConfig;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record NamedPlace(
        String placeId,
        PlaceType placeType,
        DeathSiteEnvironment environment,
        String dimensionId,
        WorldPos center,
        int radius,
        double score,
        long createdAtGameTime,
        long lastUpdatedGameTime,
        List<String> sourceChunks,
        NameRecipe nameRecipe,
        PlaceRarity rarity,
        PlaceBounds bounds,
        PlaceStats stats,
        String structureId,
        String firstDiscoveryKey,
        PlaceCause cause,
        String biomeId,
        String dominantBiomeId,
        String biomeGroup,
        String biomeTheme,
        String biomeSource,
        String manualName,
        boolean manuallyRenamed
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public NamedPlace(
            String placeId,
            PlaceType placeType,
            DeathSiteEnvironment environment,
            String dimensionId,
            WorldPos center,
            int radius,
            double score,
            long createdAtGameTime,
            long lastUpdatedGameTime,
            List<String> sourceChunks,
            NameRecipe nameRecipe,
            PlaceRarity rarity,
            PlaceBounds bounds,
            PlaceStats stats,
            String structureId,
            String firstDiscoveryKey,
            PlaceCause cause
    ) {
        this(
                placeId,
                placeType,
                environment,
                dimensionId,
                center,
                radius,
                score,
                createdAtGameTime,
                lastUpdatedGameTime,
                sourceChunks,
                nameRecipe,
                rarity,
                bounds,
                stats,
                structureId,
                firstDiscoveryKey,
                cause,
                "",
                "",
                "",
                "",
                "",
                "",
                false
        );
    }

    public NamedPlace(
            String placeId,
            PlaceType placeType,
            DeathSiteEnvironment environment,
            String dimensionId,
            WorldPos center,
            int radius,
            double score,
            long createdAtGameTime,
            long lastUpdatedGameTime,
            List<String> sourceChunks,
            NameRecipe nameRecipe,
            PlaceRarity rarity,
            PlaceBounds bounds,
            PlaceStats stats,
            String structureId,
            String firstDiscoveryKey,
            PlaceCause cause,
            String manualName,
            boolean manuallyRenamed
    ) {
        this(
                placeId,
                placeType,
                environment,
                dimensionId,
                center,
                radius,
                score,
                createdAtGameTime,
                lastUpdatedGameTime,
                sourceChunks,
                nameRecipe,
                rarity,
                bounds,
                stats,
                structureId,
                firstDiscoveryKey,
                cause,
                "",
                "",
                "",
                "",
                "",
                manualName,
                manuallyRenamed
        );
    }

    public NamedPlace(
            String placeId,
            PlaceType placeType,
            DeathSiteEnvironment environment,
            String dimensionId,
            WorldPos center,
            int radius,
            double score,
            long createdAtGameTime,
            long lastUpdatedGameTime,
            List<String> sourceChunks,
            NameRecipe nameRecipe,
            PlaceRarity rarity,
            PlaceBounds bounds,
            PlaceStats stats
    ) {
        this(
                placeId,
                placeType,
                environment,
                dimensionId,
                center,
                radius,
                score,
                createdAtGameTime,
                lastUpdatedGameTime,
                sourceChunks,
                nameRecipe,
                rarity,
                bounds,
                stats,
                "",
                "",
                null
        );
    }

    public NamedPlace {
        placeId = WorldPos.requireId(placeId, "placeId");
        placeType = placeType == null ? PlaceType.UNKNOWN : placeType;
        environment = environment == null ? DeathSiteEnvironment.UNKNOWN : environment;
        dimensionId = WorldPos.requireId(dimensionId, "dimensionId");
        center = center == null ? new WorldPos(dimensionId, 0, 64, 0) : center;
        if (!dimensionId.equals(center.dimensionId())) {
            center = new WorldPos(dimensionId, center.x(), center.y(), center.z());
        }
        radius = Math.max(0, radius);
        score = Math.max(0.0, score);
        createdAtGameTime = Math.max(0L, createdAtGameTime);
        lastUpdatedGameTime = Math.max(createdAtGameTime, lastUpdatedGameTime);
        sourceChunks = Collections.unmodifiableList(normalizedSourceChunks(sourceChunks));
        nameRecipe = nameRecipe == null ? NameRecipe.empty() : nameRecipe.withoutFallback();
        stats = stats == null ? PlaceStats.empty() : stats;
        rarity = rarity == null ? stats.calculatedRarity() : rarity;
        bounds = bounds == null ? defaultBounds(center, radius) : bounds;
        structureId = WorldPos.optionalId(structureId);
        firstDiscoveryKey = WorldPos.optionalId(firstDiscoveryKey);
        biomeId = normalizeBiomeValue(biomeId);
        dominantBiomeId = normalizeBiomeValue(dominantBiomeId);
        biomeGroup = normalizeBiomeValue(biomeGroup);
        biomeTheme = normalizeBiomeValue(biomeTheme);
        biomeSource = normalizeBiomeValue(biomeSource);
        if (cause == null || cause.causeType() == PlaceCauseType.UNKNOWN) {
            cause = PlaceCause.fromMetadata(placeType, environment, stats, firstDiscoveryKey, structureId);
        }
        if (!biomeGroup.isBlank() || !biomeId.isBlank()) {
            cause = cause.withBiome(new BiomeMetadata(biomeId, dominantBiomeId, biomeGroup, biomeTheme, biomeSource));
        }
        manualName = sanitizeManualName(manualName);
        manuallyRenamed = manuallyRenamed && !manualName.isBlank();
    }

    public NamedPlace(
            String placeId,
            NameRecipe nameRecipe,
            PlaceType placeType,
            PlaceRarity rarity,
            PlaceBounds bounds,
            PlaceStats stats,
            long createdAtGameTime,
            long lastUpdatedGameTime
    ) {
        this(
                placeId,
                placeType,
                DeathSiteEnvironment.UNKNOWN,
                bounds == null ? "minecraft:overworld" : bounds.dimensionId(),
                bounds == null ? new WorldPos("minecraft:overworld", 0, 64, 0) : bounds.center(),
                bounds == null ? 0 : radiusFromBounds(bounds),
                stats == null ? 0.0 : stats.basicScore(),
                createdAtGameTime,
                lastUpdatedGameTime,
                List.of(),
                nameRecipe,
                rarity,
                bounds,
                stats,
                "",
                "",
                null
        );
    }

    public NamedPlace(
            String placeId,
            String legacyDisplayName,
            PlaceType placeType,
            PlaceRarity rarity,
            NameStyle nameStyle,
            PlaceBounds bounds,
            PlaceStats stats,
            long createdAtGameTime,
            long lastUpdatedGameTime
    ) {
        this(
                placeId,
                new NameRecipe(
                        nameStyle == null ? NameStyle.PLAIN.idString() : nameStyle.idString(),
                        "living_legends.name.pattern.legacy",
                        List.of(),
                        List.of(),
                        0L,
                        legacyDisplayName
                ),
                placeType,
                rarity,
                bounds,
                stats,
                createdAtGameTime,
                lastUpdatedGameTime
        );
    }

    public static NamedPlace fromCluster(
            String placeId,
            PlaceCluster cluster,
            NameRecipe nameRecipe,
            long gameTime
    ) {
        return fromCluster(placeId, cluster, nameRecipe, gameTime, null);
    }

    public static NamedPlace fromCluster(
            String placeId,
            PlaceCluster cluster,
            NameRecipe nameRecipe,
            long gameTime,
            LivingLegendsConfig config
    ) {
        Objects.requireNonNull(cluster, "cluster");
        WorldPos center = new WorldPos(cluster.dimensionId(), cluster.centerX(), cluster.centerY(), cluster.centerZ());
        int radius = radiusForCluster(cluster, 0, config);
        return new NamedPlace(
                placeId,
                cluster.placeType(),
                cluster.environment(),
                cluster.dimensionId(),
                center,
                radius,
                cluster.totalScore(),
                Math.max(0L, gameTime),
                Math.max(0L, gameTime),
                cluster.includedChunks(),
                nameRecipe,
                cluster.combinedStats().calculatedRarity(),
                boundsForCluster(cluster, center, radius),
                cluster.combinedStats(),
                cluster.structureId(),
                cluster.firstDiscoveryKey(),
                cluster.cause(),
                cluster.biomeId(),
                cluster.dominantBiomeId(),
                cluster.biomeGroup(),
                cluster.biomeTheme(),
                cluster.biomeSource(),
                "",
                false
        );
    }

    public NamedPlace updatedFromCluster(PlaceCluster cluster, long gameTime) {
        return updatedFromCluster(cluster, gameTime, null);
    }

    public NamedPlace updatedFromCluster(PlaceCluster cluster, long gameTime, LivingLegendsConfig config) {
        Objects.requireNonNull(cluster, "cluster");
        boolean sameType = placeType == cluster.placeType();
        WorldPos clusterCenter = new WorldPos(cluster.dimensionId(), cluster.centerX(), cluster.centerY(), cluster.centerZ());
        WorldPos updatedCenter = sameType ? centerForUpdate(cluster, clusterCenter, config) : center;
        int updatedRadius = sameType ? radiusForCluster(cluster, radius, config) : radius;
        PlaceStats updatedStats = sameType ? cluster.combinedStats() : stats;
        return new NamedPlace(
                placeId,
                placeType,
                sameType ? environmentForUpdate(cluster) : environment,
                dimensionId,
                updatedCenter,
                updatedRadius,
                sameType ? Math.max(score, cluster.totalScore()) : score,
                createdAtGameTime,
                Math.max(lastUpdatedGameTime, gameTime),
                mergedSourceChunks(sourceChunks, cluster.includedChunks()),
                nameRecipe,
                sameType ? updatedStats.calculatedRarity() : rarity,
                sameType ? boundsForCluster(cluster, updatedCenter, updatedRadius) : bounds,
                updatedStats,
                sameType ? firstNonBlank(structureId, cluster.structureId()) : structureId,
                sameType ? firstNonBlank(firstDiscoveryKey, cluster.firstDiscoveryKey()) : firstDiscoveryKey,
                sameType ? causeForUpdate(cluster) : cause,
                sameType ? firstNonBlank(biomeId, cluster.biomeId()) : biomeId,
                sameType ? firstNonBlank(dominantBiomeId, cluster.dominantBiomeId()) : dominantBiomeId,
                sameType ? firstNonBlank(biomeGroup, cluster.biomeGroup()) : biomeGroup,
                sameType ? firstNonBlank(biomeTheme, cluster.biomeTheme()) : biomeTheme,
                sameType ? firstNonBlank(biomeSource, cluster.biomeSource()) : biomeSource,
                manualName,
                manuallyRenamed
        );
    }

    public boolean contains(WorldPos position) {
        return bounds.contains(position);
    }

    public boolean contains(WorldMemoryEvent event) {
        return event != null && contains(event.position());
    }

    public NamedPlace updatedWith(WorldMemoryEvent event) {
        if (!contains(event)) {
            return this;
        }

        PlaceStats updatedStats = stats.updatedWith(event);
        return new NamedPlace(
                placeId,
                placeType,
                environment,
                dimensionId,
                center,
                radius,
                Math.max(score, updatedStats.basicScore()),
                createdAtGameTime,
                Math.max(lastUpdatedGameTime, event.gameTime()),
                sourceChunks,
                nameRecipe,
                updatedStats.calculatedRarity(),
                bounds,
                updatedStats,
                structureId,
                firstDiscoveryKey,
                cause,
                biomeId,
                dominantBiomeId,
                biomeGroup,
                biomeTheme,
                biomeSource,
                manualName,
                manuallyRenamed
        );
    }

    public double basicScore() {
        return Math.max(score, stats.basicScore());
    }

    public String placeIdString() {
        return placeId;
    }

    public NameStyle nameStyle() {
        return NameStyle.fromId(nameRecipe.styleId());
    }

    public String displayName() {
        return manuallyRenamed ? manualName : nameRecipe.fallbackResolvedName();
    }

    public String centerString() {
        return center.x() + "," + center.y() + "," + center.z();
    }

    public NamedPlace withManualName(String newManualName, long gameTime) {
        return new NamedPlace(
                placeId,
                placeType,
                environment,
                dimensionId,
                center,
                radius,
                score,
                createdAtGameTime,
                Math.max(lastUpdatedGameTime, Math.max(0L, gameTime)),
                sourceChunks,
                nameRecipe,
                rarity,
                bounds,
                stats,
                structureId,
                firstDiscoveryKey,
                cause,
                biomeId,
                dominantBiomeId,
                biomeGroup,
                biomeTheme,
                biomeSource,
                newManualName,
                true
        );
    }

    public NamedPlace withGeneratedNameRecipe(NameRecipe newRecipe, long gameTime, boolean clearManualName) {
        return new NamedPlace(
                placeId,
                placeType,
                environment,
                dimensionId,
                center,
                radius,
                score,
                createdAtGameTime,
                Math.max(lastUpdatedGameTime, Math.max(0L, gameTime)),
                sourceChunks,
                newRecipe,
                rarity,
                bounds,
                stats,
                structureId,
                firstDiscoveryKey,
                cause,
                biomeId,
                dominantBiomeId,
                biomeGroup,
                biomeTheme,
                biomeSource,
                clearManualName ? "" : manualName,
                clearManualName ? false : manuallyRenamed
        );
    }

    private DeathSiteEnvironment environmentForUpdate(PlaceCluster cluster) {
        if (environment == DeathSiteEnvironment.UNKNOWN) {
            return cluster.environment();
        }
        if (cluster.placeType() == PlaceType.DEATH_SITE && cluster.environment() == DeathSiteEnvironment.UNKNOWN) {
            return environment;
        }
        if (cluster.placeType() == PlaceType.DEATH_SITE) {
            return cluster.environment();
        }
        return environment;
    }

    private PlaceCause causeForUpdate(PlaceCluster cluster) {
        if (cause == null || cause.causeType() == PlaceCauseType.UNKNOWN) {
            return cluster.cause();
        }
        if (cluster.cause() == null || cluster.cause().causeType() == PlaceCauseType.UNKNOWN) {
            return cause;
        }
        return cluster.cause();
    }

    private WorldPos centerForUpdate(PlaceCluster cluster, WorldPos desiredCenter, LivingLegendsConfig config) {
        int maxShift = PlaceGenerationLimits.maxCenterShiftBlocks(cluster.placeType(), config);
        return limitedCenterShift(center, desiredCenter, maxShift);
    }

    private static int radiusForCluster(PlaceCluster cluster, int existingRadius, LivingLegendsConfig config) {
        if (cluster.bounds() != null) {
            return Math.max(existingRadius, cluster.bounds().radius());
        }
        int clusterRadius = Math.max(0, cluster.radius());
        int maxRadius = PlaceGenerationLimits.maxClusterRadiusBlocks(cluster.placeType(), config);
        int cappedClusterRadius = Math.min(clusterRadius, Math.max(8, maxRadius));
        return Math.min(Math.max(existingRadius, cappedClusterRadius), Math.max(8, maxRadius));
    }

    private static PlaceBounds boundsForCluster(PlaceCluster cluster, WorldPos center, int radius) {
        if (cluster.bounds() != null) {
            return cluster.bounds();
        }
        return PlaceBounds.around(
                center,
                radius,
                Math.max(8, Math.max(cluster.centerY() - cluster.minY(), cluster.maxY() - cluster.centerY()))
        );
    }

    private static WorldPos limitedCenterShift(WorldPos current, WorldPos desired, int maxShift) {
        if (current == null || desired == null || maxShift <= 0 || current.squaredDistanceTo(desired) <= (long) maxShift * maxShift) {
            return current == null ? desired : current;
        }

        double dx = desired.x() - current.x();
        double dy = desired.y() - current.y();
        double dz = desired.z() - current.z();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance <= 0.000_001) {
            return current;
        }

        double ratio = maxShift / distance;
        return new WorldPos(
                current.dimensionId(),
                current.x() + (int) Math.round(dx * ratio),
                current.y() + (int) Math.round(dy * ratio),
                current.z() + (int) Math.round(dz * ratio)
        );
    }

    private static PlaceBounds defaultBounds(WorldPos center, int radius) {
        return PlaceBounds.around(center, Math.max(8, radius), 32);
    }

    private static int radiusFromBounds(PlaceBounds bounds) {
        int xRadius = Math.max(Math.abs(bounds.center().x() - bounds.minX()), Math.abs(bounds.maxX() - bounds.center().x()));
        int zRadius = Math.max(Math.abs(bounds.center().z() - bounds.minZ()), Math.abs(bounds.maxZ() - bounds.center().z()));
        return Math.max(xRadius, zRadius);
    }

    private static String firstNonBlank(String first, String second) {
        String normalizedFirst = WorldPos.optionalId(first);
        return normalizedFirst.isBlank() ? WorldPos.optionalId(second) : normalizedFirst;
    }

    private static String normalizeBiomeValue(String value) {
        String normalized = WorldPos.optionalId(value);
        return normalized.isBlank() ? "" : normalized;
    }

    private static String sanitizeManualName(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < trimmed.length() && result.length() < 80; index++) {
            char character = trimmed.charAt(index);
            if (character == '\n' || character == '\r' || Character.isISOControl(character)) {
                continue;
            }
            result.append(character);
        }
        return result.toString().trim();
    }

    private static List<String> normalizedSourceChunks(List<String> chunks) {
        List<String> result = new ArrayList<>();
        if (chunks == null) {
            return result;
        }
        for (String chunk : chunks) {
            String normalized = WorldPos.optionalId(chunk);
            if (!normalized.isBlank()) {
                result.add(normalized);
            }
        }
        return result;
    }

    private static List<String> mergedSourceChunks(List<String> existing, List<String> added) {
        Set<String> merged = new LinkedHashSet<>();
        merged.addAll(normalizedSourceChunks(existing));
        merged.addAll(normalizedSourceChunks(added));
        return List.copyOf(merged);
    }
}
