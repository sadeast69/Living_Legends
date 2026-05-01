package com.worldremembers.livinglegends;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ChunkMemoryStats(
        String dimensionId,
        int chunkX,
        int chunkZ,
        long eventCount,
        long visitCount,
        long deathCount,
        long combatEventCount,
        long buildEventCount,
        double totalImportance,
        long lastEventGameTime,
        int minY,
        int maxY,
        Map<String, Long> eventTypeCounts,
        Map<String, Long> deathSiteEnvironmentCounts,
        Map<String, Long> metadataCounts,
        Map<String, CandidateDecayState> candidateDecayStates
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public ChunkMemoryStats(
            String dimensionId,
            int chunkX,
            int chunkZ,
            long eventCount,
            long visitCount,
            long deathCount,
            long combatEventCount,
            long buildEventCount,
            double totalImportance,
            long lastEventGameTime,
            int minY,
            int maxY,
            Map<String, Long> eventTypeCounts,
            Map<String, Long> deathSiteEnvironmentCounts
    ) {
        this(
                dimensionId,
                chunkX,
                chunkZ,
                eventCount,
                visitCount,
                deathCount,
                combatEventCount,
                buildEventCount,
                totalImportance,
                lastEventGameTime,
                minY,
                maxY,
                eventTypeCounts,
                deathSiteEnvironmentCounts,
                Map.of(),
                Map.of()
        );
    }

    public ChunkMemoryStats(
            String dimensionId,
            int chunkX,
            int chunkZ,
            long eventCount,
            long visitCount,
            long deathCount,
            long combatEventCount,
            long buildEventCount,
            double totalImportance,
            long lastEventGameTime,
            int minY,
            int maxY,
            Map<String, Long> eventTypeCounts,
            Map<String, Long> deathSiteEnvironmentCounts,
            Map<String, Long> metadataCounts
    ) {
        this(
                dimensionId,
                chunkX,
                chunkZ,
                eventCount,
                visitCount,
                deathCount,
                combatEventCount,
                buildEventCount,
                totalImportance,
                lastEventGameTime,
                minY,
                maxY,
                eventTypeCounts,
                deathSiteEnvironmentCounts,
                metadataCounts,
                Map.of()
        );
    }

    public ChunkMemoryStats {
        dimensionId = WorldPos.requireId(dimensionId, "dimensionId");
        eventCount = Math.max(0L, eventCount);
        visitCount = Math.max(0L, visitCount);
        deathCount = Math.max(0L, deathCount);
        combatEventCount = Math.max(0L, combatEventCount);
        buildEventCount = Math.max(0L, buildEventCount);
        totalImportance = Math.max(0.0, totalImportance);
        lastEventGameTime = Math.max(0L, lastEventGameTime);
        if (eventCount <= 0L) {
            minY = 0;
            maxY = 0;
        } else if (minY > maxY) {
            int previousMinY = minY;
            minY = maxY;
            maxY = previousMinY;
        }
        eventTypeCounts = normalizedEventTypeCounts(eventTypeCounts);
        deathSiteEnvironmentCounts = normalizedDeathSiteEnvironmentCounts(deathSiteEnvironmentCounts);
        metadataCounts = normalizedMetadataCounts(metadataCounts);
        candidateDecayStates = normalizedCandidateDecayStates(candidateDecayStates);
    }

    public static ChunkMemoryStats empty(String dimensionId, int chunkX, int chunkZ) {
        return new ChunkMemoryStats(dimensionId, chunkX, chunkZ, 0L, 0L, 0L, 0L, 0L, 0.0, 0L, 0, 0, Map.of(), Map.of(), Map.of(), Map.of());
    }

    public static ChunkMemoryStats emptyFor(WorldPos position) {
        return empty(position.dimensionId(), position.chunkX(), position.chunkZ());
    }

    public boolean contains(WorldPos position) {
        return position != null
                && dimensionId.equals(position.dimensionId())
                && chunkX == position.chunkX()
                && chunkZ == position.chunkZ();
    }

    public boolean matches(WorldMemoryEvent event) {
        return event != null && contains(event.position());
    }

    public ChunkMemoryStats updatedWith(WorldMemoryEvent event) {
        if (!matches(event)) {
            return this;
        }

        EventType type = event.eventType();
        return new ChunkMemoryStats(
                dimensionId,
                chunkX,
                chunkZ,
                eventCount + 1L,
                visitCount + (type.isVisit() ? 1L : 0L),
                deathCount + (type.isDeath() ? 1L : 0L),
                combatEventCount + (type.isCombat() ? 1L : 0L),
                buildEventCount + (type.isBuildRelated() ? 1L : 0L),
                totalImportance + event.basicScore(),
                Math.max(lastEventGameTime, event.gameTime()),
                eventCount == 0L ? event.position().y() : Math.min(minY, event.position().y()),
                eventCount == 0L ? event.position().y() : Math.max(maxY, event.position().y()),
                incrementedEventTypeCounts(eventTypeCounts, type),
                incrementedDeathSiteEnvironmentCounts(deathSiteEnvironmentCounts, event),
                incrementedMetadataCounts(metadataCounts, event),
                candidateDecayStates
        );
    }

    public double basicScore() {
        return eventCount
                + visitCount * 0.25
                + deathCount * 4.0
                + combatEventCount * 2.0
                + buildEventCount * 0.5
                + totalImportance;
    }

    public String chunkIdString() {
        return WorldPos.chunkIdString(dimensionId, chunkX, chunkZ);
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

    public DeathSiteEnvironment primaryDeathSiteEnvironment() {
        DeathSiteEnvironment primary = DeathSiteEnvironment.UNKNOWN;
        long highestCount = 0L;

        for (DeathSiteEnvironment environment : DeathSiteEnvironment.values()) {
            if (environment == DeathSiteEnvironment.UNKNOWN) {
                continue;
            }

            long count = deathSiteEnvironmentCount(environment);
            if (count > highestCount) {
                primary = environment;
                highestCount = count;
            }
        }

        return primary;
    }

    public Map<String, Long> deathSiteEnvironmentCounts() {
        return Collections.unmodifiableMap(deathSiteEnvironmentCounts);
    }

    public Map<String, Long> metadataCounts() {
        return Collections.unmodifiableMap(metadataCounts);
    }

    public Map<String, CandidateDecayState> candidateDecayStates() {
        return Collections.unmodifiableMap(candidateDecayStates);
    }

    public CandidateDecayState candidateDecayState(PlaceType placeType) {
        PlaceType resolvedType = placeType == null ? PlaceType.UNKNOWN : placeType;
        return candidateDecayStates.get(resolvedType.name());
    }

    public ChunkMemoryStats withCandidateDecayState(PlaceType placeType, CandidateDecayState state) {
        PlaceType resolvedType = placeType == null ? PlaceType.UNKNOWN : placeType;
        if (resolvedType == PlaceType.UNKNOWN || state == null) {
            return this;
        }

        Map<String, CandidateDecayState> updatedStates = new LinkedHashMap<>(candidateDecayStates);
        updatedStates.put(resolvedType.name(), state);
        return new ChunkMemoryStats(
                dimensionId,
                chunkX,
                chunkZ,
                eventCount,
                visitCount,
                deathCount,
                combatEventCount,
                buildEventCount,
                totalImportance,
                lastEventGameTime,
                minY,
                maxY,
                eventTypeCounts,
                deathSiteEnvironmentCounts,
                metadataCounts,
                updatedStates
        );
    }

    public ChunkMemoryStats refreshedCandidateActivity(PlaceType placeType, double score, long gameTime) {
        PlaceType resolvedType = placeType == null ? PlaceType.UNKNOWN : placeType;
        if (resolvedType == PlaceType.UNKNOWN || score <= 0.0) {
            return this;
        }

        CandidateDecayState previous = candidateDecayState(resolvedType);
        CandidateDecayState refreshed = previous == null
                ? new CandidateDecayState(score, gameTime, 0L)
                : previous.refreshed(score, gameTime);
        return withCandidateDecayState(resolvedType, refreshed);
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

    private static Map<String, CandidateDecayState> normalizedCandidateDecayStates(Map<String, CandidateDecayState> states) {
        Map<String, CandidateDecayState> result = new LinkedHashMap<>();
        if (states == null) {
            return result;
        }

        for (Map.Entry<String, CandidateDecayState> entry : states.entrySet()) {
            PlaceType placeType = PlaceType.fromId(entry.getKey());
            CandidateDecayState state = entry.getValue();
            if (placeType != PlaceType.UNKNOWN && state != null) {
                result.put(placeType.name(), state);
            }
        }

        return result;
    }
}
