package com.worldremembers.livinglegends;

import java.util.Locale;

public enum DiscoveryTriggerType {
    DIMENSION_ENTRY("dimension_entry"),
    BLOCK_MINED("block_mined"),
    ENTITY_KILLED("entity_killed"),
    BLOCK_DISCOVERED("block_discovered"),
    STRUCTURE_DISCOVERED("structure_discovered"),
    CUSTOM("custom");

    private final String id;

    DiscoveryTriggerType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String idString() {
        return id;
    }

    public static DiscoveryTriggerType fromId(String id) {
        String normalized = normalizeId(id);
        for (DiscoveryTriggerType type : values()) {
            if (type.id.equals(normalized)) {
                return type;
            }
        }
        return CUSTOM;
    }

    private static String normalizeId(String id) {
        if (id == null || id.isBlank()) {
            return CUSTOM.id;
        }

        String normalized = id.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "dimension", "dimension_entered", "player_entered_dimension" -> DIMENSION_ENTRY.id;
            case "block_broken", "block_break", "valuable_block_mined" -> BLOCK_MINED.id;
            case "entity_kill", "entity_death", "boss_killed" -> ENTITY_KILLED.id;
            case "block_found" -> BLOCK_DISCOVERED.id;
            case "structure_found" -> STRUCTURE_DISCOVERED.id;
            default -> normalized;
        };
    }
}
