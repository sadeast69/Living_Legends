package com.worldremembers.livinglegends;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class WorldMemoryStorageData {
    public static final int CURRENT_SCHEMA_VERSION = WorldRemembersDataVersions.CURRENT_WORLD_STATE_VERSION;

    private int schemaVersion;
    private final Map<String, ChunkMemoryStats> chunkStatsByKey;
    private final List<NamedPlace> namedPlaces;
    private final Map<String, WorldFirstDiscoveryRecord> firstDiscoveries;
    private final Map<String, WorldMemoryEvent> firstWorldEvents;
    private final Map<String, WorldMemoryEvent> firstPlayerWorldEvents;
    private final Map<String, Long> eventCounters;
    private final List<DeletedPlaceMarker> deletedPlaceMarkers;
    private final Map<String, LinkedHashSet<String>> discoveredPlaceIdsByPlayer;

    public WorldMemoryStorageData() {
        this(
                CURRENT_SCHEMA_VERSION,
                new LinkedHashMap<>(),
                new ArrayList<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new ArrayList<>(),
                new LinkedHashMap<>()
        );
    }

    public WorldMemoryStorageData(
            int schemaVersion,
            Map<String, ChunkMemoryStats> chunkStatsByKey,
            List<NamedPlace> namedPlaces,
            Map<String, WorldMemoryEvent> firstWorldEvents,
            Map<String, WorldMemoryEvent> firstPlayerWorldEvents,
            Map<String, Long> eventCounters
    ) {
        this(
                schemaVersion,
                chunkStatsByKey,
                namedPlaces,
                new LinkedHashMap<>(),
                firstWorldEvents,
                firstPlayerWorldEvents,
                eventCounters,
                new ArrayList<>(),
                new LinkedHashMap<>()
        );
    }

    public WorldMemoryStorageData(
            int schemaVersion,
            Map<String, ChunkMemoryStats> chunkStatsByKey,
            List<NamedPlace> namedPlaces,
            Map<String, WorldFirstDiscoveryRecord> firstDiscoveries,
            Map<String, WorldMemoryEvent> firstWorldEvents,
            Map<String, WorldMemoryEvent> firstPlayerWorldEvents,
            Map<String, Long> eventCounters
    ) {
        this(
                schemaVersion,
                chunkStatsByKey,
                namedPlaces,
                firstDiscoveries,
                firstWorldEvents,
                firstPlayerWorldEvents,
                eventCounters,
                new ArrayList<>(),
                new LinkedHashMap<>()
        );
    }

    public WorldMemoryStorageData(
            int schemaVersion,
            Map<String, ChunkMemoryStats> chunkStatsByKey,
            List<NamedPlace> namedPlaces,
            Map<String, WorldFirstDiscoveryRecord> firstDiscoveries,
            Map<String, WorldMemoryEvent> firstWorldEvents,
            Map<String, WorldMemoryEvent> firstPlayerWorldEvents,
            Map<String, Long> eventCounters,
            List<DeletedPlaceMarker> deletedPlaceMarkers
    ) {
        this(
                schemaVersion,
                chunkStatsByKey,
                namedPlaces,
                firstDiscoveries,
                firstWorldEvents,
                firstPlayerWorldEvents,
                eventCounters,
                deletedPlaceMarkers,
                new LinkedHashMap<>()
        );
    }

    public WorldMemoryStorageData(
            int schemaVersion,
            Map<String, ChunkMemoryStats> chunkStatsByKey,
            List<NamedPlace> namedPlaces,
            Map<String, WorldFirstDiscoveryRecord> firstDiscoveries,
            Map<String, WorldMemoryEvent> firstWorldEvents,
            Map<String, WorldMemoryEvent> firstPlayerWorldEvents,
            Map<String, Long> eventCounters,
            List<DeletedPlaceMarker> deletedPlaceMarkers,
            Map<String, ? extends Iterable<String>> discoveredPlaceIdsByPlayer
    ) {
        this.schemaVersion = Math.max(CURRENT_SCHEMA_VERSION, schemaVersion);
        this.chunkStatsByKey = new LinkedHashMap<>(Objects.requireNonNullElseGet(chunkStatsByKey, LinkedHashMap::new));
        this.namedPlaces = new ArrayList<>(Objects.requireNonNullElseGet(namedPlaces, ArrayList::new));
        this.firstDiscoveries = normalizedFirstDiscoveries(firstDiscoveries);
        this.firstWorldEvents = new LinkedHashMap<>(Objects.requireNonNullElseGet(firstWorldEvents, LinkedHashMap::new));
        this.firstPlayerWorldEvents = new LinkedHashMap<>(Objects.requireNonNullElseGet(firstPlayerWorldEvents, LinkedHashMap::new));
        this.eventCounters = normalizedEventCounters(eventCounters);
        this.deletedPlaceMarkers = new ArrayList<>(Objects.requireNonNullElseGet(deletedPlaceMarkers, ArrayList::new));
        this.discoveredPlaceIdsByPlayer = normalizedDiscoveredPlaces(discoveredPlaceIdsByPlayer);
        ensureEventCounters();
    }

    public synchronized boolean recordEvent(WorldMemoryEvent event) {
        if (event == null) {
            return false;
        }

        boolean changed = incrementEventCounter(event.eventType());
        changed |= recordFirstWorldEvent(event);
        changed |= recordFirstPlayerWorldEvent(event);
        changed |= updateChunkStats(event) != null;
        changed |= recordMatchingFirstDiscoveries(event);
        return changed;
    }

    public synchronized boolean hasFirstPlayerWorldEvent(String playerId, String dimensionId, EventType eventType) {
        return firstPlayerWorldEvents.containsKey(firstPlayerWorldEventKey(playerId, dimensionId, eventType));
    }

    public synchronized int schemaVersion() {
        return schemaVersion;
    }

    public synchronized void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = Math.max(1, schemaVersion);
    }

    public synchronized Map<String, ChunkMemoryStats> chunkStatsByKey() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(chunkStatsByKey));
    }

    public synchronized ChunkMemoryStats chunkStats(String dimensionId, int chunkX, int chunkZ) {
        return chunkStatsByKey.get(chunkStatsKey(dimensionId, chunkX, chunkZ));
    }

    public synchronized List<NamedPlace> namedPlaces() {
        return Collections.unmodifiableList(new ArrayList<>(namedPlaces));
    }

    public synchronized List<DeletedPlaceMarker> deletedPlaceMarkers() {
        return Collections.unmodifiableList(new ArrayList<>(deletedPlaceMarkers));
    }

    public synchronized NamedPlace namedPlace(String placeId) {
        String id = WorldPos.optionalId(placeId);
        if (id.isBlank()) {
            return null;
        }
        for (NamedPlace place : namedPlaces) {
            if (place.placeIdString().equals(id)) {
                return place;
            }
        }
        return null;
    }

    public synchronized Map<String, WorldMemoryEvent> firstWorldEvents() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(firstWorldEvents));
    }

    public synchronized Map<String, WorldFirstDiscoveryRecord> firstDiscoveries() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(firstDiscoveries));
    }

    public synchronized Map<String, WorldMemoryEvent> firstPlayerWorldEvents() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(firstPlayerWorldEvents));
    }

    public synchronized Map<String, Long> eventCounters() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(eventCounters));
    }

    public synchronized Map<String, List<String>> discoveredPlaceIdsByPlayer() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashSet<String>> entry : discoveredPlaceIdsByPlayer.entrySet()) {
            result.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    public synchronized Set<String> discoveredPlaceIds(String playerId) {
        String id = WorldPos.optionalId(playerId);
        if (id.isBlank()) {
            return Set.of();
        }
        LinkedHashSet<String> places = discoveredPlaceIdsByPlayer.get(id);
        return places == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(places));
    }

    public synchronized boolean recordDiscoveredPlace(String playerId, String placeId) {
        String player = WorldPos.optionalId(playerId);
        String place = WorldPos.optionalId(placeId);
        if (player.isBlank() || place.isBlank()) {
            return false;
        }
        LinkedHashSet<String> places = discoveredPlaceIdsByPlayer.computeIfAbsent(player, ignored -> new LinkedHashSet<>());
        return places.add(place);
    }

    public synchronized int chunkStatsCount() {
        return chunkStatsByKey.size();
    }

    public synchronized int namedPlaceCount() {
        return namedPlaces.size();
    }

    public synchronized int deletedPlaceMarkerCount() {
        return deletedPlaceMarkers.size();
    }

    public synchronized int firstEventCount() {
        return firstDiscoveries.size() + firstWorldEvents.size() + firstPlayerWorldEvents.size();
    }

    public static String chunkStatsKey(WorldPos position) {
        return chunkStatsKey(position.dimensionId(), position.chunkX(), position.chunkZ());
    }

    public static String chunkStatsKey(String dimensionId, int chunkX, int chunkZ) {
        return WorldPos.chunkIdString(dimensionId, chunkX, chunkZ);
    }

    public static String firstPlayerWorldEventKey(String playerId, String dimensionId, EventType eventType) {
        return WorldPos.optionalId(playerId)
                + "@" + WorldPos.requireId(dimensionId, "dimensionId")
                + ":" + Objects.requireNonNullElse(eventType, EventType.CUSTOM).idString();
    }

    public synchronized ChunkMemoryStats updateChunkStats(WorldMemoryEvent event) {
        if (event == null) {
            return null;
        }

        String key = chunkStatsKey(event.position());
        ChunkMemoryStats previous = chunkStatsByKey.getOrDefault(key, ChunkMemoryStats.emptyFor(event.position()));
        ChunkMemoryStats rawUpdated = previous.updatedWith(event);
        ChunkMemoryStats updated = rawUpdated;
        for (PlaceType placeType : CandidateDecayEngine.relevantPlaceTypes(event)) {
            double previousRawScore = ScoreEngine.scoreChunkRaw(previous, placeType, this);
            double updatedRawScore = ScoreEngine.scoreChunkRaw(rawUpdated, placeType, this);
            if (updatedRawScore <= 0.0) {
                continue;
            }
            CandidateDecayState previousState = previous.candidateDecayState(placeType);
            double scoreIncrease = Math.max(0.0, updatedRawScore - previousRawScore);
            double candidateScore = previousState == null
                    ? updatedRawScore
                    : previousState.score() + scoreIncrease;
            updated = updated.withCandidateDecayState(
                    placeType,
                    new CandidateDecayState(
                            candidateScore,
                            event.gameTime(),
                            previousState == null ? 0L : previousState.lastDecayGameTime()
                    )
            );
        }
        chunkStatsByKey.put(key, updated);
        return updated.equals(previous) ? null : updated;
    }

    public synchronized boolean replaceChunkStats(ChunkMemoryStats stats) {
        if (stats == null) {
            return false;
        }

        String key = chunkStatsKey(stats.dimensionId(), stats.chunkX(), stats.chunkZ());
        ChunkMemoryStats previous = chunkStatsByKey.get(key);
        if (stats.equals(previous)) {
            return false;
        }
        chunkStatsByKey.put(key, stats);
        return true;
    }

    public synchronized CandidateDecayState candidateDecayState(
            String dimensionId,
            int chunkX,
            int chunkZ,
            PlaceType placeType
    ) {
        ChunkMemoryStats stats = chunkStats(dimensionId, chunkX, chunkZ);
        return stats == null ? null : stats.candidateDecayState(placeType);
    }

    public synchronized boolean setCandidateDecayScore(
            String dimensionId,
            int chunkX,
            int chunkZ,
            PlaceType placeType,
            double score,
            long gameTime
    ) {
        PlaceType resolvedType = placeType == null ? PlaceType.UNKNOWN : placeType;
        if (resolvedType == PlaceType.UNKNOWN) {
            return false;
        }

        String key = chunkStatsKey(dimensionId, chunkX, chunkZ);
        ChunkMemoryStats previous = chunkStatsByKey.getOrDefault(key, ChunkMemoryStats.empty(dimensionId, chunkX, chunkZ));
        CandidateDecayState previousState = previous.candidateDecayState(resolvedType);
        long lastRelevant = previousState == null
                ? Math.max(previous.lastEventGameTime(), gameTime)
                : Math.max(previousState.lastRelevantEventGameTime(), gameTime);
        long lastDecay = previousState == null ? 0L : previousState.lastDecayGameTime();
        ChunkMemoryStats updated = previous.withCandidateDecayState(
                resolvedType,
                new CandidateDecayState(score, lastRelevant, lastDecay)
        );
        chunkStatsByKey.put(key, updated);
        return !updated.equals(previous);
    }

    public synchronized boolean touchCandidateDecay(
            String dimensionId,
            int chunkX,
            int chunkZ,
            long gameTime
    ) {
        String key = chunkStatsKey(dimensionId, chunkX, chunkZ);
        ChunkMemoryStats previous = chunkStatsByKey.getOrDefault(key, ChunkMemoryStats.empty(dimensionId, chunkX, chunkZ));
        ChunkMemoryStats updated = previous;
        for (PlaceType placeType : ScoreEngine.alphaCandidateTypes()) {
            CandidateDecayState state = updated.candidateDecayState(placeType);
            double rawScore = ScoreEngine.scoreChunkRaw(updated, placeType, this);
            if (state == null && rawScore <= 0.0) {
                continue;
            }
            double score = state == null ? rawScore : Math.max(state.score(), rawScore);
            updated = updated.withCandidateDecayState(placeType, new CandidateDecayState(score, gameTime, state == null ? 0L : state.lastDecayGameTime()));
        }
        chunkStatsByKey.put(key, updated);
        return !updated.equals(previous);
    }

    public synchronized double effectiveCandidateScore(ChunkMemoryStats stats, PlaceType placeType, double rawScore) {
        if (stats == null || rawScore <= 0.0) {
            return Math.max(0.0, rawScore);
        }

        PlaceType resolvedType = placeType == null ? PlaceType.UNKNOWN : placeType;
        if (resolvedType == PlaceType.UNKNOWN
                || candidateActivityProtected(stats.dimensionId(), stats.chunkX(), stats.chunkZ(), resolvedType)) {
            return rawScore;
        }

        CandidateDecayState state = stats.candidateDecayState(resolvedType);
        if (state == null) {
            return rawScore;
        }
        return Math.max(0.0, Math.min(rawScore, state.score()));
    }

    public synchronized boolean candidateActivityProtected(
            String dimensionId,
            int chunkX,
            int chunkZ,
            PlaceType placeType
    ) {
        PlaceType resolvedType = placeType == null ? PlaceType.UNKNOWN : placeType;
        if (resolvedType == PlaceType.UNKNOWN) {
            return false;
        }

        String key = chunkStatsKey(dimensionId, chunkX, chunkZ);
        WorldPos chunkCenter = new WorldPos(dimensionId, chunkX * 16 + 8, 64, chunkZ * 16 + 8);
        for (NamedPlace place : namedPlaces) {
            if (place == null || place.placeType() != resolvedType || !dimensionId.equals(place.dimensionId())) {
                continue;
            }
            if (place.sourceChunks().contains(key)) {
                return true;
            }
            WorldPos yAdjustedCenter = new WorldPos(dimensionId, chunkCenter.x(), place.center().y(), chunkCenter.z());
            if (place.bounds().contains(yAdjustedCenter)) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean updateNamedPlaces(WorldMemoryEvent event) {
        boolean changed = false;

        for (int index = 0; index < namedPlaces.size(); index++) {
            NamedPlace previous = namedPlaces.get(index);
            NamedPlace updated = previous.updatedWith(event);
            if (!updated.equals(previous)) {
                namedPlaces.set(index, updated);
                changed = true;
            }
        }

        return changed;
    }

    public synchronized boolean upsertNamedPlace(NamedPlace place) {
        if (place == null) {
            return false;
        }

        for (int index = 0; index < namedPlaces.size(); index++) {
            NamedPlace previous = namedPlaces.get(index);
            if (previous.placeIdString().equals(place.placeIdString())) {
                if (previous.equals(place)) {
                    return false;
                }
                namedPlaces.set(index, place);
                return true;
            }
        }

        namedPlaces.add(place);
        return true;
    }

    public synchronized boolean deleteNamedPlace(String placeId) {
        String id = WorldPos.optionalId(placeId);
        if (id.isBlank()) {
            return false;
        }
        return namedPlaces.removeIf(place -> place.placeIdString().equals(id));
    }

    public synchronized boolean recordDeletedPlaceMarker(DeletedPlaceMarker marker) {
        if (marker == null) {
            return false;
        }
        removeDeletedPlaceMarker(marker.originalPlaceId());
        deletedPlaceMarkers.add(marker);
        return true;
    }

    public synchronized boolean removeDeletedPlaceMarker(String originalPlaceId) {
        String id = WorldPos.optionalId(originalPlaceId);
        if (id.isBlank()) {
            return false;
        }
        return deletedPlaceMarkers.removeIf(marker -> marker.originalPlaceId().equals(id));
    }

    public synchronized int removeMatchingDeletedPlaceMarkers(NamedPlace place, com.worldremembers.livinglegends.config.LivingLegendsConfig config) {
        if (place == null) {
            return 0;
        }
        int before = deletedPlaceMarkers.size();
        deletedPlaceMarkers.removeIf(marker -> marker.matchesImportedPlace(place, config));
        return before - deletedPlaceMarkers.size();
    }

    public synchronized int clearDeletedPlaceMarkers() {
        int count = deletedPlaceMarkers.size();
        deletedPlaceMarkers.clear();
        return count;
    }

    public synchronized DeletedPlaceMarker suppressingDeletedMarker(
            PlaceCluster cluster,
            com.worldremembers.livinglegends.config.LivingLegendsConfig config,
            long currentGameTime
    ) {
        if (cluster == null || config == null || config.generation == null || !config.generation.deletedPlaceSuppressionEnabled) {
            return null;
        }
        if (config.generation.deletedPlaceSuppressionDays == 0) {
            return null;
        }

        boolean removedExpired = deletedPlaceMarkers.removeIf(marker -> marker.expired(currentGameTime, config));
        for (DeletedPlaceMarker marker : deletedPlaceMarkers) {
            if (marker.suppresses(cluster, config, currentGameTime)) {
                return marker;
            }
        }
        return removedExpired ? null : null;
    }

    public synchronized boolean incrementEventCounter(EventType eventType) {
        EventType resolvedType = Objects.requireNonNullElse(eventType, EventType.CUSTOM);
        String key = resolvedType.idString();
        eventCounters.put(key, eventCounters.getOrDefault(key, 0L) + 1L);
        return true;
    }

    public synchronized boolean recordFirstWorldEvent(WorldMemoryEvent event) {
        if (event == null) {
            return false;
        }

        String key = event.eventType().idString();
        if (firstWorldEvents.containsKey(key)) {
            return false;
        }

        firstWorldEvents.put(key, event);
        return true;
    }

    public synchronized boolean hasWorldFirstDiscovery(String discoveryId) {
        String key = WorldPos.optionalId(discoveryId);
        return !key.isBlank() && firstDiscoveries.containsKey(key);
    }

    public synchronized boolean recordWorldFirstDiscovery(WorldFirstDiscoveryRecord record) {
        if (record == null) {
            return false;
        }

        String key = record.discoveryIdString();
        if (firstDiscoveries.containsKey(key)) {
            return false;
        }

        firstDiscoveries.put(key, record);
        return true;
    }

    public synchronized WorldFirstDiscoveryRecord firstDiscovery(String discoveryId) {
        String key = WorldPos.optionalId(discoveryId);
        return key.isBlank() ? null : firstDiscoveries.get(key);
    }

    public synchronized boolean recordFirstPlayerWorldEvent(WorldMemoryEvent event) {
        if (event == null) {
            return false;
        }

        if (event.actorIdString().isBlank()) {
            return false;
        }

        String key = firstPlayerWorldEventKey(event.actorIdString(), event.position().dimensionId(), event.eventType());
        if (firstPlayerWorldEvents.containsKey(key)) {
            return false;
        }

        firstPlayerWorldEvents.put(key, event);
        return true;
    }

    private void ensureEventCounters() {
        for (EventType eventType : EventType.values()) {
            eventCounters.putIfAbsent(eventType.idString(), 0L);
        }
    }

    private synchronized boolean recordMatchingFirstDiscoveries(WorldMemoryEvent event) {
        boolean changed = false;
        for (FirstDiscoveryDefinition definition : FirstDiscoveryDefinitions.matchingDefinitions(event)) {
            WorldFirstDiscoveryRecord record = WorldFirstDiscoveryRecord.from(definition, event);
            if (recordWorldFirstDiscovery(record)) {
                WorldMemoryEvent discoveryEvent = record.toDiscoveryEvent();
                changed |= incrementEventCounter(discoveryEvent.eventType());
                changed |= updateChunkStats(discoveryEvent) != null;
            }
        }
        return changed;
    }

    private static Map<String, WorldFirstDiscoveryRecord> normalizedFirstDiscoveries(
            Map<String, WorldFirstDiscoveryRecord> records
    ) {
        Map<String, WorldFirstDiscoveryRecord> result = new LinkedHashMap<>();
        if (records == null) {
            return result;
        }

        for (Map.Entry<String, WorldFirstDiscoveryRecord> entry : records.entrySet()) {
            WorldFirstDiscoveryRecord record = entry.getValue();
            if (record == null) {
                continue;
            }

            String key = record.discoveryIdString();
            result.putIfAbsent(key, record);
        }

        return result;
    }

    private static Map<String, Long> normalizedEventCounters(Map<String, Long> counters) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (counters == null) {
            return result;
        }

        for (Map.Entry<String, Long> counter : counters.entrySet()) {
            String normalizedKey = EventType.fromId(counter.getKey()).idString();
            long value = Math.max(0L, counter.getValue() == null ? 0L : counter.getValue());
            result.put(normalizedKey, result.getOrDefault(normalizedKey, 0L) + value);
        }

        return result;
    }

    private static Map<String, LinkedHashSet<String>> normalizedDiscoveredPlaces(
            Map<String, ? extends Iterable<String>> values
    ) {
        Map<String, LinkedHashSet<String>> result = new LinkedHashMap<>();
        if (values == null) {
            return result;
        }

        for (Map.Entry<String, ? extends Iterable<String>> entry : values.entrySet()) {
            String playerId = WorldPos.optionalId(entry.getKey());
            if (playerId.isBlank() || entry.getValue() == null) {
                continue;
            }
            LinkedHashSet<String> places = new LinkedHashSet<>();
            for (String placeId : entry.getValue()) {
                String normalizedPlaceId = WorldPos.optionalId(placeId);
                if (!normalizedPlaceId.isBlank()) {
                    places.add(normalizedPlaceId);
                }
            }
            if (!places.isEmpty()) {
                result.put(playerId, places);
            }
        }
        return result;
    }
}
