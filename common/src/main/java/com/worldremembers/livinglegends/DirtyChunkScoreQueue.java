package com.worldremembers.livinglegends;

import com.worldremembers.livinglegends.config.LivingLegendsConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;

public final class DirtyChunkScoreQueue {
    private static final int MAX_RECENT_CLUSTER_DEBUG_LINES = 80;

    private final Queue<String> queuedChunkKeys = new ArrayDeque<>();
    private final Set<String> queuedChunkKeySet = new HashSet<>();
    private final Map<String, Long> lastCandidateGenerationTickByChunk = new HashMap<>();
    private final List<String> recentClusterDebugLines = new ArrayList<>();

    public synchronized void enqueue(ChunkMemoryStats stats) {
        if (stats != null) {
            enqueue(stats.chunkIdString());
        }
    }

    public synchronized void enqueue(String chunkKey) {
        String key = WorldPos.optionalId(chunkKey);
        if (!key.isBlank() && queuedChunkKeySet.add(key)) {
            queuedChunkKeys.add(key);
        }
    }

    public synchronized int size() {
        return queuedChunkKeys.size();
    }

    public synchronized ProcessResult process(
            WorldMemoryStorageData data,
            LivingLegendsConfig config,
            long gameTime,
            Consumer<String> debugLogger
    ) {
        return process(data, config, gameTime, debugLogger, null, null);
    }

    public synchronized ProcessResult process(
            WorldMemoryStorageData data,
            LivingLegendsConfig config,
            long gameTime,
            Consumer<String> debugLogger,
            BiomeResolver biomeResolver,
            Consumer<NamedPlace> createdPlaceListener
    ) {
        Objects.requireNonNull(data, "data");
        if (config == null) {
            return new ProcessResult(0, 0);
        }

        int limit = Math.max(0, config.performance.maxScoreEvaluationsPerTick);
        int processed = 0;
        int namedPlaceChanges = 0;
        while (processed < limit && !queuedChunkKeys.isEmpty()) {
            String chunkKey = queuedChunkKeys.poll();
            queuedChunkKeySet.remove(chunkKey);

            ChunkMemoryStats stats = data.chunkStatsByKey().get(chunkKey);
            if (stats == null) {
                continue;
            }

            EvaluationResult result = evaluateDirtyChunk(
                    stats,
                    data,
                    config,
                    Math.max(0L, gameTime),
                    debugLogger,
                    biomeResolver,
                    createdPlaceListener
            );
            namedPlaceChanges += result.namedPlaceChanges();
            processed++;
        }

        trimCandidateCooldowns();
        return new ProcessResult(processed, namedPlaceChanges);
    }

    public synchronized String debugClusters() {
        if (recentClusterDebugLines.isEmpty()) {
            return "World Remembers recent clusters: none";
        }
        return "World Remembers recent clusters:\n" + String.join("\n", recentClusterDebugLines);
    }

    private EvaluationResult evaluateDirtyChunk(
            ChunkMemoryStats stats,
            WorldMemoryStorageData data,
            LivingLegendsConfig config,
            long gameTime,
            Consumer<String> debugLogger,
            BiomeResolver biomeResolver,
            Consumer<NamedPlace> createdPlaceListener
    ) {
        boolean debugEnabled = config.debug.enabled && debugLogger != null;
        boolean namingVerboseEnabled = config.debug.namingVerbose && debugLogger != null;
        boolean candidateCooldownActive = candidateCooldownActive(stats.chunkIdString(), gameTime, config);
        int candidateCount = 0;
        int namedPlaceChanges = 0;
        List<PlaceClusterer.ClusterBuildResult> clusterResults = new ArrayList<>();
        NamedPlaceCreator.Result namedPlaceResult = null;

        for (ScoreEngine.ScoreEvaluation evaluation : ScoreEngine.evaluateChunk(stats, data, config)) {
            if (debugEnabled) {
                debugLogger.accept(ScoreEngine.formatEvaluationLog(evaluation));
            }

            if (!PlaceClusterer.isClusterSeed(evaluation)) {
                continue;
            }

            if (candidateCooldownActive) {
                continue;
            }

            if (debugEnabled && evaluation.candidate()) {
                debugLogger.accept(ScoreEngine.formatCandidateLog(evaluation));
            }

            PlaceClusterer.ClusterBuildResult clusterResult = PlaceClusterer.buildCluster(evaluation, stats, data, config);
            if (clusterResult != null) {
                clusterResults.add(clusterResult);
                if (clusterResult.cluster() != null && clusterResult.cluster().candidate()) {
                    candidateCount++;
                }
                rememberClusterLine(PlaceClusterer.formatClusterDetailsLog(clusterResult));
                if (debugEnabled) {
                    debugLogger.accept(PlaceClusterer.formatClusterDetailsLog(clusterResult));
                }
            }
        }

        PlaceClusterer.ClusterSelection selection = PlaceClusterer.selectDominantCluster(clusterResults, data, config);
        if (!candidateCooldownActive && selection.selected() != null && selection.selected().cluster() != null) {
            PlaceCluster selectedCluster = enrichClusterWithBiome(selection.selected().cluster(), config, biomeResolver, debugLogger, debugEnabled);
            lastCandidateGenerationTickByChunk.put(stats.chunkIdString(), gameTime);
            NamedPlaceCreator.Result result = NamedPlaceCreator.createOrUpdate(
                    selectedCluster,
                    data,
                    config,
                    gameTime
            );
            namedPlaceResult = result;
            if (result.changed()) {
                namedPlaceChanges++;
            }
            if (result.created() && result.place() != null && createdPlaceListener != null) {
                createdPlaceListener.accept(result.place());
            }
            for (String debugLine : result.debugLines()) {
                rememberClusterLine(debugLine);
                if (debugEnabled || (namingVerboseEnabled && isNameGeneratorDiagnosticLine(debugLine))) {
                    debugLogger.accept(debugLine);
                }
            }
            if (debugEnabled) {
                debugLogger.accept(PlaceClusterer.formatClusterLog(selectedCluster));
            }
        }
        String selectionLog = PlaceClusterer.formatSelectionLog(selection, namedPlaceResult);
        if (!candidateCooldownActive && debugEnabled && !clusterResults.isEmpty()) {
            debugLogger.accept(selectionLog);
        }
        if (!candidateCooldownActive && !clusterResults.isEmpty()) {
            rememberClusterLine(selectionLog);
        }

        if (candidateCooldownActive && debugEnabled) {
            debugLogger.accept("ScoreEngine candidate generation skipped by cooldown"
                    + " chunk=" + stats.chunkIdString()
                    + " candidateCount=" + clusterResults.size()
                    + " remainingTicks=" + candidateCooldownRemainingTicks(stats.chunkIdString(), gameTime, config));
        }

        if (debugEnabled) {
            debugLogger.accept("Scored dirty world memory chunk "
                    + stats.chunkIdString()
                    + " score=" + ScoreEngine.scoreChunk(stats)
                    + " events=" + stats.eventCount()
                    + " candidates=" + candidateCount
                    + " queuedChunks=" + queuedChunkKeys.size());
        }

        return new EvaluationResult(namedPlaceChanges);
    }

    private static boolean isNameGeneratorDiagnosticLine(String debugLine) {
        return debugLine != null
                && (debugLine.startsWith("NameGenerator diagnostics:")
                || debugLine.startsWith("rejected patternKey="));
    }

    private PlaceCluster enrichClusterWithBiome(
            PlaceCluster cluster,
            LivingLegendsConfig config,
            BiomeResolver biomeResolver,
            Consumer<String> debugLogger,
            boolean debugEnabled
    ) {
        if (cluster == null
                || cluster.placeType() != PlaceType.GENERAL_LANDMARK
                || config == null
                || config.biomeThemes == null
                || !config.biomeThemes.enabled
                || !config.biomeThemes.useBiomeForGeneralLandmarks) {
            return cluster;
        }

        BiomeMetadata metadata = biomeResolver == null
                ? BiomeMetadata.fromDimensionFallback(cluster.dimensionId())
                : biomeResolver.resolve(cluster.dimensionId(), cluster.centerX(), cluster.centerY(), cluster.centerZ());
        PlaceCluster enriched = cluster.withBiome(metadata);
        if (debugEnabled && debugLogger != null) {
            debugLogger.accept("GENERAL_LANDMARK biome context:"
                    + " biomeId=" + enriched.biomeId()
                    + " dominantBiomeId=" + enriched.dominantBiomeId()
                    + " biomeGroup=" + enriched.biomeGroup()
                    + " biomeTheme=" + enriched.biomeTheme()
                    + " biomeSource=" + enriched.biomeSource());
        }
        return enriched;
    }

    private boolean candidateCooldownActive(String chunkKey, long gameTime, LivingLegendsConfig config) {
        return candidateCooldownRemainingTicks(chunkKey, gameTime, config) > 0L;
    }

    private long candidateCooldownRemainingTicks(String chunkKey, long gameTime, LivingLegendsConfig config) {
        long cooldownTicks = Math.max(0L, config.performance.candidateGenerationCooldownTicks);
        if (cooldownTicks == 0L) {
            return 0L;
        }

        long lastCandidateTick = lastCandidateGenerationTickByChunk.getOrDefault(chunkKey, Long.MIN_VALUE);
        if (lastCandidateTick == Long.MIN_VALUE) {
            return 0L;
        }

        long elapsed = Math.max(0L, gameTime - lastCandidateTick);
        return Math.max(0L, cooldownTicks - elapsed);
    }

    private void trimCandidateCooldowns() {
        if (lastCandidateGenerationTickByChunk.size() <= 4096) {
            return;
        }

        int entriesToRemove = lastCandidateGenerationTickByChunk.size() - 4096;
        var iterator = lastCandidateGenerationTickByChunk.keySet().iterator();
        while (entriesToRemove > 0 && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
            entriesToRemove--;
        }
    }

    private void rememberClusterLine(String line) {
        String normalized = line == null ? "" : line.trim();
        if (normalized.isBlank()) {
            return;
        }
        recentClusterDebugLines.add(normalized);
        while (recentClusterDebugLines.size() > MAX_RECENT_CLUSTER_DEBUG_LINES) {
            recentClusterDebugLines.remove(0);
        }
    }

    private record EvaluationResult(int namedPlaceChanges) {
    }

    public record ProcessResult(int processedChunks, int namedPlaceChanges) {
    }

    @FunctionalInterface
    public interface BiomeResolver {
        BiomeMetadata resolve(String dimensionId, int x, int y, int z);
    }
}
