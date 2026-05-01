package com.worldremembers.livinglegends;

import java.io.Serializable;

public record NameContext(
        PlaceType placeType,
        DeathSiteEnvironment environment,
        PlaceCauseType causeType,
        EventType primaryEventType,
        String firstDiscoveryKey,
        String discoveryKind,
        String structureId,
        String blockId,
        String entityId,
        String bossId,
        String dominantMobType,
        String dominantPassiveMobType,
        String dominantHostileMobType,
        String dominantNeutralMobType,
        String dominantValuableBlock,
        String portalType,
        String fromDimension,
        String toDimension,
        String biomeId,
        String dominantBiomeId,
        String biomeGroup,
        String biomeTheme,
        String biomeSource,
        String deathCause,
        String petName,
        String petType,
        String namedMobName,
        String namedMobType,
        String style
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public NameContext {
        placeType = placeType == null ? PlaceType.UNKNOWN : placeType;
        environment = environment == null ? DeathSiteEnvironment.UNKNOWN : environment;
        causeType = causeType == null ? PlaceCauseType.UNKNOWN : causeType;
        primaryEventType = primaryEventType == null ? EventType.CUSTOM : primaryEventType;
        firstDiscoveryKey = WorldPos.optionalId(firstDiscoveryKey);
        discoveryKind = WorldPos.optionalId(discoveryKind);
        structureId = WorldPos.optionalId(structureId);
        blockId = WorldPos.optionalId(blockId);
        entityId = WorldPos.optionalId(entityId);
        bossId = WorldPos.optionalId(bossId);
        dominantMobType = WorldPos.optionalId(dominantMobType);
        dominantPassiveMobType = WorldPos.optionalId(dominantPassiveMobType);
        dominantHostileMobType = WorldPos.optionalId(dominantHostileMobType);
        dominantNeutralMobType = WorldPos.optionalId(dominantNeutralMobType);
        dominantValuableBlock = WorldPos.optionalId(dominantValuableBlock);
        portalType = WorldPos.optionalId(portalType);
        fromDimension = WorldPos.optionalId(fromDimension);
        toDimension = WorldPos.optionalId(toDimension);
        biomeId = WorldPos.optionalId(biomeId);
        dominantBiomeId = WorldPos.optionalId(dominantBiomeId);
        biomeGroup = WorldPos.optionalId(biomeGroup);
        biomeTheme = WorldPos.optionalId(biomeTheme);
        biomeSource = WorldPos.optionalId(biomeSource);
        deathCause = WorldPos.optionalId(deathCause);
        petName = RuntimeNameFormatter.sanitize(petName);
        petType = WorldPos.optionalId(petType);
        namedMobName = RuntimeNameFormatter.sanitize(namedMobName);
        namedMobType = WorldPos.optionalId(namedMobType);
        style = WorldPos.optionalId(style).isBlank() ? BuiltInNameData.DEFAULT_STYLE_ID : WorldPos.optionalId(style);
    }

    public static NameContext from(PlaceCluster cluster, PlaceType placeType, DeathSiteEnvironment environment, String style) {
        PlaceCause cause = cluster == null
                ? PlaceCause.fromStats(placeType, environment, PlaceStats.empty())
                : CauseContextResolver.fromCluster(cluster);
        return from(placeType == null && cluster != null ? cluster.placeType() : placeType,
                environment == null && cluster != null ? cluster.environment() : environment,
                cause,
                style);
    }

    public static NameContext from(PlaceType placeType, DeathSiteEnvironment environment, PlaceCause cause, String style) {
        PlaceCause resolvedCause = cause == null ? PlaceCause.unknown() : cause;
        return new NameContext(
                placeType,
                environment,
                resolvedCause.causeType(),
                resolvedCause.primaryEventType(),
                resolvedCause.firstDiscoveryKey(),
                resolvedCause.discoveryKind(),
                resolvedCause.structureId(),
                resolvedCause.blockId(),
                resolvedCause.entityId(),
                resolvedCause.bossId(),
                resolvedCause.dominantMobType(),
                resolvedCause.dominantPassiveMobType(),
                resolvedCause.dominantHostileMobType(),
                resolvedCause.dominantNeutralMobType(),
                resolvedCause.dominantValuableBlock(),
                resolvedCause.portalType(),
                resolvedCause.fromDimension(),
                resolvedCause.toDimension(),
                resolvedCause.biomeId(),
                resolvedCause.dominantBiomeId(),
                resolvedCause.biomeGroup(),
                resolvedCause.biomeTheme(),
                resolvedCause.biomeSource(),
                resolvedCause.deathCause(),
                resolvedCause.petName(),
                resolvedCause.petType(),
                resolvedCause.namedMobName(),
                resolvedCause.namedMobType(),
                style
        );
    }
}
