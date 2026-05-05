package com.worldremembers.livinglegends.visual;

import java.io.Serializable;
import java.util.Locale;

public record PlaceVisualTheme(
        String toneKey,
        String mapLabelStyleKey,
        String titleOverlayStyleKey,
        int mainColor,
        int secondaryColor,
        int accentColor,
        int lineColor,
        int outlineColor,
        int shadowColor,
        int tooltipAccentColor,
        String glyph,
        String glyphPlacement,
        String lineStyle,
        String labelWeight,
        String curveStyle,
        String shadowStyle,
        int emphasis
) implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_TONE_KEY = "remembered";
    private static final String DEFAULT_MAP_LABEL_STYLE_KEY = "fantasy_label";
    private static final String DEFAULT_TITLE_OVERLAY_STYLE_KEY = "classic_title";
    private static final int DEFAULT_MAIN_COLOR = 0xFFE7C27A;
    private static final int DEFAULT_SECONDARY_COLOR = 0xFFD8C59A;
    private static final int DEFAULT_ACCENT_COLOR = 0xFFE0B45D;
    private static final int DEFAULT_LINE_COLOR = 0xFFB88E55;
    private static final int DEFAULT_OUTLINE_COLOR = 0xFF3D2815;
    private static final int DEFAULT_SHADOW_COLOR = 0xAA050505;
    private static final int DEFAULT_TOOLTIP_ACCENT_COLOR = 0xFFB88E55;
    private static final String DEFAULT_GLYPH_PLACEMENT = "none";
    private static final String DEFAULT_LINE_STYLE = "simple";
    private static final String DEFAULT_LABEL_WEIGHT = "normal";
    private static final String DEFAULT_CURVE_STYLE = "normal";
    private static final String DEFAULT_SHADOW_STYLE = "normal";

    public static final PlaceVisualTheme DEFAULT = new PlaceVisualTheme(
            DEFAULT_TONE_KEY,
            DEFAULT_MAP_LABEL_STYLE_KEY,
            DEFAULT_TITLE_OVERLAY_STYLE_KEY,
            DEFAULT_MAIN_COLOR,
            DEFAULT_SECONDARY_COLOR,
            DEFAULT_ACCENT_COLOR,
            DEFAULT_LINE_COLOR,
            DEFAULT_OUTLINE_COLOR,
            DEFAULT_SHADOW_COLOR,
            DEFAULT_TOOLTIP_ACCENT_COLOR,
            "",
            DEFAULT_GLYPH_PLACEMENT,
            DEFAULT_LINE_STYLE,
            DEFAULT_LABEL_WEIGHT,
            DEFAULT_CURVE_STYLE,
            DEFAULT_SHADOW_STYLE,
            2
    );

    public PlaceVisualTheme {
        toneKey = cleanKey(toneKey, DEFAULT_TONE_KEY);
        mapLabelStyleKey = cleanKey(mapLabelStyleKey, DEFAULT_MAP_LABEL_STYLE_KEY);
        titleOverlayStyleKey = cleanKey(titleOverlayStyleKey, DEFAULT_TITLE_OVERLAY_STYLE_KEY);
        mainColor = normalizeColor(mainColor, DEFAULT_MAIN_COLOR, true);
        secondaryColor = normalizeColor(secondaryColor, DEFAULT_SECONDARY_COLOR, true);
        accentColor = normalizeColor(accentColor, DEFAULT_ACCENT_COLOR, true);
        lineColor = normalizeColor(lineColor, DEFAULT_LINE_COLOR, true);
        outlineColor = normalizeColor(outlineColor, DEFAULT_OUTLINE_COLOR, true);
        shadowColor = normalizeColor(shadowColor, DEFAULT_SHADOW_COLOR, false);
        tooltipAccentColor = normalizeColor(tooltipAccentColor, DEFAULT_TOOLTIP_ACCENT_COLOR, true);
        glyph = safeGlyph(glyph);
        glyphPlacement = choice(glyphPlacement, DEFAULT_GLYPH_PLACEMENT, "none", "sides", "line_ends", "center_mark");
        lineStyle = choice(lineStyle, DEFAULT_LINE_STYLE, "simple", "faded", "double", "broken", "sharp", "soft", "mystic", "rough");
        labelWeight = choice(labelWeight, DEFAULT_LABEL_WEIGHT, "faint", "normal", "emphasized", "legendary");
        curveStyle = choice(curveStyle, DEFAULT_CURVE_STYLE, "none", "soft", "normal", "tense");
        shadowStyle = choice(shadowStyle, DEFAULT_SHADOW_STYLE, "subtle", "normal", "heavy", "glowing_soft");
        emphasis = Math.max(1, Math.min(5, emphasis));
    }

    public String decoratedLabel(String label) {
        String clean = label == null ? "" : label.trim();
        if (clean.isBlank() || glyph.isBlank()) {
            return clean;
        }
        if ("sides".equals(glyphPlacement)) {
            return glyph + " " + clean + " " + glyph;
        }
        return clean;
    }

    public String decoratedTitle(String title) {
        String clean = title == null ? "" : title.trim();
        if (clean.isBlank() || glyph.isBlank()) {
            return clean;
        }
        return glyph + " " + clean + " " + glyph;
    }

    public String fingerprintKey() {
        return toneKey
                + "|" + mapLabelStyleKey
                + "|" + titleOverlayStyleKey
                + "|" + Integer.toHexString(mainColor)
                + "|" + Integer.toHexString(secondaryColor)
                + "|" + Integer.toHexString(accentColor)
                + "|" + Integer.toHexString(lineColor)
                + "|" + Integer.toHexString(outlineColor)
                + "|" + Integer.toHexString(shadowColor)
                + "|" + Integer.toHexString(tooltipAccentColor)
                + "|" + glyph
                + "|" + glyphPlacement
                + "|" + lineStyle
                + "|" + labelWeight
                + "|" + curveStyle
                + "|" + shadowStyle
                + "|" + emphasis;
    }

    private static String cleanKey(String value, String fallback) {
        String resolved = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return resolved.isBlank() ? fallback : resolved;
    }

    private static String choice(String value, String fallback, String... allowed) {
        String resolved = cleanKey(value, fallback);
        for (String option : allowed) {
            if (option.equals(resolved)) {
                return resolved;
            }
        }
        return fallback;
    }

    private static String safeGlyph(String value) {
        String resolved = value == null ? "" : value.trim();
        return switch (resolved) {
            case "\u2726", "\u2727", "\u25C6", "\u25C7", "\u25C8", "\u2020", "\u00B7" -> resolved;
            case "\u2694" -> "\u25C6";
            case "\u263E" -> "\u2727";
            default -> "";
        };
    }

    private static int normalizeColor(int value, int fallback, boolean forceOpaque) {
        if (value == 0) {
            value = fallback;
        }
        if (forceOpaque && (value & 0xFF00_0000) == 0) {
            value |= 0xFF00_0000;
        }
        return value;
    }
}
