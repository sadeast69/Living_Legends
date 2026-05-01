package com.worldremembers.livinglegends;

import com.worldremembers.livinglegends.config.LivingLegendsConfig;

import java.util.List;
import java.util.Locale;

public final class CandidateDecayEngine {
    private CandidateDecayEngine() {
    }

    public static List<PlaceType> relevantPlaceTypes(WorldMemoryEvent event) {
        if (event == null) {
            return List.of();
        }

        return switch (event.eventType()) {
            case PLAYER_DEATH -> List.of(PlaceType.DEATH_SITE, PlaceType.GENERAL_LANDMARK);
            case PLAYER_KILLED_HOSTILE_MOB, PLAYER_KILLED_NEUTRAL_MOB, PLAYER_KILL_HOSTILE_MOB, PLAYER_KILL_NEUTRAL_MOB ->
                    List.of(PlaceType.BATTLEFIELD, PlaceType.GENERAL_LANDMARK);
            case PLAYER_KILLED_PASSIVE_MOB, PLAYER_KILL_PASSIVE_MOB ->
                    List.of(PlaceType.SLAUGHTER_FIELD, PlaceType.GENERAL_LANDMARK);
            case PLAYER_KILLED_PLAYER, PLAYER_KILL_PLAYER ->
                    List.of(PlaceType.PVP_ARENA, PlaceType.BATTLEFIELD, PlaceType.GENERAL_LANDMARK);
            case VALUABLE_BLOCK_MINED -> List.of(PlaceType.MINING_SITE, PlaceType.GENERAL_LANDMARK);
            case NETHER_PORTAL_USED, END_PORTAL_USED, PLAYER_ENTERED_DIMENSION ->
                    List.of(PlaceType.PORTAL_LANDMARK, PlaceType.GENERAL_LANDMARK);
            case PLAYER_FIRST_ENTERED_DIMENSION ->
                    List.of(PlaceType.DIMENSION_THRESHOLD, PlaceType.PORTAL_LANDMARK, PlaceType.FIRST_DISCOVERY, PlaceType.GENERAL_LANDMARK);
            case PLAYER_BLOCK_PLACED, BLOCK_PLACED, RESPAWN_POINT_SET, STRUCTURE_BUILT ->
                    List.of(PlaceType.SETTLEMENT, PlaceType.GENERAL_LANDMARK);
            case DISCOVERY, STRUCTURE_DISCOVERED ->
                    List.of(PlaceType.FIRST_DISCOVERY, PlaceType.GENERAL_LANDMARK);
            case BOSS_KILLED, BOSS_KILL -> List.of(PlaceType.BOSS_SITE, PlaceType.BATTLEFIELD);
            case RAID_WON -> List.of(PlaceType.RAID_SITE, PlaceType.BATTLEFIELD, PlaceType.GENERAL_LANDMARK);
            case PET_DIED -> List.of(PlaceType.PET_MEMORIAL);
            case NAMED_MOB_DIED -> List.of(PlaceType.NAMED_MOB_MEMORIAL);
            case VISIT, PLAYER_VISIT -> List.of(PlaceType.GENERAL_LANDMARK);
            default -> List.of(PlaceType.GENERAL_LANDMARK);
        };
    }

    public static DecaySummary run(
            WorldMemoryStorageData data,
            LivingLegendsConfig config,
            long currentGameTime,
            long simulatedTicks
    ) {
        if (data == null || config == null || config.candidateDecay == null || !config.candidateDecay.enabled) {
            return DecaySummary.disabled();
        }

        long effectiveTime = safeAdd(currentGameTime, Math.max(0L, simulatedTicks));
        DecaySummary summary = DecaySummary.empty(effectiveTime);
        for (ChunkMemoryStats stats : data.chunkStatsByKey().values()) {
            ChunkMemoryStats updatedStats = stats;
            boolean changedStats = false;
            for (PlaceType placeType : ScoreEngine.alphaCandidateTypes()) {
                double multiplier = config.candidateDecay.typeMultiplier(placeType);
                if (multiplier <= 0.0) {
                    continue;
                }

                double rawScore = ScoreEngine.scoreChunkRaw(updatedStats, placeType, data);
                CandidateDecayState state = updatedStats.candidateDecayState(placeType);
                if (rawScore <= 0.0 && state == null) {
                    continue;
                }

                summary = summary.withChecked();
                if (data.candidateActivityProtected(updatedStats.dimensionId(), updatedStats.chunkX(), updatedStats.chunkZ(), placeType)) {
                    summary = summary.withProtectedNamedPlace();
                    continue;
                }

                CandidateDecayState workingState = state == null
                        ? new CandidateDecayState(rawScore, updatedStats.lastEventGameTime(), updatedStats.lastEventGameTime())
                        : state;
                long lastRelevant = workingState.lastRelevantEventGameTime();
                long graceEnd = safeAdd(lastRelevant, Math.max(0L, config.candidateDecay.gracePeriodTicks));
                if (effectiveTime < graceEnd) {
                    if (state == null && rawScore > 0.0) {
                        updatedStats = updatedStats.withCandidateDecayState(placeType, workingState);
                        changedStats = true;
                    }
                    summary = summary.withSkippedGrace();
                    continue;
                }

                long decayStart = Math.max(workingState.lastDecayGameTime(), graceEnd);
                long elapsed = Math.max(0L, effectiveTime - decayStart);
                long intervals = elapsed / Math.max(1L, config.candidateDecay.intervalTicks);
                if (intervals <= 0L) {
                    if (state == null && rawScore > 0.0) {
                        updatedStats = updatedStats.withCandidateDecayState(placeType, workingState);
                        changedStats = true;
                    }
                    continue;
                }

                double oldScore = state == null ? rawScore : Math.min(rawScore <= 0.0 ? state.score() : rawScore, state.score());
                double scoreLoss = intervals * config.candidateDecay.baseDecayPerInterval * multiplier;
                double newScore = Math.max(config.candidateDecay.minCandidateScore, oldScore - scoreLoss);
                boolean pruned = false;
                if (config.candidateDecay.pruneBelowScore && newScore <= config.candidateDecay.pruneThreshold) {
                    newScore = config.candidateDecay.minCandidateScore;
                    pruned = true;
                }

                long lastDecay = safeAdd(decayStart, intervals * Math.max(1L, config.candidateDecay.intervalTicks));
                CandidateDecayState decayedState = new CandidateDecayState(newScore, lastRelevant, lastDecay);
                updatedStats = updatedStats.withCandidateDecayState(placeType, decayedState);
                changedStats = true;
                summary = summary.withDecayed(Math.max(0.0, oldScore - newScore), pruned);
            }

            if (changedStats) {
                data.replaceChunkStats(updatedStats);
            }
        }

        return summary;
    }

    public static String status(LivingLegendsConfig config) {
        LivingLegendsConfig.CandidateDecay decay = config == null ? null : config.candidateDecay;
        if (decay == null) {
            return "Candidate decay config unavailable";
        }
        return "Candidate decay status"
                + " enabled=" + decay.enabled
                + " intervalTicks=" + decay.intervalTicks
                + " gracePeriodTicks=" + decay.gracePeriodTicks
                + " baseDecayPerInterval=" + formatScore(decay.baseDecayPerInterval)
                + " minCandidateScore=" + formatScore(decay.minCandidateScore)
                + " pruneBelowScore=" + decay.pruneBelowScore
                + " pruneThreshold=" + formatScore(decay.pruneThreshold)
                + " debugLogging=" + decay.debugLogging;
    }

    public static String chunkInfo(
            WorldMemoryStorageData data,
            LivingLegendsConfig config,
            String dimensionId,
            int chunkX,
            int chunkZ,
            long currentGameTime
    ) {
        if (data == null) {
            return "Candidate decay chunk info unavailable: storage missing";
        }

        ChunkMemoryStats stats = data.chunkStats(dimensionId, chunkX, chunkZ);
        if (stats == null) {
            stats = ChunkMemoryStats.empty(dimensionId, chunkX, chunkZ);
        }

        StringBuilder message = new StringBuilder("Candidate decay chunk ")
                .append(WorldMemoryStorageData.chunkStatsKey(dimensionId, chunkX, chunkZ))
                .append(" lastEventGameTime=").append(stats.lastEventGameTime());
        for (PlaceType placeType : ScoreEngine.alphaCandidateTypes()) {
            double rawScore = ScoreEngine.scoreChunkRaw(stats, placeType, data);
            CandidateDecayState state = stats.candidateDecayState(placeType);
            if (rawScore <= 0.0 && state == null) {
                continue;
            }
            boolean protectedNamedPlace = data.candidateActivityProtected(dimensionId, chunkX, chunkZ, placeType);
            CandidateDecayState projectionState = state == null
                    ? new CandidateDecayState(rawScore, stats.lastEventGameTime(), stats.lastEventGameTime())
                    : state;
            Projection projection = project(projectionState, rawScore, placeType, config, currentGameTime);
            message.append('\n')
                    .append(placeType.name())
                    .append(" rawScore=").append(formatScore(rawScore))
                    .append(" candidateScore=").append(formatScore(state == null ? rawScore : state.score()))
                    .append(" lastRelevantEventGameTime=").append(state == null ? stats.lastEventGameTime() : state.lastRelevantEventGameTime())
                    .append(" lastDecayGameTime=").append(state == null ? 0L : state.lastDecayGameTime())
                    .append(" protectedNamedPlace=").append(protectedNamedPlace)
                    .append(" projectedDecay=").append(formatScore(projection.decayAmount()))
                    .append(" projectedScore=").append(formatScore(projection.projectedScore()));
        }
        return message.toString();
    }

    public static Projection project(
            CandidateDecayState state,
            double rawScore,
            PlaceType placeType,
            LivingLegendsConfig config,
            long currentGameTime
    ) {
        if (config == null || config.candidateDecay == null || !config.candidateDecay.enabled) {
            double score = state == null ? rawScore : state.score();
            return new Projection(0.0, score);
        }
        double multiplier = config.candidateDecay.typeMultiplier(placeType);
        double score = state == null ? rawScore : state.score();
        if (multiplier <= 0.0 || score <= 0.0) {
            return new Projection(0.0, score);
        }
        long lastRelevant = state == null ? 0L : state.lastRelevantEventGameTime();
        long graceEnd = safeAdd(lastRelevant, config.candidateDecay.gracePeriodTicks);
        if (currentGameTime < graceEnd) {
            return new Projection(0.0, score);
        }
        long decayStart = Math.max(state == null ? 0L : state.lastDecayGameTime(), graceEnd);
        long intervals = Math.max(0L, currentGameTime - decayStart) / Math.max(1L, config.candidateDecay.intervalTicks);
        double decayAmount = intervals * config.candidateDecay.baseDecayPerInterval * multiplier;
        double projected = Math.max(config.candidateDecay.minCandidateScore, score - decayAmount);
        if (config.candidateDecay.pruneBelowScore && projected <= config.candidateDecay.pruneThreshold) {
            projected = config.candidateDecay.minCandidateScore;
        }
        return new Projection(Math.max(0.0, score - projected), projected);
    }

    private static long safeAdd(long first, long second) {
        long result = first + second;
        if (((first ^ result) & (second ^ result)) < 0) {
            return Long.MAX_VALUE;
        }
        return result;
    }

    private static String formatScore(double value) {
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 0.000_001) {
            return Long.toString((long) rounded);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public record Projection(double decayAmount, double projectedScore) {
    }

    public record DecaySummary(
            boolean enabled,
            long effectiveGameTime,
            int checked,
            int decayed,
            int protectedNamedPlace,
            int skippedGrace,
            int pruned,
            double totalScoreLost
    ) {
        public static DecaySummary disabled() {
            return new DecaySummary(false, 0L, 0, 0, 0, 0, 0, 0.0);
        }

        public static DecaySummary empty(long effectiveGameTime) {
            return new DecaySummary(true, effectiveGameTime, 0, 0, 0, 0, 0, 0.0);
        }

        public DecaySummary withChecked() {
            return new DecaySummary(enabled, effectiveGameTime, checked + 1, decayed, protectedNamedPlace, skippedGrace, pruned, totalScoreLost);
        }

        public DecaySummary withProtectedNamedPlace() {
            return new DecaySummary(enabled, effectiveGameTime, checked, decayed, protectedNamedPlace + 1, skippedGrace, pruned, totalScoreLost);
        }

        public DecaySummary withSkippedGrace() {
            return new DecaySummary(enabled, effectiveGameTime, checked, decayed, protectedNamedPlace, skippedGrace + 1, pruned, totalScoreLost);
        }

        public DecaySummary withDecayed(double scoreLost, boolean prunedNow) {
            return new DecaySummary(
                    enabled,
                    effectiveGameTime,
                    checked,
                    decayed + 1,
                    protectedNamedPlace,
                    skippedGrace,
                    pruned + (prunedNow ? 1 : 0),
                    totalScoreLost + Math.max(0.0, scoreLost)
            );
        }

        public String logLine() {
            if (!enabled) {
                return "CandidateDecay pass: disabled";
            }
            return "CandidateDecay pass:"
                    + " checked=" + checked
                    + " decayed=" + decayed
                    + " protectedNamedPlace=" + protectedNamedPlace
                    + " skippedGrace=" + skippedGrace
                    + " pruned=" + pruned
                    + " totalScoreLost=" + formatScore(totalScoreLost)
                    + " effectiveGameTime=" + effectiveGameTime;
        }
    }
}
