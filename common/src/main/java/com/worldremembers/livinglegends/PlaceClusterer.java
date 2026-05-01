package com.worldremembers.livinglegends;

import com.worldremembers.livinglegends.config.LivingLegendsConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlaceClusterer {
    private static final int CHUNK_SIZE = 16;

    private PlaceClusterer() {
    }

    public static ClusterBuildResult buildCluster(
            ScoreEngine.ScoreEvaluation seed,
            ChunkMemoryStats seedStats,
            WorldMemoryStorageData data,
            LivingLegendsConfig config
    ) {
        if (!canAttemptCluster(seed, seedStats, data, config)) {
            return null;
        }

        int radius = effectiveSearchRadius(seed.placeType(), config);
        int maxClusterRadiusBlocks = PlaceGenerationLimits.maxClusterRadiusBlocks(seed.placeType(), config);
        List<ChunkContribution> contributions = new ArrayList<>();
        List<String> excludedChunks = new ArrayList<>();
        int seedCenterX = chunkCenterBlock(seed.chunkX());
        int seedCenterZ = chunkCenterBlock(seed.chunkZ());

        for (int chunkX = seed.chunkX() - radius; chunkX <= seed.chunkX() + radius; chunkX++) {
            for (int chunkZ = seed.chunkZ() - radius; chunkZ <= seed.chunkZ() + radius; chunkZ++) {
                String chunkKey = WorldMemoryStorageData.chunkStatsKey(seed.dimensionId(), chunkX, chunkZ);
                int chunkCenterX = chunkCenterBlock(chunkX);
                int chunkCenterZ = chunkCenterBlock(chunkZ);
                if (horizontalDistance(seedCenterX, seedCenterZ, chunkCenterX, chunkCenterZ) > maxClusterRadiusBlocks) {
                    excludedChunks.add(chunkKey + " reason=beyond_" + seed.placeType().idString() + "_cluster_radius"
                            + " maxRadius=" + maxClusterRadiusBlocks);
                    continue;
                }

                ChunkMemoryStats stats = sameChunk(seed, seedStats, chunkX, chunkZ)
                        ? seedStats
                        : data.chunkStats(seed.dimensionId(), chunkX, chunkZ);
                if (stats == null) {
                    excludedChunks.add(chunkKey + " reason=no_stats");
                    continue;
                }
                if (!seed.dimensionId().equals(stats.dimensionId())) {
                    excludedChunks.add(chunkKey + " reason=different_dimension");
                    continue;
                }

                if (seed.placeType() == PlaceType.DEATH_SITE && strictDeathSiteEnvironments(config)) {
                    long matchingDeaths = ScoreEngine.deathSiteActivityCount(stats, seed.environment());
                    long otherDeaths = Math.max(0L, stats.eventTypeCount(EventType.PLAYER_DEATH) - matchingDeaths);
                    if (matchingDeaths <= 0L && otherDeaths > 0L) {
                        excludedChunks.add(chunkKey
                                + " reason=environment_mismatch"
                                + " candidateEnvironment=" + seed.environment().name()
                                + " chunkEnvironmentCounts=" + stats.deathSiteEnvironmentCounts());
                        continue;
                    }
                    if (matchingDeaths > 0L && otherDeaths > 0L) {
                        excludedChunks.add(chunkKey
                                + " reason=environment_mismatch"
                                + " candidateEnvironment=" + seed.environment().name()
                                + " excludedEnvironmentCounts=" + excludedEnvironmentCounts(stats, seed.environment()));
                    }
                }

                double score = contributionScore(seed, stats, data, config);
                long rawCount = contributionActivityCount(seed, stats, data, config);
                if (rawCount <= 0L || score <= 0.0) {
                    excludedChunks.add(chunkKey + " reason=no_" + seed.placeType().idString() + "_activity");
                    continue;
                }

                contributions.add(new ChunkContribution(stats, score, rawCount));
            }
        }

        contributions.sort(Comparator
                .comparingInt((ChunkContribution contribution) -> contribution.stats().chunkX())
                .thenComparingInt(contribution -> contribution.stats().chunkZ()));
        PlaceCluster cluster = toCluster(seed, contributions, data, config);
        return new ClusterBuildResult(seed.placeType(), seed.environment(), seed.chunkDebugId(), cluster, excludedChunks);
    }

    public static ClusterSelection selectDominantCluster(
            List<ClusterBuildResult> results,
            WorldMemoryStorageData data,
            LivingLegendsConfig config
    ) {
        if (results == null || results.isEmpty()) {
            return new ClusterSelection(null, List.of(), List.of());
        }

        List<ClusterBuildResult> eligible = new ArrayList<>();
        List<String> suppressed = new ArrayList<>();
        for (ClusterBuildResult result : results) {
            PlaceCluster cluster = result.cluster();
            if (cluster == null) {
                continue;
            }
            if (!cluster.candidate()) {
                suppressed.add(result.placeType().name() + " reason=cluster_threshold_not_met"
                        + " raw=" + cluster.rawCount() + "/" + cluster.requiredRawCount()
                        + " score=" + formatScore(cluster.totalScore()) + "/" + formatScore(cluster.scoreThreshold())
                        + causeLogPart(cluster));
                continue;
            }

            eligible.add(result);
        }

        if (eligible.isEmpty()) {
            return new ClusterSelection(null, List.copyOf(results), List.copyOf(suppressed));
        }

        eligible.sort(Comparator
                .comparingInt((ClusterBuildResult result) -> result.cluster().priorityRank())
                .thenComparing((ClusterBuildResult result) -> result.placeType() == PlaceType.GENERAL_LANDMARK ? 1 : 0)
                .thenComparing(Comparator.comparingDouble((ClusterBuildResult result) -> result.cluster().totalScore()).reversed())
                .thenComparing(Comparator.comparingLong((ClusterBuildResult result) -> result.cluster().rawCount()).reversed()));
        ClusterBuildResult selected = eligible.get(0);

        for (ClusterBuildResult result : eligible) {
            if (result == selected) {
                continue;
            }
            if (result.cluster().priorityRank() > selected.cluster().priorityRank()
                    || result.placeType() == PlaceType.GENERAL_LANDMARK) {
                suppressed.add(result.placeType().name()
                        + " reason=lower_priority"
                        + " selected=" + selected.placeType().name()
                        + " priority=" + result.cluster().priorityRank()
                        + " selectedPriority=" + selected.cluster().priorityRank()
                        + causeLogPart(result.cluster()));
            }
        }

        return new ClusterSelection(selected, List.copyOf(results), List.copyOf(suppressed));
    }

    public static int maxClusterRadiusChunks(LivingLegendsConfig config) {
        if (config == null || config.performance == null) {
            return 2;
        }
        return Math.max(1, config.performance.maxClusterRadiusChunks);
    }

    public static boolean isClusterSeed(ScoreEngine.ScoreEvaluation evaluation) {
        if (evaluation == null || evaluation.score() <= 0.0 || evaluation.activityCount() <= 0L) {
            return false;
        }
        return evaluation.candidate() || supportsClusterLevelThreshold(evaluation.placeType());
    }

    public static String formatClusterLog(PlaceCluster cluster) {
        return "PlaceClusterer cluster:"
                + " type=" + cluster.placeType().name()
                + " environment=" + cluster.environment().name()
                + " chunks=" + cluster.chunkCount()
                + " center=" + cluster.centerString()
                + " radius=" + cluster.radius()
                + " score=" + formatScore(cluster.totalScore())
                + firstDiscoveryLogPart(cluster);
    }

    public static String formatClusterDetailsLog(ClusterBuildResult result) {
        PlaceCluster cluster = result.cluster();
        if (cluster == null) {
            return "PlaceClusterer cluster details:"
                    + " type=" + result.placeType().name()
                    + " candidateEnvironment=" + result.seedEnvironment().name()
                    + " seedChunk=" + result.seedChunkId()
                    + " includedChunks=[]"
                    + " excludedChunks=" + result.excludedChunks()
                    + " finalRawCount=0"
                    + " finalClusterScore=0";
        }

        return "PlaceClusterer cluster details:"
                + " type=" + cluster.placeType().name()
                + deathSiteClusterLogPart(cluster)
                + " seedChunk=" + cluster.seedChunkId()
                + " includedChunks=" + cluster.includedChunks()
                + " excludedChunks=" + result.excludedChunks()
                + " finalClusterRawCount=" + cluster.rawCount()
                + " requiredRawCount=" + cluster.requiredRawCount()
                + " finalClusterScore=" + formatScore(cluster.totalScore())
                + " scoreThreshold=" + formatScore(cluster.scoreThreshold())
                + " clusterCandidate=" + cluster.candidate()
                + firstDiscoveryLogPart(cluster)
                + causeLogPart(cluster);
    }

    public static String formatSelectionLog(ClusterSelection selection) {
        if (selection.selected() == null || selection.selected().cluster() == null) {
            return "PlaceClusterer selection:"
                    + " selectedDominantPlaceType=none"
                    + " suppressedLowerPriorityCandidates=" + selection.suppressedCandidates();
        }

        return "PlaceClusterer selection:"
                + " selectedDominantPlaceType=" + selection.selected().placeType().name()
                + " selectedPriority=" + selection.selected().cluster().priorityRank()
                + " finalClusterRawCount=" + selection.selected().cluster().rawCount()
                + " finalClusterScore=" + formatScore(selection.selected().cluster().totalScore())
                + " suppressedLowerPriorityCandidates=" + selection.suppressedCandidates();
    }

    public static String formatSelectionLog(ClusterSelection selection, NamedPlaceCreator.Result result) {
        if (result == null || !result.suppressed()) {
            return formatSelectionLog(selection);
        }

        String suppressedType = selection.selected() == null ? "none" : selection.selected().placeType().name();
        return "PlaceClusterer selection:"
                + " selectedDominantPlaceType=none"
                + " finalDecision=" + result.finalDecision().name()
                + " suppressedCandidateType=" + suppressedType
                + " reason=" + result.finalDecisionReason()
                + " suppressedLowerPriorityCandidates=" + selection.suppressedCandidates();
    }

    private static boolean canAttemptCluster(
            ScoreEngine.ScoreEvaluation seed,
            ChunkMemoryStats seedStats,
            WorldMemoryStorageData data,
            LivingLegendsConfig config
    ) {
        return seed != null
                && seedStats != null
                && data != null
                && config != null
                && sameChunk(seed, seedStats)
                && isClusterSeed(seed);
    }

    private static boolean supportsClusterLevelThreshold(PlaceType placeType) {
        return placeType == PlaceType.BATTLEFIELD
                || placeType == PlaceType.SLAUGHTER_FIELD
                || placeType == PlaceType.SETTLEMENT
                || placeType == PlaceType.GENERAL_LANDMARK
                || placeType == PlaceType.MINING_SITE;
    }

    private static int effectiveSearchRadius(PlaceType placeType, LivingLegendsConfig config) {
        if (placeType == PlaceType.FIRST_DISCOVERY) {
            return 0;
        }
        return Math.min(maxClusterRadiusChunks(config), PlaceGenerationLimits.maxSearchRadiusChunks(placeType, config));
    }

    private static double contributionScore(
            ScoreEngine.ScoreEvaluation seed,
            ChunkMemoryStats stats,
            WorldMemoryStorageData data,
            LivingLegendsConfig config
    ) {
        if (seed.placeType() == PlaceType.DEATH_SITE && strictDeathSiteEnvironments(config)) {
            return ScoreEngine.candidateAdjustedScore(
                    stats,
                    PlaceType.DEATH_SITE,
                    ScoreEngine.deathSiteScore(stats, seed.environment()),
                    data
            );
        }
        return ScoreEngine.scoreChunk(stats, seed.placeType(), data);
    }

    private static long contributionActivityCount(
            ScoreEngine.ScoreEvaluation seed,
            ChunkMemoryStats stats,
            WorldMemoryStorageData data,
            LivingLegendsConfig config
    ) {
        if (seed.placeType() == PlaceType.DEATH_SITE && strictDeathSiteEnvironments(config)) {
            return ScoreEngine.deathSiteActivityCount(stats, seed.environment());
        }
        return ScoreEngine.activityCount(stats, seed.placeType(), data, config);
    }

    private static PlaceCluster toCluster(
            ScoreEngine.ScoreEvaluation seed,
            List<ChunkContribution> contributions,
            WorldMemoryStorageData data,
            LivingLegendsConfig config
    ) {
        if (contributions == null || contributions.isEmpty()) {
            return null;
        }

        double weightedX = 0.0;
        double weightedY = 0.0;
        double weightedZ = 0.0;
        double totalWeight = 0.0;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int minBlockX = Integer.MAX_VALUE;
        int minBlockZ = Integer.MAX_VALUE;
        int maxBlockX = Integer.MIN_VALUE;
        int maxBlockZ = Integer.MIN_VALUE;
        List<String> includedChunks = new ArrayList<>();
        CombinedStatsBuilder statsBuilder = new CombinedStatsBuilder();

        for (ChunkContribution contribution : contributions) {
            ChunkMemoryStats stats = contribution.stats();
            double weight = Math.max(1.0, contribution.score());
            int chunkCenterX = chunkCenterBlock(stats.chunkX());
            int chunkCenterZ = chunkCenterBlock(stats.chunkZ());
            int chunkCenterY = chunkCenterY(stats);

            totalWeight += weight;
            weightedX += chunkCenterX * weight;
            weightedY += chunkCenterY * weight;
            weightedZ += chunkCenterZ * weight;
            minY = Math.min(minY, stats.eventCount() > 0L ? stats.minY() : chunkCenterY);
            maxY = Math.max(maxY, stats.eventCount() > 0L ? stats.maxY() : chunkCenterY);
            minBlockX = Math.min(minBlockX, stats.chunkX() * CHUNK_SIZE);
            minBlockZ = Math.min(minBlockZ, stats.chunkZ() * CHUNK_SIZE);
            maxBlockX = Math.max(maxBlockX, stats.chunkX() * CHUNK_SIZE + CHUNK_SIZE - 1);
            maxBlockZ = Math.max(maxBlockZ, stats.chunkZ() * CHUNK_SIZE + CHUNK_SIZE - 1);
            includedChunks.add(stats.chunkIdString());
            statsBuilder.add(stats, seed.placeType(), seed.environment(), config);
        }

        WorldFirstDiscoveryRecord firstDiscovery = firstDiscoveryFor(seed, data);
        PlaceBounds preferredBounds = firstDiscovery == null ? null : firstDiscovery.effectiveBounds();
        int centerX = totalWeight <= 0.0 ? chunkCenterBlock(seed.chunkX()) : (int) Math.round(weightedX / totalWeight);
        int centerY = totalWeight <= 0.0 ? 64 : (int) Math.round(weightedY / totalWeight);
        int centerZ = totalWeight <= 0.0 ? chunkCenterBlock(seed.chunkZ()) : (int) Math.round(weightedZ / totalWeight);
        if (preferredBounds != null) {
            WorldPos boundsCenter = preferredBounds.center();
            centerX = boundsCenter.x();
            centerY = boundsCenter.y();
            centerZ = boundsCenter.z();
            minY = preferredBounds.minY();
            maxY = preferredBounds.maxY();
        }
        if (minY == Integer.MAX_VALUE) {
            minY = centerY;
            maxY = centerY;
        }

        PlaceStats combinedStats = statsBuilder.build();
        double totalScore = ScoreEngine.scorePlace(combinedStats, seed.placeType());
        long rawCount = ScoreEngine.activityCount(combinedStats, seed.placeType(), config);
        double scoreThreshold = ScoreEngine.scoreThresholdFor(seed.placeType(), config);
        long requiredRawCount = ScoreEngine.requiredRawCountFor(seed.placeType(), config);
        boolean candidate = config.generation.enabled && rawCount >= requiredRawCount && totalScore >= scoreThreshold;
        int radius = preferredBounds == null ? Math.min(
                radiusFor(centerX, centerZ, minBlockX, minBlockZ, maxBlockX, maxBlockZ),
                PlaceGenerationLimits.maxClusterRadiusBlocks(seed.placeType(), config)
        ) : Math.max(0, preferredBounds.radius());
        DeathSiteEnvironment environment = environmentFor(seed, combinedStats, config);
        PlaceCause cause = firstDiscovery == null
                ? CauseContextResolver.fromStats(seed.placeType(), environment, combinedStats, "", "")
                : CauseContextResolver.fromFirstDiscovery(firstDiscovery);

        return new PlaceCluster(
                seed.placeType().idString() + ":" + seed.dimensionId() + "@" + seed.chunkX() + "," + seed.chunkZ(),
                seed.placeType(),
                seed.dimensionId(),
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
                seed.placeType().priorityRank(),
                seed.chunkDebugId(),
                combinedStats,
                includedChunks,
                preferredBounds,
                firstDiscovery == null ? "" : firstDiscovery.structureIdString(),
                firstDiscovery == null ? "" : firstDiscovery.discoveryIdString(),
                cause
        );
    }

    private static DeathSiteEnvironment environmentFor(
            ScoreEngine.ScoreEvaluation seed,
            PlaceStats combinedStats,
            LivingLegendsConfig config
    ) {
        if (seed.placeType() != PlaceType.DEATH_SITE) {
            return DeathSiteEnvironment.UNKNOWN;
        }
        if (strictDeathSiteEnvironments(config)) {
            return seed.environment();
        }

        DeathSiteEnvironment dominant = DeathSiteEnvironment.UNKNOWN;
        long highestCount = 0L;
        for (DeathSiteEnvironment environment : DeathSiteEnvironment.values()) {
            if (environment == DeathSiteEnvironment.UNKNOWN) {
                continue;
            }

            long count = combinedStats.deathSiteEnvironmentCount(environment);
            if (count > highestCount) {
                dominant = environment;
                highestCount = count;
            }
        }

        return dominant == DeathSiteEnvironment.UNKNOWN ? seed.environment() : dominant;
    }

    private static String deathSiteClusterLogPart(PlaceCluster cluster) {
        if (cluster.placeType() != PlaceType.DEATH_SITE) {
            return "";
        }

        return " candidateEnvironment=" + cluster.environment().name()
                + " includedEnvironmentCounts=" + cluster.combinedStats().deathSiteEnvironmentCounts();
    }

    private static String firstDiscoveryLogPart(PlaceCluster cluster) {
        if (cluster.placeType() != PlaceType.FIRST_DISCOVERY) {
            return "";
        }
        PlaceBounds bounds = cluster.bounds();
        return " firstDiscoveryKey=" + cluster.firstDiscoveryKey()
                + " structureId=" + cluster.structureId()
                + " bounds=" + (bounds == null ? "POINT_RADIUS" : bounds.shape().name());
    }

    private static String causeLogPart(PlaceCluster cluster) {
        if (cluster == null) {
            return "";
        }
        PlaceCause cause = cluster.cause();
        if (cause == null || cause.causeType() == PlaceCauseType.UNKNOWN) {
            return "";
        }
        String result = " causeType=" + cause.causeType().name();
        if (!cause.dominantValuableBlock().isBlank()) {
            result += " dominantValuableBlock=" + cause.dominantValuableBlock();
        }
        if (!cause.dominantMobType().isBlank()) {
            result += " dominantMobType=" + cause.dominantMobType();
        }
        if (!cause.dominantHostileMobType().isBlank()) {
            result += " dominantHostileMobType=" + cause.dominantHostileMobType();
        }
        if (!cause.dominantPassiveMobType().isBlank()) {
            result += " dominantPassiveMobType=" + cause.dominantPassiveMobType();
        }
        if (!cause.portalType().isBlank()) {
            result += " portalType=" + cause.portalType();
        }
        if (cluster.placeType() == PlaceType.PORTAL_LANDMARK) {
            CauseContextResolver.PortalResolution portalResolution = CauseContextResolver.resolvePortal(cluster.combinedStats());
            result += " portalEvidenceCounts={" + portalResolution.evidenceCounts() + "}"
                    + " resolvedPortalType=" + portalResolution.portalType()
                    + " portalTypeResolutionReason=" + portalResolution.reason();
        }
        return result;
    }

    private static WorldFirstDiscoveryRecord firstDiscoveryFor(
            ScoreEngine.ScoreEvaluation seed,
            WorldMemoryStorageData data
    ) {
        if (seed == null || seed.placeType() != PlaceType.FIRST_DISCOVERY || data == null) {
            return null;
        }

        ChunkMemoryStats seedStats = data.chunkStats(seed.dimensionId(), seed.chunkX(), seed.chunkZ());
        if (seedStats == null) {
            return null;
        }

        WorldFirstDiscoveryRecord selected = null;
        for (WorldFirstDiscoveryRecord discovery : data.firstDiscoveries().values()) {
            if (discovery == null || !discovery.matches(seedStats)) {
                continue;
            }
            if (selected == null || discovery.weight() > selected.weight()) {
                selected = discovery;
            }
        }
        return selected;
    }

    private static boolean sameChunk(ScoreEngine.ScoreEvaluation seed, ChunkMemoryStats stats) {
        return seed.dimensionId().equals(stats.dimensionId())
                && seed.chunkX() == stats.chunkX()
                && seed.chunkZ() == stats.chunkZ();
    }

    private static boolean sameChunk(
            ScoreEngine.ScoreEvaluation seed,
            ChunkMemoryStats stats,
            int chunkX,
            int chunkZ
    ) {
        return sameChunk(seed, stats) && seed.chunkX() == chunkX && seed.chunkZ() == chunkZ;
    }

    private static int chunkCenterBlock(int chunkCoordinate) {
        return chunkCoordinate * CHUNK_SIZE + CHUNK_SIZE / 2;
    }

    private static int chunkCenterY(ChunkMemoryStats stats) {
        if (stats.eventCount() <= 0L) {
            return 64;
        }
        return Math.floorDiv(stats.minY() + stats.maxY(), 2);
    }

    private static int radiusFor(
            int centerX,
            int centerZ,
            int minBlockX,
            int minBlockZ,
            int maxBlockX,
            int maxBlockZ
    ) {
        int maxDistance = Math.max(
                Math.max(Math.abs(centerX - minBlockX), Math.abs(centerX - maxBlockX)),
                Math.max(Math.abs(centerZ - minBlockZ), Math.abs(centerZ - maxBlockZ))
        );
        return Math.max(CHUNK_SIZE / 2, maxDistance);
    }

    private static double horizontalDistance(int firstX, int firstZ, int secondX, int secondZ) {
        long dx = (long) firstX - secondX;
        long dz = (long) firstZ - secondZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static String formatScore(double value) {
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 0.000_001) {
            return Long.toString((long) rounded);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static boolean strictDeathSiteEnvironments(LivingLegendsConfig config) {
        return config == null
                || config.generation == null
                || !config.generation.allowMixedDeathSiteEnvironments;
    }

    private static String excludedEnvironmentCounts(ChunkMemoryStats stats, DeathSiteEnvironment includedEnvironment) {
        Map<String, Long> excluded = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : stats.deathSiteEnvironmentCounts().entrySet()) {
            DeathSiteEnvironment environment = DeathSiteEnvironment.fromId(entry.getKey());
            if (environment == includedEnvironment) {
                continue;
            }

            long count = Math.max(0L, entry.getValue() == null ? 0L : entry.getValue());
            if (count > 0L) {
                excluded.put(environment.idString(), count);
            }
        }
        return excluded.toString();
    }

    public record ClusterBuildResult(
            PlaceType placeType,
            DeathSiteEnvironment seedEnvironment,
            String seedChunkId,
            PlaceCluster cluster,
            List<String> excludedChunks
    ) {
        public ClusterBuildResult {
            placeType = placeType == null ? PlaceType.UNKNOWN : placeType;
            seedEnvironment = seedEnvironment == null ? DeathSiteEnvironment.UNKNOWN : seedEnvironment;
            seedChunkId = WorldPos.requireId(seedChunkId, "seedChunkId");
            excludedChunks = List.copyOf(excludedChunks == null ? List.of() : excludedChunks);
        }
    }

    public record ClusterSelection(
            ClusterBuildResult selected,
            List<ClusterBuildResult> attempted,
            List<String> suppressedCandidates
    ) {
        public ClusterSelection {
            attempted = List.copyOf(attempted == null ? List.of() : attempted);
            suppressedCandidates = List.copyOf(suppressedCandidates == null ? List.of() : suppressedCandidates);
        }
    }

    private record ChunkContribution(ChunkMemoryStats stats, double score, long activityCount) {
    }

    private static final class CombinedStatsBuilder {
        private long eventCount;
        private long visitCount;
        private long deathCount;
        private long combatEventCount;
        private long buildEventCount;
        private double totalImportance;
        private long lastEventGameTime;
        private final Map<String, Long> eventTypeCounts = new LinkedHashMap<>();
        private final Map<String, Long> deathSiteEnvironmentCounts = new LinkedHashMap<>();
        private final Map<String, Long> metadataCounts = new LinkedHashMap<>();

        private void add(
                ChunkMemoryStats stats,
                PlaceType placeType,
                DeathSiteEnvironment environment,
                LivingLegendsConfig config
        ) {
            if (placeType == PlaceType.DEATH_SITE && strictDeathSiteEnvironments(config)) {
                addDeathSiteEnvironment(stats, environment);
                return;
            }

            eventCount += stats.eventCount();
            visitCount += stats.visitCount();
            deathCount += stats.deathCount();
            combatEventCount += stats.combatEventCount();
            buildEventCount += stats.buildEventCount();
            totalImportance += stats.totalImportance();
            lastEventGameTime = Math.max(lastEventGameTime, stats.lastEventGameTime());
            merge(eventTypeCounts, stats.eventTypeCounts());
            merge(deathSiteEnvironmentCounts, stats.deathSiteEnvironmentCounts());
            merge(metadataCounts, stats.metadataCounts());
        }

        private void addDeathSiteEnvironment(ChunkMemoryStats stats, DeathSiteEnvironment environment) {
            DeathSiteEnvironment resolvedEnvironment = environment == null ? DeathSiteEnvironment.UNKNOWN : environment;
            long deaths = ScoreEngine.deathSiteActivityCount(stats, resolvedEnvironment);
            if (deaths <= 0L) {
                return;
            }

            long totalDeaths = Math.max(1L, stats.eventTypeCount(EventType.PLAYER_DEATH));
            double importanceShare = deaths / (double) totalDeaths;
            eventCount += deaths;
            deathCount += deaths;
            totalImportance += stats.totalImportance() * importanceShare;
            lastEventGameTime = Math.max(lastEventGameTime, stats.lastEventGameTime());
            eventTypeCounts.put(
                    EventType.PLAYER_DEATH.idString(),
                    eventTypeCounts.getOrDefault(EventType.PLAYER_DEATH.idString(), 0L) + deaths
            );
            deathSiteEnvironmentCounts.put(
                    resolvedEnvironment.idString(),
                    deathSiteEnvironmentCounts.getOrDefault(resolvedEnvironment.idString(), 0L) + deaths
            );
            merge(metadataCounts, stats.metadataCounts());
        }

        private PlaceStats build() {
            return new PlaceStats(
                    eventCount,
                    visitCount,
                    deathCount,
                    combatEventCount,
                    buildEventCount,
                    totalImportance,
                    0L,
                    lastEventGameTime,
                    dominantEventType(eventTypeCounts),
                    eventTypeCounts,
                    deathSiteEnvironmentCounts,
                    metadataCounts
            );
        }

        private static void merge(Map<String, Long> target, Map<String, Long> source) {
            for (Map.Entry<String, Long> entry : source.entrySet()) {
                long value = Math.max(0L, entry.getValue() == null ? 0L : entry.getValue());
                if (value > 0L) {
                    target.put(entry.getKey(), target.getOrDefault(entry.getKey(), 0L) + value);
                }
            }
        }

        private static EventType dominantEventType(Map<String, Long> counts) {
            EventType dominant = EventType.CUSTOM;
            long highestCount = 0L;
            for (Map.Entry<String, Long> entry : counts.entrySet()) {
                long count = Math.max(0L, entry.getValue() == null ? 0L : entry.getValue());
                if (count > highestCount) {
                    dominant = EventType.fromId(entry.getKey());
                    highestCount = count;
                }
            }
            return dominant;
        }
    }
}
