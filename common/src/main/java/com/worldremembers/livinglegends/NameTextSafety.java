package com.worldremembers.livinglegends;

import java.util.Locale;

public final class NameTextSafety {
    public static final String SAFE_FALLBACK_PATTERN_KEY = "living_legends.name.pattern.safe_fallback";

    private NameTextSafety() {
    }

    public static boolean looksBrokenOrTechnical(String text) {
        String value = text == null ? "" : text.trim();
        if (value.isBlank()) {
            return true;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("living_legends.name.")
                || lower.contains("name.pattern")
                || lower.contains("name.token")
                || lower.contains("minecraft:")) {
            return true;
        }
        if (value.indexOf('\uFFFD') >= 0
                || value.contains("\u00C3")
                || value.contains("\u00D0")
                || value.contains("\u00D1")) {
            return true;
        }
        if (value.contains("???")) {
            return true;
        }

        int visible = 0;
        int questions = 0;
        for (int index = 0; index < value.length(); index++) {
            char c = value.charAt(index);
            if (Character.isWhitespace(c)) {
                continue;
            }
            visible++;
            if (c == '?') {
                questions++;
            }
        }
        return visible > 0 && questions >= 3 && questions * 2 >= visible;
    }

    public static String safeLiteralFallback(String fallback) {
        String value = fallback == null ? "" : fallback.trim();
        return looksBrokenOrTechnical(value) ? "Remembered Place" : value;
    }
}
