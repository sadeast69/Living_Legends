package com.worldremembers.livinglegends;

import java.util.Locale;

public enum PlaceType {
    UNKNOWN("unknown"),
    CAMP("camp"),
    SETTLEMENT("settlement"),
    DEATH_SITE("death_site"),
    BATTLEFIELD("battlefield"),
    SLAUGHTER_FIELD("slaughter_field"),
    PVP_ARENA("pvp_arena"),
    MINING_SITE("mining_site"),
    PORTAL_LANDMARK("portal_landmark"),
    GENERAL_LANDMARK("general_landmark"),
    FIRST_DISCOVERY("first_discovery"),
    BOSS_SITE("boss_site"),
    PET_MEMORIAL("pet_memorial"),
    NAMED_MOB_MEMORIAL("named_mob_memorial"),
    RAID_SITE("raid_site"),
    DIMENSION_THRESHOLD("dimension_threshold"),
    GRAVE("grave"),
    LANDMARK("landmark"),
    MINE("mine"),
    FARM("farm"),
    WORKSHOP("workshop"),
    ROAD("road"),
    PORTAL_SITE("portal_site"),
    CUSTOM("custom");

    private final String id;

    PlaceType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String idString() {
        return id;
    }

    public int priorityRank() {
        return switch (this) {
            case PET_MEMORIAL -> 10;
            case NAMED_MOB_MEMORIAL -> 20;
            case BOSS_SITE -> 30;
            case RAID_SITE -> 40;
            case FIRST_DISCOVERY -> 50;
            case DIMENSION_THRESHOLD, PORTAL_LANDMARK -> 60;
            case DEATH_SITE -> 70;
            case PVP_ARENA -> 80;
            case BATTLEFIELD -> 90;
            case SLAUGHTER_FIELD -> 100;
            case MINING_SITE -> 110;
            case SETTLEMENT -> 120;
            case GENERAL_LANDMARK -> 1000;
            default -> 500;
        };
    }

    public boolean sameOrHigherPriorityThan(PlaceType other) {
        PlaceType resolvedOther = other == null ? UNKNOWN : other;
        return priorityRank() <= resolvedOther.priorityRank();
    }

    public static PlaceType fromId(String id) {
        String normalized = normalizeId(id);

        for (PlaceType type : values()) {
            if (type.id.equals(normalized)) {
                return type;
            }
        }

        return CUSTOM;
    }

    private static String normalizeId(String id) {
        if (id == null || id.isBlank()) {
            return UNKNOWN.id;
        }

        String normalized = id.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "death_cave" -> DEATH_SITE.id;
            default -> normalized;
        };
    }
}
