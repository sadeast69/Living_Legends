package com.worldremembers.livinglegends;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record PlaceCause(
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
        Map<String, Long> evidenceCounts
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public PlaceCause(
            PlaceCauseType causeType,
            EventType primaryEventType,
            String firstDiscoveryKey,
            String discoveryKind,
            String structureId,
            String blockId,
            String entityId,
            String bossId,
            String mobType,
            String deathCause,
            String fromDimension,
            String toDimension,
            Map<String, Long> evidenceCounts
    ) {
        this(
                causeType,
                primaryEventType,
                firstDiscoveryKey,
                discoveryKind,
                structureId,
                blockId,
                entityId,
                bossId,
                mobType,
                "",
                "",
                "",
                "",
                "",
                fromDimension,
                toDimension,
                "",
                "",
                "",
                "",
                "",
                deathCause,
                "",
                "",
                "",
                "",
                evidenceCounts
        );
    }

    public PlaceCause(
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
            String deathCause,
            String petName,
            String petType,
            String namedMobName,
            String namedMobType,
            Map<String, Long> evidenceCounts
    ) {
        this(
                causeType,
                primaryEventType,
                firstDiscoveryKey,
                discoveryKind,
                structureId,
                blockId,
                entityId,
                bossId,
                dominantMobType,
                dominantPassiveMobType,
                dominantHostileMobType,
                dominantNeutralMobType,
                dominantValuableBlock,
                portalType,
                fromDimension,
                toDimension,
                "",
                "",
                "",
                "",
                "",
                deathCause,
                petName,
                petType,
                namedMobName,
                namedMobType,
                evidenceCounts
        );
    }

    public PlaceCause {
        causeType = causeType == null ? PlaceCauseType.UNKNOWN : causeType;
        primaryEventType = EventType.fromId(Objects.requireNonNullElse(primaryEventType, EventType.CUSTOM).idString());
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
        biomeId = normalizeBiomeValue(biomeId);
        dominantBiomeId = normalizeBiomeValue(dominantBiomeId);
        biomeGroup = normalizeBiomeValue(biomeGroup);
        biomeTheme = normalizeBiomeValue(biomeTheme);
        biomeSource = normalizeBiomeValue(biomeSource);
        deathCause = WorldPos.optionalId(deathCause);
        petName = sanitizeRuntimeName(petName);
        petType = WorldPos.optionalId(petType);
        namedMobName = sanitizeRuntimeName(namedMobName);
        namedMobType = WorldPos.optionalId(namedMobType);
        evidenceCounts = Collections.unmodifiableMap(normalizedEvidenceCounts(evidenceCounts));
    }

    public static PlaceCause unknown() {
        return new PlaceCause(
                PlaceCauseType.UNKNOWN,
                EventType.CUSTOM,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                Map.of()
        );
    }

    public static PlaceCause fromFirstDiscovery(WorldFirstDiscoveryRecord discovery) {
        return CauseContextResolver.fromFirstDiscovery(discovery);
    }

    public static PlaceCause fromStats(
            PlaceType placeType,
            DeathSiteEnvironment environment,
            PlaceStats stats
    ) {
        return CauseContextResolver.fromStats(placeType, environment, stats, "", "");
    }

    public static PlaceCause fromMetadata(
            PlaceType placeType,
            DeathSiteEnvironment environment,
            PlaceStats stats,
            String firstDiscoveryKey,
            String structureId
    ) {
        return CauseContextResolver.fromStats(placeType, environment, stats, firstDiscoveryKey, structureId);
    }

    public String mobType() {
        if (!dominantMobType.isBlank()) {
            return dominantMobType;
        }
        if (!dominantHostileMobType.isBlank()) {
            return dominantHostileMobType;
        }
        if (!dominantNeutralMobType.isBlank()) {
            return dominantNeutralMobType;
        }
        if (!dominantPassiveMobType.isBlank()) {
            return dominantPassiveMobType;
        }
        if (!namedMobType.isBlank()) {
            return namedMobType;
        }
        return petType;
    }

    public PlaceCause withBiome(BiomeMetadata metadata) {
        BiomeMetadata biome = metadata == null ? BiomeMetadata.unknown() : metadata;
        return new PlaceCause(
                causeType,
                primaryEventType,
                firstDiscoveryKey,
                discoveryKind,
                structureId,
                blockId,
                entityId,
                bossId,
                dominantMobType,
                dominantPassiveMobType,
                dominantHostileMobType,
                dominantNeutralMobType,
                dominantValuableBlock,
                portalType,
                fromDimension,
                toDimension,
                biome.biomeId(),
                biome.dominantBiomeId(),
                biome.biomeGroup(),
                biome.biomeTheme(),
                biome.biomeSource(),
                deathCause,
                petName,
                petType,
                namedMobName,
                namedMobType,
                evidenceCounts
        );
    }

    public String debugString() {
        return "causeType=" + causeType.name()
                + " primaryEventType=" + primaryEventType.idString()
                + " firstDiscoveryKey=" + firstDiscoveryKey
                + " discoveryKind=" + discoveryKind
                + " structureId=" + structureId
                + " blockId=" + blockId
                + " entityId=" + entityId
                + " bossId=" + bossId
                + " dominantMobType=" + dominantMobType
                + " dominantPassiveMobType=" + dominantPassiveMobType
                + " dominantHostileMobType=" + dominantHostileMobType
                + " dominantNeutralMobType=" + dominantNeutralMobType
                + " dominantValuableBlock=" + dominantValuableBlock
                + " portalType=" + portalType
                + " fromDimension=" + fromDimension
                + " toDimension=" + toDimension
                + " biomeId=" + biomeId
                + " dominantBiomeId=" + dominantBiomeId
                + " biomeGroup=" + biomeGroup
                + " biomeTheme=" + biomeTheme
                + " biomeSource=" + biomeSource
                + " deathCause=" + deathCause
                + " petName=" + petName
                + " petType=" + petType
                + " namedMobName=" + namedMobName
                + " namedMobType=" + namedMobType
                + " evidenceCounts=" + evidenceCounts;
    }

    private static String sanitizeRuntimeName(String value) {
        return RuntimeNameFormatter.sanitize(value);
    }

    private static String normalizeBiomeValue(String value) {
        String normalized = WorldPos.optionalId(value);
        return normalized.isBlank() ? "" : normalized;
    }

    private static Map<String, Long> normalizedEvidenceCounts(Map<String, Long> counts) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (counts == null) {
            return result;
        }

        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            String key = WorldPos.optionalId(entry.getKey());
            long value = Math.max(0L, entry.getValue() == null ? 0L : entry.getValue());
            if (!key.isBlank() && value > 0L) {
                result.put(key, result.getOrDefault(key, 0L) + value);
            }
        }
        return result;
    }
}
