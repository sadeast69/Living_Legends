package com.worldremembers.livinglegends;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CauseContextResolver {
    private CauseContextResolver() {
    }

    public static PlaceCause fromCluster(PlaceCluster cluster) {
        if (cluster == null) {
            return PlaceCause.unknown();
        }
        if (cluster.cause() != null && cluster.cause().causeType() != PlaceCauseType.UNKNOWN) {
            return cluster.cause();
        }
        return fromStats(
                cluster.placeType(),
                cluster.environment(),
                cluster.combinedStats(),
                cluster.firstDiscoveryKey(),
                cluster.structureId()
        );
    }

    public static PlaceCause fromFirstDiscovery(WorldFirstDiscoveryRecord discovery) {
        if (discovery == null) {
            return PlaceCause.unknown();
        }

        DiscoveryTriggerType triggerType = discovery.triggerType();
        String targetId = discovery.targetIdString();
        Map<String, Long> evidence = Map.of(discovery.discoveryIdString(), 1L);
        return switch (triggerType) {
            case STRUCTURE_DISCOVERED -> new PlaceCause(
                    PlaceCauseType.FIRST_STRUCTURE_DISCOVERY,
                    EventType.STRUCTURE_DISCOVERED,
                    discovery.discoveryIdString(),
                    "structure",
                    firstNonBlank(discovery.structureIdString(), targetId),
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
                    evidence
            );
            case BLOCK_MINED, BLOCK_DISCOVERED -> new PlaceCause(
                    PlaceCauseType.FIRST_BLOCK_DISCOVERY,
                    EventType.VALUABLE_BLOCK_MINED,
                    discovery.discoveryIdString(),
                    "block",
                    "",
                    targetId,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    targetId,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    evidence
            );
            case DIMENSION_ENTRY -> new PlaceCause(
                    PlaceCauseType.FIRST_DIMENSION_DISCOVERY,
                    EventType.PLAYER_FIRST_ENTERED_DIMENSION,
                    discovery.discoveryIdString(),
                    "dimension",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    dimensionPortalType(targetId),
                    "",
                    targetId,
                    "",
                    "",
                    "",
                    "",
                    "",
                    evidence
            );
            case ENTITY_KILLED -> new PlaceCause(
                    PlaceCauseType.FIRST_BOSS_KILL,
                    EventType.BOSS_KILLED,
                    discovery.discoveryIdString(),
                    "boss_kill",
                    "",
                    "",
                    targetId,
                    targetId,
                    targetId,
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
                    evidence
            );
            default -> PlaceCause.unknown();
        };
    }

    public static PlaceCause fromStats(
            PlaceType placeType,
            DeathSiteEnvironment environment,
            PlaceStats stats,
            String firstDiscoveryKey,
            String structureId
    ) {
        PlaceStats resolvedStats = stats == null ? PlaceStats.empty() : stats;
        PlaceType resolvedType = placeType == null ? PlaceType.UNKNOWN : placeType;
        Map<String, Long> evidence = new LinkedHashMap<>(resolvedStats.eventTypeCounts());
        evidence.putAll(resolvedStats.metadataCounts());
        return switch (resolvedType) {
            case DEATH_SITE -> new PlaceCause(PlaceCauseType.PLAYER_DEATHS, EventType.PLAYER_DEATH, "", "", "", "", "", "", "", "", "", "", "", "", "", "", dominant(resolvedStats, "death_cause"), "", "", "", "", evidence);
            case BATTLEFIELD -> battleCause(resolvedStats, evidence);
            case SLAUGHTER_FIELD -> {
                String passive = themedEntityId(dominant(resolvedStats, "passive_mob"));
                yield new PlaceCause(PlaceCauseType.PASSIVE_SLAUGHTER, EventType.PLAYER_KILLED_PASSIVE_MOB, "", "", "", "", "", "", passive, passive, "", "", "", "", "", "", "", "", "", "", "", evidence);
            }
            case PVP_ARENA -> new PlaceCause(PlaceCauseType.PVP, EventType.PLAYER_KILLED_PLAYER, "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", evidence);
            case MINING_SITE, MINE -> new PlaceCause(PlaceCauseType.MINING, EventType.VALUABLE_BLOCK_MINED, "", "", "", dominant(resolvedStats, "valuable_block"), "", "", "", "", "", "", dominant(resolvedStats, "valuable_block"), "", "", "", "", "", "", "", "", evidence);
            case PORTAL_LANDMARK, PORTAL_SITE -> portalCause(PlaceCauseType.PORTAL_USAGE, resolvedStats, evidence);
            case DIMENSION_THRESHOLD -> portalCause(PlaceCauseType.DIMENSION_THRESHOLD, resolvedStats, evidence);
            case FIRST_DISCOVERY -> firstDiscoveryFromMetadata(firstDiscoveryKey, structureId, resolvedStats, evidence);
            case GENERAL_LANDMARK, LANDMARK, CUSTOM -> new PlaceCause(PlaceCauseType.VISITS, EventType.PLAYER_VISIT, "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", evidence);
            case SETTLEMENT, CAMP, FARM, WORKSHOP, ROAD -> new PlaceCause(PlaceCauseType.SETTLEMENT, EventType.PLAYER_BLOCK_PLACED, "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", evidence);
            case BOSS_SITE -> {
                String bossId = themedEntityId(firstNonBlank(dominant(resolvedStats, "boss"), dominant(resolvedStats, "entity")));
                yield new PlaceCause(PlaceCauseType.BOSS_KILL, EventType.BOSS_KILLED, "", "", "", "", bossId, bossId, bossId, "", "", "", "", "", "", "", "", "", "", "", "", evidence);
            }
            case PET_MEMORIAL -> {
                String petType = themedEntityId(dominant(resolvedStats, "pet_type"));
                yield new PlaceCause(PlaceCauseType.PET_DEATH, EventType.PET_DIED, "", "", "", "", "", "", petType, "", "", "", "", "", "", "", dominant(resolvedStats, "death_cause"), dominant(resolvedStats, "pet_name"), petType, "", "", evidence);
            }
            case NAMED_MOB_MEMORIAL -> {
                String namedMobType = themedEntityId(dominant(resolvedStats, "named_mob_type"));
                yield new PlaceCause(PlaceCauseType.NAMED_MOB_DEATH, EventType.NAMED_MOB_DIED, "", "", "", "", "", "", namedMobType, "", "", "", "", "", "", "", dominant(resolvedStats, "death_cause"), "", "", dominant(resolvedStats, "named_mob_name"), namedMobType, evidence);
            }
            case RAID_SITE -> new PlaceCause(PlaceCauseType.RAID, EventType.RAID_WON, "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", evidence);
            default -> PlaceCause.unknown();
        };
    }

    public static Map<String, Long> metadataCountsForEvent(WorldMemoryEvent event) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (event == null) {
            return result;
        }

        EventType type = event.eventType();
        switch (type) {
            case PLAYER_DEATH -> increment(result, "death_cause", firstNonBlank(noteValue(event.note(), "cause"), event.subjectIdString()));
            case PLAYER_KILLED_HOSTILE_MOB -> {
                String mob = firstNonBlank(noteValue(event.note(), "target_type"), event.subjectIdString());
                increment(result, "hostile_mob", mob);
                increment(result, "mob", mob);
            }
            case PLAYER_KILLED_PASSIVE_MOB -> {
                String mob = firstNonBlank(noteValue(event.note(), "target_type"), event.subjectIdString());
                increment(result, "passive_mob", mob);
                increment(result, "mob", mob);
            }
            case PLAYER_KILLED_NEUTRAL_MOB -> {
                String mob = firstNonBlank(noteValue(event.note(), "target_type"), event.subjectIdString());
                increment(result, "neutral_mob", mob);
                increment(result, "mob", mob);
            }
            case PLAYER_KILLED_PLAYER -> increment(result, "pvp_death", "player");
            case VALUABLE_BLOCK_MINED -> increment(result, "valuable_block", firstNonBlank(noteValue(event.note(), "block"), event.subjectIdString()));
            case NETHER_PORTAL_USED, END_PORTAL_USED, PLAYER_ENTERED_DIMENSION, PLAYER_FIRST_ENTERED_DIMENSION -> {
                String toDimension = firstNonBlank(noteValue(event.note(), "to"), noteValue(event.note(), "dimension"), event.subjectIdString());
                String fromDimension = noteValue(event.note(), "from");
                increment(result, "to_dimension", toDimension);
                increment(result, "from_dimension", fromDimension);
                increment(result, "portal_type", type == EventType.END_PORTAL_USED ? "end" : type == EventType.NETHER_PORTAL_USED ? "nether" : dimensionPortalType(toDimension));
            }
            case BOSS_KILLED -> {
                String boss = firstNonBlank(noteValue(event.note(), "boss_type"), event.subjectIdString());
                increment(result, "boss", boss);
                increment(result, "mob", boss);
            }
            case PET_DIED -> {
                increment(result, "pet_type", firstNonBlank(noteValue(event.note(), "pet_type"), event.subjectIdString()));
                increment(result, "pet_name", RuntimeNameFormatter.decodeNoteValue(noteValue(event.note(), "pet_name")));
                increment(result, "death_cause", noteValue(event.note(), "cause"));
            }
            case NAMED_MOB_DIED -> {
                increment(result, "named_mob_type", firstNonBlank(noteValue(event.note(), "mob_type"), event.subjectIdString()));
                increment(result, "named_mob_name", RuntimeNameFormatter.decodeNoteValue(noteValue(event.note(), "mob_name")));
                increment(result, "death_cause", noteValue(event.note(), "cause"));
            }
            case STRUCTURE_DISCOVERED -> increment(result, "structure", firstNonBlank(event.structureIdString(), event.subjectIdString()));
            default -> {
            }
        }
        return result;
    }

    public static String noteValue(String note, String key) {
        if (note == null || note.isBlank() || key == null || key.isBlank()) {
            return "";
        }
        String prefix = key + "=";
        for (String part : note.split("\\s+")) {
            if (part.startsWith(prefix) && part.length() > prefix.length()) {
                return part.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    private static PlaceCause firstDiscoveryFromMetadata(
            String firstDiscoveryKey,
            String structureId,
            PlaceStats stats,
            Map<String, Long> evidence
    ) {
        String key = WorldPos.optionalId(firstDiscoveryKey);
        String structure = firstNonBlank(structureId, dominant(stats, "structure"));
        if (!structure.isBlank() || key.contains("stronghold")) {
            return new PlaceCause(PlaceCauseType.FIRST_STRUCTURE_DISCOVERY, EventType.STRUCTURE_DISCOVERED, key, "structure", structure.isBlank() ? "minecraft:stronghold" : structure, "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", evidence);
        }
        String block = firstDiscoveryBlock(key);
        if (!block.isBlank()) {
            return new PlaceCause(PlaceCauseType.FIRST_BLOCK_DISCOVERY, EventType.VALUABLE_BLOCK_MINED, key, "block", "", block, "", "", "", "", "", "", block, "", "", "", "", "", "", "", "", evidence);
        }
        String dimension = firstDiscoveryDimension(key);
        if (!dimension.isBlank()) {
            return new PlaceCause(PlaceCauseType.FIRST_DIMENSION_DISCOVERY, EventType.PLAYER_FIRST_ENTERED_DIMENSION, key, "dimension", "", "", "", "", "", "", "", "", "", dimensionPortalType(dimension), "", dimension, "", "", "", "", "", evidence);
        }
        String boss = firstDiscoveryBoss(key);
        if (!boss.isBlank()) {
            return new PlaceCause(PlaceCauseType.FIRST_BOSS_KILL, EventType.BOSS_KILLED, key, "boss_kill", "", "", boss, boss, boss, "", "", "", "", "", "", "", "", "", "", "", "", evidence);
        }
        return PlaceCause.unknown();
    }

    private static PlaceCause battleCause(PlaceStats stats, Map<String, Long> evidence) {
        String hostile = themedEntityId(dominant(stats, "hostile_mob"));
        String neutral = themedEntityId(dominant(stats, "neutral_mob"));
        String mob = themedEntityId(dominantOf(stats, "hostile_mob", "neutral_mob", "boss", "mob"));
        return new PlaceCause(PlaceCauseType.MOB_BATTLE, EventType.PLAYER_KILLED_HOSTILE_MOB, "", "", "", "", firstNonBlank(dominant(stats, "boss"), mob), dominant(stats, "boss"), mob, "", hostile, neutral, "", "", "", "", "", "", "", "", "", evidence);
    }

    private static PlaceCause portalCause(PlaceCauseType causeType, PlaceStats stats, Map<String, Long> evidence) {
        PortalResolution portalResolution = resolvePortal(stats);
        String toDimension = portalResolution.toDimension();
        String fromDimension = portalResolution.fromDimension();
        String portalType = portalResolution.portalType();
        EventType eventType = "end".equals(portalType) ? EventType.END_PORTAL_USED : "nether".equals(portalType) ? EventType.NETHER_PORTAL_USED : EventType.PLAYER_ENTERED_DIMENSION;
        return new PlaceCause(causeType, eventType, "", "", "", "", "", "", "", "", "", "", "", portalType, fromDimension, toDimension, "", "", "", "", "", evidence);
    }

    public static PortalResolution resolvePortal(PlaceStats stats) {
        PlaceStats resolvedStats = stats == null ? PlaceStats.empty() : stats;
        String toDimension = dominant(resolvedStats, "to_dimension");
        String fromDimension = dominant(resolvedStats, "from_dimension");

        long endPortalEvents = resolvedStats.eventTypeCount(EventType.END_PORTAL_USED);
        long netherPortalEvents = resolvedStats.eventTypeCount(EventType.NETHER_PORTAL_USED);
        long enteredDimensionEvents = resolvedStats.eventTypeCount(EventType.PLAYER_ENTERED_DIMENSION)
                + resolvedStats.eventTypeCount(EventType.PLAYER_FIRST_ENTERED_DIMENSION);
        long metadataEnd = resolvedStats.metadataCount("portal_type=end");
        long metadataNether = resolvedStats.metadataCount("portal_type=nether");
        long metadataDimension = resolvedStats.metadataCount("portal_type=dimension");

        String portalType;
        String reason;
        if (endPortalEvents > 0L) {
            portalType = "end";
            reason = "event_type=end_portal_used";
        } else if (metadataEnd > 0L) {
            portalType = "end";
            reason = "metadata=portal_type=end";
        } else if (netherPortalEvents > 0L) {
            portalType = "nether";
            reason = "event_type=nether_portal_used";
        } else if (metadataNether > 0L) {
            portalType = "nether";
            reason = "metadata=portal_type=nether";
        } else if ("end".equals(dimensionPortalType(toDimension)) || "end".equals(dimensionPortalType(fromDimension))) {
            portalType = "end";
            reason = "dimension_transition=end";
        } else if ("nether".equals(dimensionPortalType(toDimension)) || "nether".equals(dimensionPortalType(fromDimension))) {
            portalType = "nether";
            reason = "dimension_transition=nether";
        } else if (metadataDimension > 0L || enteredDimensionEvents > 0L) {
            portalType = "dimension";
            reason = metadataDimension > 0L ? "metadata=portal_type=dimension" : "event_type=player_entered_dimension";
        } else {
            portalType = firstNonBlank(dominant(resolvedStats, "portal_type"), dimensionPortalType(toDimension), dimensionPortalType(fromDimension));
            reason = portalType.isBlank() ? "no_portal_evidence" : "fallback";
        }

        String evidenceCounts = "end_portal_used=" + endPortalEvents
                + ",nether_portal_used=" + netherPortalEvents
                + ",dimension_entries=" + enteredDimensionEvents
                + ",portal_type=end=" + metadataEnd
                + ",portal_type=nether=" + metadataNether
                + ",portal_type=dimension=" + metadataDimension
                + ",from_dimension=" + fromDimension
                + ",to_dimension=" + toDimension;
        return new PortalResolution(portalType, fromDimension, toDimension, reason, evidenceCounts);
    }

    private static String dominant(PlaceStats stats, String prefix) {
        return stats == null ? "" : stats.dominantMetadataValue(prefix);
    }

    private static String dominantOf(PlaceStats stats, String... prefixes) {
        String selected = "";
        long highest = 0L;
        for (String prefix : prefixes) {
            String value = dominant(stats, prefix);
            long count = stats == null ? 0L : stats.metadataCount(WorldPos.optionalId(prefix) + "=" + value);
            if (!value.isBlank() && count > highest) {
                selected = value;
                highest = count;
            }
        }
        return selected;
    }

    private static void increment(Map<String, Long> target, String prefix, String value) {
        String normalizedPrefix = WorldPos.optionalId(prefix);
        String normalizedValue = WorldPos.optionalId(value);
        if (normalizedPrefix.isBlank() || normalizedValue.isBlank()) {
            return;
        }
        String key = normalizedPrefix + "=" + normalizedValue;
        target.put(key, target.getOrDefault(key, 0L) + 1L);
    }

    private static String firstDiscoveryBlock(String key) {
        for (FirstDiscoveryDefinition definition : WorldRemembersCompatRegistries.compatFirstDiscoveryDefinitions()) {
            if (definition.triggerType() == DiscoveryTriggerType.BLOCK_MINED && definition.discoveryIdString().equals(key)) {
                return definition.targetIdString();
            }
        }
        if (key.contains("diamond")) {
            return "minecraft:diamond_ore";
        }
        if (key.contains("ancient_debris")) {
            return "minecraft:ancient_debris";
        }
        if (key.contains("trial_spawner")) {
            return "minecraft:trial_spawner";
        }
        if (key.contains("vault")) {
            return "minecraft:vault";
        }
        return "";
    }

    private static String firstDiscoveryDimension(String key) {
        for (FirstDiscoveryDefinition definition : WorldRemembersCompatRegistries.compatFirstDiscoveryDefinitions()) {
            if (definition.triggerType() == DiscoveryTriggerType.DIMENSION_ENTRY && definition.discoveryIdString().equals(key)) {
                return definition.targetIdString();
            }
        }
        if (key.contains("nether_entry")) {
            return "minecraft:the_nether";
        }
        if (key.contains("end_entry")) {
            return "minecraft:the_end";
        }
        return "";
    }

    private static String firstDiscoveryBoss(String key) {
        for (FirstDiscoveryDefinition definition : WorldRemembersCompatRegistries.compatFirstDiscoveryDefinitions()) {
            if (definition.triggerType() == DiscoveryTriggerType.ENTITY_KILLED && definition.discoveryIdString().equals(key)) {
                return definition.targetIdString();
            }
        }
        if (key.contains("ender_dragon")) {
            return "minecraft:ender_dragon";
        }
        if (key.contains("wither")) {
            return "minecraft:wither";
        }
        if (key.contains("warden")) {
            return "minecraft:warden";
        }
        return "";
    }

    private static String dimensionPortalType(String dimensionId) {
        String normalized = WorldPos.optionalId(dimensionId);
        WorldRemembersCompatRegistries.CompatLookup<CompatThemeDefinitions.DimensionThemeDefinition> compat =
                WorldRemembersCompatRegistries.dimensionTheme(normalized);
        if (compat.matched() && !compat.definition().portalTheme().isBlank()) {
            return compat.definition().portalTheme();
        }
        if ("minecraft:the_nether".equals(normalized)) {
            return "nether";
        }
        if ("minecraft:the_end".equals(normalized)) {
            return "end";
        }
        return normalized.isBlank() ? "" : "dimension";
    }

    private static String themedEntityId(String entityId) {
        String normalized = WorldPos.optionalId(entityId);
        if (normalized.isBlank()) {
            return "";
        }
        return VanillaMobThemeRegistry.lookup(normalized).entityId();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String normalized = WorldPos.optionalId(value);
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        return "";
    }

    public record PortalResolution(
            String portalType,
            String fromDimension,
            String toDimension,
            String reason,
            String evidenceCounts
    ) {
        public PortalResolution {
            portalType = WorldPos.optionalId(portalType);
            fromDimension = WorldPos.optionalId(fromDimension);
            toDimension = WorldPos.optionalId(toDimension);
            reason = reason == null ? "" : reason.trim();
            evidenceCounts = evidenceCounts == null ? "" : evidenceCounts.trim();
        }
    }
}
