package com.worldremembers.livinglegends;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record PlaceCluster(
        String clusterId,
        PlaceType placeType,
        String dimensionId,
        DeathSiteEnvironment environment,
        int centerX,
        int centerY,
        int centerZ,
        int minY,
        int maxY,
        int radius,
        double totalScore,
        double scoreThreshold,
        long rawCount,
        long requiredRawCount,
        boolean candidate,
        int priorityRank,
        String seedChunkId,
        PlaceStats combinedStats,
        List<String> includedChunks,
        PlaceBounds bounds,
        String structureId,
        String firstDiscoveryKey,
        PlaceCause cause,
        String biomeId,
        String dominantBiomeId,
        String biomeGroup,
        String biomeTheme,
        String biomeSource
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public PlaceCluster(
            String clusterId,
            PlaceType placeType,
            String dimensionId,
            DeathSiteEnvironment environment,
            int centerX,
            int centerY,
            int centerZ,
            int minY,
            int maxY,
            int radius,
            double totalScore,
            double scoreThreshold,
            long rawCount,
            long requiredRawCount,
            boolean candidate,
            int priorityRank,
            String seedChunkId,
            PlaceStats combinedStats,
            List<String> includedChunks
    ) {
        this(
                clusterId,
                placeType,
                dimensionId,
                environment,
                centerX,
                centerY,
                centerZ,
                minY,
                maxY,
                radius,
                totalScore,
                scoreThreshold,
                rawCount,
                requiredRawCount,
                candidate,
                priorityRank,
                seedChunkId,
                combinedStats,
                includedChunks,
                null,
                "",
                "",
                null,
                "",
                "",
                "",
                "",
                ""
        );
    }

    public PlaceCluster(
            String clusterId,
            PlaceType placeType,
            String dimensionId,
            DeathSiteEnvironment environment,
            int centerX,
            int centerY,
            int centerZ,
            int minY,
            int maxY,
            int radius,
            double totalScore,
            double scoreThreshold,
            long rawCount,
            long requiredRawCount,
            boolean candidate,
            int priorityRank,
            String seedChunkId,
            PlaceStats combinedStats,
            List<String> includedChunks,
            PlaceBounds bounds,
            String structureId,
            String firstDiscoveryKey
    ) {
        this(
                clusterId,
                placeType,
                dimensionId,
                environment,
                centerX,
                centerY,
                centerZ,
                minY,
                maxY,
                radius,
                totalScore,
                scoreThreshold,
                rawCount,
                requiredRawCount,
                candidate,
                priorityRank,
                seedChunkId,
                combinedStats,
                includedChunks,
                bounds,
                structureId,
                firstDiscoveryKey,
                null,
                "",
                "",
                "",
                "",
                ""
        );
    }

    public PlaceCluster(
            String clusterId,
            PlaceType placeType,
            String dimensionId,
            DeathSiteEnvironment environment,
            int centerX,
            int centerY,
            int centerZ,
            int minY,
            int maxY,
            int radius,
            double totalScore,
            double scoreThreshold,
            long rawCount,
            long requiredRawCount,
            boolean candidate,
            int priorityRank,
            String seedChunkId,
            PlaceStats combinedStats,
            List<String> includedChunks,
            PlaceBounds bounds,
            String structureId,
            String firstDiscoveryKey,
            PlaceCause cause
    ) {
        this(
                clusterId,
                placeType,
                dimensionId,
                environment,
                centerX,
                centerY,
                centerZ,
                minY,
                maxY,
                radius,
                totalScore,
                scoreThreshold,
                rawCount,
                requiredRawCount,
                candidate,
                priorityRank,
                seedChunkId,
                combinedStats,
                includedChunks,
                bounds,
                structureId,
                firstDiscoveryKey,
                cause,
                "",
                "",
                "",
                "",
                ""
        );
    }

    public PlaceCluster {
        clusterId = WorldPos.requireId(clusterId, "clusterId");
        placeType = placeType == null ? PlaceType.UNKNOWN : placeType;
        dimensionId = WorldPos.requireId(dimensionId, "dimensionId");
        environment = environment == null ? DeathSiteEnvironment.UNKNOWN : environment;
        if (minY > maxY) {
            int previousMinY = minY;
            minY = maxY;
            maxY = previousMinY;
        }
        radius = Math.max(0, radius);
        totalScore = Math.max(0.0, totalScore);
        scoreThreshold = Math.max(0.0, scoreThreshold);
        rawCount = Math.max(0L, rawCount);
        requiredRawCount = Math.max(0L, requiredRawCount);
        priorityRank = Math.max(0, priorityRank);
        seedChunkId = WorldPos.requireId(seedChunkId, "seedChunkId");
        combinedStats = combinedStats == null ? PlaceStats.empty() : combinedStats;
        includedChunks = Collections.unmodifiableList(new ArrayList<>(includedChunks == null ? List.of() : includedChunks));
        structureId = WorldPos.optionalId(structureId);
        firstDiscoveryKey = WorldPos.optionalId(firstDiscoveryKey);
        biomeId = normalizeBiomeValue(biomeId);
        dominantBiomeId = normalizeBiomeValue(dominantBiomeId);
        biomeGroup = normalizeBiomeValue(biomeGroup);
        biomeTheme = normalizeBiomeValue(biomeTheme);
        biomeSource = normalizeBiomeValue(biomeSource);
        if (cause == null || cause.causeType() == PlaceCauseType.UNKNOWN) {
            cause = PlaceCause.fromMetadata(placeType, environment, combinedStats, firstDiscoveryKey, structureId);
        }
        if (!biomeGroup.isBlank() || !biomeId.isBlank()) {
            cause = cause.withBiome(new BiomeMetadata(biomeId, dominantBiomeId, biomeGroup, biomeTheme, biomeSource));
        }
    }

    public String centerString() {
        return centerX + "," + centerY + "," + centerZ;
    }

    public int chunkCount() {
        return includedChunks.size();
    }

    public PlaceCluster withBiome(BiomeMetadata metadata) {
        BiomeMetadata biome = metadata == null ? BiomeMetadata.unknown() : metadata;
        return new PlaceCluster(
                clusterId,
                placeType,
                dimensionId,
                environment,
                centerX,
                centerY,
                centerZ,
                minY,
                maxY,
                radius,
                totalScore,
                scoreThreshold,
                rawCount,
                requiredRawCount,
                candidate,
                priorityRank,
                seedChunkId,
                combinedStats,
                includedChunks,
                bounds,
                structureId,
                firstDiscoveryKey,
                cause == null ? null : cause.withBiome(biome),
                biome.biomeId(),
                biome.dominantBiomeId(),
                biome.biomeGroup(),
                biome.biomeTheme(),
                biome.biomeSource()
        );
    }

    private static String normalizeBiomeValue(String value) {
        String normalized = WorldPos.optionalId(value);
        return normalized.isBlank() ? "" : normalized;
    }
}
