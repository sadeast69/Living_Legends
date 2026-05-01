package com.worldremembers.livinglegends;

import java.io.Serializable;
import java.util.Locale;

public enum DeathSiteEnvironment implements Serializable {
    CAVE("cave"),
    SURFACE("surface"),
    WATER("water"),
    MOUNTAIN("mountain"),
    NETHER("nether"),
    END("end"),
    UNKNOWN("unknown");

    private final String id;

    DeathSiteEnvironment(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String idString() {
        return id;
    }

    public static DeathSiteEnvironment classify(WorldMemoryEvent event) {
        if (event == null) {
            return UNKNOWN;
        }

        return classify(event.position(), noteBoolean(event.note(), "underwater"));
    }

    public static DeathSiteEnvironment classify(WorldPos position, boolean underwater) {
        if (position == null) {
            return UNKNOWN;
        }

        String dimensionId = position.dimensionId();
        if ("minecraft:the_nether".equals(dimensionId)) {
            return NETHER;
        }
        if ("minecraft:the_end".equals(dimensionId)) {
            return END;
        }
        if (underwater) {
            return WATER;
        }
        if (position.y() < 50) {
            return CAVE;
        }
        if (position.y() > 110) {
            return MOUNTAIN;
        }
        return SURFACE;
    }

    public static DeathSiteEnvironment fromId(String id) {
        String normalized = normalizeId(id);
        for (DeathSiteEnvironment environment : values()) {
            if (environment.id.equals(normalized)) {
                return environment;
            }
        }
        return UNKNOWN;
    }

    private static boolean noteBoolean(String note, String key) {
        if (note == null || note.isBlank() || key == null || key.isBlank()) {
            return false;
        }

        String prefix = key + "=";
        for (String token : note.split("\\s+")) {
            if (token.startsWith(prefix)) {
                return Boolean.parseBoolean(token.substring(prefix.length()));
            }
        }
        return false;
    }

    private static String normalizeId(String id) {
        if (id == null || id.isBlank()) {
            return UNKNOWN.id;
        }

        return id.trim().toLowerCase(Locale.ROOT);
    }
}
