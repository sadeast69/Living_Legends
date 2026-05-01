package com.worldremembers.livinglegends;

import com.worldremembers.livinglegends.config.LivingLegendsConfig;

import java.util.Locale;
import java.util.Map;

public final class BiomeThemeResolver {
    private BiomeThemeResolver() {
    }

    public static BiomeMetadata resolve(
            String dimensionId,
            String biomeId,
            LivingLegendsConfig config,
            String source
    ) {
        String dimension = WorldPos.optionalId(dimensionId);
        String biome = WorldPos.optionalId(biomeId);
        if (config == null || config.biomeThemes == null || !config.biomeThemes.enabled) {
            return BiomeMetadata.fromDimensionFallback(dimension);
        }
        if (biome.isBlank() || BiomeMetadata.UNKNOWN.equals(biome)) {
            return BiomeMetadata.fromDimensionFallback(dimension);
        }

        WorldRemembersCompatRegistries.CompatLookup<CompatThemeDefinitions.BiomeThemeDefinition> compat =
                WorldRemembersCompatRegistries.biomeTheme(biome);
        if (compat.matched()) {
            CompatThemeDefinitions.BiomeThemeDefinition definition = compat.definition();
            return new BiomeMetadata(
                    biome,
                    biome,
                    definition.biomeGroup().isBlank() ? definition.biomeTheme() : definition.biomeGroup(),
                    definition.biomeTheme(),
                    source == null || source.isBlank() ? "compat_registry" : source
            );
        }

        String group = mappedGroup(biome, config.biomeThemes.mappings);
        if (group.isBlank()) {
            group = inferredVanillaGroup(dimension, biome);
        }
        if (group.isBlank()) {
            group = WorldPos.optionalId(config.biomeThemes.unknownBiomeFallbackGroup);
        }
        if (group.isBlank()) {
            group = BiomeMetadata.UNKNOWN;
        }

        return new BiomeMetadata(
                biome,
                biome,
                group,
                themeForGroup(group),
                source == null || source.isBlank() ? "center_position" : source
        );
    }

    public static String themeForGroup(String group) {
        return switch (WorldPos.optionalId(group)) {
            case "plains", "meadow" -> "open_field";
            case "forest", "birch_forest", "dark_forest", "taiga", "cherry_grove" -> "woodland_trail";
            case "snowy" -> "snowbound";
            case "desert" -> "desert_marker";
            case "savanna" -> "dry_grassland";
            case "jungle" -> "jungle_path";
            case "swamp", "mangrove_swamp" -> "swamp_path";
            case "mountain" -> "highland";
            case "river" -> "river_crossing";
            case "beach" -> "shoreline";
            case "ocean" -> "sea_edge";
            case "badlands" -> "badlands_marker";
            case "cave_or_underground" -> "underground";
            case "nether" -> "nether";
            case "end" -> "end";
            default -> BiomeMetadata.UNKNOWN;
        };
    }

    private static String mappedGroup(String biomeId, Map<String, String> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return "";
        }
        String exact = mappings.get(biomeId);
        if (exact != null && !exact.isBlank()) {
            return WorldPos.optionalId(exact);
        }
        String path = biomeId.contains(":") ? biomeId.substring(biomeId.indexOf(':') + 1) : biomeId;
        String byPath = mappings.get(path);
        return byPath == null ? "" : WorldPos.optionalId(byPath);
    }

    private static String inferredVanillaGroup(String dimensionId, String biomeId) {
        String biome = WorldPos.optionalId(biomeId).toLowerCase(Locale.ROOT);
        if ("minecraft:the_nether".equals(dimensionId) || biome.contains("nether")
                || biome.contains("crimson") || biome.contains("warped") || biome.contains("basalt")) {
            return "nether";
        }
        if ("minecraft:the_end".equals(dimensionId) || biome.contains("end_") || biome.endsWith(":the_end")) {
            return "end";
        }
        if (biome.contains("cherry")) {
            return "cherry_grove";
        }
        if (biome.contains("mangrove")) {
            return "mangrove_swamp";
        }
        if (biome.contains("swamp")) {
            return "swamp";
        }
        if (biome.contains("badlands")) {
            return "badlands";
        }
        if (biome.contains("desert")) {
            return "desert";
        }
        if (biome.contains("savanna")) {
            return "savanna";
        }
        if (biome.contains("jungle")) {
            return "jungle";
        }
        if (biome.contains("dark_forest")) {
            return "dark_forest";
        }
        if (biome.contains("birch")) {
            return "birch_forest";
        }
        if (biome.contains("forest") || biome.contains("grove")) {
            return "forest";
        }
        if (biome.contains("taiga")) {
            return "taiga";
        }
        if (biome.contains("snow") || biome.contains("ice") || biome.contains("frozen")) {
            return "snowy";
        }
        if (biome.contains("mountain") || biome.contains("peak") || biome.contains("slope")) {
            return "mountain";
        }
        if (biome.contains("meadow")) {
            return "meadow";
        }
        if (biome.contains("river")) {
            return "river";
        }
        if (biome.contains("beach")) {
            return "beach";
        }
        if (biome.contains("ocean")) {
            return "ocean";
        }
        if (biome.contains("cave") || biome.contains("deep_dark") || biome.contains("dripstone")
                || biome.contains("lush_caves")) {
            return "cave_or_underground";
        }
        if (biome.contains("plains")) {
            return "plains";
        }
        return "";
    }
}
