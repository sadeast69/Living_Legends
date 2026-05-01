package com.worldremembers.livinglegends;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record WorldFirstDiscoveryRecord(
        String discoveryId,
        DiscoveryTriggerType triggerType,
        String targetId,
        PlaceType placeType,
        double weight,
        Map<String, String> nameTokens,
        String sourceEventId,
        EventType sourceEventType,
        String actorId,
        String playerName,
        WorldPos position,
        long gameTime,
        long createdAtEpochMillis,
        String structureId,
        PlaceBounds structureBounds,
        boolean useStructureBounds,
        int fallbackRadius
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public WorldFirstDiscoveryRecord(
            String discoveryId,
            DiscoveryTriggerType triggerType,
            String targetId,
            PlaceType placeType,
            double weight,
            Map<String, String> nameTokens,
            String sourceEventId,
            EventType sourceEventType,
            String actorId,
            String playerName,
            WorldPos position,
            long gameTime,
            long createdAtEpochMillis
    ) {
        this(
                discoveryId,
                triggerType,
                targetId,
                placeType,
                weight,
                nameTokens,
                sourceEventId,
                sourceEventType,
                actorId,
                playerName,
                position,
                gameTime,
                createdAtEpochMillis,
                "",
                null,
                false,
                0
        );
    }

    public WorldFirstDiscoveryRecord {
        discoveryId = WorldPos.requireId(discoveryId, "discoveryId");
        triggerType = DiscoveryTriggerType.fromId(
                Objects.requireNonNullElse(triggerType, DiscoveryTriggerType.CUSTOM).idString()
        );
        targetId = WorldPos.requireId(targetId, "targetId");
        placeType = PlaceType.fromId(Objects.requireNonNullElse(placeType, PlaceType.FIRST_DISCOVERY).idString());
        weight = Math.max(0.0, weight);
        nameTokens = normalizedNameTokens(nameTokens);
        sourceEventId = WorldPos.optionalId(sourceEventId);
        sourceEventType = EventType.fromId(Objects.requireNonNullElse(sourceEventType, EventType.CUSTOM).idString());
        actorId = WorldPos.optionalId(actorId);
        playerName = playerName == null || playerName.isBlank() ? actorId : playerName.trim();
        position = Objects.requireNonNull(position, "position");
        gameTime = Math.max(0L, gameTime);
        createdAtEpochMillis = Math.max(0L, createdAtEpochMillis);
        structureId = WorldPos.optionalId(structureId);
        fallbackRadius = Math.max(0, fallbackRadius);
    }

    public static WorldFirstDiscoveryRecord from(FirstDiscoveryDefinition definition, WorldMemoryEvent sourceEvent) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(sourceEvent, "sourceEvent");
        return new WorldFirstDiscoveryRecord(
                definition.discoveryIdString(),
                definition.triggerType(),
                definition.targetIdString(),
                definition.placeType(),
                definition.weight(),
                definition.nameTokens(),
                sourceEvent.eventIdString(),
                sourceEvent.eventType(),
                sourceEvent.actorIdString(),
                playerName(sourceEvent),
                sourceEvent.position(),
                sourceEvent.gameTime(),
                sourceEvent.createdAtEpochMillis(),
                structureId(definition, sourceEvent),
                definition.useStructureBounds() ? sourceEvent.structureBounds() : null,
                definition.useStructureBounds(),
                definition.fallbackRadius()
        );
    }

    public String discoveryIdString() {
        return discoveryId;
    }

    public String triggerTypeIdString() {
        return triggerType.idString();
    }

    public String targetIdString() {
        return targetId;
    }

    public Map<String, String> nameTokens() {
        return Collections.unmodifiableMap(nameTokens);
    }

    public String structureIdString() {
        return structureId;
    }

    public PlaceBounds effectiveBounds() {
        if (structureBounds != null) {
            return structureBounds;
        }
        if (useStructureBounds && fallbackRadius > 0) {
            return PlaceBounds.around(position, fallbackRadius, fallbackRadius);
        }
        return null;
    }

    public WorldMemoryEvent toDiscoveryEvent() {
        return new WorldMemoryEvent(
                EventType.DISCOVERY.idString() + ":" + discoveryId + ":" + position.blockIdString() + ":" + gameTime,
                EventType.DISCOVERY,
                position,
                actorId,
                targetId,
                gameTime,
                createdAtEpochMillis,
                weight,
                discoveryNote(),
                discoveryId,
                structureId,
                effectiveBounds()
        );
    }

    public String discoveryNote() {
        return "first_discovery"
                + " firstDiscoveryKey=" + discoveryId
                + " scope=world"
                + " triggerType=" + triggerType.idString()
                + " targetId=" + targetId
                + " player_name=" + safeToken(playerName)
                + " sourceEventType=" + sourceEventType.idString()
                + (structureId.isBlank() ? "" : " structureId=" + structureId)
                + (effectiveBounds() == null ? "" : " bounds=" + effectiveBounds().shape().idString());
    }

    public boolean matches(ChunkMemoryStats stats) {
        return stats != null && stats.contains(position);
    }

    private static String playerName(WorldMemoryEvent event) {
        String fromNote = noteValue(event.note(), "player_name");
        if (!fromNote.isBlank()) {
            return fromNote;
        }

        fromNote = noteValue(event.note(), "playerName");
        if (!fromNote.isBlank()) {
            return fromNote;
        }

        return event.actorIdString();
    }

    private static String structureId(FirstDiscoveryDefinition definition, WorldMemoryEvent event) {
        if (definition.triggerType() != DiscoveryTriggerType.STRUCTURE_DISCOVERED) {
            return "";
        }
        String fromEvent = event.structureIdString();
        return fromEvent.isBlank() ? definition.targetIdString() : fromEvent;
    }

    private static String noteValue(String note, String key) {
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

    private static String safeToken(String value) {
        return value == null ? "" : value.trim().replace(' ', '_');
    }

    private static Map<String, String> normalizedNameTokens(Map<String, String> tokens) {
        Map<String, String> result = new LinkedHashMap<>();
        if (tokens == null) {
            return result;
        }

        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            String key = WorldPos.optionalId(entry.getKey());
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            if (!key.isBlank() && !value.isBlank()) {
                result.put(key, value);
            }
        }

        return result;
    }
}
