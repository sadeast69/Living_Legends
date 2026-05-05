package com.worldremembers.livinglegends;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class NameCauseConstraints implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final NameCauseConstraints NONE = builder().build();

    private final PlaceCauseType requiredCauseType;
    private final String requiredFirstDiscoveryKey;
    private final String requiredDiscoveryKind;
    private final String requiredStructureId;
    private final Set<String> requiredBlockIds;
    private final Set<String> requiredEntityIds;
    private final String requiredBossId;
    private final Set<String> requiredMobTypes;
    private final Set<String> requiredDominantMobTypes;
    private final Set<String> requiredDominantValuableBlocks;
    private final String requiredPortalType;
    private final Set<String> requiredBiomeGroups;
    private final Set<String> requiredBiomeThemes;
    private final String requiredFromDimension;
    private final String requiredToDimension;
    private final String requiredDeathCause;
    private final Set<PlaceCauseType> forbiddenCauseTypes;
    private final Set<String> forbiddenFirstDiscoveryKeys;
    private final Set<String> forbiddenStructureIds;
    private final Set<String> forbiddenBlockIds;
    private final Set<String> forbiddenEntityIds;
    private final Set<String> forbiddenMobTypes;
    private final Set<PlaceCauseType> allowedCauseTypes;
    private final Set<String> allowedFirstDiscoveryKeys;
    private final Set<String> allowedDiscoveryKinds;
    private final Set<String> allowedStructureIds;
    private final Set<String> allowedBlockIds;
    private final Set<String> allowedEntityIds;
    private final Set<String> allowedBossIds;
    private final Set<String> allowedMobTypes;
    private final Set<String> allowedDominantMobTypes;
    private final Set<String> allowedDominantValuableBlocks;
    private final Set<String> allowedPortalTypes;
    private final Set<String> allowedBiomeGroups;
    private final Set<String> allowedFromDimensions;
    private final Set<String> allowedToDimensions;
    private final Set<String> allowedDeathCauses;

    private NameCauseConstraints(Builder builder) {
        requiredCauseType = builder.requiredCauseType == null ? PlaceCauseType.UNKNOWN : builder.requiredCauseType;
        requiredFirstDiscoveryKey = WorldPos.optionalId(builder.requiredFirstDiscoveryKey);
        requiredDiscoveryKind = WorldPos.optionalId(builder.requiredDiscoveryKind);
        requiredStructureId = WorldPos.optionalId(builder.requiredStructureId);
        requiredBlockIds = immutableIds(builder.requiredBlockIds);
        requiredEntityIds = immutableIds(builder.requiredEntityIds);
        requiredBossId = WorldPos.optionalId(builder.requiredBossId);
        requiredMobTypes = immutableIds(builder.requiredMobTypes);
        requiredDominantMobTypes = immutableIds(builder.requiredDominantMobTypes);
        requiredDominantValuableBlocks = immutableIds(builder.requiredDominantValuableBlocks);
        requiredPortalType = WorldPos.optionalId(builder.requiredPortalType);
        requiredBiomeGroups = immutableIds(builder.requiredBiomeGroups);
        requiredBiomeThemes = immutableIds(builder.requiredBiomeThemes);
        requiredFromDimension = WorldPos.optionalId(builder.requiredFromDimension);
        requiredToDimension = WorldPos.optionalId(builder.requiredToDimension);
        requiredDeathCause = WorldPos.optionalId(builder.requiredDeathCause);
        forbiddenCauseTypes = immutableCauseTypes(builder.forbiddenCauseTypes);
        forbiddenFirstDiscoveryKeys = immutableIds(builder.forbiddenFirstDiscoveryKeys);
        forbiddenStructureIds = immutableIds(builder.forbiddenStructureIds);
        forbiddenBlockIds = immutableIds(builder.forbiddenBlockIds);
        forbiddenEntityIds = immutableIds(builder.forbiddenEntityIds);
        forbiddenMobTypes = immutableIds(builder.forbiddenMobTypes);
        allowedCauseTypes = immutableCauseTypes(builder.allowedCauseTypes);
        allowedFirstDiscoveryKeys = immutableIds(builder.allowedFirstDiscoveryKeys);
        allowedDiscoveryKinds = immutableIds(builder.allowedDiscoveryKinds);
        allowedStructureIds = immutableIds(builder.allowedStructureIds);
        allowedBlockIds = immutableIds(builder.allowedBlockIds);
        allowedEntityIds = immutableIds(builder.allowedEntityIds);
        allowedBossIds = immutableIds(builder.allowedBossIds);
        allowedMobTypes = immutableIds(builder.allowedMobTypes);
        allowedDominantMobTypes = immutableIds(builder.allowedDominantMobTypes);
        allowedDominantValuableBlocks = immutableIds(builder.allowedDominantValuableBlocks);
        allowedPortalTypes = immutableIds(builder.allowedPortalTypes);
        allowedBiomeGroups = immutableIds(builder.allowedBiomeGroups);
        allowedFromDimensions = immutableIds(builder.allowedFromDimensions);
        allowedToDimensions = immutableIds(builder.allowedToDimensions);
        allowedDeathCauses = immutableIds(builder.allowedDeathCauses);
    }

    public static NameCauseConstraints none() {
        return NONE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean matchesPattern(PlaceCause cause) {
        return matchesPattern(NameContext.from(PlaceType.UNKNOWN, DeathSiteEnvironment.UNKNOWN, cause, BuiltInNameData.DEFAULT_STYLE_ID));
    }

    public boolean matchesPattern(NameContext context) {
        NameContext resolvedContext = context == null
                ? NameContext.from(PlaceType.UNKNOWN, DeathSiteEnvironment.UNKNOWN, PlaceCause.unknown(), BuiltInNameData.DEFAULT_STYLE_ID)
                : context;
        if (requiredCauseType != PlaceCauseType.UNKNOWN && resolvedContext.causeType() != requiredCauseType) {
            return false;
        }
        if (!requiredFirstDiscoveryKey.isBlank()
                && !requiredFirstDiscoveryKey.equals(resolvedContext.firstDiscoveryKey())) {
            return false;
        }
        if (!requiredDiscoveryKind.isBlank() && !requiredDiscoveryKind.equals(resolvedContext.discoveryKind())) {
            return false;
        }
        if (!requiredStructureId.isBlank() && !requiredStructureId.equals(resolvedContext.structureId())) {
            return false;
        }
        if (!requiredBlockIds.isEmpty() && !requiredBlockIds.contains(resolvedContext.blockId())) {
            return false;
        }
        if (!requiredEntityIds.isEmpty() && !requiredEntityIds.contains(resolvedContext.entityId())) {
            return false;
        }
        if (!requiredBossId.isBlank() && !requiredBossId.equals(resolvedContext.bossId())) {
            return false;
        }
        if (!requiredMobTypes.isEmpty() && !matchesAnyMobType(resolvedContext, requiredMobTypes)) {
            return false;
        }
        if (!requiredDominantMobTypes.isEmpty() && !matchesAnyDominantMobType(resolvedContext, requiredDominantMobTypes)) {
            return false;
        }
        if (!requiredDominantValuableBlocks.isEmpty()
                && !requiredDominantValuableBlocks.contains(resolvedContext.dominantValuableBlock())) {
            return false;
        }
        if (!requiredPortalType.isBlank() && !requiredPortalType.equals(resolvedContext.portalType())) {
            return false;
        }
        if (!requiredBiomeGroups.isEmpty() && !requiredBiomeGroups.contains(resolvedContext.biomeGroup())) {
            return false;
        }
        if (!requiredBiomeThemes.isEmpty() && !requiredBiomeThemes.contains(resolvedContext.biomeTheme())) {
            return false;
        }
        if (!requiredFromDimension.isBlank() && !requiredFromDimension.equals(resolvedContext.fromDimension())) {
            return false;
        }
        if (!requiredToDimension.isBlank() && !requiredToDimension.equals(resolvedContext.toDimension())) {
            return false;
        }
        if (!requiredDeathCause.isBlank() && !requiredDeathCause.equals(resolvedContext.deathCause())) {
            return false;
        }
        return !matchesForbidden(resolvedContext);
    }

    public boolean matchesToken(PlaceCause cause) {
        return matchesToken(NameContext.from(PlaceType.UNKNOWN, DeathSiteEnvironment.UNKNOWN, cause, BuiltInNameData.DEFAULT_STYLE_ID));
    }

    public boolean matchesToken(NameContext context) {
        NameContext resolvedContext = context == null
                ? NameContext.from(PlaceType.UNKNOWN, DeathSiteEnvironment.UNKNOWN, PlaceCause.unknown(), BuiltInNameData.DEFAULT_STYLE_ID)
                : context;
        if (matchesForbidden(resolvedContext)) {
            return false;
        }

        if (!hasAllowedConstraint()) {
            return true;
        }

        return allowedCauseTypes.contains(resolvedContext.causeType())
                || allowedFirstDiscoveryKeys.contains(resolvedContext.firstDiscoveryKey())
                || allowedDiscoveryKinds.contains(resolvedContext.discoveryKind())
                || allowedStructureIds.contains(resolvedContext.structureId())
                || allowedBlockIds.contains(resolvedContext.blockId())
                || allowedBlockIds.contains(resolvedContext.dominantValuableBlock())
                || allowedEntityIds.contains(resolvedContext.entityId())
                || allowedBossIds.contains(resolvedContext.bossId())
                || matchesAnyMobType(resolvedContext, allowedMobTypes)
                || matchesAnyDominantMobType(resolvedContext, allowedDominantMobTypes)
                || allowedDominantValuableBlocks.contains(resolvedContext.dominantValuableBlock())
                || allowedPortalTypes.contains(resolvedContext.portalType())
                || allowedBiomeGroups.contains(resolvedContext.biomeGroup())
                || allowedFromDimensions.contains(resolvedContext.fromDimension())
                || allowedToDimensions.contains(resolvedContext.toDimension())
                || allowedDeathCauses.contains(resolvedContext.deathCause());
    }

    public boolean hasExactCauseRequirement() {
        return !requiredFirstDiscoveryKey.isBlank()
                || !requiredStructureId.isBlank()
                || !requiredBlockIds.isEmpty()
                || !requiredEntityIds.isEmpty()
                || !requiredBossId.isBlank()
                || !requiredFromDimension.isBlank()
                || !requiredToDimension.isBlank();
    }

    public boolean hasDominantTargetRequirement() {
        return !requiredMobTypes.isEmpty()
                || !requiredDominantMobTypes.isEmpty()
                || !requiredDominantValuableBlocks.isEmpty()
                || !requiredPortalType.isBlank()
                || !requiredBiomeGroups.isEmpty()
                || !requiredBiomeThemes.isEmpty()
                || !requiredDeathCause.isBlank();
    }

    public boolean hasCauseTypeRequirement() {
        return requiredCauseType != PlaceCauseType.UNKNOWN;
    }

    public Set<PlaceCauseType> allowedCauseTypes() {
        return allowedCauseTypes;
    }

    public Set<String> forbiddenFirstDiscoveryKeys() {
        return forbiddenFirstDiscoveryKeys;
    }

    public Set<String> forbiddenBlockIds() {
        return forbiddenBlockIds;
    }

    public Set<String> forbiddenEntityIds() {
        return forbiddenEntityIds;
    }

    public int dominantTargetPriority(NameContext context) {
        NameContext resolvedContext = context == null
                ? NameContext.from(PlaceType.UNKNOWN, DeathSiteEnvironment.UNKNOWN, PlaceCause.unknown(), BuiltInNameData.DEFAULT_STYLE_ID)
                : context;
        int priority = 0;
        priority = Math.max(priority, mobMatchPriority(resolvedContext, requiredMobTypes));
        priority = Math.max(priority, dominantMobMatchPriority(resolvedContext, requiredDominantMobTypes));
        if (!requiredDominantValuableBlocks.isEmpty()
                && requiredDominantValuableBlocks.contains(resolvedContext.dominantValuableBlock())) {
            priority = Math.max(priority, 500);
        }
        if (!requiredPortalType.isBlank() && requiredPortalType.equals(resolvedContext.portalType())) {
            priority = Math.max(priority, 500);
        }
        if (!requiredBiomeGroups.isEmpty() && requiredBiomeGroups.contains(resolvedContext.biomeGroup())) {
            priority = Math.max(priority, 350);
        }
        if (!requiredBiomeThemes.isEmpty() && requiredBiomeThemes.contains(resolvedContext.biomeTheme())) {
            priority = Math.max(priority, 450);
        }
        if (!requiredDeathCause.isBlank() && requiredDeathCause.equals(resolvedContext.deathCause())) {
            priority = Math.max(priority, 500);
        }
        return priority;
    }

    private boolean hasAllowedConstraint() {
        return !allowedCauseTypes.isEmpty()
                || !allowedFirstDiscoveryKeys.isEmpty()
                || !allowedDiscoveryKinds.isEmpty()
                || !allowedStructureIds.isEmpty()
                || !allowedBlockIds.isEmpty()
                || !allowedEntityIds.isEmpty()
                || !allowedBossIds.isEmpty()
                || !allowedMobTypes.isEmpty()
                || !allowedDominantMobTypes.isEmpty()
                || !allowedDominantValuableBlocks.isEmpty()
                || !allowedPortalTypes.isEmpty()
                || !allowedBiomeGroups.isEmpty()
                || !allowedFromDimensions.isEmpty()
                || !allowedToDimensions.isEmpty()
                || !allowedDeathCauses.isEmpty();
    }

    private boolean matchesForbidden(PlaceCause cause) {
        return matchesForbidden(NameContext.from(PlaceType.UNKNOWN, DeathSiteEnvironment.UNKNOWN, cause, BuiltInNameData.DEFAULT_STYLE_ID));
    }

    private boolean matchesForbidden(NameContext context) {
        return forbiddenCauseTypes.contains(context.causeType())
                || forbiddenFirstDiscoveryKeys.contains(context.firstDiscoveryKey())
                || forbiddenStructureIds.contains(context.structureId())
                || forbiddenBlockIds.contains(context.blockId())
                || forbiddenBlockIds.contains(context.dominantValuableBlock())
                || forbiddenEntityIds.contains(context.entityId())
                || forbiddenEntityIds.contains(context.bossId())
                || matchesAnyMobType(context, forbiddenMobTypes);
    }

    private static boolean matchesAnyMobType(NameContext context, Set<String> mobTypes) {
        return mobMatchPriority(context, mobTypes) > 0;
    }

    private static boolean matchesAnyDominantMobType(NameContext context, Set<String> mobTypes) {
        return dominantMobMatchPriority(context, mobTypes) > 0;
    }

    private static int mobMatchPriority(NameContext context, Set<String> mobTypes) {
        if (context == null || mobTypes == null || mobTypes.isEmpty()) {
            return 0;
        }
        int priority = dominantMobMatchPriority(context, mobTypes);
        priority = Math.max(priority, VanillaMobThemeRegistry.matchPriority(context.namedMobType(), mobTypes, context.placeType()));
        priority = Math.max(priority, VanillaMobThemeRegistry.matchPriority(context.petType(), mobTypes, context.placeType()));
        return priority;
    }

    private static int dominantMobMatchPriority(NameContext context, Set<String> mobTypes) {
        if (context == null || mobTypes == null || mobTypes.isEmpty()) {
            return 0;
        }
        int priority = 0;
        priority = Math.max(priority, VanillaMobThemeRegistry.matchPriority(context.dominantMobType(), mobTypes, context.placeType()));
        priority = Math.max(priority, VanillaMobThemeRegistry.matchPriority(context.dominantHostileMobType(), mobTypes, context.placeType()));
        priority = Math.max(priority, VanillaMobThemeRegistry.matchPriority(context.dominantNeutralMobType(), mobTypes, context.placeType()));
        priority = Math.max(priority, VanillaMobThemeRegistry.matchPriority(context.dominantPassiveMobType(), mobTypes, context.placeType()));
        priority = Math.max(priority, VanillaMobThemeRegistry.matchPriority(context.entityId(), mobTypes, context.placeType()));
        priority = Math.max(priority, VanillaMobThemeRegistry.matchPriority(context.bossId(), mobTypes, context.placeType()));
        return priority;
    }

    private static Set<String> immutableIds(Set<String> ids) {
        Set<String> result = new LinkedHashSet<>();
        if (ids != null) {
            for (String id : ids) {
                String normalized = WorldPos.optionalId(id);
                if (!normalized.isBlank()) {
                    result.add(normalized);
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static Set<PlaceCauseType> immutableCauseTypes(Set<PlaceCauseType> causeTypes) {
        Set<PlaceCauseType> result = new LinkedHashSet<>();
        if (causeTypes != null) {
            for (PlaceCauseType causeType : causeTypes) {
                if (causeType != null && causeType != PlaceCauseType.UNKNOWN) {
                    result.add(causeType);
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }

    public static final class Builder {
        private PlaceCauseType requiredCauseType = PlaceCauseType.UNKNOWN;
        private String requiredFirstDiscoveryKey = "";
        private String requiredDiscoveryKind = "";
        private String requiredStructureId = "";
        private final Set<String> requiredBlockIds = new LinkedHashSet<>();
        private final Set<String> requiredEntityIds = new LinkedHashSet<>();
        private String requiredBossId = "";
        private final Set<String> requiredMobTypes = new LinkedHashSet<>();
        private final Set<String> requiredDominantMobTypes = new LinkedHashSet<>();
        private final Set<String> requiredDominantValuableBlocks = new LinkedHashSet<>();
        private String requiredPortalType = "";
        private final Set<String> requiredBiomeGroups = new LinkedHashSet<>();
        private final Set<String> requiredBiomeThemes = new LinkedHashSet<>();
        private String requiredFromDimension = "";
        private String requiredToDimension = "";
        private String requiredDeathCause = "";
        private final Set<PlaceCauseType> forbiddenCauseTypes = new LinkedHashSet<>();
        private final Set<String> forbiddenFirstDiscoveryKeys = new LinkedHashSet<>();
        private final Set<String> forbiddenStructureIds = new LinkedHashSet<>();
        private final Set<String> forbiddenBlockIds = new LinkedHashSet<>();
        private final Set<String> forbiddenEntityIds = new LinkedHashSet<>();
        private final Set<String> forbiddenMobTypes = new LinkedHashSet<>();
        private final Set<PlaceCauseType> allowedCauseTypes = new LinkedHashSet<>();
        private final Set<String> allowedFirstDiscoveryKeys = new LinkedHashSet<>();
        private final Set<String> allowedDiscoveryKinds = new LinkedHashSet<>();
        private final Set<String> allowedStructureIds = new LinkedHashSet<>();
        private final Set<String> allowedBlockIds = new LinkedHashSet<>();
        private final Set<String> allowedEntityIds = new LinkedHashSet<>();
        private final Set<String> allowedBossIds = new LinkedHashSet<>();
        private final Set<String> allowedMobTypes = new LinkedHashSet<>();
        private final Set<String> allowedDominantMobTypes = new LinkedHashSet<>();
        private final Set<String> allowedDominantValuableBlocks = new LinkedHashSet<>();
        private final Set<String> allowedPortalTypes = new LinkedHashSet<>();
        private final Set<String> allowedBiomeGroups = new LinkedHashSet<>();
        private final Set<String> allowedFromDimensions = new LinkedHashSet<>();
        private final Set<String> allowedToDimensions = new LinkedHashSet<>();
        private final Set<String> allowedDeathCauses = new LinkedHashSet<>();

        public Builder requiredCauseType(PlaceCauseType causeType) {
            requiredCauseType = causeType == null ? PlaceCauseType.UNKNOWN : causeType;
            return this;
        }

        public Builder requiredFirstDiscoveryKey(String key) {
            requiredFirstDiscoveryKey = key;
            return this;
        }

        public Builder requiredDiscoveryKind(String discoveryKind) {
            requiredDiscoveryKind = discoveryKind;
            return this;
        }

        public Builder requiredStructureId(String structureId) {
            requiredStructureId = structureId;
            return this;
        }

        public Builder requiredBlockIds(String... blockIds) {
            addIds(requiredBlockIds, blockIds);
            return this;
        }

        public Builder requiredEntityId(String entityId) {
            addIds(requiredEntityIds, entityId);
            return this;
        }

        public Builder requiredEntityIds(String... entityIds) {
            addIds(requiredEntityIds, entityIds);
            return this;
        }

        public Builder requiredBossId(String bossId) {
            requiredBossId = bossId;
            return this;
        }

        public Builder requiredMobType(String mobType) {
            addIds(requiredMobTypes, mobType);
            return this;
        }

        public Builder requiredMobTypes(String... mobTypes) {
            addIds(requiredMobTypes, mobTypes);
            return this;
        }

        public Builder requiredDominantMobTypes(String... mobTypes) {
            addIds(requiredDominantMobTypes, mobTypes);
            return this;
        }

        public Builder requiredDominantValuableBlocks(String... blockIds) {
            addIds(requiredDominantValuableBlocks, blockIds);
            return this;
        }

        public Builder requiredPortalType(String portalType) {
            requiredPortalType = portalType;
            return this;
        }

        public Builder requiredBiomeGroups(String... biomeGroups) {
            addIds(requiredBiomeGroups, biomeGroups);
            return this;
        }

        public Builder requiredBiomeThemes(String... biomeThemes) {
            addIds(requiredBiomeThemes, biomeThemes);
            return this;
        }

        public Builder requiredFromDimension(String dimensionId) {
            requiredFromDimension = dimensionId;
            return this;
        }

        public Builder requiredToDimension(String dimensionId) {
            requiredToDimension = dimensionId;
            return this;
        }

        public Builder requiredDeathCause(String deathCause) {
            requiredDeathCause = deathCause;
            return this;
        }

        public Builder forbiddenCauseTypes(PlaceCauseType... causeTypes) {
            addCauseTypes(forbiddenCauseTypes, causeTypes);
            return this;
        }

        public Builder forbiddenFirstDiscoveryKeys(String... keys) {
            addIds(forbiddenFirstDiscoveryKeys, keys);
            return this;
        }

        public Builder forbiddenStructureIds(String... structureIds) {
            addIds(forbiddenStructureIds, structureIds);
            return this;
        }

        public Builder forbiddenBlockIds(String... blockIds) {
            addIds(forbiddenBlockIds, blockIds);
            return this;
        }

        public Builder forbiddenEntityIds(String... entityIds) {
            addIds(forbiddenEntityIds, entityIds);
            return this;
        }

        public Builder forbiddenMobTypes(String... mobTypes) {
            addIds(forbiddenMobTypes, mobTypes);
            return this;
        }

        public Builder allowedCauseTypes(PlaceCauseType... causeTypes) {
            addCauseTypes(allowedCauseTypes, causeTypes);
            return this;
        }

        public Builder allowedFirstDiscoveryKeys(String... keys) {
            addIds(allowedFirstDiscoveryKeys, keys);
            return this;
        }

        public Builder allowedDiscoveryKinds(String... discoveryKinds) {
            addIds(allowedDiscoveryKinds, discoveryKinds);
            return this;
        }

        public Builder allowedStructureIds(String... structureIds) {
            addIds(allowedStructureIds, structureIds);
            return this;
        }

        public Builder allowedBlockIds(String... blockIds) {
            addIds(allowedBlockIds, blockIds);
            return this;
        }

        public Builder allowedEntityIds(String... entityIds) {
            addIds(allowedEntityIds, entityIds);
            return this;
        }

        public Builder allowedBossIds(String... bossIds) {
            addIds(allowedBossIds, bossIds);
            return this;
        }

        public Builder allowedMobTypes(String... mobTypes) {
            addIds(allowedMobTypes, mobTypes);
            return this;
        }

        public Builder allowedDominantMobTypes(String... mobTypes) {
            addIds(allowedDominantMobTypes, mobTypes);
            return this;
        }

        public Builder allowedDominantValuableBlocks(String... blockIds) {
            addIds(allowedDominantValuableBlocks, blockIds);
            return this;
        }

        public Builder allowedPortalTypes(String... portalTypes) {
            addIds(allowedPortalTypes, portalTypes);
            return this;
        }

        public Builder allowedBiomeGroups(String... biomeGroups) {
            addIds(allowedBiomeGroups, biomeGroups);
            return this;
        }

        public Builder allowedFromDimensions(String... dimensionIds) {
            addIds(allowedFromDimensions, dimensionIds);
            return this;
        }

        public Builder allowedToDimensions(String... dimensionIds) {
            addIds(allowedToDimensions, dimensionIds);
            return this;
        }

        public Builder allowedDeathCauses(String... deathCauses) {
            addIds(allowedDeathCauses, deathCauses);
            return this;
        }

        public NameCauseConstraints build() {
            return new NameCauseConstraints(this);
        }

        private static void addIds(Set<String> target, String... ids) {
            if (ids == null) {
                return;
            }
            for (String id : ids) {
                String normalized = WorldPos.optionalId(id);
                if (!normalized.isBlank()) {
                    target.add(normalized);
                }
            }
        }

        private static void addCauseTypes(Set<PlaceCauseType> target, PlaceCauseType... causeTypes) {
            if (causeTypes == null) {
                return;
            }
            for (PlaceCauseType causeType : causeTypes) {
                if (causeType != null && causeType != PlaceCauseType.UNKNOWN) {
                    target.add(causeType);
                }
            }
        }
    }
}
