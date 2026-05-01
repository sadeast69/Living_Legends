package com.worldremembers.livinglegends;

import java.io.Serializable;
import java.util.List;

public final class CompatThemeDefinitions {
    public static final int BUILTIN_PRIORITY = 10;
    public static final int DATAPACK_PRIORITY = 100;
    public static final String SOURCE_BUILTIN = "builtin";
    public static final String SOURCE_JSON = "json";
    public static final String SOURCE_API = "api";

    private CompatThemeDefinitions() {
    }

    public interface CompatDefinition extends Serializable {
        String targetId();

        int priority();

        boolean enabled();

        String source();

        default boolean exactTarget() {
            return true;
        }
    }

    public record BossThemeDefinition(
            String id,
            String entity,
            String bossTheme,
            PlaceType placeType,
            PlaceCauseType causeType,
            int priority,
            boolean enabled,
            List<String> tags,
            String source,
            boolean replace
    ) implements CompatDefinition {
        private static final long serialVersionUID = 1L;

        public BossThemeDefinition {
            id = normalizeId(id);
            entity = normalizeId(entity);
            bossTheme = normalizeSimple(bossTheme);
            placeType = placeType == null || placeType == PlaceType.UNKNOWN ? PlaceType.BOSS_SITE : placeType;
            causeType = causeType == null || causeType == PlaceCauseType.UNKNOWN ? PlaceCauseType.BOSS_KILL : causeType;
            priority = Math.max(0, priority);
            tags = normalizeList(tags);
            source = normalizeSource(source);
        }

        @Override
        public String targetId() {
            return entity;
        }
    }

    public record StructureThemeDefinition(
            String id,
            String structure,
            String structureTheme,
            PlaceType placeType,
            String discoveryKind,
            int priority,
            boolean useStructureBounds,
            boolean enabled,
            List<String> tags,
            String source,
            boolean replace
    ) implements CompatDefinition {
        private static final long serialVersionUID = 1L;

        public StructureThemeDefinition {
            id = normalizeId(id);
            structure = normalizeId(structure);
            structureTheme = normalizeSimple(structureTheme);
            placeType = placeType == null || placeType == PlaceType.UNKNOWN ? PlaceType.FIRST_DISCOVERY : placeType;
            discoveryKind = normalizeSimple(discoveryKind).isBlank() ? "structure" : normalizeSimple(discoveryKind);
            priority = Math.max(0, priority);
            tags = normalizeList(tags);
            source = normalizeSource(source);
        }

        @Override
        public String targetId() {
            return structure;
        }
    }

    public record BiomeThemeDefinition(
            String id,
            String biome,
            String biomeTag,
            String biomeTheme,
            String biomeGroup,
            int priority,
            boolean enabled,
            String source,
            boolean replace
    ) implements CompatDefinition {
        private static final long serialVersionUID = 1L;

        public BiomeThemeDefinition {
            id = normalizeId(id);
            biome = normalizeId(biome);
            biomeTag = normalizeId(biomeTag);
            biomeTheme = normalizeSimple(biomeTheme);
            biomeGroup = normalizeSimple(biomeGroup);
            priority = Math.max(0, priority);
            source = normalizeSource(source);
        }

        @Override
        public String targetId() {
            return biome.isBlank() ? biomeTag : biome;
        }

        @Override
        public boolean exactTarget() {
            return !biome.isBlank();
        }
    }

    public record MobThemeDefinition(
            String id,
            String entity,
            String entityTag,
            String mobTheme,
            String mobGroup,
            String combatRole,
            PlaceType placeType,
            int priority,
            boolean enabled,
            String source,
            boolean replace
    ) implements CompatDefinition {
        private static final long serialVersionUID = 1L;

        public MobThemeDefinition {
            id = normalizeId(id);
            entity = normalizeId(entity);
            entityTag = normalizeId(entityTag);
            mobTheme = normalizeSimple(mobTheme);
            mobGroup = normalizeSimple(mobGroup);
            combatRole = normalizeSimple(combatRole).isBlank() ? "unknown" : normalizeSimple(combatRole);
            placeType = placeType == null ? PlaceType.UNKNOWN : placeType;
            priority = Math.max(0, priority);
            source = normalizeSource(source);
        }

        @Override
        public String targetId() {
            return entity.isBlank() ? entityTag : entity;
        }

        @Override
        public boolean exactTarget() {
            return !entity.isBlank();
        }
    }

    public record BlockThemeDefinition(
            String id,
            String block,
            String blockTag,
            String blockTheme,
            String miningTheme,
            boolean valuable,
            String firstDiscoveryKey,
            int priority,
            boolean enabled,
            String source,
            boolean replace
    ) implements CompatDefinition {
        private static final long serialVersionUID = 1L;

        public BlockThemeDefinition {
            id = normalizeId(id);
            block = normalizeId(block);
            blockTag = normalizeId(blockTag);
            blockTheme = normalizeSimple(blockTheme);
            miningTheme = normalizeSimple(miningTheme);
            firstDiscoveryKey = normalizeId(firstDiscoveryKey);
            priority = Math.max(0, priority);
            source = normalizeSource(source);
        }

        @Override
        public String targetId() {
            return block.isBlank() ? blockTag : block;
        }

        @Override
        public boolean exactTarget() {
            return !block.isBlank();
        }
    }

    public record DimensionThemeDefinition(
            String id,
            String dimension,
            String dimensionTheme,
            String portalTheme,
            String firstDiscoveryKey,
            int priority,
            boolean enabled,
            String source,
            boolean replace
    ) implements CompatDefinition {
        private static final long serialVersionUID = 1L;

        public DimensionThemeDefinition {
            id = normalizeId(id);
            dimension = normalizeId(dimension);
            dimensionTheme = normalizeSimple(dimensionTheme);
            portalTheme = normalizeSimple(portalTheme);
            firstDiscoveryKey = normalizeId(firstDiscoveryKey);
            priority = Math.max(0, priority);
            source = normalizeSource(source);
        }

        @Override
        public String targetId() {
            return dimension;
        }
    }

    static String normalizeId(String value) {
        return WorldPos.optionalId(value).toLowerCase(java.util.Locale.ROOT);
    }

    static String normalizeSimple(String value) {
        String normalized = WorldPos.optionalId(value).toLowerCase(java.util.Locale.ROOT);
        return normalized.replace(' ', '_');
    }

    private static List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(CompatThemeDefinitions::normalizeSimple)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private static String normalizeSource(String value) {
        String normalized = normalizeSimple(value);
        return normalized.isBlank() ? SOURCE_API : normalized;
    }
}
