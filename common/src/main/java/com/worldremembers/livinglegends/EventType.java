package com.worldremembers.livinglegends;

import java.util.Locale;

public enum EventType {
    VISIT("player_visit", 1.0),
    DISCOVERY("discovery", 5.0),
    PLAYER_ENTERED_DIMENSION("player_entered_dimension", 4.0),
    PLAYER_FIRST_ENTERED_DIMENSION("player_first_entered_dimension", 8.0),
    PLAYER_DEATH("player_death", 12.0),
    PLAYER_KILL("player_kill", 10.0),
    PLAYER_KILLED_HOSTILE_MOB("player_killed_hostile_mob", 8.0),
    PLAYER_KILLED_PASSIVE_MOB("player_killed_passive_mob", 3.0),
    PLAYER_KILLED_NEUTRAL_MOB("player_killed_neutral_mob", 5.0),
    PLAYER_KILLED_PLAYER("player_killed_player", 15.0),
    PLAYER_KILL_HOSTILE_MOB("player_killed_hostile_mob", 8.0),
    PLAYER_KILL_PASSIVE_MOB("player_killed_passive_mob", 3.0),
    PLAYER_KILL_NEUTRAL_MOB("player_killed_neutral_mob", 5.0),
    PLAYER_KILL_PLAYER("player_killed_player", 15.0),
    MOB_KILL("mob_kill", 4.0),
    BOSS_KILLED("boss_killed", 30.0),
    BOSS_KILL("boss_killed", 30.0),
    NAMED_MOB_DIED("named_mob_died", 6.0),
    PET_TAMED("pet_tamed", 6.0),
    PET_DIED("pet_died", 9.0),
    RAID_WON("raid_won", 15.0),
    VALUABLE_BLOCK_MINED("valuable_block_mined", 6.0),
    NETHER_PORTAL_USED("nether_portal_used", 5.0),
    END_PORTAL_USED("end_portal_used", 12.0),
    RESPAWN_POINT_SET("respawn_point_set", 4.0),
    PLAYER_VISIT("player_visit", 1.0),
    PLAYER_BLOCK_PLACED("player_block_placed", 0.25),
    BLOCK_PLACED("player_block_placed", 0.25),
    BLOCK_BROKEN("block_broken", 0.2),
    ITEM_CREATED("item_created", 2.0),
    STRUCTURE_BUILT("structure_built", 8.0),
    STRUCTURE_DISCOVERED("structure_discovered", 18.0),
    CUSTOM("custom", 0.0);

    private final String id;
    private final double scoreWeight;

    EventType(String id, double scoreWeight) {
        this.id = id;
        this.scoreWeight = scoreWeight;
    }

    public String id() {
        return id;
    }

    public String idString() {
        return id;
    }

    public double scoreWeight() {
        return scoreWeight;
    }

    public boolean isVisit() {
        return this == VISIT
                || this == PLAYER_VISIT
                || this == DISCOVERY
                || this == PLAYER_ENTERED_DIMENSION
                || this == PLAYER_FIRST_ENTERED_DIMENSION;
    }

    public boolean isDeath() {
        return this == PLAYER_DEATH
                || this == NAMED_MOB_DIED
                || this == PET_DIED;
    }

    public boolean isCombat() {
        return this == PLAYER_KILL
                || this == PLAYER_KILLED_HOSTILE_MOB
                || this == PLAYER_KILLED_PASSIVE_MOB
                || this == PLAYER_KILLED_NEUTRAL_MOB
                || this == PLAYER_KILLED_PLAYER
                || this == PLAYER_KILL_HOSTILE_MOB
                || this == PLAYER_KILL_PASSIVE_MOB
                || this == PLAYER_KILL_NEUTRAL_MOB
                || this == PLAYER_KILL_PLAYER
                || this == MOB_KILL
                || this == BOSS_KILLED
                || this == BOSS_KILL
                || this == RAID_WON;
    }

    public boolean isBuildRelated() {
        return this == PLAYER_BLOCK_PLACED
                || this == BLOCK_PLACED
                || this == BLOCK_BROKEN
                || this == VALUABLE_BLOCK_MINED
                || this == ITEM_CREATED
                || this == STRUCTURE_BUILT
                || this == STRUCTURE_DISCOVERED;
    }

    public static EventType fromId(String id) {
        String normalized = normalizeId(id);

        for (EventType type : values()) {
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
            case "visit" -> PLAYER_VISIT.id;
            case "block_placed" -> PLAYER_BLOCK_PLACED.id;
            case "player_kill_hostile_mob" -> PLAYER_KILLED_HOSTILE_MOB.id;
            case "player_kill_passive_mob" -> PLAYER_KILLED_PASSIVE_MOB.id;
            case "player_kill_neutral_mob" -> PLAYER_KILLED_NEUTRAL_MOB.id;
            case "player_kill_player" -> PLAYER_KILLED_PLAYER.id;
            case "boss_kill" -> BOSS_KILLED.id;
            default -> normalized;
        };
    }
}
