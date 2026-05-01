package com.worldremembers.livinglegends;

import java.util.Locale;

public enum NameStyleSelectionMode {
    DEFAULT_ONLY("DEFAULT_ONLY"),
    RANDOM_ENABLED("RANDOM_ENABLED"),
    WEIGHTED("WEIGHTED"),
    PER_PLACE_TYPE("PER_PLACE_TYPE");

    private final String id;

    NameStyleSelectionMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String idString() {
        return id;
    }

    public static NameStyleSelectionMode fromId(String id) {
        String normalized = normalizeId(id);
        for (NameStyleSelectionMode mode : values()) {
            if (mode.id.equals(normalized)) {
                return mode;
            }
        }
        return DEFAULT_ONLY;
    }

    private static String normalizeId(String id) {
        if (id == null || id.isBlank()) {
            return DEFAULT_ONLY.id;
        }
        return id.trim().toUpperCase(Locale.ROOT);
    }
}
