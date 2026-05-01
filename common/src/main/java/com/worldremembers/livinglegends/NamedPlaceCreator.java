package com.worldremembers.livinglegends;

import com.worldremembers.livinglegends.config.LivingLegendsConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class NamedPlaceCreator {
    private NamedPlaceCreator() {
    }

    public static Result createOrUpdate(
            PlaceCluster cluster,
            WorldMemoryStorageData data,
            LivingLegendsConfig config,
            long gameTime
    ) {
        Objects.requireNonNull(data, "data");
        if (cluster == null || !cluster.candidate()) {
            return Result.noChange("cluster_not_candidate");
        }
        if (config == null || !config.generation.enabled || !config.naming.enabled) {
            return Result.noChange("generation_or_naming_disabled");
        }
        if (config.placeTypes != null && !config.placeTypes.autoGenerationEnabled(cluster.placeType())) {
            return new Result(
                    false,
                    false,
                    false,
                    null,
                    List.of("NamedPlace creation suppressed:"
                            + " reason=place_type_disabled"
                            + " candidateType=" + cluster.placeType().name()),
                    FinalDecision.SUPPRESSED,
                    "place_type_disabled"
            );
        }

        List<String> debugLines = new ArrayList<>();
        DeletedPlaceMarker deletedMarker = data.suppressingDeletedMarker(cluster, config, gameTime);
        if (deletedMarker != null) {
            debugLines.add("NamedPlace creation suppressed:"
                    + " reason=deleted_place_suppressed"
                    + " candidateType=" + cluster.placeType().name()
                    + " candidateEnvironment=" + cluster.environment().name()
                    + " marker=" + deletedMarker.debugString());
            return new Result(false, false, false, null, debugLines, FinalDecision.SUPPRESSED, "deleted_place_suppressed");
        }

        DuplicateAnalysis duplicate = analyzeDuplicate(cluster, data, config);
        debugLines.add(formatDuplicateDecision(cluster, duplicate, config));
        if (!duplicate.debugDetails().isBlank()) {
            debugLines.add("NamedPlace spacing check: " + duplicate.debugDetails());
        }

        if (duplicate.decision() == DuplicateDecision.LOWER_PRIORITY_SUPPRESSED
                || duplicate.decision() == DuplicateDecision.ENVIRONMENT_MISMATCH_SUPPRESSED) {
            if (cluster.placeType() == PlaceType.GENERAL_LANDMARK && duplicate.existingPlace() != null) {
                debugLines.add("GENERAL_LANDMARK suppressed by spacing"
                        + " existingId=" + duplicate.existingPlace().placeIdString()
                        + " existingType=" + duplicate.existingPlace().placeType().name()
                        + " reason=" + duplicate.reason());
            }
            return new Result(false, false, false, null, debugLines, FinalDecision.SUPPRESSED, duplicate.reason());
        }

        if (duplicate.decision() == DuplicateDecision.SAME_TYPE_UPDATE && duplicate.existingPlace() != null) {
            NamedPlace previous = duplicate.existingPlace();
            NamedPlace updated = previous.updatedFromCluster(cluster, Math.max(0L, gameTime), config);
            boolean changed = data.upsertNamedPlace(updated);
            debugLines.add("Updated existing NamedPlace:"
                    + " id=" + updated.placeIdString()
                    + " type=" + updated.placeType().name()
                    + " center=" + updated.centerString()
                    + " radius=" + updated.radius()
                    + metadataLogPart(updated)
                    + " score=" + formatScore(updated.score())
                    + " nameRecipePreserved=" + !config.naming.allowAutoRenameOnCauseShift
                    + " changed=" + changed);
            return new Result(changed, false, true, updated, debugLines, FinalDecision.UPDATED, duplicate.reason());
        }

        if (duplicate.decision() == DuplicateDecision.HIGHER_PRIORITY_CONFLICT && duplicate.existingPlace() != null) {
            debugLines.add("NamedPlace higher-priority candidate will create a new place without mutating lower-priority existing place"
                    + " existingId=" + duplicate.existingPlace().placeIdString()
                    + " existingType=" + duplicate.existingPlace().placeType().name());
        }
        if (duplicate.decision() == DuplicateDecision.ENVIRONMENT_MISMATCH_CREATE_NEW && duplicate.existingPlace() != null) {
            debugLines.add("NamedPlace environment mismatch will create a separate DEATH_SITE"
                    + " existingId=" + duplicate.existingPlace().placeIdString()
                    + " existingEnvironment=" + duplicate.existingPlace().environment().name()
                    + " candidateEnvironment=" + cluster.environment().name());
        }

        long seed = nameSeed(cluster);
        String styleId = NameStyleSelector.selectStyle(cluster, config, seed);
        NameDataPack nameData = BuiltInNameData.packForStyle(styleId, config.naming.enabledStyles, config.naming.allowMixedStyleTokens);
        NameContext liveNameContext = NameContext.from(cluster, cluster.placeType(), cluster.environment(), nameData.styleId());
        boolean collectNameDiagnostics = config.debug.enabled || config.debug.namingVerbose;
        NameGenerationDiagnostics nameDiagnostics = collectNameDiagnostics
                ? new NameGenerationDiagnostics(config.debug.namingVerbose)
                : null;
        NameRecipe recipe = NameGenerator.generate(
                cluster,
                liveNameContext,
                seed,
                nameData,
                NameGenerator.nearbyRecipes(data, cluster, config.naming.duplicateNameAvoidanceRadiusBlocks),
                nameDiagnostics
        ).withoutFallback();
        String placeId = uniquePlaceId(basePlaceId(cluster), data);
        NamedPlace place = NamedPlace.fromCluster(placeId, cluster, recipe, Math.max(0L, gameTime), config);
        boolean changed = data.upsertNamedPlace(place);
        debugLines.add("Live NameContext:"
                + " liveNameContext=true"
                + " placeType=" + liveNameContext.placeType().name()
                + " environment=" + liveNameContext.environment().name()
                + " causeType=" + liveNameContext.causeType().name()
                + " primaryEventType=" + liveNameContext.primaryEventType().idString()
                + " firstDiscoveryKey=" + liveNameContext.firstDiscoveryKey()
                + " discoveryKind=" + liveNameContext.discoveryKind()
                + " structureId=" + liveNameContext.structureId()
                + " blockId=" + liveNameContext.blockId()
                + " entityId=" + liveNameContext.entityId()
                + " bossId=" + liveNameContext.bossId()
                + " dominantMobType=" + liveNameContext.dominantMobType()
                + " dominantHostileMobType=" + liveNameContext.dominantHostileMobType()
                + " dominantPassiveMobType=" + liveNameContext.dominantPassiveMobType()
                + " dominantNeutralMobType=" + liveNameContext.dominantNeutralMobType()
                + " dominantValuableBlock=" + liveNameContext.dominantValuableBlock()
                + " portalType=" + liveNameContext.portalType()
                + " fromDimension=" + liveNameContext.fromDimension()
                + " toDimension=" + liveNameContext.toDimension()
                + " biomeId=" + liveNameContext.biomeId()
                + " dominantBiomeId=" + liveNameContext.dominantBiomeId()
                + " biomeGroup=" + liveNameContext.biomeGroup()
                + " biomeTheme=" + liveNameContext.biomeTheme()
                + " biomeSource=" + liveNameContext.biomeSource()
                + " selectedPatternSource=" + (nameDiagnostics == null ? "not_collected" : nameDiagnostics.selectedPatternSource().name())
                + portalDebugPart(cluster));
        debugLines.add("Generated NameRecipe:"
                + " placeId=" + place.placeIdString()
                + " " + place.cause().debugString()
                + " selectedPatternSource=" + (nameDiagnostics == null ? "not_collected" : nameDiagnostics.selectedPatternSource().name())
                + " " + NameGenerator.debugRecipe(recipe));
        if (nameDiagnostics != null) {
            debugLines.add("NameGenerator diagnostics: " + nameDiagnostics.summary());
            if (config.debug.namingVerbose) {
                debugLines.addAll(nameDiagnostics.rejectionDetails());
            }
        }
        debugLines.add("Created NamedPlace:"
                + " id=" + place.placeIdString()
                + " type=" + place.placeType().name()
                + " environment=" + place.environment().name()
                + " center=" + place.centerString()
                + " radius=" + place.radius()
                + metadataLogPart(place)
                + " score=" + formatScore(place.score())
                + " sourceChunks=" + place.sourceChunks()
                + " changed=" + changed);
        return new Result(changed, true, false, place, debugLines, FinalDecision.CREATED, duplicate.reason());
    }

    private static DuplicateAnalysis analyzeDuplicate(
            PlaceCluster cluster,
            WorldMemoryStorageData data,
            LivingLegendsConfig config
    ) {
        DuplicateAnalysis spacingDuplicate = analyzeGeneralLandmarkSpacing(cluster, data, config);
        if (spacingDuplicate != null) {
            return spacingDuplicate;
        }

        if (cluster.placeType() == PlaceType.GENERAL_LANDMARK) {
            for (NamedPlace place : data.namedPlaces()) {
                if (place != null
                        && place.placeType() != PlaceType.GENERAL_LANDMARK
                        && place.dimensionId().equals(cluster.dimensionId())
                        && overlapsOrNearby(place, cluster, config)) {
                    return new DuplicateAnalysis(
                            DuplicateDecision.LOWER_PRIORITY_SUPPRESSED,
                            place,
                            "specific_place_exists_nearby"
                    );
                }
            }
        }

        NamedPlace lowerPriorityConflict = null;
        NamedPlace deathSiteEnvironmentMismatch = null;
        String deathSiteEnvironmentMismatchReason = null;
        for (NamedPlace place : data.namedPlaces()) {
            if (place == null || !place.dimensionId().equals(cluster.dimensionId())) {
                continue;
            }
            if (!overlapsOrNearby(place, cluster, config)) {
                continue;
            }

            if (place.placeType() == cluster.placeType()) {
                if (cluster.placeType() == PlaceType.DEATH_SITE
                        && !deathSiteEnvironmentsCompatible(place.environment(), cluster.environment(), config)) {
                    int verticalGap = verticalGap(place, cluster);
                    int requiredVerticalGap = verticalSeparationMinYGap(config);
                    if (verticalGap < requiredVerticalGap) {
                        return new DuplicateAnalysis(
                                DuplicateDecision.ENVIRONMENT_MISMATCH_SUPPRESSED,
                                place,
                                "environment_mismatch_vertical_gap_below_min"
                        );
                    }
                    if (deathSiteEnvironmentMismatch == null) {
                        deathSiteEnvironmentMismatch = place;
                        deathSiteEnvironmentMismatchReason = "environment_mismatch_vertical_separation"
                                + " verticalGap=" + verticalGap
                                + " requiredVerticalGap=" + requiredVerticalGap;
                    }
                    continue;
                }
                if (!sameTypeMayMerge(place, cluster, config)) {
                    continue;
                }
                return new DuplicateAnalysis(DuplicateDecision.SAME_TYPE_UPDATE, place, "same_type_nearby");
            }

            if (cluster.placeType() == PlaceType.GENERAL_LANDMARK && place.placeType() != PlaceType.GENERAL_LANDMARK) {
                return new DuplicateAnalysis(
                        DuplicateDecision.LOWER_PRIORITY_SUPPRESSED,
                        place,
                        "specific_place_exists_nearby"
                );
            }

            if (place.placeType().sameOrHigherPriorityThan(cluster.placeType())) {
                return new DuplicateAnalysis(
                        DuplicateDecision.LOWER_PRIORITY_SUPPRESSED,
                        place,
                        "existing_place_has_higher_priority"
                );
            }

            if (cluster.placeType().sameOrHigherPriorityThan(place.placeType())) {
                lowerPriorityConflict = place;
            }
        }

        if (lowerPriorityConflict != null) {
            return new DuplicateAnalysis(
                    DuplicateDecision.HIGHER_PRIORITY_CONFLICT,
                    lowerPriorityConflict,
                    "candidate_has_higher_priority"
            );
        }

        if (deathSiteEnvironmentMismatch != null) {
            return new DuplicateAnalysis(
                    DuplicateDecision.ENVIRONMENT_MISMATCH_CREATE_NEW,
                    deathSiteEnvironmentMismatch,
                    deathSiteEnvironmentMismatchReason
            );
        }

        return new DuplicateAnalysis(DuplicateDecision.CREATE_NEW, null, "no_duplicate_found");
    }

    private static DuplicateAnalysis analyzeGeneralLandmarkSpacing(
            PlaceCluster cluster,
            WorldMemoryStorageData data,
            LivingLegendsConfig config
    ) {
        if (cluster == null || cluster.placeType() != PlaceType.GENERAL_LANDMARK || !PlaceGenerationLimits.spacingEnabled(config)) {
            return null;
        }

        PlaceSpacingRules.GeneralLandmarkSpacingResult result = PlaceSpacingRules.analyzeGeneralLandmark(
                cluster.dimensionId(),
                cluster.centerX(),
                cluster.centerY(),
                cluster.centerZ(),
                data,
                config
        );
        if (result.merges() && result.blockingPlace() != null) {
            return new DuplicateAnalysis(
                    DuplicateDecision.SAME_TYPE_UPDATE,
                    result.blockingPlace(),
                    result.reason(),
                    result.debugDetails()
            );
        }
        if (result.suppressed() && result.blockingPlace() != null) {
            return new DuplicateAnalysis(
                    DuplicateDecision.LOWER_PRIORITY_SUPPRESSED,
                    result.blockingPlace(),
                    result.reason(),
                    result.debugDetails()
            );
        }
        return null;
    }

    private static boolean deathSiteEnvironmentsCompatible(
            DeathSiteEnvironment existingEnvironment,
            DeathSiteEnvironment candidateEnvironment,
            LivingLegendsConfig config
    ) {
        DeathSiteEnvironment existing = existingEnvironment == null ? DeathSiteEnvironment.UNKNOWN : existingEnvironment;
        DeathSiteEnvironment candidate = candidateEnvironment == null ? DeathSiteEnvironment.UNKNOWN : candidateEnvironment;
        if (existing == candidate) {
            return true;
        }
        return config != null
                && config.generation != null
                && config.generation.allowUnknownDeathSiteEnvironmentFallback
                && (existing == DeathSiteEnvironment.UNKNOWN || candidate == DeathSiteEnvironment.UNKNOWN);
    }

    private static boolean overlapsOrNearby(NamedPlace place, PlaceCluster cluster, LivingLegendsConfig config) {
        WorldPos center = new WorldPos(cluster.dimensionId(), cluster.centerX(), cluster.centerY(), cluster.centerZ());
        int mergeDistance = PlaceGenerationLimits.maxMergeDistanceBlocks(cluster.placeType(), config);
        int clusterRadius = Math.min(
                Math.max(8, cluster.radius()),
                PlaceGenerationLimits.maxClusterRadiusBlocks(cluster.placeType(), config)
        );
        int horizontalExpansion = Math.max(clusterRadius, mergeDistance);
        int verticalExpansion = Math.max(
                16,
                Math.max(Math.abs(cluster.maxY() - cluster.minY()), config.generation.defaultVerticalRadiusBlocks)
        );
        PlaceBounds clusterBounds = PlaceBounds.around(
                center,
                clusterRadius,
                verticalExpansion
        );
        if (cluster.bounds() != null) {
            clusterBounds = cluster.bounds();
        }
        PlaceBounds existingBounds = effectiveBounds(place, config);
        return horizontalDistanceSquared(place.center(), center) <= (long) horizontalExpansion * horizontalExpansion
                || existingBounds.expanded(horizontalExpansion, verticalExpansion).contains(center)
                || clusterBounds.expanded(horizontalExpansion, verticalExpansion).contains(place.center());
    }

    private static boolean sameTypeMayMerge(NamedPlace place, PlaceCluster cluster, LivingLegendsConfig config) {
        WorldPos center = new WorldPos(cluster.dimensionId(), cluster.centerX(), cluster.centerY(), cluster.centerZ());
        long maxDistance = Math.max(8L, PlaceGenerationLimits.maxMergeDistanceBlocks(cluster.placeType(), config));
        return horizontalDistanceSquared(place.center(), center) <= maxDistance * maxDistance;
    }

    private static int verticalGap(NamedPlace place, PlaceCluster cluster) {
        if (place == null || place.center() == null || cluster == null) {
            return 0;
        }
        return Math.abs(place.center().y() - cluster.centerY());
    }

    private static int verticalSeparationMinYGap(LivingLegendsConfig config) {
        if (config == null || config.generation == null) {
            return 32;
        }
        return Math.max(0, config.generation.verticalSeparationMinYGap);
    }

    private static PlaceBounds effectiveBounds(NamedPlace place, LivingLegendsConfig config) {
        if (place.bounds() != null && place.bounds().isStructureBounds()) {
            return place.bounds();
        }
        int maxRadius = PlaceGenerationLimits.maxClusterRadiusBlocks(place.placeType(), config);
        int radius = Math.min(Math.max(8, place.radius()), Math.max(8, maxRadius));
        int verticalRadius = Math.max(8, config.generation.defaultVerticalRadiusBlocks);
        return PlaceBounds.around(place.center(), radius, verticalRadius);
    }

    private static long horizontalDistanceSquared(WorldPos first, WorldPos second) {
        if (first == null || second == null || !first.sameDimension(second)) {
            return Long.MAX_VALUE;
        }

        long dx = (long) first.x() - second.x();
        long dz = (long) first.z() - second.z();
        return dx * dx + dz * dz;
    }

    private static String formatDuplicateDecision(
            PlaceCluster cluster,
            DuplicateAnalysis duplicate,
            LivingLegendsConfig config
    ) {
        NamedPlace existing = duplicate.existingPlace();
        String existingType = existing == null ? "none" : existing.placeType().name();
        return "NamedPlace duplicate prevention:"
                + " candidateType=" + cluster.placeType().name()
                + " existingType=" + existingType
                + " candidateEnvironment=" + cluster.environment().name()
                + " existingEnvironment=" + (existing == null ? "none" : existing.environment().name())
                + " verticalGap=" + (existing == null ? "none" : verticalGap(existing, cluster))
                + " requiredVerticalGap=" + verticalSeparationMinYGap(config)
                + " priorityComparison=" + priorityComparison(cluster.placeType(), existing == null ? null : existing.placeType())
                + " decision=" + duplicate.decision().name()
                + " mutated=" + (duplicate.decision() == DuplicateDecision.SAME_TYPE_UPDATE)
                + " reason=" + duplicate.reason()
                + (duplicate.debugDetails().isBlank() ? "" : " " + duplicate.debugDetails());
    }

    private static String priorityComparison(PlaceType candidateType, PlaceType existingType) {
        if (existingType == null) {
            return "none";
        }
        if (candidateType == existingType) {
            return "same_type";
        }
        if (candidateType.priorityRank() < existingType.priorityRank()) {
            return "candidate_higher_priority";
        }
        if (candidateType.priorityRank() > existingType.priorityRank()) {
            return "candidate_lower_priority";
        }
        return "same_priority";
    }

    private static long nameSeed(PlaceCluster cluster) {
        long result = 1125899906842597L;
        result = 31L * result + cluster.placeType().idString().hashCode();
        result = 31L * result + cluster.environment().idString().hashCode();
        result = 31L * result + cluster.dimensionId().hashCode();
        result = 31L * result + cluster.seedChunkId().hashCode();
        result = 31L * result + cluster.includedChunks().hashCode();
        return result;
    }

    private static String basePlaceId(PlaceCluster cluster) {
        return WorldRemembersLivingLegends.MOD_ID
                + ":" + cluster.placeType().idString()
                + ":" + cluster.dimensionId()
                + "@" + cluster.centerX()
                + "," + cluster.centerY()
                + "," + cluster.centerZ();
    }

    private static String uniquePlaceId(String baseId, WorldMemoryStorageData data) {
        String id = baseId;
        int suffix = 2;
        while (data.namedPlace(id) != null) {
            id = baseId + "#" + suffix;
            suffix++;
        }
        return id;
    }

    private static String formatScore(double value) {
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 0.000_001) {
            return Long.toString((long) rounded);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static String portalDebugPart(PlaceCluster cluster) {
        if (cluster == null || cluster.placeType() != PlaceType.PORTAL_LANDMARK) {
            return "";
        }
        CauseContextResolver.PortalResolution portalResolution = CauseContextResolver.resolvePortal(cluster.combinedStats());
        return " portalEvidenceCounts={" + portalResolution.evidenceCounts() + "}"
                + " resolvedPortalType=" + portalResolution.portalType()
                + " portalTypeResolutionReason=" + portalResolution.reason();
    }

    private static String metadataLogPart(NamedPlace place) {
        String metadata = "";
        if (!place.firstDiscoveryKey().isBlank()) {
            metadata += " firstDiscoveryKey=" + place.firstDiscoveryKey();
        }
        if (!place.structureId().isBlank()) {
            metadata += " structureId=" + place.structureId();
        }
        if (place.bounds() != null) {
            metadata += " bounds=" + place.bounds().shape().name();
        }
        if (!place.biomeGroup().isBlank()) {
            metadata += " biomeId=" + place.biomeId()
                    + " dominantBiomeId=" + place.dominantBiomeId()
                    + " biomeGroup=" + place.biomeGroup()
                    + " biomeSource=" + place.biomeSource();
        }
        return metadata;
    }

    private enum DuplicateDecision {
        SAME_TYPE_UPDATE,
        LOWER_PRIORITY_SUPPRESSED,
        HIGHER_PRIORITY_CONFLICT,
        ENVIRONMENT_MISMATCH_CREATE_NEW,
        ENVIRONMENT_MISMATCH_SUPPRESSED,
        CREATE_NEW
    }

    public enum FinalDecision {
        NO_CHANGE,
        CREATED,
        UPDATED,
        SUPPRESSED
    }

    private record DuplicateAnalysis(
            DuplicateDecision decision,
            NamedPlace existingPlace,
            String reason,
            String debugDetails
    ) {
        private DuplicateAnalysis(DuplicateDecision decision, NamedPlace existingPlace, String reason) {
            this(decision, existingPlace, reason, "");
        }

        private DuplicateAnalysis {
            reason = WorldPos.optionalId(reason);
            debugDetails = debugDetails == null ? "" : debugDetails.trim();
        }
    }

    public record Result(
            boolean changed,
            boolean created,
            boolean updated,
            NamedPlace place,
            List<String> debugLines,
            FinalDecision finalDecision,
            String finalDecisionReason
    ) {
        public Result {
            debugLines = List.copyOf(debugLines == null ? List.of() : debugLines);
            finalDecision = finalDecision == null ? FinalDecision.NO_CHANGE : finalDecision;
            finalDecisionReason = WorldPos.optionalId(finalDecisionReason);
        }

        public boolean suppressed() {
            return finalDecision == FinalDecision.SUPPRESSED;
        }

        public static Result noChange(String reason) {
            return new Result(
                    false,
                    false,
                    false,
                    null,
                    List.of("NamedPlace unchanged: reason=" + reason),
                    FinalDecision.NO_CHANGE,
                    reason
            );
        }
    }
}
