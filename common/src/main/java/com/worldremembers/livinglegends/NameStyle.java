package com.worldremembers.livinglegends;

import java.util.Locale;

public enum NameStyle {
    PLAIN("plain"),
    HISTORIC("historic"),
    MYTHIC("mythic"),
    SOMBER("somber"),
    WORKING("working"),
    WILDERNESS("wilderness"),
    VANILLA_ADVENTURE("vanilla_adventure"),
    DARK_FANTASY("dark_fantasy"),
    COZY_SURVIVAL("cozy_survival"),
    EPIC_MYTHOLOGY("epic_mythology"),
    NEUTRAL_SERVER("neutral_server"),
    FUNNY_COMMUNITY("funny_community"),
    PLAYER_NAMED("player_named"),
    CUSTOM("custom");

    private final String id;

    NameStyle(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String idString() {
        return id;
    }

    public static NameStyle fromId(String id) {
        String normalized = normalizeId(id);

        for (NameStyle style : values()) {
            if (style.id.equals(normalized)) {
                return style;
            }
        }

        return CUSTOM;
    }

    private static String normalizeId(String id) {
        if (id == null || id.isBlank()) {
            return PLAIN.id;
        }

        return id.trim().toLowerCase(Locale.ROOT);
    }
}
