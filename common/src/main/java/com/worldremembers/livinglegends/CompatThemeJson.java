package com.worldremembers.livinglegends;

import com.worldremembers.livinglegends.CompatThemeDefinitions.BiomeThemeDefinition;
import com.worldremembers.livinglegends.CompatThemeDefinitions.BlockThemeDefinition;
import com.worldremembers.livinglegends.CompatThemeDefinitions.BossThemeDefinition;
import com.worldremembers.livinglegends.CompatThemeDefinitions.DimensionThemeDefinition;
import com.worldremembers.livinglegends.CompatThemeDefinitions.MobThemeDefinition;
import com.worldremembers.livinglegends.CompatThemeDefinitions.StructureThemeDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CompatThemeJson {
    private CompatThemeJson() {
    }

    public static void addEntry(
            String folder,
            String sourceId,
            Map<String, Object> values,
            MutableData target,
            List<String> warnings
    ) {
        String normalizedFolder = CompatThemeDefinitions.normalizeSimple(folder);
        try {
            switch (normalizedFolder) {
                case "boss_themes" -> target.bossThemes().add(boss(sourceId, values));
                case "structure_themes" -> target.structureThemes().add(structure(sourceId, values));
                case "biome_themes" -> target.biomeThemes().add(biome(sourceId, values));
                case "mob_themes" -> target.mobThemes().add(mob(sourceId, values));
                case "block_themes" -> target.blockThemes().add(block(sourceId, values));
                case "dimension_themes" -> target.dimensionThemes().add(dimension(sourceId, values));
                default -> warnings.add("Unknown compat registry folder '" + folder + "' for " + sourceId + ".");
            }
        } catch (IllegalArgumentException exception) {
            warnings.add("Skipped compat entry " + sourceId + ": " + exception.getMessage());
        }
    }

    public static List<Map<String, Object>> entries(Map<String, Object> root) {
        Object entries = root.get("entries");
        if (entries instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object value : list) {
                if (value instanceof Map<?, ?> map) {
                    result.add(castMap(map));
                }
            }
            return result;
        }
        return List.of(root);
    }

    private static BossThemeDefinition boss(String sourceId, Map<String, Object> values) {
        String entity = required(values, "entity");
        String theme = required(values, "bossTheme");
        return new BossThemeDefinition(
                id(values, sourceId),
                entity,
                theme,
                PlaceType.fromId(string(values, "placeType", PlaceType.BOSS_SITE.idString())),
                PlaceCauseType.fromId(string(values, "causeType", PlaceCauseType.BOSS_KILL.idString())),
                integer(values, "priority", CompatThemeDefinitions.DATAPACK_PRIORITY),
                bool(values, "enabled", true),
                stringList(values, "tags"),
                CompatThemeDefinitions.SOURCE_JSON,
                bool(values, "replace", false)
        );
    }

    private static StructureThemeDefinition structure(String sourceId, Map<String, Object> values) {
        String structure = required(values, "structure");
        String theme = required(values, "structureTheme");
        return new StructureThemeDefinition(
                id(values, sourceId),
                structure,
                theme,
                PlaceType.fromId(string(values, "placeType", PlaceType.FIRST_DISCOVERY.idString())),
                string(values, "discoveryKind", "structure"),
                integer(values, "priority", CompatThemeDefinitions.DATAPACK_PRIORITY),
                bool(values, "useStructureBounds", true),
                bool(values, "enabled", true),
                stringList(values, "tags"),
                CompatThemeDefinitions.SOURCE_JSON,
                bool(values, "replace", false)
        );
    }

    private static BiomeThemeDefinition biome(String sourceId, Map<String, Object> values) {
        String biome = string(values, "biome", "");
        String tag = string(values, "biomeTag", "");
        if (biome.isBlank() && tag.isBlank()) {
            throw new IllegalArgumentException("field 'biome' or 'biomeTag' is required");
        }
        String theme = required(values, "biomeTheme");
        return new BiomeThemeDefinition(
                id(values, sourceId),
                biome,
                tag,
                theme,
                string(values, "biomeGroup", theme),
                integer(values, "priority", CompatThemeDefinitions.DATAPACK_PRIORITY),
                bool(values, "enabled", true),
                CompatThemeDefinitions.SOURCE_JSON,
                bool(values, "replace", false)
        );
    }

    private static MobThemeDefinition mob(String sourceId, Map<String, Object> values) {
        String entity = string(values, "entity", "");
        String tag = string(values, "entityTag", "");
        if (entity.isBlank() && tag.isBlank()) {
            throw new IllegalArgumentException("field 'entity' or 'entityTag' is required");
        }
        return new MobThemeDefinition(
                id(values, sourceId),
                entity,
                tag,
                required(values, "mobTheme"),
                string(values, "mobGroup", ""),
                string(values, "combatRole", "unknown"),
                PlaceType.fromId(string(values, "placeType", PlaceType.UNKNOWN.idString())),
                integer(values, "priority", CompatThemeDefinitions.DATAPACK_PRIORITY),
                bool(values, "enabled", true),
                CompatThemeDefinitions.SOURCE_JSON,
                bool(values, "replace", false)
        );
    }

    private static BlockThemeDefinition block(String sourceId, Map<String, Object> values) {
        String block = string(values, "block", "");
        String tag = string(values, "blockTag", "");
        if (block.isBlank() && tag.isBlank()) {
            throw new IllegalArgumentException("field 'block' or 'blockTag' is required");
        }
        return new BlockThemeDefinition(
                id(values, sourceId),
                block,
                tag,
                string(values, "blockTheme", ""),
                string(values, "miningTheme", string(values, "blockTheme", "")),
                bool(values, "valuable", false),
                string(values, "firstDiscoveryKey", ""),
                integer(values, "priority", CompatThemeDefinitions.DATAPACK_PRIORITY),
                bool(values, "enabled", true),
                CompatThemeDefinitions.SOURCE_JSON,
                bool(values, "replace", false)
        );
    }

    private static DimensionThemeDefinition dimension(String sourceId, Map<String, Object> values) {
        String dimension = required(values, "dimension");
        return new DimensionThemeDefinition(
                id(values, sourceId),
                dimension,
                required(values, "dimensionTheme"),
                string(values, "portalTheme", ""),
                string(values, "firstDiscoveryKey", ""),
                integer(values, "priority", CompatThemeDefinitions.DATAPACK_PRIORITY),
                bool(values, "enabled", true),
                CompatThemeDefinitions.SOURCE_JSON,
                bool(values, "replace", false)
        );
    }

    private static String id(Map<String, Object> values, String sourceId) {
        return string(values, "id", sourceId);
    }

    private static String required(Map<String, Object> values, String key) {
        String value = string(values, key, "");
        if (value.isBlank()) {
            throw new IllegalArgumentException("field '" + key + "' is required");
        }
        return value;
    }

    private static String string(Map<String, Object> values, String key, String fallback) {
        Object value = values.get(key);
        return value instanceof String text ? text.trim() : fallback;
    }

    private static int integer(Map<String, Object> values, String key, int fallback) {
        Object value = values.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    private static boolean bool(Map<String, Object> values, String key, boolean fallback) {
        Object value = values.get(key);
        return value instanceof Boolean bool ? bool : fallback;
    }

    private static List<String> stringList(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object entry : list) {
            if (entry instanceof String text && !text.isBlank()) {
                result.add(text.trim());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    public record MutableData(
            List<BossThemeDefinition> bossThemes,
            List<StructureThemeDefinition> structureThemes,
            List<BiomeThemeDefinition> biomeThemes,
            List<MobThemeDefinition> mobThemes,
            List<BlockThemeDefinition> blockThemes,
            List<DimensionThemeDefinition> dimensionThemes
    ) {
        public static MutableData empty() {
            return new MutableData(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        public WorldRemembersCompatRegistries.LoadedData toLoadedData(List<String> warnings) {
            return new WorldRemembersCompatRegistries.LoadedData(
                    bossThemes,
                    structureThemes,
                    biomeThemes,
                    mobThemes,
                    blockThemes,
                    dimensionThemes,
                    warnings
            );
        }
    }
}
