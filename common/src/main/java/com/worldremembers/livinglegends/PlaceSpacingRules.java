package com.worldremembers.livinglegends;

import com.worldremembers.livinglegends.config.LivingLegendsConfig;

import java.util.Locale;

public final class PlaceSpacingRules {
    private PlaceSpacingRules() {
    }

    public static GeneralLandmarkSpacingResult analyzeGeneralLandmark(
            String dimensionId,
            int x,
            int y,
            int z,
            WorldMemoryStorageData data,
            LivingLegendsConfig config
    ) {
        WorldPos candidateCenter = new WorldPos(dimensionId, x, y, z);
        LivingLegendsConfig.Generation.Spacing spacing = spacing(config);
        int sameTypeMinDistance = PlaceGenerationLimits.sameTypeMinDistanceBlocks(PlaceType.GENERAL_LANDMARK, config);
        int anyPlaceMinDistance = PlaceGenerationLimits.anyPlaceMinDistanceBlocks(PlaceType.GENERAL_LANDMARK, config);
        int mergeDistance = PlaceGenerationLimits.spacingMergeDistanceBlocks(PlaceType.GENERAL_LANDMARK, config);

        if (spacing == null || !spacing.enabled) {
            return new GeneralLandmarkSpacingResult(
                    GeneralLandmarkSpacingDecision.DISABLED,
                    candidateCenter,
                    null,
                    Double.POSITIVE_INFINITY,
                    null,
                    Double.POSITIVE_INFINITY,
                    sameTypeMinDistance,
                    anyPlaceMinDistance,
                    mergeDistance
            );
        }

        NamedPlace nearestSameType = null;
        double nearestSameTypeDistance = Double.POSITIVE_INFINITY;
        NamedPlace nearestAnyPlace = null;
        double nearestAnyPlaceDistance = Double.POSITIVE_INFINITY;

        if (data != null) {
            for (NamedPlace place : data.namedPlaces()) {
                if (place == null || place.center() == null || !dimensionId.equals(place.dimensionId())) {
                    continue;
                }

                double distance = horizontalDistance(place.center(), candidateCenter);
                if (distance < nearestAnyPlaceDistance) {
                    nearestAnyPlace = place;
                    nearestAnyPlaceDistance = distance;
                }
                if (place.placeType() == PlaceType.GENERAL_LANDMARK && distance < nearestSameTypeDistance) {
                    nearestSameType = place;
                    nearestSameTypeDistance = distance;
                }
            }
        }

        GeneralLandmarkSpacingDecision decision = GeneralLandmarkSpacingDecision.ALLOWED;
        if (nearestSameType != null && mergeDistance > 0 && nearestSameTypeDistance <= mergeDistance) {
            decision = GeneralLandmarkSpacingDecision.MERGE_NEARBY;
        } else if (nearestSameType != null && sameTypeMinDistance > 0 && nearestSameTypeDistance <= sameTypeMinDistance) {
            decision = GeneralLandmarkSpacingDecision.SUPPRESS_SAME_TYPE;
        } else if (nearestAnyPlace != null && anyPlaceMinDistance > 0 && nearestAnyPlaceDistance <= anyPlaceMinDistance) {
            decision = GeneralLandmarkSpacingDecision.SUPPRESS_ANY_PLACE;
        }

        return new GeneralLandmarkSpacingResult(
                decision,
                candidateCenter,
                nearestSameType,
                nearestSameTypeDistance,
                nearestAnyPlace,
                nearestAnyPlaceDistance,
                sameTypeMinDistance,
                anyPlaceMinDistance,
                mergeDistance
        );
    }

    private static LivingLegendsConfig.Generation.Spacing spacing(LivingLegendsConfig config) {
        LivingLegendsConfig resolved = config == null ? LivingLegendsConfig.defaults() : config;
        return resolved.generation == null ? LivingLegendsConfig.defaults().generation.spacing : resolved.generation.spacing;
    }

    private static double horizontalDistance(WorldPos first, WorldPos second) {
        if (first == null || second == null || !first.sameDimension(second)) {
            return Double.POSITIVE_INFINITY;
        }
        long dx = (long) first.x() - second.x();
        long dz = (long) first.z() - second.z();
        return Math.sqrt((double) dx * dx + (double) dz * dz);
    }

    public enum GeneralLandmarkSpacingDecision {
        DISABLED,
        ALLOWED,
        MERGE_NEARBY,
        SUPPRESS_SAME_TYPE,
        SUPPRESS_ANY_PLACE
    }

    public record GeneralLandmarkSpacingResult(
            GeneralLandmarkSpacingDecision decision,
            WorldPos candidateCenter,
            NamedPlace nearestSameType,
            double nearestSameTypeDistance,
            NamedPlace nearestAnyPlace,
            double nearestAnyPlaceDistance,
            int sameTypeMinDistanceBlocks,
            int anyPlaceMinDistanceBlocks,
            int mergeDistanceBlocks
    ) {
        public GeneralLandmarkSpacingResult {
            decision = decision == null ? GeneralLandmarkSpacingDecision.ALLOWED : decision;
        }

        public boolean suppressed() {
            return decision == GeneralLandmarkSpacingDecision.SUPPRESS_SAME_TYPE
                    || decision == GeneralLandmarkSpacingDecision.SUPPRESS_ANY_PLACE;
        }

        public boolean merges() {
            return decision == GeneralLandmarkSpacingDecision.MERGE_NEARBY;
        }

        public NamedPlace blockingPlace() {
            return switch (decision) {
                case MERGE_NEARBY, SUPPRESS_SAME_TYPE -> nearestSameType;
                case SUPPRESS_ANY_PLACE -> nearestAnyPlace;
                default -> null;
            };
        }

        public String reason() {
            return switch (decision) {
                case DISABLED -> "general_landmark_spacing_disabled";
                case ALLOWED -> "general_landmark_spacing_allowed";
                case MERGE_NEARBY -> "general_landmark_merged_nearby";
                case SUPPRESS_SAME_TYPE -> "general_landmark_too_close_same_type";
                case SUPPRESS_ANY_PLACE -> "general_landmark_too_close_any_named_place";
            };
        }

        public String resultLabel() {
            return switch (decision) {
                case DISABLED -> "disabled";
                case ALLOWED -> "allowed";
                case MERGE_NEARBY -> "merge";
                case SUPPRESS_SAME_TYPE, SUPPRESS_ANY_PLACE -> "suppress";
            };
        }

        public String debugDetails() {
            return "candidateCenter=" + (candidateCenter == null ? "none" : candidateCenter.blockIdString())
                    + " nearestSameType=" + placeSummary(nearestSameType)
                    + " nearestSameTypeDistance=" + formatDistance(nearestSameTypeDistance)
                    + " nearestAnyPlace=" + placeSummary(nearestAnyPlace)
                    + " nearestAnyPlaceDistance=" + formatDistance(nearestAnyPlaceDistance)
                    + " sameTypeMinDistanceBlocks=" + sameTypeMinDistanceBlocks
                    + " anyPlaceMinDistanceBlocks=" + anyPlaceMinDistanceBlocks
                    + " mergeDistanceBlocks=" + mergeDistanceBlocks
                    + " result=" + resultLabel()
                    + " reason=" + reason();
        }

        private static String placeSummary(NamedPlace place) {
            if (place == null) {
                return "none";
            }
            return place.placeIdString()
                    + "/" + place.placeType().name()
                    + "@" + place.centerString();
        }

        private static String formatDistance(double distance) {
            if (Double.isInfinite(distance)) {
                return "none";
            }
            double rounded = Math.rint(distance);
            if (Math.abs(distance - rounded) < 0.000_001) {
                return Long.toString((long) rounded);
            }
            return String.format(Locale.ROOT, "%.1f", distance);
        }
    }
}
