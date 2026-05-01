package com.worldremembers.livinglegends;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record PlaceStats(
        long eventCount,
        long visitCount,
        long deathCount,
        long combatEventCount,
        long buildEventCount,
        double totalImportance,
        long firstEventGameTime,
        long lastEventGameTime,
        EventType lastEventType,
        Map<String, Long> eventTypeCounts,
        Map<String, Long> deathSiteEnvironmentCounts,
        Map<String, Long> metadataCounts
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public PlaceStats(
            long eventCount,
            long visitCount,
            long deathCount,
            long combatEventCount,
            long buildEventCount,
            double totalImportance,
            long firstEventGameTime,
            long lastEventGameTime,
            EventType lastEventType,
            Map<String, Long> eventTypeCounts,
            Map<String, Long> deathSiteEnvironmentCounts
    ) {
        this(
                eventCount,
                visitCount,
                deathCount,
                combatEventCount,
                buildEventCount,
                totalImportance,
                firstEventGameTime,
                lastEventGameTime,
                lastEventType,
                eventTypeCounts,
                deathSiteEnvironmentCounts,
                Map.of()
        );
    }

    public PlaceStats {
        eventCount = Math.max(0L, eventCount);
        visitCount = Math.max(0L, visitCount);
        deathCount = Math.max(0L, deathCount);
        combatEventCount = Math.max(0L, combatEventCount);
        buildEventCount = Math.max(0L, buildEventCount);
        totalImportance = Math.max(0.0, totalImportance);
        firstEventGameTime = Math.max(0L, firstEventGameTime);
        lastEventGameTime = Math.max(0L, lastEventGameTime);
        lastEventType = lastEventType == null ? EventType.CUSTOM : lastEventType;
        eventTypeCounts = normalizedEventTypeCounts(eventTypeCounts);
        deathSiteEnvironmentCounts = normalizedDeathSiteEnvironmentCounts(deathSiteEnvironmentCounts);
        metadataCounts = normalizedMetadataCounts(metadataCounts);
    }

    public static PlaceStats empty() {
        return new PlaceStats(0L, 0L, 0L, 0L, 0L, 0.0, 0L, 0L, EventType.CUSTOM, Map.of(), Map.of(), Map.of());
    }

    public PlaceStats updatedWith(WorldMemoryEvent event) {
        EventType type = event.eventType();
        long firstTime = firstEventGameTime == 0L ? event.gameTime() : Math.min(firstEventGameTime, event.gameTime());

        return new PlaceStats(
                eventCount + 1L,
                visitCount + (type.isVisit() ? 1L : 0L),
                deathCount + (type.isDeath() ? 1L : 0L),
                combatEventCount + (type.isCombat() ? 1L : 0L),
                buildEventCount + (type.isBuildRelated() ? 1L : 0L),
                totalImportance + event.basicScore(),
                firstTime,
                Math.max(lastEventGameTime, event.gameTime()),
                type,
                incrementedEventTypeCounts(eventTypeCounts, type),
                incrementedDeathSiteEnvironmentCounts(deathSiteEnvironmentCounts, event),
                incrementedMetadataCounts(metadataCounts, event)
        );
    }

    public double basicScore() {
        return eventCount
                + visitCount * 0.5
                + deathCount * 5.0
                + combatEventCount * 2.5
                + buildEventCount
                + totalImportance;
    }

    public PlaceRarity calculatedRarity() {
        return PlaceRarity.fromScore(basicScore());
    }

    public long eventTypeCount(EventType eventType) {
        EventType resolvedType = eventType == null ? EventType.CUSTOM : EventType.fromId(eventType.idString());
        return eventTypeCounts.getOrDefault(resolvedType.idString(), 0L);
    }

    public Map<String, Long> eventTypeCounts() {
        return Collections.unmodifiableMap(eventTypeCounts);
    }

    public long deathSiteEnvironmentCount(DeathSiteEnvironment environment) {
        DeathSiteEnvironment resolvedEnvironment = environment == null ? DeathSiteEnvironment.UNKNOWN : environment;
        return deathSiteEnvironmentCounts.getOrDefault(resolvedEnvironment.idString(), 0L);
    }

    public Map<String, Long> deathSiteEnvironmentCounts() {
        return Collections.unmodifiableMap(deathSiteEnvironmentCounts);
    }

    public long metadataCount(String key) {
        String normalized = WorldPos.optionalId(key);
        return normalized.isBlank() ? 0L : metadataCounts.getOrDefault(normalized, 0L);
    }

    public Map<String, Long> metadataCounts() {
        return Collections.unmodifiableMap(metadataCounts);
    }

    public Map<String, Long> hostileMobKillsByType() {
        return metadataByPrefix("hostile_mob");
    }

    public Map<String, Long> neutralMobKillsByType() {
        return metadataByPrefix("neutral_mob");
    }

    public Map<String, Long> passiveMobKillsByType() {
        return metadataByPrefix("passive_mob");
    }

    public Map<String, Long> bossKillsByType() {
        return metadataByPrefix("boss");
    }

    public Map<String, Long> namedMobDeathsByType() {
        return metadataByPrefix("named_mob_type");
    }

    public Map<String, Long> petDeathsByType() {
        return metadataByPrefix("pet_type");
    }

    public String dominantHostileMobType() {
        return dominantMetadataValue("hostile_mob");
    }

    public String dominantNeutralMobType() {
        return dominantMetadataValue("neutral_mob");
    }

    public String dominantPassiveMobType() {
        return dominantMetadataValue("passive_mob");
    }

    public String dominantBossType() {
        return dominantMetadataValue("boss");
    }

    public String dominantMetadataValue(String prefix) {
        String normalizedPrefix = WorldPos.optionalId(prefix);
        if (normalizedPrefix.isBlank()) {
            return "";
        }
        String fullPrefix = normalizedPrefix + "=";
        String selected = "";
        long highest = 0L;
        for (Map.Entry<String, Long> entry : metadataCounts.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(fullPrefix)) {
                continue;
            }
            long count = Math.max(0L, entry.getValue() == null ? 0L : entry.getValue());
            if (count > highest || (count == highest && !selected.isBlank() && key.compareTo(selected) < 0)) {
                selected = key;
                highest = count;
            }
        }
        return selected.isBlank() ? "" : selected.substring(fullPrefix.length());
    }

    private Map<String, Long> metadataByPrefix(String prefix) {
        String normalizedPrefix = WorldPos.optionalId(prefix);
        if (normalizedPrefix.isBlank()) {
            return Map.of();
        }
        String fullPrefix = normalizedPrefix + "=";
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : metadataCounts.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(fullPrefix)) {
                continue;
            }
            String entityId = key.substring(fullPrefix.length());
            long count = Math.max(0L, entry.getValue() == null ? 0L : entry.getValue());
            if (!entityId.isBlank() && count > 0L) {
                result.put(entityId, result.getOrDefault(entityId, 0L) + count);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, Long> incrementedEventTypeCounts(Map<String, Long> counts, EventType eventType) {
        Map<String, Long> result = new LinkedHashMap<>(normalizedEventTypeCounts(counts));
        String key = EventType.fromId((eventType == null ? EventType.CUSTOM : eventType).idString()).idString();
        result.put(key, result.getOrDefault(key, 0L) + 1L);
        return result;
    }

    private static Map<String, Long> incrementedDeathSiteEnvironmentCounts(
            Map<String, Long> counts,
            WorldMemoryEvent event
    ) {
        Map<String, Long> result = new LinkedHashMap<>(normalizedDeathSiteEnvironmentCounts(counts));
        if (event == null || event.eventType() != EventType.PLAYER_DEATH) {
            return result;
        }

        String key = DeathSiteEnvironment.classify(event).idString();
        result.put(key, result.getOrDefault(key, 0L) + 1L);
        return result;
    }

    private static Map<String, Long> incrementedMetadataCounts(Map<String, Long> counts, WorldMemoryEvent event) {
        Map<String, Long> result = new LinkedHashMap<>(normalizedMetadataCounts(counts));
        for (Map.Entry<String, Long> entry : CauseContextResolver.metadataCountsForEvent(event).entrySet()) {
            String key = WorldPos.optionalId(entry.getKey());
            long value = Math.max(0L, entry.getValue() == null ? 0L : entry.getValue());
            if (!key.isBlank() && value > 0L) {
                result.put(key, result.getOrDefault(key, 0L) + value);
            }
        }
        return result;
    }

    private static Map<String, Long> normalizedEventTypeCounts(Map<String, Long> counts) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (counts == null) {
            return result;
        }

        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            String key = EventType.fromId(entry.getKey()).idString();
            long value = Math.max(0L, entry.getValue() == null ? 0L : entry.getValue());
            if (value > 0L) {
                result.put(key, result.getOrDefault(key, 0L) + value);
            }
        }

        return result;
    }

    private static Map<String, Long> normalizedDeathSiteEnvironmentCounts(Map<String, Long> counts) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (counts == null) {
            return result;
        }

        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            String key = DeathSiteEnvironment.fromId(entry.getKey()).idString();
            long value = Math.max(0L, entry.getValue() == null ? 0L : entry.getValue());
            if (value > 0L) {
                result.put(key, result.getOrDefault(key, 0L) + value);
            }
        }

        return result;
    }

    private static Map<String, Long> normalizedMetadataCounts(Map<String, Long> counts) {
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
