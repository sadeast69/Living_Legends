package com.worldremembers.livinglegends;

import java.util.Locale;

public enum PlaceCauseType {
    PLAYER_DEATHS("player_deaths"),
    MOB_BATTLE("mob_battle"),
    PASSIVE_SLAUGHTER("passive_slaughter"),
    PVP("pvp"),
    MINING("mining"),
    PORTAL_USAGE("portal_usage"),
    VISITS("visits"),
    SETTLEMENT("settlement"),
    FIRST_STRUCTURE_DISCOVERY("first_structure_discovery"),
    FIRST_BLOCK_DISCOVERY("first_block_discovery"),
    FIRST_DIMENSION_DISCOVERY("first_dimension_discovery"),
    FIRST_BOSS_KILL("first_boss_kill"),
    BOSS_KILL("boss_kill"),
    PET_DEATH("pet_death"),
    NAMED_MOB_DEATH("named_mob_death"),
    RAID("raid"),
    DIMENSION_THRESHOLD("dimension_threshold"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String id;

    PlaceCauseType(String id) {
        this.id = id;
    }

    public String idString() {
        return id;
    }

    public boolean isFirstDiscovery() {
        return this == FIRST_STRUCTURE_DISCOVERY
                || this == FIRST_BLOCK_DISCOVERY
                || this == FIRST_DIMENSION_DISCOVERY
                || this == FIRST_BOSS_KILL;
    }

    public static PlaceCauseType fromId(String id) {
        if (id == null || id.isBlank()) {
            return UNKNOWN;
        }

        String normalized = id.trim().toLowerCase(Locale.ROOT);
        normalized = switch (normalized) {
            case "pet_died" -> PET_DEATH.id;
            case "named_mob_died" -> NAMED_MOB_DEATH.id;
            default -> normalized;
        };
        for (PlaceCauseType type : values()) {
            if (type.id.equals(normalized) || type.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
