package com.worldremembers.livinglegends;

import com.worldremembers.livinglegends.config.LivingLegendsConfig;

public final class PlaceGenerationLimits {
    private static final int CHUNK_SIZE = 16;

    private PlaceGenerationLimits() {
    }

    public static int maxMergeDistanceBlocks(PlaceType placeType, LivingLegendsConfig config) {
        LivingLegendsConfig.Generation generation = generation(config);
        return switch (placeType == null ? PlaceType.UNKNOWN : placeType) {
            case DEATH_SITE -> generation.deathSiteMaxMergeDistanceBlocks;
            case GENERAL_LANDMARK -> generation.generalLandmarkMaxMergeDistanceBlocks;
            case PORTAL_LANDMARK, DIMENSION_THRESHOLD -> generation.portalLandmarkMaxMergeDistanceBlocks;
            case FIRST_DISCOVERY, BOSS_SITE, PET_MEMORIAL, NAMED_MOB_MEMORIAL, RAID_SITE ->
                    generation.pointPlaceMaxMergeDistanceBlocks;
            default -> generation.placeMaxMergeDistanceBlocks;
        };
    }

    public static int maxClusterRadiusBlocks(PlaceType placeType, LivingLegendsConfig config) {
        LivingLegendsConfig.Generation generation = generation(config);
        return switch (placeType == null ? PlaceType.UNKNOWN : placeType) {
            case DEATH_SITE -> generation.deathSiteMaxClusterRadiusBlocks;
            case GENERAL_LANDMARK -> generation.generalLandmarkMaxClusterRadiusBlocks;
            case PORTAL_LANDMARK, DIMENSION_THRESHOLD -> generation.portalLandmarkMaxClusterRadiusBlocks;
            case FIRST_DISCOVERY, BOSS_SITE, PET_MEMORIAL, NAMED_MOB_MEMORIAL, RAID_SITE ->
                    generation.pointPlaceMaxClusterRadiusBlocks;
            default -> generation.placeMaxClusterRadiusBlocks;
        };
    }

    public static int maxCenterShiftBlocks(PlaceType placeType, LivingLegendsConfig config) {
        LivingLegendsConfig.Generation generation = generation(config);
        return switch (placeType == null ? PlaceType.UNKNOWN : placeType) {
            case DEATH_SITE -> generation.deathSiteMaxCenterShiftOnUpdateBlocks;
            case GENERAL_LANDMARK -> generation.generalLandmarkMaxCenterShiftOnUpdateBlocks;
            case PORTAL_LANDMARK, DIMENSION_THRESHOLD -> generation.portalLandmarkMaxCenterShiftOnUpdateBlocks;
            case FIRST_DISCOVERY, BOSS_SITE, PET_MEMORIAL, NAMED_MOB_MEMORIAL, RAID_SITE ->
                    generation.pointPlaceMaxCenterShiftOnUpdateBlocks;
            default -> generation.placeMaxCenterShiftOnUpdateBlocks;
        };
    }

    public static int maxSearchRadiusChunks(PlaceType placeType, LivingLegendsConfig config) {
        if (placeType == PlaceType.FIRST_DISCOVERY) {
            return 0;
        }
        return Math.max(0, maxClusterRadiusBlocks(placeType, config) / CHUNK_SIZE);
    }

    public static boolean spacingEnabled(LivingLegendsConfig config) {
        LivingLegendsConfig.Generation.Spacing spacing = spacing(config);
        return spacing != null && spacing.enabled;
    }

    public static int sameTypeMinDistanceBlocks(PlaceType placeType, LivingLegendsConfig config) {
        LivingLegendsConfig.Generation.Spacing spacing = spacing(config);
        return spacing == null ? 0 : Math.max(0, spacing.sameTypeMinDistanceBlocks(placeType));
    }

    public static int anyPlaceMinDistanceBlocks(PlaceType placeType, LivingLegendsConfig config) {
        LivingLegendsConfig.Generation.Spacing spacing = spacing(config);
        return spacing == null ? 0 : Math.max(0, spacing.anyPlaceMinDistanceBlocks(placeType));
    }

    public static int spacingMergeDistanceBlocks(PlaceType placeType, LivingLegendsConfig config) {
        LivingLegendsConfig.Generation.Spacing spacing = spacing(config);
        return spacing == null ? 0 : Math.max(0, spacing.mergeDistanceBlocks(placeType));
    }

    private static LivingLegendsConfig.Generation generation(LivingLegendsConfig config) {
        return (config == null ? LivingLegendsConfig.defaults() : config).generation;
    }

    private static LivingLegendsConfig.Generation.Spacing spacing(LivingLegendsConfig config) {
        LivingLegendsConfig.Generation generation = generation(config);
        if (generation.spacing == null) {
            return new LivingLegendsConfig.Generation.Spacing();
        }
        return generation.spacing;
    }
}
