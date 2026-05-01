package com.worldremembers.livinglegends;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class NameSemanticRoots {
    public static final String UNKNOWN = "unknown";

    private NameSemanticRoots() {
    }

    public static String normalize(String root) {
        String value = WorldPos.optionalId(root).toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            return UNKNOWN;
        }
        return switch (value) {
            case "remembered", "remembrance", "memorial" -> "memory";
            case "first_step", "first_light", "footstep" -> "first";
            case "gate", "portal" -> "gate";
            case "threshold" -> "threshold";
            case "rift" -> "rift";
            case "crossing", "ford", "passage" -> "crossing";
            case "path", "road", "way" -> "path";
            case "trail" -> "trail";
            case "rest", "camp" -> "rest";
            case "stone", "rock", "waystone" -> "stone";
            case "mark", "marker", "sign" -> "mark";
            case "trace", "track" -> "trace";
            case "ember", "embers", "coal", "charcoal" -> "ember";
            case "ash", "ashes" -> "ash";
            case "flame", "fire" -> "flame";
            case "lava" -> "lava";
            case "basalt" -> "basalt";
            case "blackstone" -> "blackstone";
            case "shadow", "shade" -> "shadow";
            case "scar" -> "scar";
            case "wound" -> "wound";
            case "battlefield", "clash", "duel", "pvp", "bannerfall" -> "battle";
            case "death_site", "fallen", "lost", "last_breath", "bones" -> "death";
            case "mine", "ore", "vein", "quarry", "diamond", "emerald", "ancient_debris" -> "mining";
            case "slaughter", "hunt", "pasture", "trampled" -> "slaughter";
            case "lair", "boss", "dragon", "wither" -> "boss";
            case "field", "surface", "meadow" -> "field";
            case "shore", "bank", "coast" -> "shore";
            case "place", "site", "spot" -> "place";
            case "cave", "cavern", "deep", "summit" -> "place";
            default -> value;
        };
    }

    public static Set<String> conflictRoots(String root) {
        Set<String> result = new LinkedHashSet<>();
        String raw = WorldPos.optionalId(root).toLowerCase(Locale.ROOT);
        String normalized = normalize(root);
        addRoot(result, normalized);
        addContainedRootGroups(result, raw);
        addContainedRootGroups(result, normalized);
        return result;
    }

    public static Set<String> normalizeSet(Iterable<String> roots) {
        Set<String> result = new LinkedHashSet<>();
        if (roots == null) {
            return result;
        }
        for (String root : roots) {
            String normalized = normalize(root);
            if (!UNKNOWN.equals(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    public static String inferPatternRoot(String patternId) {
        String id = WorldPos.optionalId(patternId).toLowerCase(Locale.ROOT);
        String specific = inferSpecificRoot(id);
        if (!specific.isBlank()) {
            return specific;
        }
        if (id.contains("first_discovery") || id.contains("first_")) {
            return "first";
        }
        if (id.contains("portal") || id.contains("gate") || id.contains("dimension")) {
            return "gate";
        }
        if (id.contains("general") || id.contains("memory") || id.contains("custom") || id.contains("remembered")) {
            return "memory";
        }
        if (id.contains("death") || id.contains("grave") || id.contains("rest")) {
            return "death";
        }
        if (id.contains("battle") || id.contains("pvp") || id.contains("raid")) {
            return "battle";
        }
        if (id.contains("slaughter") || id.contains("hunt")) {
            return "slaughter";
        }
        if (id.contains("mine") || id.contains("mining") || id.contains("ore") || id.contains("quarry")) {
            return "mining";
        }
        if (id.contains("boss") || id.contains("lair")) {
            return "boss";
        }
        return id;
    }

    public static String inferTokenRoot(String tokenId) {
        return normalize(tokenId);
    }

    private static String inferSpecificRoot(String id) {
        if (containsAny(id, "shore", "bank", "coast")) {
            return "shore";
        }
        if (containsAny(id, "crossing", "ford", "passage")) {
            return "crossing";
        }
        if (containsAny(id, "path", "road", "_way", "way_")) {
            return "path";
        }
        if (id.contains("trail")) {
            return "trail";
        }
        if (containsAny(id, "rest", "camp")) {
            return "rest";
        }
        if (containsAny(id, "stone", "rock", "waystone")) {
            return "stone";
        }
        if (containsAny(id, "mark", "marker", "sign")) {
            return "mark";
        }
        if (containsAny(id, "gate", "portal")) {
            return "gate";
        }
        if (id.contains("rift")) {
            return "rift";
        }
        if (id.contains("threshold")) {
            return "threshold";
        }
        if (containsAny(id, "field", "meadow")) {
            return "field";
        }
        if (containsAny(id, "place", "site", "spot")) {
            return "place";
        }
        if (containsAny(id, "trace", "track")) {
            return "trace";
        }
        if (containsAny(id, "ember", "embers", "coal", "charcoal")) {
            return "ember";
        }
        if (containsAny(id, "ash", "ashes")) {
            return "ash";
        }
        if (containsAny(id, "flame", "fire")) {
            return "flame";
        }
        if (id.contains("lava")) {
            return "lava";
        }
        if (id.contains("basalt")) {
            return "basalt";
        }
        if (id.contains("blackstone")) {
            return "blackstone";
        }
        if (containsAny(id, "shadow", "shade")) {
            return "shadow";
        }
        if (id.contains("scar")) {
            return "scar";
        }
        if (id.contains("wound")) {
            return "wound";
        }
        return "";
    }

    private static void addContainedRootGroups(Set<String> result, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (containsAny(value, "shore", "bank", "coast")) {
            addRoot(result, "shore");
        }
        if (containsAny(value, "crossing", "ford", "passage")) {
            addRoot(result, "crossing");
        }
        if (containsAny(value, "path", "road", "_way", "way_")) {
            addRoot(result, "path");
        }
        if (value.contains("trail")) {
            addRoot(result, "trail");
        }
        if (containsAny(value, "rest", "camp")) {
            addRoot(result, "rest");
        }
        if (containsAny(value, "stone", "rock", "waystone")) {
            addRoot(result, "stone");
        }
        if (containsAny(value, "mark", "marker", "sign")) {
            addRoot(result, "mark");
        }
        if (containsAny(value, "gate", "portal")) {
            addRoot(result, "gate");
        }
        if (value.contains("rift")) {
            addRoot(result, "rift");
        }
        if (value.contains("threshold")) {
            addRoot(result, "threshold");
        }
        if (containsAny(value, "field", "meadow")) {
            addRoot(result, "field");
        }
        if (containsAny(value, "place", "site", "spot")) {
            addRoot(result, "place");
        }
        if (containsAny(value, "trace", "track")) {
            addRoot(result, "trace");
        }
        if (containsAny(value, "ember", "embers", "coal", "charcoal")) {
            addRoot(result, "ember");
        }
        if (containsAny(value, "ash", "ashes")) {
            addRoot(result, "ash");
        }
        if (containsAny(value, "flame", "fire")) {
            addRoot(result, "flame");
        }
        if (value.contains("lava")) {
            addRoot(result, "lava");
        }
        if (value.contains("basalt")) {
            addRoot(result, "basalt");
        }
        if (value.contains("blackstone")) {
            addRoot(result, "blackstone");
        }
        if (containsAny(value, "shadow", "shade")) {
            addRoot(result, "shadow");
        }
        if (value.contains("scar")) {
            addRoot(result, "scar");
        }
        if (value.contains("wound")) {
            addRoot(result, "wound");
        }
    }

    private static void addRoot(Set<String> result, String root) {
        String normalized = normalize(root);
        if (!UNKNOWN.equals(normalized)) {
            result.add(normalized);
        }
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
