package com.worldremembers.livinglegends;

import com.worldremembers.livinglegends.config.LivingLegendsConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ScoreEngine {
    private static final int CHUNK_SIZE = 16;

    private static final List<PlaceType> ALPHA_CANDIDATE_TYPES = List.of(
            PlaceType.DEATH_SITE,
            PlaceType.BATTLEFIELD,
            PlaceType.SLAUGHTER_FIELD,
            PlaceType.PVP_ARENA,
            PlaceType.MINING_SITE,
            PlaceType.PORTAL_LANDMARK,
            PlaceType.GENERAL_LANDMARK,
            PlaceType.SETTLEMENT,
            PlaceType.FIRST_DISCOVERY,
            PlaceType.BOSS_SITE,
            PlaceType.PET_MEMORIAL,
            PlaceType.NAMED_MOB_MEMORIAL,
            PlaceType.RAID_SITE,
            PlaceType.DIMENSION_THRESHOLD
    );

    private ScoreEngine() {
    }

    public static double scoreChunk(ChunkMemoryStats stats) {
        return stats == null ? 0.0 : stats.basicScore();
    }

    public static double scoreChunk(ChunkMemoryStats stats, PlaceType placeType) {
        return scoreChunk(stats, placeType, null);
    }

    public static double scoreChunk(
            ChunkMemoryStats stats,
            PlaceType placeType,
            WorldMemoryStorageData data
    ) {
        if (stats == null) {
            return 0.0;
        }

        double rawScore = scoreChunkRaw(stats, placeType, data);
        return data == null ? rawScore : data.effectiveCandidateScore(stats, placeType, rawScore);
    }

    public static double scoreChunkRaw(
            ChunkMemoryStats stats,
            PlaceType placeType,
            WorldMemoryStorageData data
    ) {
        if (stats == null) {
            return 0.0;
        }

        PlaceType resolvedType = placeType == null ? PlaceType.UNKNOWN : placeType;
        return switch (resolvedType) {
            case DEATH_SITE -> deathSiteScore(stats);
            case BATTLEFIELD -> battlefieldScore(stats);
            case SLAUGHTER_FIELD -> slaughterFieldScore(stats);
            case PVP_ARENA -> pvpArenaScore(stats);
            case MINING_SITE -> miningSiteScore(stats);
            case PORTAL_LANDMARK -> portalLandmarkScore(stats);
            case SETTLEMENT -> settlementScore(stats);
            case GENERAL_LANDMARK -> stats.eventCount() * 1.25
                    + stats.visitCount() * 2.0
                    + stats.deathCount() * 1.5
                    + stats.combatEventCount()
                    + stats.buildEventCount() * 0.75
                    + stats.totalImportance() * 0.35;
            case FIRST_DISCOVERY -> firstDiscoveryScore(stats, data);
            case BOSS_SITE -> stats.eventTypeCount(EventType.BOSS_KILLED) * 30.0;
            case PET_MEMORIAL -> stats.eventTypeCount(EventType.PET_DIED) * 20.0;
            case NAMED_MOB_MEMORIAL -> stats.eventTypeCount(EventType.NAMED_MOB_DIED) * 20.0;
            case RAID_SITE -> stats.eventTypeCount(EventType.RAID_WON) * 40.0;
            case DIMENSION_THRESHOLD -> stats.eventTypeCount(EventType.PLAYER_FIRST_ENTERED_DIMENSION) * 30.0
                    + stats.eventTypeCount(EventType.PLAYER_ENTERED_DIMENSION) * 6.0;
            default -> scoreChunk(stats);
        };
    }

    public static double candidateAdjustedScore(
            ChunkMemoryStats stats,
            PlaceType placeType,
            double rawScore,
            WorldMemoryStorageData data
    ) {
        return data == null ? rawScore : data.effectiveCandidateScore(stats, placeType, rawScore);
    }

    public static double scorePlace(PlaceStats stats) {
        return stats == null ? 0.0 : stats.basicScore();
    }

    public static double scorePlace(PlaceStats stats, PlaceType placeType) {
        if (stats == null) {
            return 0.0;
        }

        PlaceType resolvedType = placeType == null ? PlaceType.UNKNOWN : placeType;
        return switch (resolvedType) {
            case DEATH_SITE -> stats.eventTypeCount(EventType.PLAYER_DEATH) * 25.0;
            case BATTLEFIELD -> battlefieldScore(stats);
            case SLAUGHTER_FIELD -> stats.eventTypeCount(EventType.PLAYER_KILLED_PASSIVE_MOB) * 12.0;
            case PVP_ARENA -> stats.eventTypeCount(EventType.PLAYER_KILLED_PLAYER) * 18.0;
            case MINING_SITE -> stats.eventTypeCount(EventType.VALUABLE_BLOCK_MINED) * 20.0;
            case PORTAL_LANDMARK -> stats.eventTypeCount(EventType.NETHER_PORTAL_USED) * 12.0
                    + stats.eventTypeCount(EventType.END_PORTAL_USED) * 18.0
                    + stats.eventTypeCount(EventType.PLAYER_ENTERED_DIMENSION) * 5.0
                    + stats.eventTypeCount(EventType.PLAYER_FIRST_ENTERED_DIMENSION) * 12.0;
            case SETTLEMENT -> stats.eventTypeCount(EventType.PLAYER_BLOCK_PLACED)
                    + stats.eventTypeCount(EventType.RESPAWN_POINT_SET) * 60.0;
            case GENERAL_LANDMARK -> stats.eventCount() * 1.25
                    + stats.visitCount() * 2.0
                    + stats.deathCount() * 1.5
                    + stats.combatEventCount()
                    + stats.buildEventCount() * 0.75
                    + stats.totalImportance() * 0.35;
            case FIRST_DISCOVERY -> stats.eventTypeCount(EventType.DISCOVERY) * 28.0;
            case BOSS_SITE -> stats.eventTypeCount(EventType.BOSS_KILLED) * 30.0;
            case PET_MEMORIAL -> stats.eventTypeCount(EventType.PET_DIED) * 20.0;
            case NAMED_MOB_MEMORIAL -> stats.eventTypeCount(EventType.NAMED_MOB_DIED) * 20.0;
            case RAID_SITE -> stats.eventTypeCount(EventType.RAID_WON) * 40.0;
            case DIMENSION_THRESHOLD -> stats.eventTypeCount(EventType.PLAYER_FIRST_ENTERED_DIMENSION) * 30.0
                    + stats.eventTypeCount(EventType.PLAYER_ENTERED_DIMENSION) * 6.0;
            default -> scorePlace(stats);
        };
    }

    public static List<PlaceCandidate> findCandidates(
            WorldMemoryEvent event,
            ChunkMemoryStats stats,
            WorldMemoryStorageData data,
            LivingLegendsConfig config
    ) {
        if (stats == null || config == null || !config.generation.enabled) {
            return List.of();
        }

        List<PlaceCandidate> candidates = new ArrayList<>();
        for (ScoreEvaluation evaluation : evaluateChunk(stats, data, config)) {
            if (!evaluation.candidate()) {
                continue;
            }

            candidates.add(new PlaceCandidate(
                    candidateId(evaluation.placeType(), stats),
                    evaluation.placeType(),
                    stats.chunkIdString(),
                    candidateBounds(stats, config),
                    evaluation.score(),
                    evaluation.threshold(),
                    evaluation.activityCount(),
                    rarityFor(evaluation.score(), config),
                    reason(evaluation.placeType(), event, evaluation.activityCount())
            ));
        }

        candidates.sort(Comparator.comparingDouble(PlaceCandidate::score).reversed());
        return List.copyOf(candidates);
    }

    public static List<ScoreEvaluation> evaluateChunk(
            ChunkMemoryStats stats,
            WorldMemoryStorageData data,
            LivingLegendsConfig config
    ) {
        if (stats == null || config == null) {
            return List.of();
        }

        List<ScoreEvaluation> evaluations = new ArrayList<>();
        for (PlaceType placeType : ALPHA_CANDIDATE_TYPES) {
            if (placeType == PlaceType.DEATH_SITE && !config.generation.allowMixedDeathSiteEnvironments) {
                addStrictDeathSiteEvaluations(evaluations, stats, data, config);
                continue;
            }

            double score = scoreChunk(stats, placeType, data);
            double threshold = scoreThresholdFor(placeType, config);
            long activityCount = activityCount(stats, placeType, data, config);
            long minimumActivityCount = requiredRawCountFor(placeType, config);
            boolean thresholdReached = score >= threshold;
            boolean rawCountReached = activityCount >= minimumActivityCount;
            boolean placeTypeEnabled = placeTypeAutoGenerationEnabled(placeType, config);
            boolean candidate = thresholdReached && rawCountReached && config.generation.enabled && placeTypeEnabled;
            String rejectionReason = rejectionReason(thresholdReached, rawCountReached, config, placeTypeEnabled);
            DeathSiteEnvironment environment = environmentFor(placeType, stats, data);
            evaluations.add(new ScoreEvaluation(
                    placeType,
                    stats.dimensionId(),
                    stats.chunkX(),
                    stats.chunkZ(),
                    environment,
                    score,
                    threshold,
                    activityCount,
                    minimumActivityCount,
                    candidate,
                    rejectionReason,
                    topContributingStats(placeType, stats, data, environment)
            ));
        }

        return List.copyOf(evaluations);
    }

    private static void addStrictDeathSiteEvaluations(
            List<ScoreEvaluation> evaluations,
            ChunkMemoryStats stats,
            WorldMemoryStorageData data,
            LivingLegendsConfig config
    ) {
        for (DeathSiteEnvironment environment : DeathSiteEnvironment.values()) {
            if (environment == DeathSiteEnvironment.UNKNOWN) {
                continue;
            }

            long activityCount = deathSiteActivityCount(stats, environment);
            if (activityCount <= 0L) {
                continue;
            }

            double score = candidateAdjustedScore(stats, PlaceType.DEATH_SITE, deathSiteScore(stats, environment), data);
            double threshold = scoreThresholdFor(PlaceType.DEATH_SITE, config);
            long minimumActivityCount = requiredRawCountFor(PlaceType.DEATH_SITE, config);
            boolean thresholdReached = score >= threshold;
            boolean rawCountReached = activityCount >= minimumActivityCount;
            boolean placeTypeEnabled = placeTypeAutoGenerationEnabled(PlaceType.DEATH_SITE, config);
            boolean candidate = thresholdReached && rawCountReached && config.generation.enabled && placeTypeEnabled;
            String rejectionReason = rejectionReason(thresholdReached, rawCountReached, config, placeTypeEnabled);
            evaluations.add(new ScoreEvaluation(
                    PlaceType.DEATH_SITE,
                    stats.dimensionId(),
                    stats.chunkX(),
                    stats.chunkZ(),
                    environment,
                    score,
                    threshold,
                    activityCount,
                    minimumActivityCount,
                    candidate,
                    rejectionReason,
                    topContributingStats(PlaceType.DEATH_SITE, stats, data, environment)
            ));
        }

        if (evaluations.stream().noneMatch(evaluation ->
                evaluation.placeType() == PlaceType.DEATH_SITE
                        && evaluation.dimensionId().equals(stats.dimensionId())
                        && evaluation.chunkX() == stats.chunkX()
                        && evaluation.chunkZ() == stats.chunkZ())
                && stats.eventTypeCount(EventType.PLAYER_DEATH) > 0L
                && stats.deathSiteEnvironmentCounts().isEmpty()) {
            DeathSiteEnvironment environment = environmentFor(PlaceType.DEATH_SITE, stats, data);
            if (environment != DeathSiteEnvironment.UNKNOWN
                    && config.generation.allowUnknownDeathSiteEnvironmentFallback) {
                long activityCount = stats.eventTypeCount(EventType.PLAYER_DEATH);
                double score = candidateAdjustedScore(
                        stats,
                        PlaceType.DEATH_SITE,
                        activityCount * 25.0 + stats.totalImportance() * 0.1,
                        data
                );
                double threshold = scoreThresholdFor(PlaceType.DEATH_SITE, config);
                long minimumActivityCount = requiredRawCountFor(PlaceType.DEATH_SITE, config);
                boolean thresholdReached = score >= threshold;
                boolean rawCountReached = activityCount >= minimumActivityCount;
                evaluations.add(new ScoreEvaluation(
                        PlaceType.DEATH_SITE,
                        stats.dimensionId(),
                        stats.chunkX(),
                        stats.chunkZ(),
                        environment,
                        score,
                        threshold,
                        activityCount,
                        minimumActivityCount,
                        thresholdReached && rawCountReached && config.generation.enabled
                                && placeTypeAutoGenerationEnabled(PlaceType.DEATH_SITE, config),
                        rejectionReason(
                                thresholdReached,
                                rawCountReached,
                                config,
                                placeTypeAutoGenerationEnabled(PlaceType.DEATH_SITE, config)
                        ),
                        topContributingStats(PlaceType.DEATH_SITE, stats, data, environment)
                ));
            }
        }
    }

    public static List<PlaceType> alphaCandidateTypes() {
        return ALPHA_CANDIDATE_TYPES;
    }

    public static String formatEvaluationLog(ScoreEvaluation evaluation) {
        return "ScoreEngine evaluated:"
                + " type=" + evaluation.placeType().name()
                + " score=" + formatScore(evaluation.score())
                + " scoreThreshold=" + formatScore(evaluation.threshold())
                + " candidate=" + evaluation.candidate()
                + environmentLogPart(evaluation)
                + " reason=" + evaluation.reason()
                + " actualRawCount=" + evaluation.activityCount()
                + " requiredRawCount=" + requiredRawCountLabel(evaluation)
                + " dimension=" + evaluation.dimensionId()
                + " chunkX=" + evaluation.chunkX()
                + " chunkZ=" + evaluation.chunkZ()
                + " top=" + evaluation.topContributingStats();
    }

    public static String formatCandidateLog(ScoreEvaluation evaluation) {
        return "ScoreEngine candidate: type=" + evaluation.placeType().name()
                + " score=" + formatScore(evaluation.score())
                + " scoreThreshold=" + formatScore(evaluation.threshold())
                + " chunk=" + chunkDebugId(evaluation.dimensionId(), evaluation.chunkX(), evaluation.chunkZ());
    }

    public static String formatCommandLine(ScoreEvaluation evaluation) {
        return evaluation.placeType().name()
                + " score=" + formatScore(evaluation.score())
                + " scoreThreshold=" + formatScore(evaluation.threshold())
                + " candidate=" + evaluation.candidate()
                + environmentLogPart(evaluation)
                + " reason=" + evaluation.reason()
                + " actualRawCount=" + evaluation.activityCount()
                + " requiredRawCount=" + requiredRawCountLabel(evaluation)
                + " top=" + evaluation.topContributingStats();
    }

    public static double scoreThresholdFor(PlaceType placeType, LivingLegendsConfig config) {
        if (config.debug.useTestingThresholds) {
            double testingThreshold = testingScoreThresholdFor(placeType);
            if (testingThreshold >= 0.0) {
                return testingThreshold;
            }
        }

        return switch (placeType) {
            case DEATH_SITE -> config.scoreThresholds.deathSite;
            case BATTLEFIELD -> config.scoreThresholds.battlefield;
            case SLAUGHTER_FIELD -> config.scoreThresholds.slaughterField;
            case PVP_ARENA -> config.scoreThresholds.pvpArena;
            case MINING_SITE -> config.scoreThresholds.miningSite;
            case PORTAL_LANDMARK -> config.scoreThresholds.portalLandmark;
            case GENERAL_LANDMARK -> config.scoreThresholds.generalLandmark;
            case SETTLEMENT -> config.scoreThresholds.settlement;
            case FIRST_DISCOVERY -> config.scoreThresholds.firstDiscovery;
            case BOSS_SITE -> config.scoreThresholds.bossSite;
            case PET_MEMORIAL -> config.scoreThresholds.petMemorial;
            case NAMED_MOB_MEMORIAL -> config.scoreThresholds.namedMobMemorial;
            case RAID_SITE -> config.scoreThresholds.raidSite;
            case DIMENSION_THRESHOLD -> config.scoreThresholds.dimensionThreshold;
            default -> config.thresholds.namedPlaceScore;
        };
    }

    public static long requiredRawCountFor(PlaceType placeType, LivingLegendsConfig config) {
        if (config.debug.useTestingThresholds) {
            long testingCount = testingRequiredRawCountFor(placeType);
            if (testingCount > 0L) {
                return testingCount;
            }
        }

        return switch (placeType) {
            case DEATH_SITE -> Math.max(1L, config.requiredCounts.playerDeathsForDeathSite);
            case BATTLEFIELD -> Math.max(1L, config.requiredCounts.hostileKillsForBattlefield);
            case SLAUGHTER_FIELD -> Math.max(1L, config.requiredCounts.passiveKillsForSlaughterField);
            case PVP_ARENA -> Math.max(1L, config.requiredCounts.pvpDeathsForPvpArena);
            case MINING_SITE -> Math.max(1L, config.requiredCounts.valuableBlocksForMiningSite);
            case PORTAL_LANDMARK -> Math.max(1L, config.requiredCounts.portalUsesForLandmark);
            case GENERAL_LANDMARK -> Math.max(1L, config.requiredCounts.visitsForGeneralLandmark);
            case SETTLEMENT -> Math.max(1L, config.requiredCounts.blocksPlacedForSettlementCandidate);
            case FIRST_DISCOVERY -> Math.max(1L, config.requiredCounts.firstMajorDiscovery);
            case BOSS_SITE -> Math.max(1L, config.requiredCounts.bossKillsForBossSite);
            case PET_MEMORIAL -> Math.max(1L, config.requiredCounts.petDeathsForPetMemorial);
            case NAMED_MOB_MEMORIAL -> Math.max(1L, config.requiredCounts.namedMobDeathsForMemorial);
            case RAID_SITE -> Math.max(1L, config.requiredCounts.raidWinsForRaidSite);
            case DIMENSION_THRESHOLD -> Math.max(1L, config.requiredCounts.firstDimensionEntryForDiscovery);
            default -> 0L;
        };
    }

    public static long activityCount(
            ChunkMemoryStats stats,
            PlaceType placeType,
            WorldMemoryStorageData data,
            LivingLegendsConfig config
    ) {
        return switch (placeType) {
            case DEATH_SITE -> stats.eventTypeCount(EventType.PLAYER_DEATH);
            case BATTLEFIELD -> battlefieldRawCombatCount(stats);
            case SLAUGHTER_FIELD -> stats.eventTypeCount(EventType.PLAYER_KILLED_PASSIVE_MOB);
            case PVP_ARENA -> stats.eventTypeCount(EventType.PLAYER_KILLED_PLAYER);
            case MINING_SITE -> stats.eventTypeCount(EventType.VALUABLE_BLOCK_MINED);
            case PORTAL_LANDMARK -> stats.eventTypeCount(EventType.NETHER_PORTAL_USED)
                    + stats.eventTypeCount(EventType.END_PORTAL_USED)
                    + stats.eventTypeCount(EventType.PLAYER_ENTERED_DIMENSION)
                    + stats.eventTypeCount(EventType.PLAYER_FIRST_ENTERED_DIMENSION);
            case SETTLEMENT -> settlementRawCount(stats, config);
            case FIRST_DISCOVERY -> firstDiscoveryCount(stats, data);
            case BOSS_SITE -> stats.eventTypeCount(EventType.BOSS_KILLED);
            case PET_MEMORIAL -> stats.eventTypeCount(EventType.PET_DIED);
            case NAMED_MOB_MEMORIAL -> stats.eventTypeCount(EventType.NAMED_MOB_DIED);
            case RAID_SITE -> stats.eventTypeCount(EventType.RAID_WON);
            case DIMENSION_THRESHOLD -> stats.eventTypeCount(EventType.PLAYER_FIRST_ENTERED_DIMENSION);
            case GENERAL_LANDMARK -> stats.visitCount();
            default -> 0L;
        };
    }

    public static long activityCount(PlaceStats stats, PlaceType placeType, LivingLegendsConfig config) {
        if (stats == null) {
            return 0L;
        }

        return switch (placeType == null ? PlaceType.UNKNOWN : placeType) {
            case DEATH_SITE -> stats.eventTypeCount(EventType.PLAYER_DEATH);
            case BATTLEFIELD -> battlefieldRawCombatCount(stats);
            case SLAUGHTER_FIELD -> stats.eventTypeCount(EventType.PLAYER_KILLED_PASSIVE_MOB);
            case PVP_ARENA -> stats.eventTypeCount(EventType.PLAYER_KILLED_PLAYER);
            case MINING_SITE -> stats.eventTypeCount(EventType.VALUABLE_BLOCK_MINED);
            case PORTAL_LANDMARK -> stats.eventTypeCount(EventType.NETHER_PORTAL_USED)
                    + stats.eventTypeCount(EventType.END_PORTAL_USED)
                    + stats.eventTypeCount(EventType.PLAYER_ENTERED_DIMENSION)
                    + stats.eventTypeCount(EventType.PLAYER_FIRST_ENTERED_DIMENSION);
            case SETTLEMENT -> settlementRawCount(stats, config);
            case FIRST_DISCOVERY -> stats.eventTypeCount(EventType.DISCOVERY);
            case BOSS_SITE -> stats.eventTypeCount(EventType.BOSS_KILLED);
            case PET_MEMORIAL -> stats.eventTypeCount(EventType.PET_DIED);
            case NAMED_MOB_MEMORIAL -> stats.eventTypeCount(EventType.NAMED_MOB_DIED);
            case RAID_SITE -> stats.eventTypeCount(EventType.RAID_WON);
            case DIMENSION_THRESHOLD -> stats.eventTypeCount(EventType.PLAYER_FIRST_ENTERED_DIMENSION);
            case GENERAL_LANDMARK -> stats.visitCount();
            default -> 0L;
        };
    }

    private static String topContributingStats(
            PlaceType placeType,
            ChunkMemoryStats stats,
            WorldMemoryStorageData data
    ) {
        return topContributingStats(placeType, stats, data, DeathSiteEnvironment.UNKNOWN);
    }

    private static String topContributingStats(
            PlaceType placeType,
            ChunkMemoryStats stats,
            WorldMemoryStorageData data,
            DeathSiteEnvironment environment
    ) {
        return switch (placeType) {
            case DEATH_SITE -> "player_death=" + deathSiteActivityCount(stats, environment)
                    + ",candidateEnvironment=" + environment.name()
                    + ",environmentCounts=" + stats.deathSiteEnvironmentCounts()
                    + ",totalImportance=" + formatScore(stats.totalImportance());
            case BATTLEFIELD -> "hostile_kills=" + stats.eventTypeCount(EventType.PLAYER_KILLED_HOSTILE_MOB)
                    + ",neutral_kills=" + stats.eventTypeCount(EventType.PLAYER_KILLED_NEUTRAL_MOB)
                    + ",pvpDeaths=" + stats.eventTypeCount(EventType.PLAYER_KILLED_PLAYER)
                    + ",player_deaths=" + stats.eventTypeCount(EventType.PLAYER_DEATH)
                    + ",boss_killed=" + stats.eventTypeCount(EventType.BOSS_KILLED)
                    + ",raid_won=" + stats.eventTypeCount(EventType.RAID_WON);
            case SLAUGHTER_FIELD -> "passive_kills=" + stats.eventTypeCount(EventType.PLAYER_KILLED_PASSIVE_MOB)
                    + ",totalImportance=" + formatScore(stats.totalImportance());
            case PVP_ARENA -> "pvpDeaths=" + stats.eventTypeCount(EventType.PLAYER_KILLED_PLAYER)
                    + ",totalImportance=" + formatScore(stats.totalImportance());
            case MINING_SITE -> "valuable_blocks=" + stats.eventTypeCount(EventType.VALUABLE_BLOCK_MINED)
                    + ",buildEvents=" + stats.buildEventCount()
                    + ",totalImportance=" + formatScore(stats.totalImportance());
            case PORTAL_LANDMARK -> "nether_portal=" + stats.eventTypeCount(EventType.NETHER_PORTAL_USED)
                    + ",end_portal=" + stats.eventTypeCount(EventType.END_PORTAL_USED)
                    + ",dimension_entry=" + stats.eventTypeCount(EventType.PLAYER_ENTERED_DIMENSION)
                    + ",first_dimension_entry=" + stats.eventTypeCount(EventType.PLAYER_FIRST_ENTERED_DIMENSION);
            case GENERAL_LANDMARK -> "events=" + stats.eventCount()
                    + ",visits=" + stats.visitCount()
                    + ",deaths=" + stats.deathCount()
                    + ",combat=" + stats.combatEventCount()
                    + ",build=" + stats.buildEventCount();
            case SETTLEMENT -> "blocks_placed=" + stats.eventTypeCount(EventType.PLAYER_BLOCK_PLACED)
                    + ",respawn_points=" + stats.eventTypeCount(EventType.RESPAWN_POINT_SET)
                    + ",totalImportance=" + formatScore(stats.totalImportance());
            case FIRST_DISCOVERY -> "firstDiscoveries=" + firstDiscoveryCount(stats, data)
                    + ",firstDiscoveryKeys=" + firstDiscoveryKeys(stats, data)
                    + ",discovery=" + stats.eventTypeCount(EventType.DISCOVERY);
            case BOSS_SITE -> "boss_killed=" + stats.eventTypeCount(EventType.BOSS_KILLED);
            case PET_MEMORIAL -> "pet_died=" + stats.eventTypeCount(EventType.PET_DIED);
            case NAMED_MOB_MEMORIAL -> "named_mob_died=" + stats.eventTypeCount(EventType.NAMED_MOB_DIED);
            case RAID_SITE -> "raid_won=" + stats.eventTypeCount(EventType.RAID_WON);
            case DIMENSION_THRESHOLD -> "first_dimension_entry=" + stats.eventTypeCount(EventType.PLAYER_FIRST_ENTERED_DIMENSION)
                    + ",dimension_entry=" + stats.eventTypeCount(EventType.PLAYER_ENTERED_DIMENSION);
            default -> "events=" + stats.eventCount()
                    + ",totalImportance=" + formatScore(stats.totalImportance());
        };
    }

    private static double deathSiteScore(ChunkMemoryStats stats) {
        long playerDeaths = stats.eventTypeCount(EventType.PLAYER_DEATH);
        if (playerDeaths <= 0L) {
            return 0.0;
        }

        return playerDeaths * 25.0 + stats.totalImportance() * 0.1;
    }

    public static double deathSiteScore(ChunkMemoryStats stats, DeathSiteEnvironment environment) {
        long playerDeaths = deathSiteActivityCount(stats, environment);
        if (playerDeaths <= 0L) {
            return 0.0;
        }

        long totalDeaths = Math.max(1L, stats.eventTypeCount(EventType.PLAYER_DEATH));
        double importanceShare = playerDeaths / (double) totalDeaths;
        return playerDeaths * 25.0 + stats.totalImportance() * 0.1 * importanceShare;
    }

    public static long deathSiteActivityCount(ChunkMemoryStats stats, DeathSiteEnvironment environment) {
        if (stats == null) {
            return 0L;
        }

        DeathSiteEnvironment resolvedEnvironment = environment == null ? DeathSiteEnvironment.UNKNOWN : environment;
        if (resolvedEnvironment == DeathSiteEnvironment.UNKNOWN) {
            return stats.eventTypeCount(EventType.PLAYER_DEATH);
        }
        return stats.deathSiteEnvironmentCount(resolvedEnvironment);
    }

    private static double slaughterFieldScore(ChunkMemoryStats stats) {
        long passiveKills = stats.eventTypeCount(EventType.PLAYER_KILLED_PASSIVE_MOB);
        if (passiveKills <= 0L) {
            return 0.0;
        }

        return passiveKills * 12.0 + stats.totalImportance() * 0.05;
    }

    private static double pvpArenaScore(ChunkMemoryStats stats) {
        long pvpKills = stats.eventTypeCount(EventType.PLAYER_KILLED_PLAYER);
        if (pvpKills <= 0L) {
            return 0.0;
        }

        return pvpKills * 18.0 + stats.totalImportance() * 0.1;
    }

    private static double miningSiteScore(ChunkMemoryStats stats) {
        long valuableBlocks = stats.eventTypeCount(EventType.VALUABLE_BLOCK_MINED);
        if (valuableBlocks <= 0L) {
            return 0.0;
        }

        return valuableBlocks * 20.0
                + stats.buildEventCount() * 0.25
                + stats.totalImportance() * 0.05;
    }

    private static double settlementScore(ChunkMemoryStats stats) {
        long blocksPlaced = stats.eventTypeCount(EventType.PLAYER_BLOCK_PLACED);
        long respawnPoints = stats.eventTypeCount(EventType.RESPAWN_POINT_SET);
        if (blocksPlaced <= 0L && respawnPoints <= 0L) {
            return 0.0;
        }

        return blocksPlaced
                + respawnPoints * 60.0
                + stats.totalImportance() * 0.1;
    }

    private static double portalLandmarkScore(ChunkMemoryStats stats) {
        long portalActivity = portalRawCount(stats);
        if (portalActivity <= 0L) {
            return 0.0;
        }

        return stats.eventTypeCount(EventType.NETHER_PORTAL_USED) * 12.0
                + stats.eventTypeCount(EventType.END_PORTAL_USED) * 18.0
                + stats.eventTypeCount(EventType.PLAYER_ENTERED_DIMENSION) * 5.0
                + stats.eventTypeCount(EventType.PLAYER_FIRST_ENTERED_DIMENSION) * 12.0
                + stats.totalImportance() * 0.05;
    }

    private static double firstDiscoveryScore(ChunkMemoryStats stats, WorldMemoryStorageData data) {
        long firstDiscoveries = firstDiscoveryCount(stats, data);
        if (firstDiscoveries <= 0L) {
            return 0.0;
        }

        return firstDiscoveries * 28.0
                + firstDiscoveryWeight(stats, data)
                + stats.eventTypeCount(EventType.DISCOVERY) * 4.0;
    }

    private static double battlefieldScore(ChunkMemoryStats stats) {
        return stats.eventTypeCount(EventType.PLAYER_KILLED_HOSTILE_MOB) * 7.0
                + stats.eventTypeCount(EventType.PLAYER_KILLED_NEUTRAL_MOB) * 6.0
                + stats.eventTypeCount(EventType.PLAYER_KILLED_PLAYER) * 10.0
                + stats.eventTypeCount(EventType.PLAYER_DEATH) * 8.0
                + stats.eventTypeCount(EventType.BOSS_KILLED) * 12.0
                + stats.eventTypeCount(EventType.RAID_WON) * 10.0;
    }

    private static double battlefieldScore(PlaceStats stats) {
        return stats.eventTypeCount(EventType.PLAYER_KILLED_HOSTILE_MOB) * 7.0
                + stats.eventTypeCount(EventType.PLAYER_KILLED_NEUTRAL_MOB) * 6.0
                + stats.eventTypeCount(EventType.PLAYER_KILLED_PLAYER) * 10.0
                + stats.eventTypeCount(EventType.PLAYER_DEATH) * 8.0
                + stats.eventTypeCount(EventType.BOSS_KILLED) * 12.0
                + stats.eventTypeCount(EventType.RAID_WON) * 10.0;
    }

    private static long battlefieldRawCombatCount(ChunkMemoryStats stats) {
        return stats.eventTypeCount(EventType.PLAYER_KILLED_HOSTILE_MOB)
                + stats.eventTypeCount(EventType.PLAYER_KILLED_NEUTRAL_MOB)
                + stats.eventTypeCount(EventType.PLAYER_KILLED_PLAYER)
                + stats.eventTypeCount(EventType.PLAYER_DEATH)
                + stats.eventTypeCount(EventType.BOSS_KILLED)
                + stats.eventTypeCount(EventType.RAID_WON);
    }

    private static long battlefieldRawCombatCount(PlaceStats stats) {
        return stats.eventTypeCount(EventType.PLAYER_KILLED_HOSTILE_MOB)
                + stats.eventTypeCount(EventType.PLAYER_KILLED_NEUTRAL_MOB)
                + stats.eventTypeCount(EventType.PLAYER_KILLED_PLAYER)
                + stats.eventTypeCount(EventType.PLAYER_DEATH)
                + stats.eventTypeCount(EventType.BOSS_KILLED)
                + stats.eventTypeCount(EventType.RAID_WON);
    }

    private static long portalRawCount(ChunkMemoryStats stats) {
        return stats.eventTypeCount(EventType.NETHER_PORTAL_USED)
                + stats.eventTypeCount(EventType.END_PORTAL_USED)
                + stats.eventTypeCount(EventType.PLAYER_ENTERED_DIMENSION)
                + stats.eventTypeCount(EventType.PLAYER_FIRST_ENTERED_DIMENSION);
    }

    private static long settlementRawCount(ChunkMemoryStats stats, LivingLegendsConfig config) {
        long blocksPlaced = stats.eventTypeCount(EventType.PLAYER_BLOCK_PLACED);
        long respawnPoints = stats.eventTypeCount(EventType.RESPAWN_POINT_SET);
        long requiredBlocks = Math.max(1L, config.requiredCounts.blocksPlacedForSettlementCandidate);
        long requiredRespawns = Math.max(1L, config.requiredCounts.respawnPointsForSettlementCandidate);
        long scaledRespawns = Math.floorDiv(respawnPoints * requiredBlocks, requiredRespawns);
        return Math.max(blocksPlaced, scaledRespawns);
    }

    private static long settlementRawCount(PlaceStats stats, LivingLegendsConfig config) {
        long blocksPlaced = stats.eventTypeCount(EventType.PLAYER_BLOCK_PLACED);
        long respawnPoints = stats.eventTypeCount(EventType.RESPAWN_POINT_SET);
        long requiredBlocks = Math.max(1L, config.requiredCounts.blocksPlacedForSettlementCandidate);
        long requiredRespawns = Math.max(1L, config.requiredCounts.respawnPointsForSettlementCandidate);
        long scaledRespawns = Math.floorDiv(respawnPoints * requiredBlocks, requiredRespawns);
        return Math.max(blocksPlaced, scaledRespawns);
    }

    private static long firstDiscoveryCount(ChunkMemoryStats stats, WorldMemoryStorageData data) {
        if (data == null) {
            return 0L;
        }

        Set<String> eventIds = new LinkedHashSet<>();
        collectFirstDiscoveryIds(stats, data.firstDiscoveries().values(), eventIds);
        return eventIds.size();
    }

    private static void collectFirstDiscoveryIds(
            ChunkMemoryStats stats,
            Iterable<WorldFirstDiscoveryRecord> discoveries,
            Set<String> eventIds
    ) {
        for (WorldFirstDiscoveryRecord discovery : discoveries) {
            if (discovery != null && discovery.matches(stats)) {
                eventIds.add(discovery.discoveryIdString());
            }
        }
    }

    private static double firstDiscoveryWeight(ChunkMemoryStats stats, WorldMemoryStorageData data) {
        if (data == null) {
            return 0.0;
        }

        double weight = 0.0;
        for (WorldFirstDiscoveryRecord discovery : data.firstDiscoveries().values()) {
            if (discovery != null && discovery.matches(stats)) {
                weight += discovery.weight();
            }
        }
        return weight;
    }

    private static String firstDiscoveryKeys(ChunkMemoryStats stats, WorldMemoryStorageData data) {
        if (data == null) {
            return "[]";
        }

        List<String> keys = new ArrayList<>();
        for (WorldFirstDiscoveryRecord discovery : data.firstDiscoveries().values()) {
            if (discovery != null && discovery.matches(stats)) {
                keys.add(discovery.discoveryIdString());
            }
        }
        return keys.toString();
    }

    private static PlaceBounds candidateBounds(ChunkMemoryStats stats, LivingLegendsConfig config) {
        WorldPos center = new WorldPos(
                stats.dimensionId(),
                stats.chunkX() * CHUNK_SIZE + CHUNK_SIZE / 2,
                64,
                stats.chunkZ() * CHUNK_SIZE + CHUNK_SIZE / 2
        );
        return PlaceBounds.around(
                center,
                config.generation.defaultHorizontalRadiusBlocks,
                config.generation.defaultVerticalRadiusBlocks
        );
    }

    private static String candidateId(PlaceType placeType, ChunkMemoryStats stats) {
        return placeType.idString() + ":" + stats.chunkIdString();
    }

    private static String chunkDebugId(String dimensionId, int chunkX, int chunkZ) {
        return dimensionId + "@" + chunkX + "," + chunkZ;
    }

    private static String rejectionReason(
            boolean thresholdReached,
            boolean rawCountReached,
            LivingLegendsConfig config,
            boolean placeTypeEnabled
    ) {
        if (thresholdReached && !rawCountReached) {
            return "raw_count_below_required";
        }
        if (!thresholdReached) {
            return "below_score_threshold";
        }
        if (!rawCountReached) {
            return "raw_count_below_required";
        }
        if (!config.generation.enabled) {
            return "generation_disabled";
        }
        if (!placeTypeEnabled) {
            return "place_type_disabled";
        }
        return "none";
    }

    private static boolean placeTypeAutoGenerationEnabled(PlaceType placeType, LivingLegendsConfig config) {
        return config == null
                || config.placeTypes == null
                || config.placeTypes.autoGenerationEnabled(placeType);
    }

    private static long testingRequiredRawCountFor(PlaceType placeType) {
        return switch (placeType) {
            case DEATH_SITE -> 1L;
            case BATTLEFIELD -> 3L;
            case SLAUGHTER_FIELD -> 3L;
            case PVP_ARENA -> 2L;
            case MINING_SITE -> 2L;
            case PORTAL_LANDMARK -> 3L;
            case GENERAL_LANDMARK -> 4L;
            default -> 0L;
        };
    }

    private static double testingScoreThresholdFor(PlaceType placeType) {
        return switch (placeType) {
            case DEATH_SITE -> 20.0;
            case BATTLEFIELD -> 20.0;
            case SLAUGHTER_FIELD -> 20.0;
            case PVP_ARENA -> 30.0;
            case MINING_SITE -> 30.0;
            case PORTAL_LANDMARK -> 30.0;
            case GENERAL_LANDMARK -> 8.0;
            default -> -1.0;
        };
    }

    private static String requiredRawCountLabel(ScoreEvaluation evaluation) {
        return Long.toString(evaluation.minimumActivityCount());
    }

    private static DeathSiteEnvironment environmentFor(
            PlaceType placeType,
            ChunkMemoryStats stats,
            WorldMemoryStorageData data
    ) {
        if (placeType != PlaceType.DEATH_SITE || stats == null) {
            return DeathSiteEnvironment.UNKNOWN;
        }

        DeathSiteEnvironment primaryEnvironment = stats.primaryDeathSiteEnvironment();
        if (primaryEnvironment != DeathSiteEnvironment.UNKNOWN) {
            return primaryEnvironment;
        }

        DeathSiteEnvironment storedEnvironment = storedDeathEnvironment(stats, data);
        if (storedEnvironment != DeathSiteEnvironment.UNKNOWN) {
            return storedEnvironment;
        }

        if ("minecraft:the_nether".equals(stats.dimensionId())) {
            return DeathSiteEnvironment.NETHER;
        }
        if ("minecraft:the_end".equals(stats.dimensionId())) {
            return DeathSiteEnvironment.END;
        }
        return DeathSiteEnvironment.UNKNOWN;
    }

    private static DeathSiteEnvironment storedDeathEnvironment(ChunkMemoryStats stats, WorldMemoryStorageData data) {
        if (data == null) {
            return DeathSiteEnvironment.UNKNOWN;
        }

        DeathSiteEnvironment environment = storedDeathEnvironment(stats, data.firstWorldEvents().values());
        if (environment != DeathSiteEnvironment.UNKNOWN) {
            return environment;
        }
        return storedDeathEnvironment(stats, data.firstPlayerWorldEvents().values());
    }

    private static DeathSiteEnvironment storedDeathEnvironment(
            ChunkMemoryStats stats,
            Iterable<WorldMemoryEvent> events
    ) {
        for (WorldMemoryEvent event : events) {
            if (event != null && event.eventType() == EventType.PLAYER_DEATH && stats.matches(event)) {
                return DeathSiteEnvironment.classify(event);
            }
        }
        return DeathSiteEnvironment.UNKNOWN;
    }

    private static String environmentLogPart(ScoreEvaluation evaluation) {
        if (evaluation.placeType() != PlaceType.DEATH_SITE) {
            return "";
        }

        return " environment=" + evaluation.environment().name();
    }

    private static String formatScore(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }

        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 0.000_001) {
            return Long.toString((long) rounded);
        }

        String formatted = String.format(Locale.ROOT, "%.2f", value);
        while (formatted.contains(".") && formatted.endsWith("0")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        if (formatted.endsWith(".")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        return formatted;
    }

    private static PlaceRarity rarityFor(double score, LivingLegendsConfig config) {
        if (score >= config.thresholds.mythicPlaceScore) {
            return PlaceRarity.MYTHIC;
        }
        if (score >= config.thresholds.legendaryPlaceScore) {
            return PlaceRarity.LEGENDARY;
        }
        if (score >= config.thresholds.rarePlaceScore) {
            return PlaceRarity.RARE;
        }
        if (score >= config.thresholds.namedPlaceScore) {
            return PlaceRarity.NOTABLE;
        }
        return PlaceRarity.COMMON;
    }

    private static String reason(PlaceType placeType, WorldMemoryEvent event, long activityCount) {
        String lastType = event == null ? "unknown" : event.eventType().idString();
        return "placeType=" + placeType.idString()
                + " activity=" + activityCount
                + " lastEvent=" + lastType;
    }

    public record ScoreEvaluation(
            PlaceType placeType,
            String dimensionId,
            int chunkX,
            int chunkZ,
            DeathSiteEnvironment environment,
            double score,
            double threshold,
            long activityCount,
            long minimumActivityCount,
            boolean candidate,
            String reason,
            String topContributingStats
    ) {
        public ScoreEvaluation {
            placeType = placeType == null ? PlaceType.UNKNOWN : placeType;
            dimensionId = WorldPos.requireId(dimensionId, "dimensionId");
            environment = environment == null ? DeathSiteEnvironment.UNKNOWN : environment;
            score = Math.max(0.0, score);
            threshold = Math.max(0.0, threshold);
            activityCount = Math.max(0L, activityCount);
            minimumActivityCount = Math.max(0L, minimumActivityCount);
            reason = reason == null || reason.isBlank() ? "none" : reason;
            topContributingStats = topContributingStats == null ? "" : topContributingStats;
        }

        public String chunkDebugId() {
            return dimensionId + "@" + chunkX + "," + chunkZ;
        }
    }
}
