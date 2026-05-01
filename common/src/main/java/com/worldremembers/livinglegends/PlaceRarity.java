package com.worldremembers.livinglegends;

import java.util.Locale;

public enum PlaceRarity {
    COMMON("common", 0.0),
    NOTABLE("notable", 25.0),
    RARE("rare", 75.0),
    LEGENDARY("legendary", 150.0),
    MYTHIC("mythic", 300.0);

    private final String id;
    private final double minimumScore;

    PlaceRarity(String id, double minimumScore) {
        this.id = id;
        this.minimumScore = minimumScore;
    }

    public String id() {
        return id;
    }

    public String idString() {
        return id;
    }

    public double minimumScore() {
        return minimumScore;
    }

    public static PlaceRarity fromScore(double score) {
        PlaceRarity result = COMMON;

        for (PlaceRarity rarity : values()) {
            if (score >= rarity.minimumScore) {
                result = rarity;
            }
        }

        return result;
    }

    public static PlaceRarity fromId(String id) {
        String normalized = normalizeId(id);

        for (PlaceRarity rarity : values()) {
            if (rarity.id.equals(normalized)) {
                return rarity;
            }
        }

        return COMMON;
    }

    private static String normalizeId(String id) {
        if (id == null || id.isBlank()) {
            return COMMON.id;
        }

        return id.trim().toLowerCase(Locale.ROOT);
    }
}
