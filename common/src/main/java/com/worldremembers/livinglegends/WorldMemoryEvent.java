package com.worldremembers.livinglegends;

import java.io.Serializable;
import java.util.Objects;

public record WorldMemoryEvent(
        String eventId,
        EventType eventType,
        WorldPos position,
        String actorId,
        String subjectId,
        long gameTime,
        long createdAtEpochMillis,
        double importance,
        String note,
        String firstDiscoveryKey,
        String structureId,
        PlaceBounds structureBounds
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public WorldMemoryEvent(
            String eventId,
            EventType eventType,
            WorldPos position,
            String actorId,
            String subjectId,
            long gameTime,
            long createdAtEpochMillis,
            double importance,
            String note
    ) {
        this(
                eventId,
                eventType,
                position,
                actorId,
                subjectId,
                gameTime,
                createdAtEpochMillis,
                importance,
                note,
                "",
                "",
                null
        );
    }

    public WorldMemoryEvent {
        eventId = WorldPos.optionalId(eventId);
        eventType = EventType.fromId(Objects.requireNonNullElse(eventType, EventType.CUSTOM).idString());
        position = Objects.requireNonNull(position, "position");
        actorId = WorldPos.optionalId(actorId);
        subjectId = WorldPos.optionalId(subjectId);
        importance = Math.max(0.0, importance);
        note = note == null ? "" : note;
        firstDiscoveryKey = WorldPos.optionalId(firstDiscoveryKey);
        structureId = WorldPos.optionalId(structureId);
    }

    public double basicScore() {
        return eventType.scoreWeight() + importance;
    }

    public String eventIdString() {
        if (!eventId.isBlank()) {
            return eventId;
        }

        return eventType.idString() + ":" + position.blockIdString() + ":" + gameTime;
    }

    public String actorIdString() {
        return actorId;
    }

    public String subjectIdString() {
        return subjectId;
    }

    public String firstDiscoveryKeyString() {
        return firstDiscoveryKey;
    }

    public String structureIdString() {
        return structureId;
    }

    public boolean hasStructureBounds() {
        return structureBounds != null;
    }
}
