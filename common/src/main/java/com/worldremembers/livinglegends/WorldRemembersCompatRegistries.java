package com.worldremembers.livinglegends;

import com.worldremembers.livinglegends.CompatThemeDefinitions.BiomeThemeDefinition;
import com.worldremembers.livinglegends.CompatThemeDefinitions.BlockThemeDefinition;
import com.worldremembers.livinglegends.CompatThemeDefinitions.BossThemeDefinition;
import com.worldremembers.livinglegends.CompatThemeDefinitions.CompatDefinition;
import com.worldremembers.livinglegends.CompatThemeDefinitions.DimensionThemeDefinition;
import com.worldremembers.livinglegends.CompatThemeDefinitions.MobThemeDefinition;
import com.worldremembers.livinglegends.CompatThemeDefinitions.StructureThemeDefinition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class WorldRemembersCompatRegistries {
    private static final Object LOCK = new Object();
    private static final List<BossThemeDefinition> API_BOSS_THEMES = new ArrayList<>();
    private static final List<StructureThemeDefinition> API_STRUCTURE_THEMES = new ArrayList<>();
    private static final List<BiomeThemeDefinition> API_BIOME_THEMES = new ArrayList<>();
    private static final List<MobThemeDefinition> API_MOB_THEMES = new ArrayList<>();
    private static final List<BlockThemeDefinition> API_BLOCK_THEMES = new ArrayList<>();
    private static final List<DimensionThemeDefinition> API_DIMENSION_THEMES = new ArrayList<>();
    private static volatile State state = State.from(
            builtInBossThemes(),
            builtInStructureThemes(),
            builtInBiomeThemes(),
            builtInMobThemes(),
            builtInBlockThemes(),
            builtInDimensionThemes(),
            List.of()
    );

    private WorldRemembersCompatRegistries() {
    }

    public static void reloadDatapack(LoadedData loaded) {
        LoadedData safe = loaded == null ? LoadedData.empty() : loaded;
        synchronized (LOCK) {
            state = State.from(
                    concat(builtInBossThemes(), API_BOSS_THEMES, safe.bossThemes()),
                    concat(builtInStructureThemes(), API_STRUCTURE_THEMES, safe.structureThemes()),
                    concat(builtInBiomeThemes(), API_BIOME_THEMES, safe.biomeThemes()),
                    concat(builtInMobThemes(), API_MOB_THEMES, safe.mobThemes()),
                    concat(builtInBlockThemes(), API_BLOCK_THEMES, safe.blockThemes()),
                    concat(builtInDimensionThemes(), API_DIMENSION_THEMES, safe.dimensionThemes()),
                    safe.warnings()
            );
        }
    }

    public static void registerBossTheme(BossThemeDefinition definition) {
        if (definition == null) {
            return;
        }
        synchronized (LOCK) {
            API_BOSS_THEMES.add(withSource(definition, CompatThemeDefinitions.SOURCE_API));
            reloadDatapack(state.dataOnly());
        }
    }

    public static void registerStructureTheme(StructureThemeDefinition definition) {
        if (definition == null) {
            return;
        }
        synchronized (LOCK) {
            API_STRUCTURE_THEMES.add(withSource(definition, CompatThemeDefinitions.SOURCE_API));
            reloadDatapack(state.dataOnly());
        }
    }

    public static void registerBiomeTheme(BiomeThemeDefinition definition) {
        if (definition == null) {
            return;
        }
        synchronized (LOCK) {
            API_BIOME_THEMES.add(withSource(definition, CompatThemeDefinitions.SOURCE_API));
            reloadDatapack(state.dataOnly());
        }
    }

    public static void registerMobTheme(MobThemeDefinition definition) {
        if (definition == null) {
            return;
        }
        synchronized (LOCK) {
            API_MOB_THEMES.add(withSource(definition, CompatThemeDefinitions.SOURCE_API));
            reloadDatapack(state.dataOnly());
        }
    }

    public static void registerBlockTheme(BlockThemeDefinition definition) {
        if (definition == null) {
            return;
        }
        synchronized (LOCK) {
            API_BLOCK_THEMES.add(withSource(definition, CompatThemeDefinitions.SOURCE_API));
            reloadDatapack(state.dataOnly());
        }
    }

    public static void registerDimensionTheme(DimensionThemeDefinition definition) {
        if (definition == null) {
            return;
        }
        synchronized (LOCK) {
            API_DIMENSION_THEMES.add(withSource(definition, CompatThemeDefinitions.SOURCE_API));
            reloadDatapack(state.dataOnly());
        }
    }

    public static CompatLookup<BossThemeDefinition> bossTheme(String entityId) {
        return lookup(state.bossThemes(), entityId);
    }

    public static CompatLookup<StructureThemeDefinition> structureTheme(String structureId) {
        return lookup(state.structureThemes(), structureId);
    }

    public static CompatLookup<BiomeThemeDefinition> biomeTheme(String biomeId) {
        return lookup(state.biomeThemes(), biomeId);
    }

    public static CompatLookup<MobThemeDefinition> mobTheme(String entityId) {
        return lookup(state.mobThemes(), entityId);
    }

    public static CompatLookup<BlockThemeDefinition> blockTheme(String blockId) {
        return lookup(state.blockThemes(), blockId);
    }

    public static CompatLookup<DimensionThemeDefinition> dimensionTheme(String dimensionId) {
        return lookup(state.dimensionThemes(), dimensionId);
    }

    public static boolean isBoss(String entityId) {
        return bossTheme(entityId).matched();
    }

    public static boolean isValuableBlock(String blockId) {
        CompatLookup<BlockThemeDefinition> lookup = blockTheme(blockId);
        return lookup.matched() && lookup.definition().valuable();
    }

    public static List<FirstDiscoveryDefinition> compatFirstDiscoveryDefinitions() {
        List<FirstDiscoveryDefinition> result = new ArrayList<>();
        for (StructureThemeDefinition definition : state.structureThemes()) {
            if (definition.enabled() && !definition.structure().isBlank()) {
                String discoveryId = "compat:first_structure:" + definition.structure();
                result.add(new FirstDiscoveryDefinition(
                        discoveryId,
                        DiscoveryTriggerType.STRUCTURE_DISCOVERED,
                        definition.structure(),
                        definition.placeType(),
                        Math.max(1.0, definition.priority() / 4.0),
                        Map.of("structure", definition.structureTheme().isBlank() ? definition.structure() : definition.structureTheme()),
                        definition.useStructureBounds(),
                        96
                ));
            }
        }
        for (BlockThemeDefinition definition : state.blockThemes()) {
            if (definition.enabled() && definition.valuable() && !definition.firstDiscoveryKey().isBlank() && !definition.block().isBlank()) {
                result.add(new FirstDiscoveryDefinition(
                        definition.firstDiscoveryKey(),
                        DiscoveryTriggerType.BLOCK_MINED,
                        definition.block(),
                        PlaceType.FIRST_DISCOVERY,
                        Math.max(1.0, definition.priority() / 4.0),
                        Map.of("block", definition.blockTheme().isBlank() ? definition.block() : definition.blockTheme())
                ));
            }
        }
        for (DimensionThemeDefinition definition : state.dimensionThemes()) {
            if (definition.enabled() && !definition.firstDiscoveryKey().isBlank() && !definition.dimension().isBlank()) {
                result.add(new FirstDiscoveryDefinition(
                        definition.firstDiscoveryKey(),
                        DiscoveryTriggerType.DIMENSION_ENTRY,
                        definition.dimension(),
                        PlaceType.FIRST_DISCOVERY,
                        Math.max(1.0, definition.priority() / 4.0),
                        Map.of("dimension", definition.dimensionTheme().isBlank() ? definition.dimension() : definition.dimensionTheme())
                ));
            }
        }
        for (BossThemeDefinition definition : state.bossThemes()) {
            if (definition.enabled() && !definition.entity().isBlank()) {
                result.add(new FirstDiscoveryDefinition(
                        "compat:first_boss:" + definition.entity(),
                        DiscoveryTriggerType.ENTITY_KILLED,
                        definition.entity(),
                        PlaceType.FIRST_DISCOVERY,
                        Math.max(1.0, definition.priority() / 4.0),
                        Map.of("boss", definition.bossTheme().isBlank() ? definition.entity() : definition.bossTheme())
                ));
            }
        }
        return List.copyOf(result);
    }

    public static Summary summary() {
        return state.summary();
    }

    public static String summaryLine() {
        return summary().toString();
    }

    public static List<String> warnings() {
        return state.warnings();
    }

    public static String lookupDebug(String kind, String id) {
        String normalizedKind = CompatThemeDefinitions.normalizeSimple(kind);
        return switch (normalizedKind) {
            case "boss", "boss_theme" -> formatLookup("boss", bossTheme(id));
            case "structure", "structure_theme" -> formatLookup("structure", structureTheme(id));
            case "biome", "biome_theme" -> formatLookup("biome", biomeTheme(id));
            case "mob", "mob_theme" -> formatLookup("mob", mobTheme(id));
            case "block", "block_theme" -> formatLookup("block", blockTheme(id));
            case "dimension", "dimension_theme" -> formatLookup("dimension", dimensionTheme(id));
            default -> "World Remembers compat lookup failed: unknown kind=" + kind;
        };
    }

    public static boolean selfTestPassesVanillaExamples() {
        return bossTheme("minecraft:wither").matched()
                && bossTheme("minecraft:ender_dragon").matched()
                && structureTheme("minecraft:stronghold").matched()
                && mobTheme("minecraft:creeper").matched()
                && blockTheme("minecraft:diamond_ore").matched()
                && dimensionTheme("minecraft:the_nether").matched();
    }

    private static <T extends CompatDefinition> CompatLookup<T> lookup(List<T> definitions, String id) {
        String normalized = CompatThemeDefinitions.normalizeId(id);
        if (normalized.isBlank()) {
            return CompatLookup.missing("");
        }
        T best = null;
        for (T definition : definitions) {
            if (!definition.enabled() || !definition.exactTarget()) {
                continue;
            }
            if (!definition.targetId().equals(normalized)) {
                continue;
            }
            if (best == null || compareDefinition(definition, best) < 0) {
                best = definition;
            }
        }
        return best == null ? CompatLookup.missing(normalized) : CompatLookup.matched(normalized, best);
    }

    private static <T extends CompatDefinition> int compareDefinition(T left, T right) {
        int priority = Integer.compare(right.priority(), left.priority());
        if (priority != 0) {
            return priority;
        }
        int exact = Boolean.compare(right.exactTarget(), left.exactTarget());
        if (exact != 0) {
            return exact;
        }
        return left.source().compareTo(right.source());
    }

    private static String formatLookup(String kind, CompatLookup<? extends CompatDefinition> lookup) {
        if (!lookup.matched()) {
            return "World Remembers compat lookup"
                    + " kind=" + kind
                    + " id=" + lookup.requestedId()
                    + " matched=false fallbackUsed=true";
        }
        CompatDefinition definition = lookup.definition();
        return "World Remembers compat lookup"
                + " kind=" + kind
                + " id=" + lookup.requestedId()
                + " matched=true"
                + " source=" + definition.source()
                + " priority=" + definition.priority()
                + " enabled=" + definition.enabled()
                + " exact=" + definition.exactTarget()
                + " definition=" + definition;
    }

    private static <T> List<T> concat(List<T> first, List<T> second, List<T> third) {
        List<T> result = new ArrayList<>(first.size() + second.size() + third.size());
        result.addAll(first);
        result.addAll(second);
        result.addAll(third);
        return List.copyOf(result);
    }

    private static <T extends CompatDefinition> List<T> sorted(List<T> definitions) {
        return definitions.stream()
                .filter(Objects::nonNull)
                .sorted(WorldRemembersCompatRegistries::compareDefinition)
                .toList();
    }

    private static BossThemeDefinition withSource(BossThemeDefinition value, String source) {
        return new BossThemeDefinition(value.id(), value.entity(), value.bossTheme(), value.placeType(), value.causeType(), value.priority(), value.enabled(), value.tags(), source, value.replace());
    }

    private static StructureThemeDefinition withSource(StructureThemeDefinition value, String source) {
        return new StructureThemeDefinition(value.id(), value.structure(), value.structureTheme(), value.placeType(), value.discoveryKind(), value.priority(), value.useStructureBounds(), value.enabled(), value.tags(), source, value.replace());
    }

    private static BiomeThemeDefinition withSource(BiomeThemeDefinition value, String source) {
        return new BiomeThemeDefinition(value.id(), value.biome(), value.biomeTag(), value.biomeTheme(), value.biomeGroup(), value.priority(), value.enabled(), source, value.replace());
    }

    private static MobThemeDefinition withSource(MobThemeDefinition value, String source) {
        return new MobThemeDefinition(value.id(), value.entity(), value.entityTag(), value.mobTheme(), value.mobGroup(), value.combatRole(), value.placeType(), value.priority(), value.enabled(), source, value.replace());
    }

    private static BlockThemeDefinition withSource(BlockThemeDefinition value, String source) {
        return new BlockThemeDefinition(value.id(), value.block(), value.blockTag(), value.blockTheme(), value.miningTheme(), value.valuable(), value.firstDiscoveryKey(), value.priority(), value.enabled(), source, value.replace());
    }

    private static DimensionThemeDefinition withSource(DimensionThemeDefinition value, String source) {
        return new DimensionThemeDefinition(value.id(), value.dimension(), value.dimensionTheme(), value.portalTheme(), value.firstDiscoveryKey(), value.priority(), value.enabled(), source, value.replace());
    }

    private static List<BossThemeDefinition> builtInBossThemes() {
        int p = CompatThemeDefinitions.BUILTIN_PRIORITY;
        String s = CompatThemeDefinitions.SOURCE_BUILTIN;
        return List.of(
                new BossThemeDefinition("builtin:wither", "minecraft:wither", "wither", PlaceType.BOSS_SITE, PlaceCauseType.BOSS_KILL, p, true, List.of("undead"), s, false),
                new BossThemeDefinition("builtin:ender_dragon", "minecraft:ender_dragon", "ender_dragon", PlaceType.BOSS_SITE, PlaceCauseType.BOSS_KILL, p, true, List.of("dragon", "end"), s, false),
                new BossThemeDefinition("builtin:warden", "minecraft:warden", "warden", PlaceType.BOSS_SITE, PlaceCauseType.BOSS_KILL, p, true, List.of("deep_dark"), s, false),
                new BossThemeDefinition("builtin:elder_guardian", "minecraft:elder_guardian", "elder_guardian", PlaceType.BOSS_SITE, PlaceCauseType.BOSS_KILL, p, true, List.of("aquatic"), s, false)
        );
    }

    private static List<StructureThemeDefinition> builtInStructureThemes() {
        return List.of(new StructureThemeDefinition(
                "builtin:stronghold",
                "minecraft:stronghold",
                "stronghold",
                PlaceType.FIRST_DISCOVERY,
                "structure",
                CompatThemeDefinitions.BUILTIN_PRIORITY,
                true,
                true,
                List.of("end"),
                CompatThemeDefinitions.SOURCE_BUILTIN,
                false
        ));
    }

    private static List<BiomeThemeDefinition> builtInBiomeThemes() {
        int p = CompatThemeDefinitions.BUILTIN_PRIORITY;
        String s = CompatThemeDefinitions.SOURCE_BUILTIN;
        return List.of(
                new BiomeThemeDefinition("builtin:plains", "minecraft:plains", "", "open_field", "plains", p, true, s, false),
                new BiomeThemeDefinition("builtin:forest", "minecraft:forest", "", "woodland_trail", "forest", p, true, s, false),
                new BiomeThemeDefinition("builtin:desert", "minecraft:desert", "", "desert_marker", "desert", p, true, s, false),
                new BiomeThemeDefinition("builtin:cherry_grove", "minecraft:cherry_grove", "", "woodland_trail", "cherry_grove", p, true, s, false),
                new BiomeThemeDefinition("builtin:swamp", "minecraft:swamp", "", "swamp_path", "swamp", p, true, s, false)
        );
    }

    private static List<MobThemeDefinition> builtInMobThemes() {
        int p = CompatThemeDefinitions.BUILTIN_PRIORITY;
        String s = CompatThemeDefinitions.SOURCE_BUILTIN;
        return List.of(
                new MobThemeDefinition("builtin:creeper", "minecraft:creeper", "", "creeper", "explosive", "hostile", PlaceType.BATTLEFIELD, p, true, s, false),
                new MobThemeDefinition("builtin:skeleton", "minecraft:skeleton", "", "skeleton", "skeleton", "hostile", PlaceType.BATTLEFIELD, p, true, s, false),
                new MobThemeDefinition("builtin:zombie", "minecraft:zombie", "", "zombie", "undead", "hostile", PlaceType.BATTLEFIELD, p, true, s, false),
                new MobThemeDefinition("builtin:pillager", "minecraft:pillager", "", "pillager", "illager", "hostile", PlaceType.BATTLEFIELD, p, true, s, false),
                new MobThemeDefinition("builtin:blaze", "minecraft:blaze", "", "blaze", "nether", "hostile", PlaceType.BATTLEFIELD, p, true, s, false),
                new MobThemeDefinition("builtin:enderman", "minecraft:enderman", "", "enderman", "end", "neutral", PlaceType.BATTLEFIELD, p, true, s, false),
                new MobThemeDefinition("builtin:breeze", "minecraft:breeze", "", "breeze", "trial", "hostile", PlaceType.BATTLEFIELD, p, true, s, false)
        );
    }

    private static List<BlockThemeDefinition> builtInBlockThemes() {
        int p = CompatThemeDefinitions.BUILTIN_PRIORITY;
        String s = CompatThemeDefinitions.SOURCE_BUILTIN;
        return List.of(
                new BlockThemeDefinition("builtin:diamond_ore", "minecraft:diamond_ore", "", "diamond", "diamond", true, "world:first_diamond_ore_mined", p, true, s, false),
                new BlockThemeDefinition("builtin:deepslate_diamond_ore", "minecraft:deepslate_diamond_ore", "", "diamond", "diamond", true, "world:first_diamond_ore_mined", p, true, s, false),
                new BlockThemeDefinition("builtin:ancient_debris", "minecraft:ancient_debris", "", "ancient_debris", "ancient_debris", true, "world:first_ancient_debris_mined", p, true, s, false)
        );
    }

    private static List<DimensionThemeDefinition> builtInDimensionThemes() {
        int p = CompatThemeDefinitions.BUILTIN_PRIORITY;
        String s = CompatThemeDefinitions.SOURCE_BUILTIN;
        return List.of(
                new DimensionThemeDefinition("builtin:nether", "minecraft:the_nether", "nether", "nether", "world:first_nether_entry", p, true, s, false),
                new DimensionThemeDefinition("builtin:end", "minecraft:the_end", "end", "end", "world:first_end_entry", p, true, s, false)
        );
    }

    private record State(
            List<BossThemeDefinition> bossThemes,
            List<StructureThemeDefinition> structureThemes,
            List<BiomeThemeDefinition> biomeThemes,
            List<MobThemeDefinition> mobThemes,
            List<BlockThemeDefinition> blockThemes,
            List<DimensionThemeDefinition> dimensionThemes,
            List<String> warnings
    ) {
        static State from(
                List<BossThemeDefinition> bossThemes,
                List<StructureThemeDefinition> structureThemes,
                List<BiomeThemeDefinition> biomeThemes,
                List<MobThemeDefinition> mobThemes,
                List<BlockThemeDefinition> blockThemes,
                List<DimensionThemeDefinition> dimensionThemes,
                List<String> warnings
        ) {
            return new State(
                    sorted(bossThemes),
                    sorted(structureThemes),
                    sorted(biomeThemes),
                    sorted(mobThemes),
                    sorted(blockThemes),
                    sorted(dimensionThemes),
                    warnings == null ? List.of() : List.copyOf(warnings)
            );
        }

        LoadedData dataOnly() {
            return new LoadedData(
                    bossThemes.stream().filter(definition -> CompatThemeDefinitions.SOURCE_JSON.equals(definition.source())).toList(),
                    structureThemes.stream().filter(definition -> CompatThemeDefinitions.SOURCE_JSON.equals(definition.source())).toList(),
                    biomeThemes.stream().filter(definition -> CompatThemeDefinitions.SOURCE_JSON.equals(definition.source())).toList(),
                    mobThemes.stream().filter(definition -> CompatThemeDefinitions.SOURCE_JSON.equals(definition.source())).toList(),
                    blockThemes.stream().filter(definition -> CompatThemeDefinitions.SOURCE_JSON.equals(definition.source())).toList(),
                    dimensionThemes.stream().filter(definition -> CompatThemeDefinitions.SOURCE_JSON.equals(definition.source())).toList(),
                    warnings
            );
        }

        Summary summary() {
            return new Summary(
                    bossThemes.size(),
                    structureThemes.size(),
                    biomeThemes.size(),
                    mobThemes.size(),
                    blockThemes.size(),
                    dimensionThemes.size(),
                    warnings.size()
            );
        }
    }

    public record CompatLookup<T extends CompatDefinition>(
            String requestedId,
            T definition,
            boolean matched
    ) {
        static <T extends CompatDefinition> CompatLookup<T> matched(String requestedId, T definition) {
            return new CompatLookup<>(requestedId, definition, true);
        }

        static <T extends CompatDefinition> CompatLookup<T> missing(String requestedId) {
            return new CompatLookup<>(requestedId, null, false);
        }
    }

    public record LoadedData(
            List<BossThemeDefinition> bossThemes,
            List<StructureThemeDefinition> structureThemes,
            List<BiomeThemeDefinition> biomeThemes,
            List<MobThemeDefinition> mobThemes,
            List<BlockThemeDefinition> blockThemes,
            List<DimensionThemeDefinition> dimensionThemes,
            List<String> warnings
    ) {
        public LoadedData {
            bossThemes = bossThemes == null ? List.of() : List.copyOf(bossThemes);
            structureThemes = structureThemes == null ? List.of() : List.copyOf(structureThemes);
            biomeThemes = biomeThemes == null ? List.of() : List.copyOf(biomeThemes);
            mobThemes = mobThemes == null ? List.of() : List.copyOf(mobThemes);
            blockThemes = blockThemes == null ? List.of() : List.copyOf(blockThemes);
            dimensionThemes = dimensionThemes == null ? List.of() : List.copyOf(dimensionThemes);
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }

        public static LoadedData empty() {
            return new LoadedData(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        }
    }

    public record Summary(
            int bossThemes,
            int structureThemes,
            int biomeThemes,
            int mobThemes,
            int blockThemes,
            int dimensionThemes,
            int warnings
    ) {
        @Override
        public String toString() {
            return "bossThemes=" + bossThemes
                    + " structureThemes=" + structureThemes
                    + " biomeThemes=" + biomeThemes
                    + " mobThemes=" + mobThemes
                    + " blockThemes=" + blockThemes
                    + " dimensionThemes=" + dimensionThemes
                    + " warnings=" + warnings;
        }
    }
}
