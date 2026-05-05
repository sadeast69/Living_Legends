package com.worldremembers.livinglegends.visual;

import com.worldremembers.livinglegends.EventType;
import com.worldremembers.livinglegends.NamedPlace;
import com.worldremembers.livinglegends.PlaceCause;
import com.worldremembers.livinglegends.PlaceCauseType;
import com.worldremembers.livinglegends.PlaceType;

import java.util.Locale;

public final class PlaceVisualThemeResolver {
    private static final PlaceVisualTheme DEATH = theme(
            "mourning", "mourning_label", "mourning_title",
            0xFFC77A5A, 0xFFD4A05C, 0xFF9A4A3E, 0xFF8E3E35, 0xFF28110F, 0xAA140606, 0xFF8E3E35,
            "\u2020", "line_ends", "broken", "emphasized", "tense", "heavy", 3
    );
    private static final PlaceVisualTheme BATTLE = theme(
            "steel_blood", "battle_label", "battle_title",
            0xFFD6C2A0, 0xFFB55243, 0xFF8A3B35, 0xFF8A3B35, 0xFF1E2024, 0xAA050505, 0xFF8A3B35,
            "\u25C6", "line_ends", "sharp", "emphasized", "tense", "heavy", 4
    );
    private static final PlaceVisualTheme SLAUGHTER = theme(
            "uneasy_earth", "slaughter_label", "slaughter_title",
            0xFFC08A58, 0xFFA9563B, 0xFF8B4B31, 0xFF75412C, 0xFF2D1B12, 0xAA160A04, 0xFF8B4B31,
            "\u25C7", "line_ends", "rough", "emphasized", "tense", "heavy", 3
    );
    private static final PlaceVisualTheme DISCOVERY = theme(
            "first_light", "discovery_label", "discovery_title",
            0xFFFFD987, 0xFFF0C56F, 0xFFD5A13C, 0xFFD5A13C, 0xFF4D3214, 0x88522F06, 0xFFD5A13C,
            "\u2726", "sides", "simple", "emphasized", "normal", "normal", 4
    );
    private static final PlaceVisualTheme BOSS = theme(
            "legendary", "boss_label", "boss_title",
            0xFFE1C16B, 0xFFC5A2F2, 0xFF8C65B7, 0xFF8C65B7, 0xFF241339, 0xAA0B0616, 0xFF8C65B7,
            "\u25C6", "sides", "double", "legendary", "normal", "glowing_soft", 5
    );
    private static final PlaceVisualTheme PET = theme(
            "warm_memorial", "pet_memorial_label", "pet_memorial_title",
            0xFFF0D7A5, 0xFF9FBBD0, 0xFFD5AA63, 0xFFD5AA63, 0xFF4E3826, 0x88432818, 0xFFD5AA63,
            "\u2726", "center_mark", "soft", "normal", "soft", "subtle", 2
    );
    private static final PlaceVisualTheme NAMED_MOB = theme(
            "candle_memorial", "named_mob_memorial_label", "named_mob_memorial_title",
            0xFFD3C6DD, 0xFFC9A972, 0xFF8E789F, 0xFF8E789F, 0xFF2E2638, 0xAA0F0B16, 0xFF8E789F,
            "\u2727", "center_mark", "mystic", "normal", "soft", "glowing_soft", 3
    );
    private static final PlaceVisualTheme GENERAL = theme(
            "quiet_landmark", "quiet_label", "quiet_title",
            0xFFBFC5A3, 0xFFA6B18C, 0xFF879069, 0xFF879069, 0xFF3C442E, 0x77303324, 0xFF879069,
            "\u00B7", "center_mark", "faded", "faint", "soft", "subtle", 1
    );
    private static final PlaceVisualTheme PORTAL_GENERIC = theme(
            "mystic_portal", "portal_label", "portal_title",
            0xFFC6B7F5, 0xFF88A1D5, 0xFF746AC6, 0xFF746AC6, 0xFF211D3C, 0xAA080616, 0xFF746AC6,
            "\u25C8", "line_ends", "mystic", "emphasized", "normal", "glowing_soft", 4
    );
    private static final PlaceVisualTheme PORTAL_NETHER = theme(
            "nether_portal", "nether_portal_label", "nether_portal_title",
            0xFFD07A62, 0xFF9B6ED0, 0xFF8E3444, 0xFF8E3444, 0xFF2B0E17, 0xAA160408, 0xFF8E3444,
            "\u25C8", "line_ends", "mystic", "emphasized", "tense", "glowing_soft", 4
    );
    private static final PlaceVisualTheme PORTAL_END = theme(
            "end_portal", "end_portal_label", "end_portal_title",
            0xFFE0D7A1, 0xFFC7B7FF, 0xFFA391CF, 0xFFA391CF, 0xFF27223E, 0xAA070613, 0xFFA391CF,
            "\u2727", "line_ends", "mystic", "emphasized", "soft", "glowing_soft", 4
    );
    private static final PlaceVisualTheme MINING = theme(
            "deep_ore", "mining_label", "mining_title",
            0xFFD1B47B, 0xFF9EA6A6, 0xFF9B7A45, 0xFF9B7A45, 0xFF302719, 0xAA0A0906, 0xFF9B7A45,
            "\u25C7", "line_ends", "simple", "normal", "normal", "normal", 2
    );
    private static final PlaceVisualTheme SETTLEMENT = theme(
            "hearth", "settlement_label", "settlement_title",
            0xFFE0C78D, 0xFFA9C88A, 0xFF7E9A5B, 0xFF7E9A5B, 0xFF33402A, 0x88402916, 0xFF7E9A5B,
            "\u2726", "center_mark", "soft", "normal", "soft", "subtle", 2
    );
    private static final PlaceVisualTheme RAID = theme(
            "raid_banner", "raid_label", "raid_title",
            0xFFD8B07A, 0xFFC05A43, 0xFF9B3E30, 0xFF9B3E30, 0xFF241A16, 0xAA0C0504, 0xFF9B3E30,
            "\u25C6", "line_ends", "sharp", "emphasized", "tense", "heavy", 4
    );

    private PlaceVisualThemeResolver() {
    }

    public static PlaceVisualTheme fromPlace(NamedPlace place) {
        if (place == null) {
            return PlaceVisualTheme.DEFAULT;
        }
        return resolve(place.placeType(), place.dimensionId(), place.cause());
    }

    public static PlaceVisualTheme resolve(PlaceType placeType, String dimensionId, PlaceCause cause) {
        PlaceType type = placeType == null ? PlaceType.UNKNOWN : placeType;
        PlaceCause resolvedCause = cause == null ? PlaceCause.unknown() : cause;
        PlaceCauseType causeType = resolvedCause.causeType();
        if (isPortal(type, causeType, resolvedCause.primaryEventType())) {
            return portalTheme(
                    dimensionId,
                    resolvedCause.portalType(),
                    resolvedCause.fromDimension(),
                    resolvedCause.toDimension(),
                    "",
                    ""
            );
        }
        if (causeType == PlaceCauseType.FIRST_BOSS_KILL || causeType == PlaceCauseType.BOSS_KILL) {
            return BOSS;
        }
        if (causeType == PlaceCauseType.PET_DEATH) {
            return PET;
        }
        if (causeType == PlaceCauseType.NAMED_MOB_DEATH) {
            return NAMED_MOB;
        }
        if (causeType == PlaceCauseType.PLAYER_DEATHS) {
            return DEATH;
        }
        if (causeType == PlaceCauseType.MOB_BATTLE || causeType == PlaceCauseType.PVP) {
            return BATTLE;
        }
        if (causeType == PlaceCauseType.PASSIVE_SLAUGHTER) {
            return SLAUGHTER;
        }
        if (causeType == PlaceCauseType.RAID) {
            return RAID;
        }
        if (causeType != null && causeType.isFirstDiscovery()) {
            return DISCOVERY;
        }
        return byType(type, dimensionId, "", "");
    }

    public static PlaceVisualTheme resolve(PlaceType placeType, String dimensionId, String subjectId, String note) {
        PlaceType type = placeType == null ? PlaceType.UNKNOWN : placeType;
        if (type == PlaceType.PORTAL_LANDMARK || type == PlaceType.DIMENSION_THRESHOLD) {
            return portalTheme(dimensionId, "", noteValue(note, "from"), noteValue(note, "to"), subjectId, note);
        }
        return byType(type, dimensionId, subjectId, note);
    }

    private static PlaceVisualTheme byType(PlaceType type, String dimensionId, String subjectId, String note) {
        return switch (type == null ? PlaceType.UNKNOWN : type) {
            case DEATH_SITE, GRAVE -> DEATH;
            case BATTLEFIELD, PVP_ARENA -> BATTLE;
            case SLAUGHTER_FIELD -> SLAUGHTER;
            case FIRST_DISCOVERY -> DISCOVERY;
            case BOSS_SITE -> BOSS;
            case PET_MEMORIAL -> PET;
            case NAMED_MOB_MEMORIAL -> NAMED_MOB;
            case GENERAL_LANDMARK, LANDMARK -> GENERAL;
            case PORTAL_LANDMARK, DIMENSION_THRESHOLD, PORTAL_SITE -> portalTheme(
                    dimensionId,
                    "",
                    noteValue(note, "from"),
                    noteValue(note, "to"),
                    subjectId,
                    note
            );
            case RAID_SITE -> RAID;
            case MINING_SITE, MINE -> MINING;
            case SETTLEMENT, CAMP, FARM, WORKSHOP, ROAD -> SETTLEMENT;
            default -> PlaceVisualTheme.DEFAULT;
        };
    }

    private static boolean isPortal(PlaceType type, PlaceCauseType causeType, EventType eventType) {
        if (type == PlaceType.PORTAL_LANDMARK || type == PlaceType.DIMENSION_THRESHOLD || type == PlaceType.PORTAL_SITE) {
            return true;
        }
        if (causeType == PlaceCauseType.PORTAL_USAGE || causeType == PlaceCauseType.DIMENSION_THRESHOLD) {
            return true;
        }
        return eventType == EventType.NETHER_PORTAL_USED
                || eventType == EventType.END_PORTAL_USED
                || eventType == EventType.PLAYER_ENTERED_DIMENSION;
    }

    private static PlaceVisualTheme portalTheme(
            String dimensionId,
            String portalType,
            String fromDimension,
            String toDimension,
            String subjectId,
            String note
    ) {
        String context = joinContext(toDimension, subjectId, portalType, noteValue(note, "to"), dimensionId, fromDimension, note);
        if (containsNether(context)) {
            return PORTAL_NETHER;
        }
        if (containsEnd(context)) {
            return PORTAL_END;
        }
        return PORTAL_GENERIC;
    }

    private static boolean containsNether(String value) {
        String clean = clean(value);
        return clean.contains("the_nether") || clean.contains(":nether") || clean.contains("nether");
    }

    private static boolean containsEnd(String value) {
        String clean = clean(value);
        return clean.contains("the_end") || clean.endsWith(":end") || clean.contains(" end") || clean.equals("end");
    }

    private static String joinContext(String... values) {
        StringBuilder builder = new StringBuilder();
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private static String noteValue(String note, String key) {
        String cleanNote = note == null ? "" : note.trim();
        String cleanKey = key == null ? "" : key.trim();
        if (cleanNote.isBlank() || cleanKey.isBlank()) {
            return "";
        }
        String token = cleanKey + "=";
        int start = cleanNote.indexOf(token);
        if (start < 0) {
            return "";
        }
        int valueStart = start + token.length();
        int end = cleanNote.indexOf(' ', valueStart);
        return end < 0 ? cleanNote.substring(valueStart) : cleanNote.substring(valueStart, end);
    }

    private static String clean(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static PlaceVisualTheme theme(
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
    ) {
        return new PlaceVisualTheme(
                toneKey,
                mapLabelStyleKey,
                titleOverlayStyleKey,
                mainColor,
                secondaryColor,
                accentColor,
                lineColor,
                outlineColor,
                shadowColor,
                tooltipAccentColor,
                glyph,
                glyphPlacement,
                lineStyle,
                labelWeight,
                curveStyle,
                shadowStyle,
                emphasis
        );
    }
}
