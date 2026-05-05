package com.worldremembers.livinglegends;

import java.util.EnumMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BuiltInNameData {
    public static final String DEFAULT_STYLE_ID = "vanilla_adventure";

    private BuiltInNameData() {
    }

    public static NameDataPack defaultPack() {
        return Holder.PACKS.getOrDefault(DEFAULT_STYLE_ID, Holder.DEFAULT_PACK);
    }

    public static NameDataPack packForStyle(String styleId) {
        return packForStyle(styleId, List.of(styleId), false);
    }

    public static NameDataPack packForStyle(
            String styleId,
            Iterable<String> enabledStyles,
            boolean allowMixedStyleTokens
    ) {
        String resolvedStyleId = builtInStyleId(styleId);
        NameDataPack selectedPack = Holder.PACKS.getOrDefault(resolvedStyleId, defaultPack());
        if (!allowMixedStyleTokens) {
            return selectedPack;
        }

        Map<String, NameToken> tokens = new LinkedHashMap<>();
        for (NameToken token : selectedPack.tokens()) {
            tokens.putIfAbsent(token.id(), token);
        }
        if (enabledStyles != null) {
            for (String enabledStyle : enabledStyles) {
                NameDataPack pack = Holder.PACKS.get(builtInStyleId(enabledStyle));
                if (pack == null) {
                    continue;
                }
                for (NameToken token : pack.tokens()) {
                    tokens.putIfAbsent(token.id(), token);
                }
            }
        }
        return new NameDataPack(resolvedStyleId, selectedPack.patterns(), new ArrayList<>(tokens.values()));
    }

    public static List<NameDataPack> allPacks() {
        return List.copyOf(Holder.PACKS.values());
    }

    public static String builtInStyleId(String styleId) {
        NameStyle style = NameStyle.fromId(styleId);
        return switch (style) {
            case VANILLA_ADVENTURE, DARK_FANTASY, COZY_SURVIVAL, EPIC_MYTHOLOGY, NEUTRAL_SERVER, FUNNY_COMMUNITY ->
                    style.idString();
            default -> DEFAULT_STYLE_ID;
        };
    }

    public static NameToken token(String id, String... tags) {
        return new NameToken(
                id,
                Set.of(tags),
                1.0,
                tokenForms(id),
                NameSemanticRoots.inferTokenRoot(id),
                inferredTokenCauseConstraints(Set.of(tags))
        );
    }

    public static NameToken weightedToken(String id, double weight, String... tags) {
        return new NameToken(
                id,
                Set.of(tags),
                weight,
                tokenForms(id),
                NameSemanticRoots.inferTokenRoot(id),
                inferredTokenCauseConstraints(Set.of(tags))
        );
    }

    private static NameToken constrainedToken(
            String id,
            NameCauseConstraints constraints,
            String... tags
    ) {
        return new NameToken(
                id,
                Set.of(tags),
                1.0,
                tokenForms(id),
                NameSemanticRoots.inferTokenRoot(id),
                constraints
        );
    }

    private static NamePattern pattern(String id, PlaceType placeType, double weight, String... slotTags) {
        return new NamePattern(
                id,
                "living_legends.name.pattern." + id,
                Set.of(placeType),
                Set.of(),
                weight,
                slots(slotTags),
                Set.of(DEFAULT_STYLE_ID)
        );
    }

    private static NamePattern patternWithSlot(
            String id,
            PlaceType placeType,
            double weight,
            String tokenTag,
            NameTokenForm tokenForm
    ) {
        return new NamePattern(
                id,
                "living_legends.name.pattern." + id,
                Set.of(placeType),
                Set.of(),
                weight,
                List.of(new NamePatternSlot("token_1", tokenTag, tokenForm)),
                Set.of(DEFAULT_STYLE_ID)
        );
    }

    private static NamePattern patternWithSlots(
            String id,
            PlaceType placeType,
            double weight,
            NamePatternSlot... patternSlots
    ) {
        return new NamePattern(
                id,
                "living_legends.name.pattern." + id,
                Set.of(placeType),
                Set.of(),
                weight,
                List.of(patternSlots),
                Set.of(DEFAULT_STYLE_ID)
        );
    }

    private static NamePatternSlot slot(String id, String tokenTag, NameTokenForm tokenForm) {
        return new NamePatternSlot(id, tokenTag, tokenForm);
    }

    private static NamePattern exactFirstDiscoveryPattern(String id, String firstDiscoveryKey, double weight) {
        return new NamePattern(
                id,
                "living_legends.name.pattern." + id,
                Set.of(PlaceType.FIRST_DISCOVERY),
                Set.of(),
                weight,
                List.of(),
                Set.of(DEFAULT_STYLE_ID, "exact_cause"),
                NameSemanticRoots.inferPatternRoot(id),
                NameCauseConstraints.builder()
                        .requiredFirstDiscoveryKey(firstDiscoveryKey)
                        .build(),
                Set.of()
        );
    }

    private static NamePattern dominantTargetPattern(
            String id,
            PlaceType placeType,
            double weight,
            NameCauseConstraints constraints
    ) {
        return new NamePattern(
                id,
                "living_legends.name.pattern." + id,
                Set.of(placeType),
                Set.of(),
                weight,
                List.of(),
                Set.of(DEFAULT_STYLE_ID, "dominant_target"),
                NameSemanticRoots.inferPatternRoot(id),
                constraints,
                Set.of()
        );
    }

    private static NamePattern causeTypePattern(
            String id,
            PlaceType placeType,
            double weight,
            PlaceCauseType causeType
    ) {
        return new NamePattern(
                id,
                "living_legends.name.pattern." + id,
                Set.of(placeType),
                Set.of(),
                weight,
                List.of(),
                Set.of(DEFAULT_STYLE_ID, "cause_type"),
                NameSemanticRoots.inferPatternRoot(id),
                NameCauseConstraints.builder()
                        .requiredCauseType(causeType)
                        .build(),
                Set.of()
        );
    }

    private static NamePattern biomeGeneralPattern(String id, String biomeGroup, double weight) {
        return new NamePattern(
                id,
                "living_legends.name.pattern." + id,
                Set.of(PlaceType.GENERAL_LANDMARK),
                Set.of(),
                weight,
                List.of(),
                Set.of(DEFAULT_STYLE_ID, "biome", "general_landmark"),
                NameSemanticRoots.inferPatternRoot(id),
                NameCauseConstraints.builder()
                        .requiredCauseType(PlaceCauseType.VISITS)
                        .requiredBiomeGroups(biomeGroup)
                        .build(),
                Set.of()
        );
    }

    private static NameDataPack withHighFrequencyCauseVariants(NameDataPack pack) {
        if (pack == null) {
            return null;
        }

        List<NamePattern> patterns = new ArrayList<>(pack.patterns());
        addHighFrequencyCauseVariants(patterns, pack.styleId());
        return new NameDataPack(pack.styleId(), patterns, pack.tokens());
    }

    private static void addHighFrequencyCauseVariants(List<NamePattern> patterns, String styleId) {
        String resolvedStyleId = builtInStyleId(styleId);
        switch (NameStyle.fromId(resolvedStyleId)) {
            case DARK_FANTASY -> {
                addDeathCausePatterns(patterns, resolvedStyleId, DeathSiteEnvironment.SURFACE, "fall", 1.15,
                        "df_death_cause_fall_broken_ledge",
                        "df_death_cause_fall_hollow_drop",
                        "df_death_cause_fall_last_step",
                        "df_death_cause_fall_pale_edge",
                        "df_death_cause_fall_silent_plunge");
                addDeathCausePatterns(patterns, resolvedStyleId, DeathSiteEnvironment.MOUNTAIN, "fall", 1.15,
                        "df_death_cause_fall_black_cliff",
                        "df_death_cause_fall_cold_descent",
                        "df_death_cause_fall_crownless_height",
                        "df_death_cause_fall_stone_below");
            }
            case COZY_SURVIVAL -> {
                addDeathCausePatterns(patterns, resolvedStyleId, DeathSiteEnvironment.SURFACE, "fall", 1.15,
                        "cs_death_cause_fall_last_step",
                        "cs_death_cause_fall_soft_marker",
                        "cs_death_cause_fall_little_drop",
                        "cs_death_cause_fall_warning_spot",
                        "cs_death_cause_fall_quiet_edge");
                addDeathCausePatterns(patterns, resolvedStyleId, DeathSiteEnvironment.MOUNTAIN, "fall", 1.15,
                        "cs_death_cause_fall_high_step",
                        "cs_death_cause_fall_cold_marker",
                        "cs_death_cause_fall_snowy_drop",
                        "cs_death_cause_fall_mountain_warning");
            }
            case EPIC_MYTHOLOGY -> {
                addDeathCausePatterns(patterns, resolvedStyleId, DeathSiteEnvironment.SURFACE, "fall", 1.15,
                        "em_death_cause_fall_last_ledge",
                        "em_death_cause_fall_fallen_step",
                        "em_death_cause_fall_oath_below",
                        "em_death_cause_fall_edge_of_fate",
                        "em_death_cause_fall_highfall_mark");
                addDeathCausePatterns(patterns, resolvedStyleId, DeathSiteEnvironment.MOUNTAIN, "fall", 1.15,
                        "em_death_cause_fall_cliff_of_fate",
                        "em_death_cause_fall_summit_oath",
                        "em_death_cause_fall_stone_descent",
                        "em_death_cause_fall_height_of_omens");
            }
            case NEUTRAL_SERVER -> {
                addDeathCausePatterns(patterns, resolvedStyleId, DeathSiteEnvironment.SURFACE, "fall", 1.15,
                        "ns_death_cause_fall_site",
                        "ns_death_cause_fall_drop_point",
                        "ns_death_cause_fall_ledge_marker",
                        "ns_death_cause_fall_highfall_site",
                        "ns_death_cause_fall_edge_record");
                addDeathCausePatterns(patterns, resolvedStyleId, DeathSiteEnvironment.MOUNTAIN, "fall", 1.15,
                        "ns_death_cause_fall_cliff_site",
                        "ns_death_cause_fall_mountain_drop",
                        "ns_death_cause_fall_high_marker",
                        "ns_death_cause_fall_summit_record");
            }
            case FUNNY_COMMUNITY -> {
                addDeathCausePatterns(patterns, resolvedStyleId, DeathSiteEnvironment.SURFACE, "fall", 1.15,
                        "fc_death_cause_fall_gravity_claim",
                        "fc_death_cause_fall_floor_late",
                        "fc_death_cause_fall_edge_review",
                        "fc_death_cause_fall_ankle_report",
                        "fc_death_cause_fall_short_flight");
                addDeathCausePatterns(patterns, resolvedStyleId, DeathSiteEnvironment.MOUNTAIN, "fall", 1.15,
                        "fc_death_cause_fall_mountain_disagreement",
                        "fc_death_cause_fall_summit_oops",
                        "fc_death_cause_fall_cliff_notes",
                        "fc_death_cause_fall_long_way_down");
            }
            default -> {
                addDeathCausePatterns(patterns, resolvedStyleId, DeathSiteEnvironment.SURFACE, "fall", 1.15,
                        "va_death_cause_fall_step",
                        "va_death_cause_fall_broken_drop",
                        "va_death_cause_fall_highfall_mark",
                        "va_death_cause_fall_edge");
                addDeathCausePatterns(patterns, resolvedStyleId, DeathSiteEnvironment.MOUNTAIN, "fall", 1.15,
                        "va_death_cause_fall_high_cliff",
                        "va_death_cause_fall_stone_descent",
                        "va_death_cause_fall_cold_drop");
                addDeathCausePatterns(patterns, resolvedStyleId, DeathSiteEnvironment.NETHER, "lava", 1.15,
                        "va_death_cause_lava_pool",
                        "va_death_cause_lava_marker",
                        "va_death_cause_lava_last_light",
                        "va_death_cause_lava_blackstone_edge");
                addDeathCausePatterns(patterns, resolvedStyleId, DeathSiteEnvironment.NETHER, "fire", 1.15,
                        "va_death_cause_fire_mark",
                        "va_death_cause_fire_threshold",
                        "va_death_cause_fire_last_spark",
                        "va_death_cause_fire_ashen_trace");
                addDeathCausePatterns(patterns, resolvedStyleId, DeathSiteEnvironment.WATER, "drowning", 1.15,
                        "va_death_cause_drowning_marker",
                        "va_death_cause_drowning_deep_water",
                        "va_death_cause_drowning_last_breath",
                        "va_death_cause_drowning_quiet_current");
                addDeathCausePatterns(patterns, resolvedStyleId, DeathSiteEnvironment.END, "void", 1.15,
                        "va_death_cause_void_edge",
                        "va_death_cause_void_threshold",
                        "va_death_cause_void_last_step",
                        "va_death_cause_void_pale_drop");
            }
        }
    }

    private static void addDeathCausePatterns(
            List<NamePattern> patterns,
            String styleId,
            DeathSiteEnvironment environment,
            String deathCause,
            double weight,
            String... ids
    ) {
        for (String id : ids) {
            patterns.add(deathCausePattern(id, environment, deathCause, weight, styleId));
        }
    }

    private static NameDataPack withContentCompatPatterns(NameDataPack pack) {
        if (pack == null) {
            return null;
        }

        List<NamePattern> patterns = new ArrayList<>(pack.patterns());
        addContentCompatPatterns(patterns, pack.styleId());
        return new NameDataPack(pack.styleId(), patterns, pack.tokens());
    }

    private static void addContentCompatPatterns(List<NamePattern> patterns, String styleId) {
        String resolvedStyleId = builtInStyleId(styleId);
        String prefix = contentCompatStylePrefix(resolvedStyleId);
        for (ContentCompatPatternDefinition definition : CONTENT_COMPAT_PATTERNS) {
            String patternId = prefix + definition.suffix();
            patterns.add(new NamePattern(
                    patternId,
                    "living_legends.name.pattern." + patternId,
                    Set.of(definition.placeType()),
                    Set.of(),
                    12.0,
                    List.of(),
                    contentCompatTags(resolvedStyleId, definition.tags()),
                    NameSemanticRoots.inferPatternRoot(patternId),
                    definition.constraints(),
                    Set.of()
            ));
        }
    }

    private static String contentCompatStylePrefix(String styleId) {
        return switch (NameStyle.fromId(styleId)) {
            case DARK_FANTASY -> "compat_df_";
            case COZY_SURVIVAL -> "compat_cs_";
            case EPIC_MYTHOLOGY -> "compat_em_";
            case NEUTRAL_SERVER -> "compat_ns_";
            case FUNNY_COMMUNITY -> "compat_fc_";
            default -> "compat_va_";
        };
    }

    private static Set<String> contentCompatTags(String styleId, Set<String> tags) {
        Set<String> result = new LinkedHashSet<>();
        result.add(styleId);
        result.add("compat");
        result.add("content_compat");
        if (tags != null) {
            result.addAll(tags);
        }
        return Set.copyOf(result);
    }

    private static ContentCompatPatternDefinition firstCompat(String suffix, String firstDiscoveryKey, String... tags) {
        return new ContentCompatPatternDefinition(
                suffix,
                PlaceType.FIRST_DISCOVERY,
                NameCauseConstraints.builder()
                        .requiredFirstDiscoveryKey(firstDiscoveryKey)
                        .build(),
                compatDefinitionTags("exact_cause", tags)
        );
    }

    private static ContentCompatPatternDefinition bossCompat(String suffix, String bossId, String... tags) {
        return new ContentCompatPatternDefinition(
                suffix,
                PlaceType.BOSS_SITE,
                bossConstraints(bossId),
                compatDefinitionTags("dominant_target", tags)
        );
    }

    private static ContentCompatPatternDefinition mobGroupCompat(String suffix, String mobGroup, String... tags) {
        return new ContentCompatPatternDefinition(
                suffix,
                PlaceType.BATTLEFIELD,
                mobConstraints("group:" + mobGroup),
                compatDefinitionTags("dominant_target", tags)
        );
    }

    private static ContentCompatPatternDefinition mobThemeCompat(String suffix, String mobTheme, String... tags) {
        return new ContentCompatPatternDefinition(
                suffix,
                PlaceType.BATTLEFIELD,
                mobConstraints("theme:" + mobTheme),
                compatDefinitionTags("dominant_target", tags)
        );
    }

    private static ContentCompatPatternDefinition biomeGroupCompat(String suffix, String biomeGroup, String... tags) {
        return new ContentCompatPatternDefinition(
                suffix,
                PlaceType.GENERAL_LANDMARK,
                NameCauseConstraints.builder()
                        .requiredCauseType(PlaceCauseType.VISITS)
                        .requiredBiomeGroups(biomeGroup)
                        .build(),
                compatDefinitionTags("biome", tags)
        );
    }

    private static ContentCompatPatternDefinition biomeThemeCompat(String suffix, String biomeTheme, String... tags) {
        return new ContentCompatPatternDefinition(
                suffix,
                PlaceType.GENERAL_LANDMARK,
                NameCauseConstraints.builder()
                        .requiredCauseType(PlaceCauseType.VISITS)
                        .requiredBiomeThemes(biomeTheme)
                        .build(),
                compatDefinitionTags("biome", tags)
        );
    }

    private static ContentCompatPatternDefinition miningCompat(String suffix, String[] blockIds, String... tags) {
        return new ContentCompatPatternDefinition(
                suffix,
                PlaceType.MINING_SITE,
                blockConstraints(blockIds),
                compatDefinitionTags("dominant_target", tags)
        );
    }

    private static Set<String> compatDefinitionTags(String baseTag, String... tags) {
        Set<String> result = new LinkedHashSet<>();
        String normalizedBase = WorldPos.optionalId(baseTag);
        if (!normalizedBase.isBlank()) {
            result.add(normalizedBase);
        }
        if (tags != null) {
            for (String tag : tags) {
                String normalized = WorldPos.optionalId(tag);
                if (!normalized.isBlank()) {
                    result.add(normalized);
                }
            }
        }
        return Set.copyOf(result);
    }

    private record ContentCompatPatternDefinition(
            String suffix,
            PlaceType placeType,
            NameCauseConstraints constraints,
            Set<String> tags
    ) {
    }

    private static final List<ContentCompatPatternDefinition> CONTENT_COMPAT_PATTERNS = List.of(
            firstCompat("first_aether_gate", "compat:first_dimension:aether:the_aether", "aether"),
            firstCompat("first_aether_bronze_dungeon", "compat:first_structure:aether:bronze_dungeon", "aether", "dungeon"),
            firstCompat("first_aether_silver_dungeon", "compat:first_structure:aether:silver_dungeon", "aether", "dungeon"),
            firstCompat("first_aether_gold_dungeon", "compat:first_structure:aether:gold_dungeon", "aether", "dungeon"),
            firstCompat("first_aether_ambrosium", "compat:first_block:aether:ambrosium_ore", "aether", "mining"),
            firstCompat("first_aether_zanite", "compat:first_block:aether:zanite_ore", "aether", "mining"),
            firstCompat("first_aether_gravitite", "compat:first_block:aether:gravitite_ore", "aether", "mining"),
            firstCompat("first_aether_slider", "compat:first_boss:aether:slider", "aether", "boss"),
            firstCompat("first_aether_valkyrie", "compat:first_boss:aether:valkyrie_queen", "aether", "boss"),
            firstCompat("first_aether_sun_spirit", "compat:first_boss:aether:sun_spirit", "aether", "boss"),
            bossCompat("boss_aether_slider", "aether:slider", "aether", "boss"),
            bossCompat("boss_aether_valkyrie", "aether:valkyrie_queen", "aether", "boss"),
            bossCompat("boss_aether_sun_spirit", "aether:sun_spirit", "aether", "boss"),
            biomeGroupCompat("general_aether_sky", "aether_sky", "aether", "sky"),
            mobGroupCompat("battle_aether_sky", "aether_sky", "aether", "sky"),
            mobGroupCompat("battle_aether_dungeon", "aether_dungeon", "aether", "dungeon"),
            miningCompat("mining_aether_gold", new String[] {"aether:ambrosium_ore"}, "aether", "mining"),
            miningCompat("mining_aether_zanite", new String[] {"aether:zanite_ore"}, "aether", "mining"),
            miningCompat("mining_aether_gravitite", new String[] {"aether:gravitite_ore"}, "aether", "mining"),

            firstCompat("first_otherside", "compat:first_dimension:deeperdarker:otherside", "otherside"),
            firstCompat("first_ancient_temple", "compat:first_structure:deeperdarker:ancient_temple", "otherside", "temple"),
            firstCompat("first_deep_gloomslate_diamond", "compat:first_block:deeperdarker:gloomslate_diamond_ore", "otherside", "mining"),
            firstCompat("first_deep_sculk_diamond", "compat:first_block:deeperdarker:sculk_stone_diamond_ore", "otherside", "mining"),
            firstCompat("first_deep_gloomslate_emerald", "compat:first_block:deeperdarker:gloomslate_emerald_ore", "otherside", "mining"),
            firstCompat("first_deep_sculk_emerald", "compat:first_block:deeperdarker:sculk_stone_emerald_ore", "otherside", "mining"),
            firstCompat("first_stalker", "compat:first_boss:deeperdarker:stalker", "otherside", "boss"),
            bossCompat("boss_stalker", "deeperdarker:stalker", "otherside", "boss"),
            biomeGroupCompat("general_otherside", "otherside", "otherside"),
            biomeGroupCompat("general_sculk_echo", "sculk_echo", "otherside", "sculk"),
            mobGroupCompat("battle_sculk_echo", "sculk_echo", "otherside", "sculk"),
            mobGroupCompat("battle_otherside", "otherside", "otherside"),
            miningCompat(
                    "mining_deep_diamond",
                    new String[] {"deeperdarker:gloomslate_diamond_ore", "deeperdarker:sculk_stone_diamond_ore"},
                    "otherside",
                    "mining"
            ),
            miningCompat(
                    "mining_deep_emerald",
                    new String[] {"deeperdarker:gloomslate_emerald_ore", "deeperdarker:sculk_stone_emerald_ore"},
                    "otherside",
                    "mining"
            ),

            firstCompat("first_bomd_lich_tower", "compat:first_structure:bosses_of_mass_destruction:lich_tower", "bomd", "boss"),
            firstCompat("first_bomd_gauntlet_arena", "compat:first_structure:bosses_of_mass_destruction:gauntlet_arena", "bomd", "boss"),
            firstCompat("first_bomd_obsidilith_arena", "compat:first_structure:bosses_of_mass_destruction:obsidilith_arena", "bomd", "boss"),
            firstCompat("first_bomd_void_blossom_structure", "compat:first_structure:bosses_of_mass_destruction:void_blossom", "bomd", "boss"),
            firstCompat("first_bomd_lich", "compat:first_boss:bosses_of_mass_destruction:lich", "bomd", "boss"),
            firstCompat("first_bomd_gauntlet", "compat:first_boss:bosses_of_mass_destruction:gauntlet", "bomd", "boss"),
            firstCompat("first_bomd_obsidilith", "compat:first_boss:bosses_of_mass_destruction:obsidilith", "bomd", "boss"),
            firstCompat("first_bomd_void_blossom_boss", "compat:first_boss:bosses_of_mass_destruction:void_blossom", "bomd", "boss"),
            bossCompat("boss_bomd_lich", "bosses_of_mass_destruction:lich", "bomd", "boss"),
            bossCompat("boss_bomd_gauntlet", "bosses_of_mass_destruction:gauntlet", "bomd", "boss"),
            bossCompat("boss_bomd_obsidilith", "bosses_of_mass_destruction:obsidilith", "bomd", "boss"),
            bossCompat("boss_bomd_void_blossom", "bosses_of_mass_destruction:void_blossom", "bomd", "boss"),

            firstCompat("first_cataclysm_soul_black_smith", "compat:first_structure:cataclysm:soul_black_smith", "cataclysm", "forge"),
            firstCompat("first_cataclysm_burning_arena", "compat:first_structure:cataclysm:burning_arena", "cataclysm", "arena"),
            firstCompat("first_cataclysm_ruined_citadel", "compat:first_structure:cataclysm:ruined_citadel", "cataclysm", "citadel"),
            firstCompat("first_cataclysm_cursed_pyramid", "compat:first_structure:cataclysm:cursed_pyramid", "cataclysm", "curse"),
            firstCompat("first_cataclysm_ancient_factory", "compat:first_structure:cataclysm:ancient_factory", "cataclysm", "factory"),
            firstCompat("first_cataclysm_sunken_city", "compat:first_structure:cataclysm:sunken_city", "cataclysm", "abyss"),
            firstCompat("first_cataclysm_frosted_prison", "compat:first_structure:cataclysm:frosted_prison", "cataclysm", "prison"),
            firstCompat("first_cataclysm_acropolis", "compat:first_structure:cataclysm:acropolis", "cataclysm", "acropolis"),
            firstCompat("first_cataclysm_amethyst_nest", "compat:first_structure:cataclysm:amethyst_nest", "cataclysm", "amethyst"),
            firstCompat("first_cataclysm_ancient_metal", "compat:first_block:cataclysm:ancient_metal_block", "cataclysm", "mining"),
            firstCompat("first_cataclysm_ignitium", "compat:first_block:cataclysm:ignitium_block", "cataclysm", "mining"),
            firstCompat("first_cataclysm_cursium", "compat:first_block:cataclysm:cursium_block", "cataclysm", "mining"),
            firstCompat("first_cataclysm_witherite", "compat:first_block:cataclysm:witherite_block", "cataclysm", "mining"),
            firstCompat("first_cataclysm_enderite", "compat:first_block:cataclysm:enderite_block", "cataclysm", "mining"),
            firstCompat("first_cataclysm_void_crystal", "compat:first_block:cataclysm:void_crystal", "cataclysm", "mining"),
            firstCompat("first_cataclysm_monstrosity", "compat:first_boss:cataclysm:netherite_monstrosity", "cataclysm", "boss"),
            firstCompat("first_cataclysm_ignis", "compat:first_boss:cataclysm:ignis", "cataclysm", "boss"),
            firstCompat("first_cataclysm_harbinger", "compat:first_boss:cataclysm:the_harbinger", "cataclysm", "boss"),
            firstCompat("first_cataclysm_leviathan", "compat:first_boss:cataclysm:the_leviathan", "cataclysm", "boss"),
            firstCompat("first_cataclysm_ender_guardian", "compat:first_boss:cataclysm:ender_guardian", "cataclysm", "boss"),
            firstCompat("first_cataclysm_maledictus", "compat:first_boss:cataclysm:maledictus", "cataclysm", "boss"),
            firstCompat("first_cataclysm_remnant", "compat:first_boss:cataclysm:ancient_remnant", "cataclysm", "boss"),
            firstCompat("first_cataclysm_scylla", "compat:first_boss:cataclysm:scylla", "cataclysm", "boss"),
            firstCompat("first_cataclysm_clawdian", "compat:first_boss:cataclysm:clawdian", "cataclysm", "boss"),
            firstCompat("first_cataclysm_revenant", "compat:first_boss:cataclysm:ignited_revenant", "cataclysm", "boss"),
            firstCompat("first_cataclysm_ender_golem", "compat:first_boss:cataclysm:ender_golem", "cataclysm", "boss"),
            bossCompat("boss_cataclysm_monstrosity", "cataclysm:netherite_monstrosity", "cataclysm", "boss"),
            bossCompat("boss_cataclysm_ignis", "cataclysm:ignis", "cataclysm", "boss"),
            bossCompat("boss_cataclysm_harbinger", "cataclysm:the_harbinger", "cataclysm", "boss"),
            bossCompat("boss_cataclysm_leviathan", "cataclysm:the_leviathan", "cataclysm", "boss"),
            bossCompat("boss_cataclysm_ender_guardian", "cataclysm:ender_guardian", "cataclysm", "boss"),
            bossCompat("boss_cataclysm_maledictus", "cataclysm:maledictus", "cataclysm", "boss"),
            bossCompat("boss_cataclysm_remnant", "cataclysm:ancient_remnant", "cataclysm", "boss"),
            bossCompat("boss_cataclysm_scylla", "cataclysm:scylla", "cataclysm", "boss"),
            bossCompat("boss_cataclysm_clawdian", "cataclysm:clawdian", "cataclysm", "boss"),
            bossCompat("boss_cataclysm_revenant", "cataclysm:ignited_revenant", "cataclysm", "boss"),
            bossCompat("boss_cataclysm_ender_golem", "cataclysm:ender_golem", "cataclysm", "boss"),
            mobGroupCompat("battle_cataclysm_abyss", "cataclysm_abyss", "cataclysm", "abyss"),
            mobGroupCompat("battle_cataclysm_frost", "cataclysm_frost", "cataclysm", "frost"),
            mobGroupCompat("battle_cataclysm_desert", "cataclysm_desert", "cataclysm", "desert"),
            mobGroupCompat("battle_cataclysm_infernal", "cataclysm_infernal", "cataclysm", "fire"),
            mobGroupCompat("battle_cataclysm_void", "cataclysm_void", "cataclysm", "void"),
            mobGroupCompat("battle_cataclysm_acropolis", "cataclysm_acropolis", "cataclysm", "acropolis"),
            miningCompat(
                    "mining_cataclysm_relic",
                    new String[] {"cataclysm:ancient_metal_block", "cataclysm:cursium_block"},
                    "cataclysm",
                    "mining"
            ),
            miningCompat("mining_cataclysm_ignitium", new String[] {"cataclysm:ignitium_block"}, "cataclysm", "mining"),
            miningCompat("mining_cataclysm_witherite", new String[] {"cataclysm:witherite_block"}, "cataclysm", "mining"),
            miningCompat("mining_cataclysm_enderite", new String[] {"cataclysm:enderite_block"}, "cataclysm", "mining"),
            miningCompat("mining_cataclysm_void_crystal", new String[] {"cataclysm:void_crystal"}, "cataclysm", "mining"),

            firstCompat("first_twilight_forest", "compat:first_dimension:twilightforest:twilight_forest", "twilightforest"),
            firstCompat("first_tf_naga_courtyard", "compat:first_structure:twilightforest:naga_courtyard", "twilightforest", "boss"),
            firstCompat("first_tf_lich_tower", "compat:first_structure:twilightforest:lich_tower", "twilightforest", "tower"),
            firstCompat("first_tf_hedge_maze", "compat:first_structure:twilightforest:hedge_maze", "twilightforest", "maze"),
            firstCompat("first_tf_labyrinth", "compat:first_structure:twilightforest:labyrinth", "twilightforest", "maze"),
            firstCompat("first_tf_hydra_lair", "compat:first_structure:twilightforest:hydra_lair", "twilightforest", "swamp"),
            firstCompat("first_tf_knight_stronghold", "compat:first_structure:twilightforest:knight_stronghold", "twilightforest", "knight"),
            firstCompat("first_tf_dark_tower", "compat:first_structure:twilightforest:dark_tower", "twilightforest", "tower"),
            firstCompat("first_tf_yeti_cave", "compat:first_structure:twilightforest:yeti_cave", "twilightforest", "snow"),
            firstCompat("first_tf_aurora_palace", "compat:first_structure:twilightforest:aurora_palace", "twilightforest", "snow"),
            firstCompat("first_tf_troll_cave", "compat:first_structure:twilightforest:troll_cave", "twilightforest", "troll"),
            firstCompat("first_tf_giant_house", "compat:first_structure:twilightforest:giant_house", "twilightforest", "giant"),
            firstCompat("first_tf_final_castle", "compat:first_structure:twilightforest:final_castle", "twilightforest", "castle"),
            firstCompat("first_tf_quest_grove", "compat:first_structure:twilightforest:quest_grove", "twilightforest", "quest"),
            firstCompat("first_tf_small_hollow_hill", "compat:first_structure:twilightforest:small_hollow_hill", "twilightforest", "cave"),
            firstCompat("first_tf_medium_hollow_hill", "compat:first_structure:twilightforest:medium_hollow_hill", "twilightforest", "cave"),
            firstCompat("first_tf_large_hollow_hill", "compat:first_structure:twilightforest:large_hollow_hill", "twilightforest", "cave"),
            firstCompat("first_tf_ironwood", "compat:first_block:twilightforest:ironwood_block", "twilightforest", "mining"),
            firstCompat("first_tf_steeleaf", "compat:first_block:twilightforest:steeleaf_block", "twilightforest", "mining"),
            firstCompat("first_tf_knightmetal", "compat:first_block:twilightforest:knightmetal_block", "twilightforest", "mining"),
            firstCompat("first_tf_fiery", "compat:first_block:twilightforest:fiery_block", "twilightforest", "mining"),
            firstCompat("first_tf_carminite", "compat:first_block:twilightforest:carminite_block", "twilightforest", "mining"),
            firstCompat("first_tf_naga", "compat:first_boss:twilightforest:naga", "twilightforest", "boss"),
            firstCompat("first_tf_lich", "compat:first_boss:twilightforest:lich", "twilightforest", "boss"),
            firstCompat("first_tf_minoshroom", "compat:first_boss:twilightforest:minoshroom", "twilightforest", "boss"),
            firstCompat("first_tf_hydra", "compat:first_boss:twilightforest:hydra", "twilightforest", "boss"),
            firstCompat("first_tf_knight_phantom", "compat:first_boss:twilightforest:knight_phantom", "twilightforest", "boss"),
            firstCompat("first_tf_ur_ghast", "compat:first_boss:twilightforest:ur_ghast", "twilightforest", "boss"),
            firstCompat("first_tf_alpha_yeti", "compat:first_boss:twilightforest:alpha_yeti", "twilightforest", "boss"),
            firstCompat("first_tf_snow_queen", "compat:first_boss:twilightforest:snow_queen", "twilightforest", "boss"),
            bossCompat("boss_tf_naga", "twilightforest:naga", "twilightforest", "boss"),
            bossCompat("boss_tf_lich", "twilightforest:lich", "twilightforest", "boss"),
            bossCompat("boss_tf_minoshroom", "twilightforest:minoshroom", "twilightforest", "boss"),
            bossCompat("boss_tf_hydra", "twilightforest:hydra", "twilightforest", "boss"),
            bossCompat("boss_tf_knight_phantom", "twilightforest:knight_phantom", "twilightforest", "boss"),
            bossCompat("boss_tf_ur_ghast", "twilightforest:ur_ghast", "twilightforest", "boss"),
            bossCompat("boss_tf_alpha_yeti", "twilightforest:alpha_yeti", "twilightforest", "boss"),
            bossCompat("boss_tf_snow_queen", "twilightforest:snow_queen", "twilightforest", "boss"),
            biomeGroupCompat("general_tf_twilight", "tf_twilight", "twilightforest", "forest"),
            biomeGroupCompat("general_tf_enchanted", "tf_enchanted", "twilightforest", "forest"),
            biomeGroupCompat("general_tf_mushroom", "tf_mushroom", "twilightforest", "mushroom"),
            biomeGroupCompat("general_tf_dark", "tf_dark", "twilightforest", "dark_forest"),
            biomeGroupCompat("general_tf_swamp", "tf_swamp", "twilightforest", "swamp"),
            biomeGroupCompat("general_tf_snow", "tf_snow", "twilightforest", "snow"),
            biomeGroupCompat("general_tf_highlands", "tf_highlands", "twilightforest", "highlands"),
            mobGroupCompat("battle_tf_forest", "tf_forest", "twilightforest", "forest"),
            mobGroupCompat("battle_tf_labyrinth", "tf_labyrinth", "twilightforest", "maze"),
            mobGroupCompat("battle_tf_dark_tower", "tf_dark_tower", "twilightforest", "tower"),
            mobGroupCompat("battle_tf_knights", "tf_knights", "twilightforest", "knight"),
            mobGroupCompat("battle_tf_snow", "tf_snow", "twilightforest", "snow"),
            mobGroupCompat("battle_tf_troll", "tf_troll", "twilightforest", "troll"),
            miningCompat("mining_tf_ironwood", new String[] {"twilightforest:ironwood_block"}, "twilightforest", "mining"),
            miningCompat("mining_tf_steeleaf", new String[] {"twilightforest:steeleaf_block"}, "twilightforest", "mining"),
            miningCompat("mining_tf_knightmetal", new String[] {"twilightforest:knightmetal_block"}, "twilightforest", "mining"),
            miningCompat("mining_tf_fiery", new String[] {"twilightforest:fiery_block"}, "twilightforest", "mining"),
            miningCompat("mining_tf_carminite", new String[] {"twilightforest:carminite_block"}, "twilightforest", "mining"),

            // Fabric-only content compat v2 start
            firstCompat("first_voidz_void", "compat:first_dimension:voidz:void", "voidz", "void_dimension"),
            firstCompat("first_betterend_end_village", "compat:first_structure:betterend:end_village", "betterend", "end_mystic"),
            firstCompat("first_betterend_eternal_portal", "compat:first_structure:betterend:eternal_portal", "betterend", "ritual_structure"),
            firstCompat("first_betterend_giant_ice_star", "compat:first_structure:betterend:giant_ice_star", "betterend", "end_crystal"),
            firstCompat("first_betterend_giant_mossy_glowshroom", "compat:first_structure:betterend:giant_mossy_glowshroom", "betterend", "strange_forest"),
            firstCompat("first_betterend_megalake", "compat:first_structure:betterend:megalake", "betterend", "end_mystic"),
            firstCompat("first_betterend_megalake_small", "compat:first_structure:betterend:megalake_small", "betterend", "end_mystic"),
            firstCompat("first_betterend_mountain", "compat:first_structure:betterend:mountain", "betterend", "end_mystic"),
            firstCompat("first_betterend_painted_mountain", "compat:first_structure:betterend:painted_mountain", "betterend", "end_mystic"),
            firstCompat("first_betternether_altars", "compat:first_structure:betternether:altars", "betternether", "ritual_structure"),
            firstCompat("first_betternether_gardens", "compat:first_structure:betternether:gardens", "betternether", "nether_wild"),
            firstCompat("first_betternether_ghast_hive", "compat:first_structure:betternether:ghast_hive", "betternether", "nether_wild"),
            firstCompat("first_betternether_jungle_temples", "compat:first_structure:betternether:jungle_temples", "betternether", "ritual_structure"),
            firstCompat("first_betternether_nether_city", "compat:first_structure:betternether:nether_city", "betternether", "nether_city"),
            firstCompat("first_betternether_pillars", "compat:first_structure:betternether:pillars", "betternether", "nether_wild"),
            firstCompat("first_betternether_portals", "compat:first_structure:betternether:portals", "betternether", "ritual_structure"),
            firstCompat("first_betternether_pyramid", "compat:first_structure:betternether:pyramid", "betternether", "ritual_structure"),
            firstCompat("first_betternether_respawn_points", "compat:first_structure:betternether:respawn_points", "betternether", "ritual_structure"),
            firstCompat("first_betternether_spawn_altar_ladder", "compat:first_structure:betternether:spawn_altar_ladder", "betternether", "ritual_structure"),
            firstCompat("first_adventurez_chiseled_polished_blackstone_holder", "compat:first_block:adventurez:chiseled_polished_blackstone_holder", "adventurez", "ritual_structure", "mining"),
            miningCompat("mining_adventurez_chiseled_polished_blackstone_holder", new String[] {"adventurez:chiseled_polished_blackstone_holder"}, "adventurez", "ritual_structure", "mining"),
            firstCompat("first_adventurez_piglin_flag", "compat:first_block:adventurez:piglin_flag", "adventurez", "ritual_structure", "mining"),
            miningCompat("mining_adventurez_piglin_flag", new String[] {"adventurez:piglin_flag"}, "adventurez", "ritual_structure", "mining"),
            firstCompat("first_adventurez_shadow_chest", "compat:first_block:adventurez:shadow_chest", "adventurez", "void_shadow", "mining"),
            miningCompat("mining_adventurez_shadow_chest", new String[] {"adventurez:shadow_chest"}, "adventurez", "void_shadow", "mining"),
            firstCompat("first_betterend_amber_ore", "compat:first_block:betterend:amber_ore", "betterend", "ancient_ore", "mining"),
            miningCompat("mining_betterend_amber_ore", new String[] {"betterend:amber_ore"}, "betterend", "ancient_ore", "mining"),
            firstCompat("first_betterend_thallasium_ore", "compat:first_block:betterend:thallasium_ore", "betterend", "ancient_ore", "mining"),
            miningCompat("mining_betterend_thallasium_ore", new String[] {"betterend:thallasium_ore"}, "betterend", "ancient_ore", "mining"),
            firstCompat("first_betterend_ender_ore", "compat:first_block:betterend:ender_ore", "betterend", "ancient_ore", "mining"),
            miningCompat("mining_betterend_ender_ore", new String[] {"betterend:ender_ore"}, "betterend", "ancient_ore", "mining"),
            firstCompat("first_betterend_aurora_crystal", "compat:first_block:betterend:aurora_crystal", "betterend", "end_crystal", "mining"),
            miningCompat("mining_betterend_aurora_crystal", new String[] {"betterend:aurora_crystal"}, "betterend", "end_crystal", "mining"),
            firstCompat("first_betterend_budding_smaragdant_crystal", "compat:first_block:betterend:budding_smaragdant_crystal", "betterend", "end_crystal", "mining"),
            miningCompat("mining_betterend_budding_smaragdant_crystal", new String[] {"betterend:budding_smaragdant_crystal"}, "betterend", "end_crystal", "mining"),
            firstCompat("first_betterend_smaragdant_crystal", "compat:first_block:betterend:smaragdant_crystal", "betterend", "end_crystal", "mining"),
            miningCompat("mining_betterend_smaragdant_crystal", new String[] {"betterend:smaragdant_crystal"}, "betterend", "end_crystal", "mining"),
            firstCompat("first_betterend_aeternium_block", "compat:first_block:betterend:aeternium_block", "betterend", "metal_discovery", "mining"),
            miningCompat("mining_betterend_aeternium_block", new String[] {"betterend:aeternium_block"}, "betterend", "metal_discovery", "mining"),
            firstCompat("first_betterend_terminite_block", "compat:first_block:betterend:terminite_block", "betterend", "metal_discovery", "mining"),
            miningCompat("mining_betterend_terminite_block", new String[] {"betterend:terminite_block"}, "betterend", "metal_discovery", "mining"),
            firstCompat("first_betterend_thallasium_block", "compat:first_block:betterend:thallasium_block", "betterend", "metal_discovery", "mining"),
            miningCompat("mining_betterend_thallasium_block", new String[] {"betterend:thallasium_block"}, "betterend", "metal_discovery", "mining"),
            firstCompat("first_betterend_ender_block", "compat:first_block:betterend:ender_block", "betterend", "metal_discovery", "mining"),
            miningCompat("mining_betterend_ender_block", new String[] {"betterend:ender_block"}, "betterend", "metal_discovery", "mining"),
            firstCompat("first_betternether_cincinnasite_ore", "compat:first_block:betternether:cincinnasite_ore", "betternether", "ancient_ore", "mining"),
            miningCompat("mining_betternether_cincinnasite_ore", new String[] {"betternether:cincinnasite_ore"}, "betternether", "ancient_ore", "mining"),
            firstCompat("first_betternether_nether_ruby_ore", "compat:first_block:betternether:nether_ruby_ore", "betternether", "nether_fire", "mining"),
            miningCompat("mining_betternether_nether_ruby_ore", new String[] {"betternether:nether_ruby_ore"}, "betternether", "nether_fire", "mining"),
            firstCompat("first_betternether_nether_lapis_ore", "compat:first_block:betternether:nether_lapis_ore", "betternether", "nether_wild", "mining"),
            miningCompat("mining_betternether_nether_lapis_ore", new String[] {"betternether:nether_lapis_ore"}, "betternether", "nether_wild", "mining"),
            firstCompat("first_betternether_nether_redstone_ore", "compat:first_block:betternether:nether_redstone_ore", "betternether", "nether_fire", "mining"),
            miningCompat("mining_betternether_nether_redstone_ore", new String[] {"betternether:nether_redstone_ore"}, "betternether", "nether_fire", "mining"),
            firstCompat("first_mythicmetals_adamantite_ore", "compat:first_block:mythicmetals:adamantite_ore", "mythicmetals", "metal_discovery", "mining"),
            miningCompat("mining_mythicmetals_adamantite_ore", new String[] {"mythicmetals:adamantite_ore"}, "mythicmetals", "metal_discovery", "mining"),
            firstCompat("first_mythicmetals_deepslate_adamantite_ore", "compat:first_block:mythicmetals:deepslate_adamantite_ore", "mythicmetals", "metal_discovery", "mining"),
            miningCompat("mining_mythicmetals_deepslate_adamantite_ore", new String[] {"mythicmetals:deepslate_adamantite_ore"}, "mythicmetals", "metal_discovery", "mining"),
            firstCompat("first_mythicmetals_mythril_ore", "compat:first_block:mythicmetals:mythril_ore", "mythicmetals", "metal_discovery", "mining"),
            miningCompat("mining_mythicmetals_mythril_ore", new String[] {"mythicmetals:mythril_ore"}, "mythicmetals", "metal_discovery", "mining"),
            firstCompat("first_mythicmetals_deepslate_mythril_ore", "compat:first_block:mythicmetals:deepslate_mythril_ore", "mythicmetals", "metal_discovery", "mining"),
            miningCompat("mining_mythicmetals_deepslate_mythril_ore", new String[] {"mythicmetals:deepslate_mythril_ore"}, "mythicmetals", "metal_discovery", "mining"),
            firstCompat("first_mythicmetals_orichalcum_ore", "compat:first_block:mythicmetals:orichalcum_ore", "mythicmetals", "metal_discovery", "mining"),
            miningCompat("mining_mythicmetals_orichalcum_ore", new String[] {"mythicmetals:orichalcum_ore"}, "mythicmetals", "metal_discovery", "mining"),
            firstCompat("first_mythicmetals_deepslate_orichalcum_ore", "compat:first_block:mythicmetals:deepslate_orichalcum_ore", "mythicmetals", "metal_discovery", "mining"),
            miningCompat("mining_mythicmetals_deepslate_orichalcum_ore", new String[] {"mythicmetals:deepslate_orichalcum_ore"}, "mythicmetals", "metal_discovery", "mining"),
            firstCompat("first_mythicmetals_prometheum_ore", "compat:first_block:mythicmetals:prometheum_ore", "mythicmetals", "metal_discovery", "mining"),
            miningCompat("mining_mythicmetals_prometheum_ore", new String[] {"mythicmetals:prometheum_ore"}, "mythicmetals", "metal_discovery", "mining"),
            firstCompat("first_mythicmetals_deepslate_prometheum_ore", "compat:first_block:mythicmetals:deepslate_prometheum_ore", "mythicmetals", "metal_discovery", "mining"),
            miningCompat("mining_mythicmetals_deepslate_prometheum_ore", new String[] {"mythicmetals:deepslate_prometheum_ore"}, "mythicmetals", "metal_discovery", "mining"),
            firstCompat("first_mythicmetals_runite_ore", "compat:first_block:mythicmetals:runite_ore", "mythicmetals", "metal_discovery", "mining"),
            miningCompat("mining_mythicmetals_runite_ore", new String[] {"mythicmetals:runite_ore"}, "mythicmetals", "metal_discovery", "mining"),
            firstCompat("first_mythicmetals_deepslate_runite_ore", "compat:first_block:mythicmetals:deepslate_runite_ore", "mythicmetals", "metal_discovery", "mining"),
            miningCompat("mining_mythicmetals_deepslate_runite_ore", new String[] {"mythicmetals:deepslate_runite_ore"}, "mythicmetals", "metal_discovery", "mining"),
            firstCompat("first_mythicmetals_unobtainium_ore", "compat:first_block:mythicmetals:unobtainium_ore", "mythicmetals", "metal_discovery", "mining"),
            miningCompat("mining_mythicmetals_unobtainium_ore", new String[] {"mythicmetals:unobtainium_ore"}, "mythicmetals", "metal_discovery", "mining"),
            firstCompat("first_mythicmetals_deepslate_unobtainium_ore", "compat:first_block:mythicmetals:deepslate_unobtainium_ore", "mythicmetals", "metal_discovery", "mining"),
            miningCompat("mining_mythicmetals_deepslate_unobtainium_ore", new String[] {"mythicmetals:deepslate_unobtainium_ore"}, "mythicmetals", "metal_discovery", "mining"),
            firstCompat("first_voidz_void_stone", "compat:first_block:voidz:void_stone", "voidz", "void_shadow", "mining"),
            miningCompat("mining_voidz_void_stone", new String[] {"voidz:void_stone"}, "voidz", "void_shadow", "mining"),
            firstCompat("first_voidz_infested_void_stone", "compat:first_block:voidz:infested_void_stone", "voidz", "void_shadow", "mining"),
            miningCompat("mining_voidz_infested_void_stone", new String[] {"voidz:infested_void_stone"}, "voidz", "void_shadow", "mining"),
            firstCompat("first_adventurez_blackstone_golem", "compat:first_boss:adventurez:blackstone_golem", "adventurez", "ritual_boss", "boss"),
            bossCompat("boss_adventurez_blackstone_golem", "adventurez:blackstone_golem", "adventurez", "ritual_boss", "boss"),
            firstCompat("first_adventurez_nightmare", "compat:first_boss:adventurez:nightmare", "adventurez", "ritual_boss", "boss"),
            bossCompat("boss_adventurez_nightmare", "adventurez:nightmare", "adventurez", "ritual_boss", "boss"),
            firstCompat("first_adventurez_soul_reaper", "compat:first_boss:adventurez:soul_reaper", "adventurez", "ritual_boss", "boss"),
            bossCompat("boss_adventurez_soul_reaper", "adventurez:soul_reaper", "adventurez", "ritual_boss", "boss"),
            firstCompat("first_adventurez_blaze_guardian", "compat:first_boss:adventurez:blaze_guardian", "adventurez", "deep_battle", "boss"),
            bossCompat("boss_adventurez_blaze_guardian", "adventurez:blaze_guardian", "adventurez", "deep_battle", "boss"),
            firstCompat("first_adventurez_the_eye", "compat:first_boss:adventurez:the_eye", "adventurez", "ritual_boss", "boss"),
            bossCompat("boss_adventurez_the_eye", "adventurez:the_eye", "adventurez", "ritual_boss", "boss"),
            firstCompat("first_adventurez_void_shadow", "compat:first_boss:adventurez:void_shadow", "adventurez", "void_shadow", "boss"),
            bossCompat("boss_adventurez_void_shadow", "adventurez:void_shadow", "adventurez", "void_shadow", "boss"),
            firstCompat("first_adventurez_dragon", "compat:first_boss:adventurez:dragon", "adventurez", "deep_battle", "boss"),
            bossCompat("boss_adventurez_dragon", "adventurez:dragon", "adventurez", "deep_battle", "boss"),
            firstCompat("first_adventurez_amethyst_golem", "compat:first_boss:adventurez:amethyst_golem", "adventurez", "deep_battle", "boss"),
            bossCompat("boss_adventurez_amethyst_golem", "adventurez:amethyst_golem", "adventurez", "deep_battle", "boss"),
            biomeGroupCompat("general_end_crystal", "end_crystal", "betterend", "end_crystal"),
            biomeGroupCompat("general_end_mystic", "end_mystic", "betterend", "end_mystic"),
            biomeGroupCompat("general_end_shadow", "end_shadow", "betterend", "end_shadow"),
            biomeGroupCompat("general_nether_ash", "nether_ash", "betternether", "nether_ash"),
            biomeGroupCompat("general_nether_fire", "nether_fire", "betternether", "nether_fire"),
            biomeGroupCompat("general_nether_soul", "nether_soul", "betternether", "nether_soul"),
            biomeGroupCompat("general_nether_wild", "nether_wild", "betternether", "nether_wild"),
            biomeGroupCompat("general_strange_forest", "strange_forest", "betterend", "strange_forest"),
            mobGroupCompat("battle_deep_battle", "deep_battle", "adventurez", "deep_battle"),
            mobGroupCompat("battle_end_mystic", "end_mystic", "betterend", "end_mystic"),
            mobGroupCompat("battle_end_shadow", "end_shadow", "betterend", "end_shadow"),
            mobGroupCompat("battle_nether_ash", "nether_ash", "betternether", "nether_ash"),
            mobGroupCompat("battle_nether_wild", "nether_wild", "betternether", "nether_wild"),
            mobGroupCompat("battle_ritual_boss", "ritual_boss", "adventurez", "ritual_boss"),
            mobGroupCompat("battle_void_shadow", "void_shadow", "adventurez", "void_shadow"),
            // Cross-loader worldgen content compat start
            firstCompat("first_nova_structures_badlands_miner_outpost", "compat:first_structure:nova_structures:badlands_miner_outpost", "dungeons_and_taverns", "dnt_roadside_camp", "camp_site"),
            firstCompat("first_nova_structures_bunker", "compat:first_structure:nova_structures:bunker", "dungeons_and_taverns", "dnt_roadside_camp", "camp_site"),
            firstCompat("first_nova_structures_conduit_ruin", "compat:first_structure:nova_structures:conduit_ruin", "dungeons_and_taverns", "dnt_sea_ruin", "ocean_ruin"),
            firstCompat("first_nova_structures_creeping_crypt", "compat:first_structure:nova_structures:creeping_crypt", "dungeons_and_taverns", "dnt_crypt", "crypt_dungeon"),
            firstCompat("first_nova_structures_deepslate_camp", "compat:first_structure:nova_structures:deepslate_camp", "dungeons_and_taverns", "dnt_roadside_camp", "camp_site"),
            firstCompat("first_nova_structures_desert_ruins", "compat:first_structure:nova_structures:desert_ruins", "dungeons_and_taverns", "dnt_adventure_ruin", "ancient_ruins"),
            firstCompat("first_nova_structures_end_castle", "compat:first_structure:nova_structures:end_castle", "dungeons_and_taverns", "dnt_end_ruins", "end_ruins"),
            firstCompat("first_nova_structures_end_lighthouse", "compat:first_structure:nova_structures:end_lighthouse", "dungeons_and_taverns", "dnt_end_ruins", "end_ruins"),
            firstCompat("first_nova_structures_end_ship", "compat:first_structure:nova_structures:end_ship", "dungeons_and_taverns", "dnt_end_ruins", "end_ruins"),
            firstCompat("first_nova_structures_firewatch_tower_birch", "compat:first_structure:nova_structures:firewatch_tower_birch", "dungeons_and_taverns", "dnt_firewatch_tower", "frontier_tower"),
            firstCompat("first_nova_structures_firewatch_tower_cherry", "compat:first_structure:nova_structures:firewatch_tower_cherry", "dungeons_and_taverns", "dnt_firewatch_tower", "frontier_tower"),
            firstCompat("first_nova_structures_firewatch_tower_dark_oak", "compat:first_structure:nova_structures:firewatch_tower_dark_oak", "dungeons_and_taverns", "dnt_firewatch_tower", "frontier_tower"),
            firstCompat("first_nova_structures_firewatch_tower_forest", "compat:first_structure:nova_structures:firewatch_tower_forest", "dungeons_and_taverns", "dnt_firewatch_tower", "frontier_tower"),
            firstCompat("first_nova_structures_firewatch_tower_jungle", "compat:first_structure:nova_structures:firewatch_tower_jungle", "dungeons_and_taverns", "dnt_firewatch_tower", "frontier_tower"),
            firstCompat("first_nova_structures_firewatch_tower_mangrove", "compat:first_structure:nova_structures:firewatch_tower_mangrove", "dungeons_and_taverns", "dnt_firewatch_tower", "frontier_tower"),
            firstCompat("first_nova_structures_firewatch_tower_savanna", "compat:first_structure:nova_structures:firewatch_tower_savanna", "dungeons_and_taverns", "dnt_firewatch_tower", "frontier_tower"),
            firstCompat("first_nova_structures_firewatch_tower_swamp", "compat:first_structure:nova_structures:firewatch_tower_swamp", "dungeons_and_taverns", "dnt_firewatch_tower", "frontier_tower"),
            firstCompat("first_nova_structures_firewatch_tower_taiga", "compat:first_structure:nova_structures:firewatch_tower_taiga", "dungeons_and_taverns", "dnt_firewatch_tower", "frontier_tower"),
            firstCompat("first_nova_structures_hamlet", "compat:first_structure:nova_structures:hamlet", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_illager_camp", "compat:first_structure:nova_structures:illager_camp", "dungeons_and_taverns", "dnt_illager_site", "illager_raid"),
            firstCompat("first_nova_structures_illager_hideout", "compat:first_structure:nova_structures:illager_hideout", "dungeons_and_taverns", "dnt_illager_site", "illager_raid"),
            firstCompat("first_nova_structures_illager_manor", "compat:first_structure:nova_structures:illager_manor", "dungeons_and_taverns", "dnt_illager_site", "illager_raid"),
            firstCompat("first_nova_structures_jungle_ruins", "compat:first_structure:nova_structures:jungle_ruins", "dungeons_and_taverns", "dnt_adventure_ruin", "ancient_ruins"),
            firstCompat("first_nova_structures_lone_citadel", "compat:first_structure:nova_structures:lone_citadel", "dungeons_and_taverns", "dnt_adventure_ruin", "ancient_ruins"),
            firstCompat("first_nova_structures_mangrove_witch_hut", "compat:first_structure:nova_structures:mangrove_witch_hut", "dungeons_and_taverns", "dnt_adventure_ruin", "ancient_ruins"),
            firstCompat("first_nova_structures_nether_keep", "compat:first_structure:nova_structures:nether_keep", "dungeons_and_taverns", "dnt_nether_site", "nether_outpost"),
            firstCompat("first_nova_structures_nether_port", "compat:first_structure:nova_structures:nether_port", "dungeons_and_taverns", "dnt_nether_site", "nether_outpost"),
            firstCompat("first_nova_structures_nether_skeleton_tower_crimson", "compat:first_structure:nova_structures:nether_skeleton_tower_crimson", "dungeons_and_taverns", "dnt_nether_site", "nether_outpost"),
            firstCompat("first_nova_structures_nether_skeleton_tower_soul", "compat:first_structure:nova_structures:nether_skeleton_tower_soul", "dungeons_and_taverns", "dnt_nether_site", "nether_outpost"),
            firstCompat("first_nova_structures_nether_skeleton_tower_warped", "compat:first_structure:nova_structures:nether_skeleton_tower_warped", "dungeons_and_taverns", "dnt_nether_site", "nether_outpost"),
            firstCompat("first_nova_structures_nether_skeleton_tower_waste", "compat:first_structure:nova_structures:nether_skeleton_tower_waste", "dungeons_and_taverns", "dnt_nether_site", "nether_outpost"),
            firstCompat("first_nova_structures_piglin_camp", "compat:first_structure:nova_structures:piglin_camp", "dungeons_and_taverns", "dnt_nether_site", "nether_outpost"),
            firstCompat("first_nova_structures_piglin_donjon", "compat:first_structure:nova_structures:piglin_donjon", "dungeons_and_taverns", "dnt_nether_site", "nether_outpost"),
            firstCompat("first_nova_structures_piglin_outstation", "compat:first_structure:nova_structures:piglin_outstation", "dungeons_and_taverns", "dnt_nether_site", "nether_outpost"),
            firstCompat("first_nova_structures_remnant_bee_keeper", "compat:first_structure:nova_structures:remnant_bee_keeper", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_remnant_big_remnant", "compat:first_structure:nova_structures:remnant_big_remnant", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_remnant_big_remnant_2", "compat:first_structure:nova_structures:remnant_big_remnant_2", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_remnant_big_remnant_3", "compat:first_structure:nova_structures:remnant_big_remnant_3", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_remnant_birch_graveyard", "compat:first_structure:nova_structures:remnant_birch_graveyard", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_remnant_bridge_remnant", "compat:first_structure:nova_structures:remnant_bridge_remnant", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_remnant_bunny_base", "compat:first_structure:nova_structures:remnant_bunny_base", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_remnant_classic_village", "compat:first_structure:nova_structures:remnant_classic_village", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_remnant_desert_remnant", "compat:first_structure:nova_structures:remnant_desert_remnant", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_remnant_forest_smith", "compat:first_structure:nova_structures:remnant_forest_smith", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_remnant_frog_ranch", "compat:first_structure:nova_structures:remnant_frog_ranch", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_remnant_graveyard", "compat:first_structure:nova_structures:remnant_graveyard", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_remnant_medium_remnant", "compat:first_structure:nova_structures:remnant_medium_remnant", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_remnant_medium_remnant_2", "compat:first_structure:nova_structures:remnant_medium_remnant_2", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_remnant_miner_hut", "compat:first_structure:nova_structures:remnant_miner_hut", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_remnant_mud_brick_constructor", "compat:first_structure:nova_structures:remnant_mud_brick_constructor", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_remnant_ominous_shop", "compat:first_structure:nova_structures:remnant_ominous_shop", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_remnant_ruin_farmer", "compat:first_structure:nova_structures:remnant_ruin_farmer", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_remnant_ruin_smith", "compat:first_structure:nova_structures:remnant_ruin_smith", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_remnant_sawmill", "compat:first_structure:nova_structures:remnant_sawmill", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_remnant_school_remnant", "compat:first_structure:nova_structures:remnant_school_remnant", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_remnant_taiga_castle", "compat:first_structure:nova_structures:remnant_taiga_castle", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_remnant_woodland_hud", "compat:first_structure:nova_structures:remnant_woodland_hud", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_remnant_zombie_horse_ranch", "compat:first_structure:nova_structures:remnant_zombie_horse_ranch", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_ruin_town", "compat:first_structure:nova_structures:ruin_town", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_shrine_combat_tier_1", "compat:first_structure:nova_structures:shrine_combat_tier_1", "dungeons_and_taverns", "dnt_trial_shrine", "trial_shrine"),
            firstCompat("first_nova_structures_shrine_combat_tier_2", "compat:first_structure:nova_structures:shrine_combat_tier_2", "dungeons_and_taverns", "dnt_trial_shrine", "trial_shrine"),
            firstCompat("first_nova_structures_shrine_combat_tier_3", "compat:first_structure:nova_structures:shrine_combat_tier_3", "dungeons_and_taverns", "dnt_trial_shrine", "trial_shrine"),
            firstCompat("first_nova_structures_shrine_combat_tier_4", "compat:first_structure:nova_structures:shrine_combat_tier_4", "dungeons_and_taverns", "dnt_trial_shrine", "trial_shrine"),
            firstCompat("first_nova_structures_shrine_combat_tier_5", "compat:first_structure:nova_structures:shrine_combat_tier_5", "dungeons_and_taverns", "dnt_trial_shrine", "trial_shrine"),
            firstCompat("first_nova_structures_shrine_tower", "compat:first_structure:nova_structures:shrine_tower", "dungeons_and_taverns", "dnt_trial_shrine", "trial_shrine"),
            firstCompat("first_nova_structures_skeleton_camp_crimson", "compat:first_structure:nova_structures:skeleton_camp_crimson", "dungeons_and_taverns", "dnt_nether_site", "nether_outpost"),
            firstCompat("first_nova_structures_skeleton_camp_soul", "compat:first_structure:nova_structures:skeleton_camp_soul", "dungeons_and_taverns", "dnt_nether_site", "nether_outpost"),
            firstCompat("first_nova_structures_skeleton_camp_warped", "compat:first_structure:nova_structures:skeleton_camp_warped", "dungeons_and_taverns", "dnt_nether_site", "nether_outpost"),
            firstCompat("first_nova_structures_skeleton_camp_waste", "compat:first_structure:nova_structures:skeleton_camp_waste", "dungeons_and_taverns", "dnt_nether_site", "nether_outpost"),
            firstCompat("first_nova_structures_stray_fort", "compat:first_structure:nova_structures:stray_fort", "dungeons_and_taverns", "dnt_adventure_ruin", "ancient_ruins"),
            firstCompat("first_nova_structures_tavern_acacia", "compat:first_structure:nova_structures:tavern_acacia", "dungeons_and_taverns", "dnt_tavern", "tavern_roadside"),
            firstCompat("first_nova_structures_tavern_birch", "compat:first_structure:nova_structures:tavern_birch", "dungeons_and_taverns", "dnt_tavern", "tavern_roadside"),
            firstCompat("first_nova_structures_tavern_cherry", "compat:first_structure:nova_structures:tavern_cherry", "dungeons_and_taverns", "dnt_tavern", "tavern_roadside"),
            firstCompat("first_nova_structures_tavern_dark_oak", "compat:first_structure:nova_structures:tavern_dark_oak", "dungeons_and_taverns", "dnt_tavern", "tavern_roadside"),
            firstCompat("first_nova_structures_tavern_desert", "compat:first_structure:nova_structures:tavern_desert", "dungeons_and_taverns", "dnt_tavern", "tavern_roadside"),
            firstCompat("first_nova_structures_tavern_jungle", "compat:first_structure:nova_structures:tavern_jungle", "dungeons_and_taverns", "dnt_tavern", "tavern_roadside"),
            firstCompat("first_nova_structures_tavern_mangrove", "compat:first_structure:nova_structures:tavern_mangrove", "dungeons_and_taverns", "dnt_tavern", "tavern_roadside"),
            firstCompat("first_nova_structures_tavern_oak", "compat:first_structure:nova_structures:tavern_oak", "dungeons_and_taverns", "dnt_tavern", "tavern_roadside"),
            firstCompat("first_nova_structures_tavern_snowy", "compat:first_structure:nova_structures:tavern_snowy", "dungeons_and_taverns", "dnt_tavern", "tavern_roadside"),
            firstCompat("first_nova_structures_tavern_spruce", "compat:first_structure:nova_structures:tavern_spruce", "dungeons_and_taverns", "dnt_tavern", "tavern_roadside"),
            firstCompat("first_nova_structures_tavern_swamp", "compat:first_structure:nova_structures:tavern_swamp", "dungeons_and_taverns", "dnt_tavern", "tavern_roadside"),
            firstCompat("first_nova_structures_toxic_lair", "compat:first_structure:nova_structures:toxic_lair", "dungeons_and_taverns", "dnt_crypt", "crypt_dungeon"),
            firstCompat("first_nova_structures_trident_trial_monument", "compat:first_structure:nova_structures:trident_trial_monument", "dungeons_and_taverns", "dnt_trial_shrine", "trial_shrine"),
            firstCompat("first_nova_structures_undead_crypt", "compat:first_structure:nova_structures:undead_crypt", "dungeons_and_taverns", "dnt_crypt", "crypt_dungeon"),
            firstCompat("first_nova_structures_underground_house", "compat:first_structure:nova_structures:underground_house", "dungeons_and_taverns", "dnt_adventure_ruin", "ancient_ruins"),
            firstCompat("first_nova_structures_village_birch", "compat:first_structure:nova_structures:village_birch", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_village_jungle", "compat:first_structure:nova_structures:village_jungle", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_village_swamp", "compat:first_structure:nova_structures:village_swamp", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_well_birch", "compat:first_structure:nova_structures:well_birch", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_well_dark_oak", "compat:first_structure:nova_structures:well_dark_oak", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_well_jungle", "compat:first_structure:nova_structures:well_jungle", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_well_oak", "compat:first_structure:nova_structures:well_oak", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_well_savana", "compat:first_structure:nova_structures:well_savana", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_well_spruce", "compat:first_structure:nova_structures:well_spruce", "dungeons_and_taverns", "dnt_settlement_ruin", "settlement_ruin"),
            firstCompat("first_nova_structures_wild_ruin", "compat:first_structure:nova_structures:wild_ruin", "dungeons_and_taverns", "dnt_adventure_ruin", "ancient_ruins"),
            firstCompat("first_nova_structures_witch_villa", "compat:first_structure:nova_structures:witch_villa", "dungeons_and_taverns", "dnt_adventure_ruin", "ancient_ruins"),
            firstCompat("first_explorify_badlands_pyramid", "compat:first_structure:explorify:badlands_pyramid", "explorify", "explorify_old_ruin", "ancient_ruins"),
            firstCompat("first_explorify_black_spiral", "compat:first_structure:explorify:black_spiral", "explorify", "explorify_old_ruin", "ancient_ruins"),
            firstCompat("first_explorify_campsite", "compat:first_structure:explorify:campsite", "explorify", "explorify_roadside_site", "roadside_stop"),
            firstCompat("first_explorify_dark_forest_settlement", "compat:first_structure:explorify:dark_forest_settlement", "explorify", "explorify_small_settlement", "small_settlement"),
            firstCompat("first_explorify_desert_shrine", "compat:first_structure:explorify:desert_shrine", "explorify", "explorify_old_ruin", "ancient_ruins"),
            firstCompat("first_explorify_end_shipwreck", "compat:first_structure:explorify:end_shipwreck", "explorify", "explorify_shipwreck", "end_wreck"),
            firstCompat("first_explorify_farmstead", "compat:first_structure:explorify:farmstead", "explorify", "explorify_roadside_site", "roadside_stop"),
            firstCompat("first_explorify_guide_post_cold", "compat:first_structure:explorify:guide_post_cold", "explorify", "explorify_waypoint", "waypoint_tower"),
            firstCompat("first_explorify_guide_post_warm", "compat:first_structure:explorify:guide_post_warm", "explorify", "explorify_waypoint", "waypoint_tower"),
            firstCompat("first_explorify_mangrove_hut", "compat:first_structure:explorify:mangrove_hut", "explorify", "explorify_small_settlement", "small_settlement"),
            firstCompat("first_explorify_mausoleum", "compat:first_structure:explorify:mausoleum", "explorify", "explorify_old_ruin", "ancient_ruins"),
            firstCompat("first_explorify_ruins", "compat:first_structure:explorify:ruins", "explorify", "explorify_ruins", "ancient_ruins"),
            firstCompat("first_explorify_supply_cache_birch", "compat:first_structure:explorify:supply_cache/birch", "explorify", "explorify_supply_cache", "supply_cache"),
            firstCompat("first_explorify_supply_cache_dark", "compat:first_structure:explorify:supply_cache/dark", "explorify", "explorify_supply_cache", "supply_cache"),
            firstCompat("first_explorify_supply_cache_desert", "compat:first_structure:explorify:supply_cache/desert", "explorify", "explorify_supply_cache", "supply_cache"),
            firstCompat("first_explorify_supply_cache_forest", "compat:first_structure:explorify:supply_cache/forest", "explorify", "explorify_supply_cache", "supply_cache"),
            firstCompat("first_explorify_supply_cache_jungle", "compat:first_structure:explorify:supply_cache/jungle", "explorify", "explorify_supply_cache", "supply_cache"),
            firstCompat("first_explorify_supply_cache_mangrove", "compat:first_structure:explorify:supply_cache/mangrove", "explorify", "explorify_supply_cache", "supply_cache"),
            firstCompat("first_explorify_supply_cache_taiga", "compat:first_structure:explorify:supply_cache/taiga", "explorify", "explorify_supply_cache", "supply_cache"),
            firstCompat("first_explorify_tavern", "compat:first_structure:explorify:tavern", "explorify", "explorify_roadside_site", "roadside_stop"),
            firstCompat("first_explorify_watchtower_plains", "compat:first_structure:explorify:watchtower/plains", "explorify", "explorify_waypoint", "waypoint_tower"),
            firstCompat("first_explorify_watchtower_savanna", "compat:first_structure:explorify:watchtower/savanna", "explorify", "explorify_waypoint", "waypoint_tower"),
            firstCompat("first_explorify_watchtower_taiga", "compat:first_structure:explorify:watchtower/taiga", "explorify", "explorify_waypoint", "waypoint_tower"),
            firstCompat("first_illagerinvasion_firecaller_hut", "compat:first_structure:illagerinvasion:firecaller_hut", "illagerinvasion", "illager_invasion_hut", "illager_hut"),
            firstCompat("first_illagerinvasion_illager_fort", "compat:first_structure:illagerinvasion:illager_fort", "illagerinvasion", "illager_invasion_stronghold", "illager_stronghold"),
            firstCompat("first_illagerinvasion_illusioner_tower", "compat:first_structure:illagerinvasion:illusioner_tower", "illagerinvasion", "illager_invasion_stronghold", "illager_stronghold"),
            firstCompat("first_illagerinvasion_labyrinth", "compat:first_structure:illagerinvasion:labyrinth", "illagerinvasion", "illager_invasion_stronghold", "illager_stronghold"),
            firstCompat("first_illagerinvasion_sorcerer_hut", "compat:first_structure:illagerinvasion:sorcerer_hut", "illagerinvasion", "illager_invasion_hut", "illager_hut"),
            firstCompat("first_incendium_abandoned_tower", "compat:first_structure:incendium:abandoned_tower", "incendium", "incendium_abandoned_tower", "nether_ruin"),
            firstCompat("first_incendium_forbidden_castle", "compat:first_structure:incendium:forbidden_castle", "incendium", "incendium_forbidden_castle", "nether_fortress"),
            firstCompat("first_incendium_infernal_altar", "compat:first_structure:incendium:infernal_altar", "incendium", "incendium_infernal_altar", "nether_ritual"),
            firstCompat("first_incendium_nether_reactor", "compat:first_structure:incendium:nether_reactor", "incendium", "incendium_nether_reactor", "nether_ritual"),
            firstCompat("first_incendium_piglin_village", "compat:first_structure:incendium:piglin_village", "incendium", "incendium_piglin_village", "nether_city"),
            firstCompat("first_incendium_pipeline", "compat:first_structure:incendium:pipeline", "incendium", "incendium_pipeline", "nether_ruin"),
            firstCompat("first_incendium_quartz_kitchen", "compat:first_structure:incendium:quartz_kitchen", "incendium", "incendium_quartz_kitchen", "nether_ruin"),
            firstCompat("first_incendium_ruined_lab", "compat:first_structure:incendium:ruined_lab", "incendium", "incendium_ruined_lab", "nether_ruin"),
            firstCompat("first_incendium_sanctum", "compat:first_structure:incendium:sanctum", "incendium", "incendium_sanctum", "nether_ritual"),
            firstCompat("first_terralith_desert_outpost", "compat:first_structure:terralith:desert_outpost", "terralith", "terralith_desert_outpost", "desert_outpost"),
            firstCompat("first_terralith_fortified_desert_village", "compat:first_structure:terralith:fortified_desert_village", "terralith", "terralith_fortified_village", "fortified_settlement"),
            firstCompat("first_terralith_fortified_village", "compat:first_structure:terralith:fortified_village", "terralith", "terralith_fortified_village", "fortified_settlement"),
            firstCompat("first_terralith_glacial_hut", "compat:first_structure:terralith:glacial_hut", "terralith", "terralith_glacial_shelter", "glacial_shelter"),
            firstCompat("first_terralith_igloo", "compat:first_structure:terralith:igloo", "terralith", "terralith_glacial_shelter", "glacial_shelter"),
            firstCompat("first_terralith_mage_complex", "compat:first_structure:terralith:mage_complex", "terralith", "terralith_mage_complex", "mage_ruin"),
            firstCompat("first_terralith_mage_tower", "compat:first_structure:terralith:mage_tower", "terralith", "terralith_mage_tower", "mage_tower"),
            firstCompat("first_terralith_mage_tower_autumn", "compat:first_structure:terralith:mage_tower_autumn", "terralith", "terralith_mage_tower", "mage_tower"),
            firstCompat("first_terralith_mage_tower_spring", "compat:first_structure:terralith:mage_tower_spring", "terralith", "terralith_mage_tower", "mage_tower"),
            firstCompat("first_terralith_mage_tower_summer", "compat:first_structure:terralith:mage_tower_summer", "terralith", "terralith_mage_tower", "mage_tower"),
            firstCompat("first_terralith_mage_tower_winter", "compat:first_structure:terralith:mage_tower_winter", "terralith", "terralith_mage_tower", "mage_tower"),
            firstCompat("first_terralith_rubble_desert", "compat:first_structure:terralith:rubble_desert", "terralith", "terralith_old_rubble", "ancient_ruins"),
            firstCompat("first_terralith_rubble_forest", "compat:first_structure:terralith:rubble_forest", "terralith", "terralith_old_rubble", "ancient_ruins"),
            firstCompat("first_terralith_rubble_jungle", "compat:first_structure:terralith:rubble_jungle", "terralith", "terralith_old_rubble", "ancient_ruins"),
            firstCompat("first_terralith_rubble_mesa", "compat:first_structure:terralith:rubble_mesa", "terralith", "terralith_old_rubble", "ancient_ruins"),
            firstCompat("first_terralith_rubble_mountain", "compat:first_structure:terralith:rubble_mountain", "terralith", "terralith_old_rubble", "ancient_ruins"),
            firstCompat("first_terralith_rubble_taiga", "compat:first_structure:terralith:rubble_taiga", "terralith", "terralith_old_rubble", "ancient_ruins"),
            firstCompat("first_terralith_spire", "compat:first_structure:terralith:spire", "terralith", "terralith_spire", "stone_spire"),
            firstCompat("first_terralith_underground_frosted_dungeon", "compat:first_structure:terralith:underground/frosted_dungeon", "terralith", "terralith_frosted_dungeon", "frost_dungeon"),
            firstCompat("first_terralith_underground_giant_bee_hive", "compat:first_structure:terralith:underground/giant_bee_hive", "terralith", "terralith_giant_hive", "hive_cavern"),
            firstCompat("first_terralith_underground_mining_outpost", "compat:first_structure:terralith:underground/mining_outpost", "terralith", "terralith_mining_outpost", "mining_outpost"),
            firstCompat("first_terralith_underground_oak_cabin", "compat:first_structure:terralith:underground/oak_cabin", "terralith", "terralith_valley_lodge", "valley_lodge"),
            firstCompat("first_terralith_underground_old_refinery", "compat:first_structure:terralith:underground/old_refinery", "terralith", "terralith_old_refinery", "industrial_ruin"),
            firstCompat("first_terralith_underground_sunken_tower", "compat:first_structure:terralith:underground/sunken_tower", "terralith", "terralith_sunken_tower", "sunken_ruin"),
            firstCompat("first_terralith_underground_witch_hut", "compat:first_structure:terralith:underground/witch_hut", "terralith", "terralith_witch_hut", "witch_site"),
            firstCompat("first_terralith_underground_cabin", "compat:first_structure:terralith:underground_cabin", "terralith", "terralith_valley_lodge", "valley_lodge"),
            firstCompat("first_terralith_valley_lodge", "compat:first_structure:terralith:valley_lodge", "terralith", "terralith_valley_lodge", "valley_lodge"),
            firstCompat("first_terralith_witch_hut", "compat:first_structure:terralith:witch_hut", "terralith", "terralith_witch_hut", "witch_site"),
            firstCompat("first_towns_and_towers_exclusives_pillager_outpost_classic", "compat:first_structure:towns_and_towers:exclusives/pillager_outpost_classic", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_exclusives_pillager_outpost_iberian", "compat:first_structure:towns_and_towers:exclusives/pillager_outpost_iberian", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_exclusives_pillager_outpost_mediterranean", "compat:first_structure:towns_and_towers:exclusives/pillager_outpost_mediterranean", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_exclusives_pillager_outpost_nilotic", "compat:first_structure:towns_and_towers:exclusives/pillager_outpost_nilotic", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_exclusives_pillager_outpost_oriental", "compat:first_structure:towns_and_towers:exclusives/pillager_outpost_oriental", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_exclusives_pillager_outpost_rustic", "compat:first_structure:towns_and_towers:exclusives/pillager_outpost_rustic", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_exclusives_pillager_outpost_swedish", "compat:first_structure:towns_and_towers:exclusives/pillager_outpost_swedish", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_exclusives_pillager_outpost_tudor", "compat:first_structure:towns_and_towers:exclusives/pillager_outpost_tudor", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_exclusives_village_classic", "compat:first_structure:towns_and_towers:exclusives/village_classic", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_exclusives_village_iberian", "compat:first_structure:towns_and_towers:exclusives/village_iberian", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_exclusives_village_mediterranean", "compat:first_structure:towns_and_towers:exclusives/village_mediterranean", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_exclusives_village_nilotic", "compat:first_structure:towns_and_towers:exclusives/village_nilotic", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_exclusives_village_piglin", "compat:first_structure:towns_and_towers:exclusives/village_piglin", "towns_and_towers", "towns_towers_piglin_village", "nether_settlement"),
            firstCompat("first_towns_and_towers_exclusives_village_rustic", "compat:first_structure:towns_and_towers:exclusives/village_rustic", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_exclusives_village_swedish", "compat:first_structure:towns_and_towers:exclusives/village_swedish", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_exclusives_village_tudor", "compat:first_structure:towns_and_towers:exclusives/village_tudor", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_exclusives_village_wandering_trader_camp", "compat:first_structure:towns_and_towers:exclusives/village_wandering_trader_camp", "towns_and_towers", "towns_towers_trader_camp", "trader_camp"),
            firstCompat("first_towns_and_towers_mimic_desert", "compat:first_structure:towns_and_towers:mimic_desert", "towns_and_towers", "towns_towers_desert_mimic", "desert_trap"),
            firstCompat("first_towns_and_towers_pillager_outpost_badlands", "compat:first_structure:towns_and_towers:pillager_outpost_badlands", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_pillager_outpost_beach", "compat:first_structure:towns_and_towers:pillager_outpost_beach", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_pillager_outpost_birch_forest", "compat:first_structure:towns_and_towers:pillager_outpost_birch_forest", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_pillager_outpost_desert", "compat:first_structure:towns_and_towers:pillager_outpost_desert", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_pillager_outpost_flower_forest", "compat:first_structure:towns_and_towers:pillager_outpost_flower_forest", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_pillager_outpost_forest", "compat:first_structure:towns_and_towers:pillager_outpost_forest", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_pillager_outpost_grove", "compat:first_structure:towns_and_towers:pillager_outpost_grove", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_pillager_outpost_jungle", "compat:first_structure:towns_and_towers:pillager_outpost_jungle", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_pillager_outpost_meadow", "compat:first_structure:towns_and_towers:pillager_outpost_meadow", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_pillager_outpost_mushroom_fields", "compat:first_structure:towns_and_towers:pillager_outpost_mushroom_fields", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_pillager_outpost_ocean", "compat:first_structure:towns_and_towers:pillager_outpost_ocean", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_pillager_outpost_old_growth_taiga", "compat:first_structure:towns_and_towers:pillager_outpost_old_growth_taiga", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_pillager_outpost_savanna", "compat:first_structure:towns_and_towers:pillager_outpost_savanna", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_pillager_outpost_savanna_plateau", "compat:first_structure:towns_and_towers:pillager_outpost_savanna_plateau", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_pillager_outpost_snowy_beach", "compat:first_structure:towns_and_towers:pillager_outpost_snowy_beach", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_pillager_outpost_snowy_plains", "compat:first_structure:towns_and_towers:pillager_outpost_snowy_plains", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_pillager_outpost_snowy_slopes", "compat:first_structure:towns_and_towers:pillager_outpost_snowy_slopes", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_pillager_outpost_snowy_taiga", "compat:first_structure:towns_and_towers:pillager_outpost_snowy_taiga", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_pillager_outpost_sparse_jungle", "compat:first_structure:towns_and_towers:pillager_outpost_sparse_jungle", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_pillager_outpost_sunflower_plains", "compat:first_structure:towns_and_towers:pillager_outpost_sunflower_plains", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_pillager_outpost_swamp", "compat:first_structure:towns_and_towers:pillager_outpost_swamp", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_pillager_outpost_taiga", "compat:first_structure:towns_and_towers:pillager_outpost_taiga", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_pillager_outpost_wooded_badlands", "compat:first_structure:towns_and_towers:pillager_outpost_wooded_badlands", "towns_and_towers", "towns_towers_outpost", "illager_outpost"),
            firstCompat("first_towns_and_towers_village_badlands", "compat:first_structure:towns_and_towers:village_badlands", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_village_beach", "compat:first_structure:towns_and_towers:village_beach", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_village_birch_forest", "compat:first_structure:towns_and_towers:village_birch_forest", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_village_flower_forest", "compat:first_structure:towns_and_towers:village_flower_forest", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_village_forest", "compat:first_structure:towns_and_towers:village_forest", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_village_grove", "compat:first_structure:towns_and_towers:village_grove", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_village_jungle", "compat:first_structure:towns_and_towers:village_jungle", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_village_meadow", "compat:first_structure:towns_and_towers:village_meadow", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_village_mushroom_fields", "compat:first_structure:towns_and_towers:village_mushroom_fields", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_village_ocean", "compat:first_structure:towns_and_towers:village_ocean", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_village_old_growth_taiga", "compat:first_structure:towns_and_towers:village_old_growth_taiga", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_village_savanna_plateau", "compat:first_structure:towns_and_towers:village_savanna_plateau", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_village_snowy_slopes", "compat:first_structure:towns_and_towers:village_snowy_slopes", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_village_snowy_taiga", "compat:first_structure:towns_and_towers:village_snowy_taiga", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_village_sparse_jungle", "compat:first_structure:towns_and_towers:village_sparse_jungle", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_village_sunflower_plains", "compat:first_structure:towns_and_towers:village_sunflower_plains", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_village_swamp", "compat:first_structure:towns_and_towers:village_swamp", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_village_wooded_badlands", "compat:first_structure:towns_and_towers:village_wooded_badlands", "towns_and_towers", "towns_towers_village", "frontier_village"),
            firstCompat("first_towns_and_towers_wreckage_ocean", "compat:first_structure:towns_and_towers:wreckage_ocean", "towns_and_towers", "towns_towers_wreckage", "ocean_wreck"),
            firstCompat("first_dungeons_arise_abandoned_temple", "compat:first_structure:dungeons_arise:abandoned_temple", "when_dungeons_arise", "wda_old_sanctuary", "old_sanctuary"),
            firstCompat("first_dungeons_arise_aviary", "compat:first_structure:dungeons_arise:aviary", "when_dungeons_arise", "wda_large_dungeon", "great_dungeon"),
            firstCompat("first_dungeons_arise_bandit_towers", "compat:first_structure:dungeons_arise:bandit_towers", "when_dungeons_arise", "wda_large_dungeon", "great_dungeon"),
            firstCompat("first_dungeons_arise_bandit_village", "compat:first_structure:dungeons_arise:bandit_village", "when_dungeons_arise", "wda_roadside_site", "roadside_refuge"),
            firstCompat("first_dungeons_arise_bathhouse", "compat:first_structure:dungeons_arise:bathhouse", "when_dungeons_arise", "wda_roadside_site", "roadside_refuge"),
            firstCompat("first_dungeons_arise_ceryneian_hind", "compat:first_structure:dungeons_arise:ceryneian_hind", "when_dungeons_arise", "wda_legendary_keep", "legendary_keep"),
            firstCompat("first_dungeons_arise_coliseum", "compat:first_structure:dungeons_arise:coliseum", "when_dungeons_arise", "wda_coliseum", "arena_dungeon"),
            firstCompat("first_dungeons_arise_fishing_hut", "compat:first_structure:dungeons_arise:fishing_hut", "when_dungeons_arise", "wda_roadside_site", "roadside_refuge"),
            firstCompat("first_dungeons_arise_foundry", "compat:first_structure:dungeons_arise:foundry", "when_dungeons_arise", "wda_industrial_ruin", "industrial_dungeon"),
            firstCompat("first_dungeons_arise_giant_mushroom", "compat:first_structure:dungeons_arise:giant_mushroom", "when_dungeons_arise", "wda_large_dungeon", "great_dungeon"),
            firstCompat("first_dungeons_arise_greenwood_pub", "compat:first_structure:dungeons_arise:greenwood_pub", "when_dungeons_arise", "wda_roadside_site", "roadside_refuge"),
            firstCompat("first_dungeons_arise_heavenly_challenger", "compat:first_structure:dungeons_arise:heavenly_challenger", "when_dungeons_arise", "wda_heavenly_fortress", "sky_fortress"),
            firstCompat("first_dungeons_arise_heavenly_conqueror", "compat:first_structure:dungeons_arise:heavenly_conqueror", "when_dungeons_arise", "wda_heavenly_fortress", "sky_fortress"),
            firstCompat("first_dungeons_arise_heavenly_rider", "compat:first_structure:dungeons_arise:heavenly_rider", "when_dungeons_arise", "wda_heavenly_fortress", "sky_fortress"),
            firstCompat("first_dungeons_arise_illager_campsite", "compat:first_structure:dungeons_arise:illager_campsite", "when_dungeons_arise", "wda_illager_stronghold", "illager_fortress"),
            firstCompat("first_dungeons_arise_illager_corsair", "compat:first_structure:dungeons_arise:illager_corsair", "when_dungeons_arise", "wda_illager_stronghold", "illager_fortress"),
            firstCompat("first_dungeons_arise_illager_fort", "compat:first_structure:dungeons_arise:illager_fort", "when_dungeons_arise", "wda_illager_stronghold", "illager_fortress"),
            firstCompat("first_dungeons_arise_illager_galley", "compat:first_structure:dungeons_arise:illager_galley", "when_dungeons_arise", "wda_illager_stronghold", "illager_fortress"),
            firstCompat("first_dungeons_arise_illager_windmill", "compat:first_structure:dungeons_arise:illager_windmill", "when_dungeons_arise", "wda_illager_stronghold", "illager_fortress"),
            firstCompat("first_dungeons_arise_infested_temple", "compat:first_structure:dungeons_arise:infested_temple", "when_dungeons_arise", "wda_old_sanctuary", "old_sanctuary"),
            firstCompat("first_dungeons_arise_jungle_tree_house", "compat:first_structure:dungeons_arise:jungle_tree_house", "when_dungeons_arise", "wda_roadside_site", "roadside_refuge"),
            firstCompat("first_dungeons_arise_keep_kayra", "compat:first_structure:dungeons_arise:keep_kayra", "when_dungeons_arise", "wda_legendary_keep", "legendary_keep"),
            firstCompat("first_dungeons_arise_kisegi_sanctuary", "compat:first_structure:dungeons_arise:kisegi_sanctuary", "when_dungeons_arise", "wda_old_sanctuary", "old_sanctuary"),
            firstCompat("first_dungeons_arise_lighthouse", "compat:first_structure:dungeons_arise:lighthouse", "when_dungeons_arise", "wda_roadside_site", "roadside_refuge"),
            firstCompat("first_dungeons_arise_mechanical_nest", "compat:first_structure:dungeons_arise:mechanical_nest", "when_dungeons_arise", "wda_large_dungeon", "great_dungeon"),
            firstCompat("first_dungeons_arise_merchant_campsite", "compat:first_structure:dungeons_arise:merchant_campsite", "when_dungeons_arise", "wda_roadside_site", "roadside_refuge"),
            firstCompat("first_dungeons_arise_mining_complex", "compat:first_structure:dungeons_arise:mining_complex", "when_dungeons_arise", "wda_industrial_ruin", "industrial_dungeon"),
            firstCompat("first_dungeons_arise_mining_system", "compat:first_structure:dungeons_arise:mining_system", "when_dungeons_arise", "wda_industrial_ruin", "industrial_dungeon"),
            firstCompat("first_dungeons_arise_monastery", "compat:first_structure:dungeons_arise:monastery", "when_dungeons_arise", "wda_old_sanctuary", "old_sanctuary"),
            firstCompat("first_dungeons_arise_mushroom_house", "compat:first_structure:dungeons_arise:mushroom_house", "when_dungeons_arise", "wda_roadside_site", "roadside_refuge"),
            firstCompat("first_dungeons_arise_mushroom_mines", "compat:first_structure:dungeons_arise:mushroom_mines", "when_dungeons_arise", "wda_large_dungeon", "great_dungeon"),
            firstCompat("first_dungeons_arise_mushroom_village", "compat:first_structure:dungeons_arise:mushroom_village", "when_dungeons_arise", "wda_roadside_site", "roadside_refuge"),
            firstCompat("first_dungeons_arise_plague_asylum", "compat:first_structure:dungeons_arise:plague_asylum", "when_dungeons_arise", "wda_plague_asylum", "plague_dungeon"),
            firstCompat("first_dungeons_arise_scorched_mines", "compat:first_structure:dungeons_arise:scorched_mines", "when_dungeons_arise", "wda_industrial_ruin", "industrial_dungeon"),
            firstCompat("first_dungeons_arise_shiraz_palace", "compat:first_structure:dungeons_arise:shiraz_palace", "when_dungeons_arise", "wda_legendary_keep", "legendary_keep"),
            firstCompat("first_dungeons_arise_small_blimp", "compat:first_structure:dungeons_arise:small_blimp", "when_dungeons_arise", "wda_large_dungeon", "great_dungeon"),
            firstCompat("first_dungeons_arise_thornborn_towers", "compat:first_structure:dungeons_arise:thornborn_towers", "when_dungeons_arise", "wda_large_dungeon", "great_dungeon"),
            firstCompat("first_dungeons_arise_typhon", "compat:first_structure:dungeons_arise:typhon", "when_dungeons_arise", "wda_legendary_keep", "legendary_keep"),
            firstCompat("first_dungeons_arise_undead_pirate_ship", "compat:first_structure:dungeons_arise:undead_pirate_ship", "when_dungeons_arise", "wda_raider_ship", "undead_ship"),
            firstCompat("first_dungeons_arise_wishing_well", "compat:first_structure:dungeons_arise:wishing_well", "when_dungeons_arise", "wda_roadside_site", "roadside_refuge"),
            biomeThemeCompat("general_incendium_ash_barrens", "incendium_ash_barrens", "incendium", "nether_ash"),
            biomeThemeCompat("general_incendium_infernal_dunes", "incendium_infernal_dunes", "incendium", "nether_dunes"),
            biomeThemeCompat("general_incendium_inverted_forest", "incendium_inverted_forest", "incendium", "nether_forest"),
            biomeThemeCompat("general_incendium_quartz_flats", "incendium_quartz_flats", "incendium", "nether_quartz"),
            biomeThemeCompat("general_incendium_toxic_heap", "incendium_toxic_heap", "incendium", "nether_toxic"),
            biomeThemeCompat("general_incendium_volcanic_deltas", "incendium_volcanic_deltas", "incendium", "nether_volcanic"),
            biomeThemeCompat("general_incendium_weeping_valley", "incendium_weeping_valley", "incendium", "nether_soul"),
            biomeThemeCompat("general_incendium_withered_forest", "incendium_withered_forest", "incendium", "nether_withered"),
            biomeThemeCompat("general_regions_alpha_grove", "regions_alpha_grove", "regions_unexplored", "forest_grove"),
            biomeThemeCompat("general_regions_ancient_delta", "regions_ancient_delta", "regions_unexplored", "wetland_delta"),
            biomeThemeCompat("general_regions_arid_mountains", "regions_arid_mountains", "regions_unexplored", "dry_highlands"),
            biomeThemeCompat("general_regions_ashen_woodland", "regions_ashen_woodland", "regions_unexplored", "ashen_forest"),
            biomeThemeCompat("general_regions_autumnal_maple_forest", "regions_autumnal_maple_forest", "regions_unexplored", "autumn_forest"),
            biomeThemeCompat("general_regions_baobab_savanna", "regions_baobab_savanna", "regions_unexplored", "savanna"),
            biomeThemeCompat("general_regions_bayou", "regions_bayou", "regions_unexplored", "wetland_bayou"),
            biomeThemeCompat("general_regions_bioshroom_caves", "regions_bioshroom_caves", "regions_unexplored", "cave_mushroom"),
            biomeThemeCompat("general_regions_blackstone_basin", "regions_blackstone_basin", "regions_unexplored", "nether_basin"),
            biomeThemeCompat("general_regions_chalk_cliffs", "regions_chalk_cliffs", "regions_unexplored", "white_cliffs"),
            biomeThemeCompat("general_regions_eucalyptus_forest", "regions_eucalyptus_forest", "regions_unexplored", "forest_grove"),
            biomeThemeCompat("general_regions_flower_fields", "regions_flower_fields", "regions_unexplored", "flower_field"),
            biomeThemeCompat("general_regions_frozen_tundra", "regions_frozen_tundra", "regions_unexplored", "frozen_wilds"),
            biomeThemeCompat("general_regions_glistering_meadow", "regions_glistering_meadow", "regions_unexplored", "bright_meadow"),
            biomeThemeCompat("general_regions_hyacinth_deeps", "regions_hyacinth_deeps", "regions_unexplored", "cave_bloom"),
            biomeThemeCompat("general_regions_infernal_holt", "regions_infernal_holt", "regions_unexplored", "nether_forest"),
            biomeThemeCompat("general_regions_joshua_desert", "regions_joshua_desert", "regions_unexplored", "desert"),
            biomeThemeCompat("general_regions_maple_forest", "regions_maple_forest", "regions_unexplored", "maple_forest"),
            biomeThemeCompat("general_regions_mycotoxic_undergrowth", "regions_mycotoxic_undergrowth", "regions_unexplored", "cave_toxic"),
            biomeThemeCompat("general_regions_old_growth_bayou", "regions_old_growth_bayou", "regions_unexplored", "wetland_bayou"),
            biomeThemeCompat("general_regions_prismachasm", "regions_prismachasm", "regions_unexplored", "cave_crystal"),
            biomeThemeCompat("general_regions_redstone_abyss", "regions_redstone_abyss", "regions_unexplored", "nether_abyss"),
            biomeThemeCompat("general_regions_redwoods", "regions_redwoods", "regions_unexplored", "redwood_forest"),
            biomeThemeCompat("general_regions_saguaro_desert", "regions_saguaro_desert", "regions_unexplored", "desert"),
            biomeThemeCompat("general_regions_silver_birch_forest", "regions_silver_birch_forest", "regions_unexplored", "birch_forest"),
            biomeThemeCompat("general_regions_spires", "regions_spires", "regions_unexplored", "stone_spires"),
            biomeThemeCompat("general_regions_towering_cliffs", "regions_towering_cliffs", "regions_unexplored", "high_cliffs"),
            biomeThemeCompat("general_regions_willow_forest", "regions_willow_forest", "regions_unexplored", "willow_forest"),
            biomeThemeCompat("general_terralith_alpha_islands", "terralith_alpha_islands", "terralith", "sky_islands"),
            biomeThemeCompat("general_terralith_amethyst_canyon", "terralith_amethyst_canyon", "terralith", "crystal_canyon"),
            biomeThemeCompat("general_terralith_amethyst_rainforest", "terralith_amethyst_rainforest", "terralith", "crystal_forest"),
            biomeThemeCompat("general_terralith_ancient_sands", "terralith_ancient_sands", "terralith", "ancient_desert"),
            biomeThemeCompat("general_terralith_ashen_savanna", "terralith_ashen_savanna", "terralith", "ashen_savanna"),
            biomeThemeCompat("general_terralith_basalt_cliffs", "terralith_basalt_cliffs", "terralith", "basalt_cliffs"),
            biomeThemeCompat("general_terralith_blooming_valley", "terralith_blooming_valley", "terralith", "flower_valley"),
            biomeThemeCompat("general_terralith_bryce_canyon", "terralith_bryce_canyon", "terralith", "canyon"),
            biomeThemeCompat("general_terralith_caldera", "terralith_caldera", "terralith", "volcanic_crater"),
            biomeThemeCompat("general_terralith_cloud_forest", "terralith_cloud_forest", "terralith", "cloud_forest"),
            biomeThemeCompat("general_terralith_desert_oasis", "terralith_desert_oasis", "terralith", "oasis"),
            biomeThemeCompat("general_terralith_emerald_peaks", "terralith_emerald_peaks", "terralith", "emerald_highlands"),
            biomeThemeCompat("general_terralith_glacial_chasm", "terralith_glacial_chasm", "terralith", "glacial_chasm"),
            biomeThemeCompat("general_terralith_haze_mountain", "terralith_haze_mountain", "terralith", "misty_highlands"),
            biomeThemeCompat("general_terralith_lavender_valley", "terralith_lavender_valley", "terralith", "flower_valley"),
            biomeThemeCompat("general_terralith_moonlight_grove", "terralith_moonlight_grove", "terralith", "moonlit_forest"),
            biomeThemeCompat("general_terralith_orchid_swamp", "terralith_orchid_swamp", "terralith", "flower_wetland"),
            biomeThemeCompat("general_terralith_painted_mountains", "terralith_painted_mountains", "terralith", "painted_highlands"),
            biomeThemeCompat("general_terralith_sakura_grove", "terralith_sakura_grove", "terralith", "blossom_grove"),
            biomeThemeCompat("general_terralith_scarlet_mountains", "terralith_scarlet_mountains", "terralith", "red_highlands"),
            biomeThemeCompat("general_terralith_skylands_autumn", "terralith_skylands_autumn", "terralith", "sky_islands"),
            biomeThemeCompat("general_terralith_volcanic_crater", "terralith_volcanic_crater", "terralith", "volcanic_crater"),
            biomeThemeCompat("general_terralith_white_cliffs", "terralith_white_cliffs", "terralith", "white_cliffs"),
            biomeThemeCompat("general_terralith_yellowstone", "terralith_yellowstone", "terralith", "hot_springs"),
            mobThemeCompat("battle_illager_alchemist", "illager_alchemist", "illagerinvasion", "illager_mob"),
            mobThemeCompat("battle_illager_archivist", "illager_archivist", "illagerinvasion", "illager_mob"),
            mobThemeCompat("battle_illager_basher", "illager_basher", "illagerinvasion", "illager_raid"),
            mobThemeCompat("battle_illager_firecaller", "illager_firecaller", "illagerinvasion", "illager_mob"),
            mobThemeCompat("battle_illager_inquisitor", "illager_inquisitor", "illagerinvasion", "illager_raid"),
            mobThemeCompat("battle_illager_invoker", "illager_invoker", "illagerinvasion", "illager_mob"),
            mobThemeCompat("battle_illager_marauder", "illager_marauder", "illagerinvasion", "illager_raid"),
            mobThemeCompat("battle_illager_necromancer", "illager_necromancer", "illagerinvasion", "illager_mob"),
            mobThemeCompat("battle_illager_provoker", "illager_provoker", "illagerinvasion", "illager_raid"),
            mobThemeCompat("battle_illager_sorcerer", "illager_sorcerer", "illagerinvasion", "illager_mob"),
            mobThemeCompat("battle_illager_surrendered", "illager_surrendered", "illagerinvasion", "illager_mob")
            // Cross-loader worldgen content compat end
            // Fabric-only content compat v2 end
    );

    private static NamePattern styledPattern(String styleId, String id, PlaceType placeType, double weight, String... slotTags) {
        return new NamePattern(
                id,
                "living_legends.name.pattern." + id,
                Set.of(placeType),
                Set.of(),
                weight,
                slots(slotTags),
                Set.of(styleId)
        );
    }

    private static NamePattern styledPatternWithSlot(
            String styleId,
            String id,
            PlaceType placeType,
            double weight,
            String tokenTag,
            NameTokenForm tokenForm
    ) {
        return new NamePattern(
                id,
                "living_legends.name.pattern." + id,
                Set.of(placeType),
                Set.of(),
                weight,
                List.of(new NamePatternSlot("token_1", tokenTag, tokenForm)),
                Set.of(styleId)
        );
    }

    private static NamePattern styledDeathPatternWithSlot(
            String styleId,
            String id,
            DeathSiteEnvironment environment,
            double weight,
            String tokenTag,
            NameTokenForm tokenForm
    ) {
        return new NamePattern(
                id,
                "living_legends.name.pattern." + id,
                Set.of(PlaceType.DEATH_SITE),
                Set.of(environment),
                weight,
                List.of(new NamePatternSlot("token_1", tokenTag, tokenForm)),
                Set.of(styleId, environment.idString())
        );
    }

    private static NamePattern deathPattern(
            String id,
            DeathSiteEnvironment environment,
            double weight,
            String... slotTags
    ) {
        return new NamePattern(
                id,
                "living_legends.name.pattern." + id,
                Set.of(PlaceType.DEATH_SITE),
                Set.of(environment),
                weight,
                slots(slotTags),
                Set.of(DEFAULT_STYLE_ID, environment.idString())
        );
    }

    private static NamePattern deathPatternWithSlot(
            String id,
            DeathSiteEnvironment environment,
            double weight,
            String tokenTag,
            NameTokenForm tokenForm
    ) {
        return new NamePattern(
                id,
                "living_legends.name.pattern." + id,
                Set.of(PlaceType.DEATH_SITE),
                Set.of(environment),
                weight,
                List.of(new NamePatternSlot("token_1", tokenTag, tokenForm)),
                Set.of(DEFAULT_STYLE_ID, environment.idString())
        );
    }

    private static NamePattern deathCausePattern(
            String id,
            DeathSiteEnvironment environment,
            String deathCause,
            double weight
    ) {
        return deathCausePattern(id, environment, deathCause, weight, DEFAULT_STYLE_ID);
    }

    private static NamePattern deathCausePattern(
            String id,
            DeathSiteEnvironment environment,
            String deathCause,
            double weight,
            String styleId
    ) {
        return new NamePattern(
                id,
                "living_legends.name.pattern." + id,
                Set.of(PlaceType.DEATH_SITE),
                Set.of(environment),
                weight,
                List.of(),
                Set.of(builtInStyleId(styleId), environment.idString(), "death_cause", WorldPos.optionalId(deathCause)),
                NameSemanticRoots.inferPatternRoot(id),
                NameCauseConstraints.builder()
                        .requiredCauseType(PlaceCauseType.PLAYER_DEATHS)
                        .requiredDeathCause(deathCause)
                        .build(),
                Set.of()
        );
    }

    private static List<NamePatternSlot> slots(String... tags) {
        return switch (tags.length) {
            case 0 -> List.of();
            case 1 -> List.of(new NamePatternSlot("token_1", tags[0], NameTokenForm.BASE));
            default -> List.of(
                    new NamePatternSlot("token_1", tags[0], NameTokenForm.BASE),
                    new NamePatternSlot("token_2", tags[1], NameTokenForm.GENITIVE)
            );
        };
    }

    private static Map<NameTokenForm, String> tokenForms(String id) {
        Map<NameTokenForm, String> result = new EnumMap<>(NameTokenForm.class);
        for (NameTokenForm form : NameTokenForm.values()) {
            result.put(form, "living_legends.name.token." + id + "." + form.idString());
        }
        return result;
    }

    private static NameCauseConstraints inferredTokenCauseConstraints(Set<String> tags) {
        NameCauseConstraints.Builder builder = NameCauseConstraints.builder();
        boolean constrained = false;
        if (hasAny(tags, "death_subject", "lost_subject", "blood_subject")) {
            builder.allowedCauseTypes(
                    PlaceCauseType.PLAYER_DEATHS,
                    PlaceCauseType.PET_DEATH,
                    PlaceCauseType.NAMED_MOB_DEATH
            );
            constrained = true;
        }
        if (hasAny(tags, "battle_subject", "battle_symbol")) {
            builder.allowedCauseTypes(
                    PlaceCauseType.MOB_BATTLE,
                    PlaceCauseType.PVP,
                    PlaceCauseType.BOSS_KILL,
                    PlaceCauseType.FIRST_BOSS_KILL,
                    PlaceCauseType.RAID
            );
            constrained = true;
        }
        if (hasAny(tags, "slaughter_subject", "slaughter_place")) {
            builder.allowedCauseTypes(PlaceCauseType.PASSIVE_SLAUGHTER);
            constrained = true;
        }
        if (hasAny(tags, "pvp_subject", "combatant", "champion", "pvp_place")) {
            builder.allowedCauseTypes(PlaceCauseType.PVP);
            constrained = true;
        }
        if (hasAny(tags, "mining_subject", "mining_place", "ore")) {
            builder.allowedCauseTypes(PlaceCauseType.MINING, PlaceCauseType.FIRST_BLOCK_DISCOVERY);
            constrained = true;
        }
        if (hasAny(tags, "portal_subject")) {
            builder.allowedCauseTypes(
                    PlaceCauseType.PORTAL_USAGE,
                    PlaceCauseType.FIRST_DIMENSION_DISCOVERY,
                    PlaceCauseType.FIRST_STRUCTURE_DISCOVERY
            );
            constrained = true;
        }
        if (hasAny(tags, "settlement_place")) {
            builder.allowedCauseTypes(PlaceCauseType.SETTLEMENT, PlaceCauseType.RAID);
            constrained = true;
        }
        if (hasAny(tags, "boss_subject", "boss_place")) {
            builder.allowedCauseTypes(PlaceCauseType.BOSS_KILL, PlaceCauseType.FIRST_BOSS_KILL);
            constrained = true;
        }
        if (hasAny(tags, "creature_subject")) {
            builder.allowedCauseTypes(PlaceCauseType.PET_DEATH, PlaceCauseType.NAMED_MOB_DEATH);
            constrained = true;
        }
        if (!constrained && hasAny(tags, "discovery_subject", "discovery_place")) {
            builder.allowedCauseTypes(
                    PlaceCauseType.FIRST_STRUCTURE_DISCOVERY,
                    PlaceCauseType.FIRST_BLOCK_DISCOVERY,
                    PlaceCauseType.FIRST_DIMENSION_DISCOVERY,
                    PlaceCauseType.FIRST_BOSS_KILL
            );
            constrained = true;
        }
        return constrained ? builder.build() : NameCauseConstraints.none();
    }

    private static boolean hasAny(Set<String> tags, String... expectedTags) {
        if (tags == null || expectedTags == null) {
            return false;
        }
        for (String expectedTag : expectedTags) {
            if (tags.contains(expectedTag)) {
                return true;
            }
        }
        return false;
    }

    private static NameDataPack smallStylePack(String styleId) {
        return new NameDataPack(styleId, smallStylePatterns(styleId), smallStyleTokens());
    }

    private static List<NamePattern> smallStylePatterns(String styleId) {
        String prefix = styleId + "_";
        return List.of(
                styledDeathPatternWithSlot(styleId, prefix + "death_surface_fallen", DeathSiteEnvironment.SURFACE, 1.0, "death_subject", NameTokenForm.GENITIVE),
                styledDeathPatternWithSlot(styleId, prefix + "death_surface_lost", DeathSiteEnvironment.SURFACE, 0.9, "lost_subject", NameTokenForm.GENITIVE),
                styledDeathPatternWithSlot(styleId, prefix + "death_surface_memory", DeathSiteEnvironment.SURFACE, 0.8, "death_subject", NameTokenForm.GENITIVE),
                styledDeathPatternWithSlot(styleId, prefix + "death_cave_fallen", DeathSiteEnvironment.CAVE, 1.0, "death_subject", NameTokenForm.GENITIVE),
                styledDeathPatternWithSlot(styleId, prefix + "death_cave_lost", DeathSiteEnvironment.CAVE, 0.9, "lost_subject", NameTokenForm.GENITIVE),
                styledDeathPatternWithSlot(styleId, prefix + "death_cave_echo", DeathSiteEnvironment.CAVE, 0.8, "death_subject", NameTokenForm.GENITIVE),
                styledPatternWithSlot(styleId, prefix + "slaughter_field", PlaceType.SLAUGHTER_FIELD, 1.0, "slaughter_subject", NameTokenForm.GENITIVE),
                styledPatternWithSlot(styleId, prefix + "battlefield", PlaceType.BATTLEFIELD, 1.0, "battle_subject", NameTokenForm.GENITIVE),
                styledPatternWithSlot(styleId, prefix + "portal_landmark", PlaceType.PORTAL_LANDMARK, 1.0, "portal_subject", NameTokenForm.GENITIVE),
                styledPatternWithSlot(styleId, prefix + "portal_gate", PlaceType.PORTAL_LANDMARK, 0.9, "portal_subject", NameTokenForm.GENITIVE),
                styledPatternWithSlot(styleId, prefix + "portal_marker", PlaceType.PORTAL_LANDMARK, 0.8, "portal_subject", NameTokenForm.GENITIVE),
                styledPatternWithSlot(styleId, prefix + "general_landmark", PlaceType.GENERAL_LANDMARK, 1.0, "memory_subject", NameTokenForm.GENITIVE),
                styledPatternWithSlot(styleId, prefix + "general_marker", PlaceType.GENERAL_LANDMARK, 0.9, "memory_subject", NameTokenForm.GENITIVE),
                styledPatternWithSlot(styleId, prefix + "general_record", PlaceType.GENERAL_LANDMARK, 0.8, "memory_subject", NameTokenForm.GENITIVE),
                styledPatternWithSlot(styleId, prefix + "mining_site", PlaceType.MINING_SITE, 1.0, "mining_subject", NameTokenForm.GENITIVE),
                styledPatternWithSlot(styleId, prefix + "first_discovery", PlaceType.FIRST_DISCOVERY, 1.0, "discovery_subject", NameTokenForm.GENITIVE),
                styledPatternWithSlot(styleId, prefix + "first_trace", PlaceType.FIRST_DISCOVERY, 0.9, "discovery_subject", NameTokenForm.GENITIVE),
                styledPatternWithSlot(styleId, prefix + "first_record", PlaceType.FIRST_DISCOVERY, 0.8, "discovery_subject", NameTokenForm.GENITIVE)
        );
    }

    private static List<NameToken> smallStyleTokens() {
        return List.of(
                token("fallen", "death", "memory", "generic", "death_subject", "memory_subject"),
                token("lost", "death", "memory", "lost_subject", "death_subject", "memory_subject"),
                token("blood", "death", "battle", "blood_subject", "death_subject", "battle_subject"),
                token("red", "battle", "abstract_quality", "battle_subject"),
                token("last_breath", "death", "memory", "death_subject", "memory_subject"),
                token("bones", "death", "memory", "death_subject", "memory_subject"),
                token("shadow", "death", "generic", "death_subject", "memory_subject", "abstract_quality"),
                token("hunt", "slaughter", "battle", "slaughter_subject"),
                token("slaughter", "slaughter", "battle", "slaughter_subject"),
                token("gate", "portal", "threshold", "portal_subject", "place_noun"),
                token("portal", "portal", "dimension", "portal_subject"),
                token("rift", "portal", "dimension", "portal_subject", "place_noun"),
                token("obsidian", "portal", "terrain", "portal_subject", "place_noun"),
                token("ender_eye", "end", "dimension", "portal_subject", "discovery_subject"),
                token("far_world", "dimension", "landmark", "portal_subject", "discovery_subject", "place_noun"),
                token("waystone", "landmark", "threshold", "place_noun", "discovery_subject", "portal_subject"),
                token("old", "memory", "abstract_quality", "memory_subject"),
                token("forgotten", "memory", "abstract_quality", "memory_subject"),
                token("echoes", "echo", "memory", "generic", "memory_subject", "abstract_quality"),
                token("valor", "battle", "memory", "battle_subject", "memory_subject", "abstract_quality"),
                token("marker", "landmark", "memory", "place_noun", "memory_subject"),
                token("mine", "mining", "mining_place", "place_noun", "mining_subject"),
                token("ore", "ore", "mining", "mining_subject"),
                token("vein", "mining", "ore", "mining_subject", "mining_place"),
                token("diamond", "ore", "discovery", "mining_subject", "discovery_subject"),
                token("emerald", "ore", "discovery", "mining_subject", "discovery_subject"),
                token("ancient_debris", "ore", "nether", "mining_subject", "discovery_subject"),
                token("first_step", "discovery", "threshold", "discovery_subject"),
                token("dawn", "discovery", "landmark", "discovery_subject", "abstract_quality"),
                token("discovery", "discovery", "memory", "discovery_subject")
        );
    }

    private static NameDataPack darkFantasyPack() {
        List<NamePattern> patterns = new ArrayList<>();

        addDarkDeath(patterns, DeathSiteEnvironment.SURFACE,
                "dark_fantasy_surface_field_blood",
                "dark_fantasy_surface_ground_fallen",
                "dark_fantasy_surface_black_mark",
                "dark_fantasy_surface_last_breath_field",
                "dark_fantasy_surface_grave_ground",
                "dark_fantasy_surface_trace_lost",
                "dark_fantasy_surface_blood_meadow",
                "dark_fantasy_surface_fallen_rest",
                "dark_fantasy_surface_cursed_ground",
                "dark_fantasy_surface_quiet_grave",
                "dark_fantasy_surface_old_wound",
                "dark_fantasy_surface_sorrow_mark",
                "dark_fantasy_surface_silent_field",
                "dark_fantasy_surface_ashen_grass",
                "dark_fantasy_surface_mourning_ground",
                "dark_fantasy_surface_oblivion_ground",
                "dark_fantasy_surface_black_field",
                "dark_fantasy_surface_dead_grass",
                "dark_fantasy_surface_blood_memory",
                "dark_fantasy_surface_lost_trail",
                "dark_fantasy_surface_red_ground",
                "dark_fantasy_surface_gloom_meadow",
                "dark_fantasy_surface_last_trace",
                "dark_fantasy_surface_silence_field",
                "dark_fantasy_surface_old_wound_ground",
                "dark_fantasy_surface_quiet_hill",
                "dark_fantasy_surface_fallen_mark");
        addDarkDeath(patterns, DeathSiteEnvironment.CAVE,
                "dark_fantasy_cave_cursed_hollow",
                "dark_fantasy_cave_black_cave",
                "dark_fantasy_cave_depths_fallen",
                "dark_fantasy_cave_last_breath_hollow",
                "dark_fantasy_cave_skull_cave",
                "dark_fantasy_cave_silent_deep",
                "dark_fantasy_cave_shadow_beneath",
                "dark_fantasy_cave_lost_picks",
                "dark_fantasy_cave_dark_rift",
                "dark_fantasy_cave_depth_rest",
                "dark_fantasy_cave_black_hollow",
                "dark_fantasy_cave_oblivion",
                "dark_fantasy_cave_stone_grave",
                "dark_fantasy_cave_depth_echo",
                "dark_fantasy_cave_path_beneath",
                "dark_fantasy_cave_lightless_rest",
                "dark_fantasy_cave_last_steps_dark",
                "dark_fantasy_cave_deep_wound",
                "dark_fantasy_cave_black_stone_hollow",
                "dark_fantasy_cave_starless",
                "dark_fantasy_cave_blind_hollow",
                "dark_fantasy_cave_deafened_deep",
                "dark_fantasy_cave_stone_rest",
                "dark_fantasy_cave_lightless_cave",
                "dark_fantasy_cave_black_rift",
                "dark_fantasy_cave_lost_echo",
                "dark_fantasy_cave_quiet_hollow",
                "dark_fantasy_cave_path_dark",
                "dark_fantasy_cave_grave_beneath",
                "dark_fantasy_cave_rock_rest",
                "dark_fantasy_cave_oblivion_hollow",
                "dark_fantasy_cave_deep_trace",
                "dark_fantasy_cave_dark_pocket",
                "dark_fantasy_cave_wound");
        addDarkDeath(patterns, DeathSiteEnvironment.WATER,
                "dark_fantasy_water_drowned_water",
                "dark_fantasy_water_drowned_site",
                "dark_fantasy_water_final_pool",
                "dark_fantasy_water_deep_grave",
                "dark_fantasy_water_quiet_fallen",
                "dark_fantasy_water_last_current",
                "dark_fantasy_water_sunken_mark");
        addDarkDeath(patterns, DeathSiteEnvironment.MOUNTAIN,
                "dark_fantasy_mountain_last_ledge",
                "dark_fantasy_mountain_black_cliff",
                "dark_fantasy_mountain_bloodied_slope",
                "dark_fantasy_mountain_summit_fallen",
                "dark_fantasy_mountain_cold_grave",
                "dark_fantasy_mountain_last_steps_ledge",
                "dark_fantasy_mountain_wind_lost",
                "dark_fantasy_mountain_dark_ascent",
                "dark_fantasy_mountain_stone_falling",
                "dark_fantasy_mountain_no_return_cliff",
                "dark_fantasy_mountain_last_day_slope",
                "dark_fantasy_mountain_frozen_mark",
                "dark_fantasy_mountain_summit_silence",
                "dark_fantasy_mountain_blood_stone",
                "dark_fantasy_mountain_wind_grave",
                "dark_fantasy_mountain_cold_ledge",
                "dark_fantasy_mountain_broken_ascent",
                "dark_fantasy_mountain_rock_trace",
                "dark_fantasy_mountain_black_summit",
                "dark_fantasy_mountain_ash_rock");
        addDarkDeath(patterns, DeathSiteEnvironment.NETHER,
                "dark_fantasy_nether_ashen_grave",
                "dark_fantasy_nether_lava_rest",
                "dark_fantasy_nether_blackstone_mark",
                "dark_fantasy_nether_underworld_rest",
                "dark_fantasy_nether_ash_fallen",
                "dark_fantasy_nether_basalt_grave",
                "dark_fantasy_nether_fire_ground",
                "dark_fantasy_nether_burning_rest",
                "dark_fantasy_nether_lava_scar",
                "dark_fantasy_nether_ashen_trace",
                "dark_fantasy_nether_blackstone_rest",
                "dark_fantasy_nether_fire_grave",
                "dark_fantasy_nether_ash_threshold",
                "dark_fantasy_nether_black_basalt",
                "dark_fantasy_nether_lava_ground",
                "dark_fantasy_nether_underworld_grave",
                "dark_fantasy_nether_ashen_rest",
                "dark_fantasy_nether_fire_trace");
        addDarkDeath(patterns, DeathSiteEnvironment.END,
                "dark_fantasy_end_void_rest",
                "dark_fantasy_end_pale_grave",
                "dark_fantasy_end_last_threshold",
                "dark_fantasy_end_silence",
                "dark_fantasy_end_void_hollow",
                "dark_fantasy_end_white_rest",
                "dark_fantasy_end_void_mark",
                "dark_fantasy_end_pale_silence");
        addDarkDeath(patterns, DeathSiteEnvironment.UNKNOWN,
                "dark_fantasy_death_remembered_grave",
                "dark_fantasy_death_black_mark",
                "dark_fantasy_death_old_wound",
                "dark_fantasy_death_fallen_rest",
                "dark_fantasy_death_quiet_grave");

        addDarkPatterns(patterns, PlaceType.BATTLEFIELD,
                "dark_fantasy_battle_blood_field",
                "dark_fantasy_battle_broken_banners",
                "dark_fantasy_battle_clash_ground",
                "dark_fantasy_battle_black_battlefield",
                "dark_fantasy_battle_fallen_blades",
                "dark_fantasy_battle_slaughter_trace",
                "dark_fantasy_battle_old_battle_site",
                "dark_fantasy_battle_blood_banner",
                "dark_fantasy_battle_ash_field",
                "dark_fantasy_battle_cursed_clash",
                "dark_fantasy_battle_black_banner",
                "dark_fantasy_battle_red_ground");
        addDarkDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:zombie", "minecraft:zombie_villager", "minecraft:husk", "minecraft:drowned", "group:undead"),
                "dark_fantasy_battle_field_dead",
                "dark_fantasy_battle_rotten_ground",
                "dark_fantasy_battle_undead_ground",
                "dark_fantasy_battle_night_slaughter",
                "dark_fantasy_battle_rot_trace",
                "dark_fantasy_battle_deadfield",
                "dark_fantasy_battle_field_groans",
                "dark_fantasy_battle_grave_march");
        addDarkDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:skeleton", "minecraft:stray", "minecraft:bogged", "minecraft:wither_skeleton", "group:skeleton"),
                "dark_fantasy_battle_bone_lowland",
                "dark_fantasy_battle_arrow_field",
                "dark_fantasy_battle_white_bones",
                "dark_fantasy_battle_bonefield",
                "dark_fantasy_battle_archer_trace",
                "dark_fantasy_battle_skull_volley",
                "dark_fantasy_battle_bone_mark",
                "dark_fantasy_battle_arrow_grave");
        addDarkDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:creeper", "group:explosive"),
                "dark_fantasy_battle_creeper_crater",
                "dark_fantasy_battle_hissing_field",
                "dark_fantasy_battle_blast_lowland",
                "dark_fantasy_battle_crater_dread",
                "dark_fantasy_battle_blast_mark",
                "dark_fantasy_battle_torn_ground",
                "dark_fantasy_battle_black_crater",
                "dark_fantasy_battle_riven_field");
        addDarkDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:enderman", "group:end"),
                "dark_fantasy_battle_field_shadows",
                "dark_fantasy_battle_wanderer_path",
                "dark_fantasy_battle_black_shadow_mark",
                "dark_fantasy_battle_clash_shadow",
                "dark_fantasy_battle_empty_trace",
                "dark_fantasy_battle_shadow_ground",
                "dark_fantasy_battle_long_dark",
                "dark_fantasy_battle_void_step");
        addDarkDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:blaze", "minecraft:ghast", "minecraft:piglin", "minecraft:piglin_brute", "minecraft:zombified_piglin", "minecraft:hoglin", "minecraft:zoglin", "group:nether"),
                "dark_fantasy_battle_ashen_clash",
                "dark_fantasy_battle_field_flame",
                "dark_fantasy_battle_underworld_mark",
                "dark_fantasy_battle_ground_fire",
                "dark_fantasy_battle_basalt_slaughter",
                "dark_fantasy_battle_fire_lowland",
                "dark_fantasy_battle_ash_mark",
                "dark_fantasy_battle_lava_clash",
                "dark_fantasy_battle_blaze_mark",
                "dark_fantasy_battle_flame_wound",
                "dark_fantasy_battle_burning_field",
                "dark_fantasy_battle_ember_clash");
        addDarkDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("group:illager"),
                "dark_fantasy_battle_raider_field",
                "dark_fantasy_battle_blood_banner_target",
                "dark_fantasy_battle_raid_mark",
                "dark_fantasy_battle_raid_ground",
                "dark_fantasy_battle_broken_banner",
                "dark_fantasy_battle_cursed_defense");
        addDarkDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:warden", "group:boss_like"),
                "dark_fantasy_battle_silent_clash",
                "dark_fantasy_battle_sculk_mark",
                "dark_fantasy_battle_deep_field",
                "dark_fantasy_battle_warden_silence",
                "dark_fantasy_battle_soundless_field");
        addDarkDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:breeze"),
                "dark_fantasy_battle_wind_field",
                "dark_fantasy_battle_trial_mark",
                "dark_fantasy_battle_whirling_clash",
                "dark_fantasy_battle_trial_hall");

        addDarkPatterns(patterns, PlaceType.SLAUGHTER_FIELD,
                "dark_fantasy_slaughter_butchered_pasture",
                "dark_fantasy_slaughter_field_blood",
                "dark_fantasy_slaughter_trace",
                "dark_fantasy_slaughter_red_pasture",
                "dark_fantasy_slaughter_hunting_ground",
                "dark_fantasy_slaughter_old_pasture",
                "dark_fantasy_slaughter_site",
                "dark_fantasy_slaughter_herd_trail",
                "dark_fantasy_slaughter_blood_trace",
                "dark_fantasy_slaughter_field_loss");
        addDarkDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("minecraft:cow", "minecraft:mooshroom", "group:farm_animal"),
                "dark_fantasy_slaughter_blood_pasture",
                "dark_fantasy_slaughter_herd_pasture",
                "dark_fantasy_slaughter_cattle_trace",
                "dark_fantasy_slaughter_red_meadow",
                "dark_fantasy_slaughter_herd_field",
                "dark_fantasy_slaughter_cattle_mark",
                "dark_fantasy_slaughter_cow_bloodfield",
                "dark_fantasy_slaughter_dark_herd",
                "dark_fantasy_slaughter_cattle_loss",
                "dark_fantasy_slaughter_red_herd");
        addDarkDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("minecraft:pig"),
                "dark_fantasy_slaughter_hog_pasture",
                "dark_fantasy_slaughter_pig_trace",
                "dark_fantasy_slaughter_red_pen",
                "dark_fantasy_slaughter_boar_field",
                "dark_fantasy_slaughter_pasture_regret",
                "dark_fantasy_slaughter_hog_mark");
        addDarkDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("minecraft:chicken"),
                "dark_fantasy_slaughter_feather_field",
                "dark_fantasy_slaughter_feather_mark",
                "dark_fantasy_slaughter_chicken_trace",
                "dark_fantasy_slaughter_feather_trail",
                "dark_fantasy_slaughter_wing_trace");
        addDarkDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("minecraft:sheep"),
                "dark_fantasy_slaughter_sheep_slope",
                "dark_fantasy_slaughter_wool_field",
                "dark_fantasy_slaughter_flock_trace",
                "dark_fantasy_slaughter_red_wool",
                "dark_fantasy_slaughter_sheep_pasture");
        addDarkDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("group:civilian"),
                "dark_fantasy_slaughter_tragedy_path",
                "dark_fantasy_slaughter_place_loss",
                "dark_fantasy_slaughter_villager_memory",
                "dark_fantasy_slaughter_trader_trace",
                "dark_fantasy_slaughter_dark_square",
                "dark_fantasy_slaughter_civilian_memory",
                "dark_fantasy_slaughter_silent_square",
                "dark_fantasy_slaughter_village_sorrow",
                "dark_fantasy_slaughter_market_shadow",
                "dark_fantasy_slaughter_lost_citizen_trace");
        addDarkDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("group:companion"),
                "dark_fantasy_slaughter_quiet_memory",
                "dark_fantasy_slaughter_companion_trace",
                "dark_fantasy_slaughter_farewell_site",
                "dark_fantasy_slaughter_dark_trail",
                "dark_fantasy_slaughter_loyal_memory");

        addDarkPatterns(patterns, PlaceType.PVP_ARENA,
                "dark_fantasy_pvp_blood_ring",
                "dark_fantasy_pvp_rival_arena",
                "dark_fantasy_pvp_duelist_ring",
                "dark_fantasy_pvp_field_honor",
                "dark_fantasy_pvp_black_arena",
                "dark_fantasy_pvp_duel_site",
                "dark_fantasy_pvp_ring_fallen",
                "dark_fantasy_pvp_old_spite",
                "dark_fantasy_pvp_rival_trace",
                "dark_fantasy_pvp_clash_zone");

        addDarkPatterns(patterns, PlaceType.MINING_SITE,
                "dark_fantasy_mining_ore_grave",
                "dark_fantasy_mining_deep_vein",
                "dark_fantasy_mining_black_vein",
                "dark_fantasy_mining_site",
                "dark_fantasy_mining_ore_trace",
                "dark_fantasy_mining_stone_scar",
                "dark_fantasy_mining_cursed_layer");
        addDarkDominant(patterns, PlaceType.MINING_SITE, blockConstraints("minecraft:diamond_ore", "minecraft:deepslate_diamond_ore"),
                "dark_fantasy_mining_diamond_vein",
                "dark_fantasy_mining_deep_diamond_vein",
                "dark_fantasy_mining_cold_glitter",
                "dark_fantasy_mining_dark_light_vein",
                "dark_fantasy_mining_diamond_trace",
                "dark_fantasy_mining_cursed_vein");
        addDarkDominant(patterns, PlaceType.MINING_SITE, blockConstraints("minecraft:emerald_ore", "minecraft:deepslate_emerald_ore"),
                "dark_fantasy_mining_emerald_vein",
                "dark_fantasy_mining_green_layer",
                "dark_fantasy_mining_emerald_trace",
                "dark_fantasy_mining_trader_vein",
                "dark_fantasy_mining_cold_vein");
        addDarkDominant(patterns, PlaceType.MINING_SITE, blockConstraints("minecraft:ancient_debris"),
                "dark_fantasy_mining_ancient_relic",
                "dark_fantasy_mining_buried_relic",
                "dark_fantasy_mining_ancient_debris",
                "dark_fantasy_mining_underworld_scar",
                "dark_fantasy_mining_netherite_trace",
                "dark_fantasy_mining_black_layer",
                "dark_fantasy_mining_ashen_vein");

        addDarkPatterns(patterns, PlaceType.PORTAL_LANDMARK,
                "dark_fantasy_portal_black_gate",
                "dark_fantasy_portal_threshold_worlds",
                "dark_fantasy_portal_worldscar",
                "dark_fantasy_portal_shadow_crossing",
                "dark_fantasy_portal_crossing_gate",
                "dark_fantasy_portal_path_between_worlds",
                "dark_fantasy_portal_grim_threshold",
                "dark_fantasy_portal_old_gate",
                "dark_fantasy_portal_betweenworld_trace",
                "dark_fantasy_portal_cursed_gate",
                "dark_fantasy_portal_cursed_crossing",
                "dark_fantasy_portal_rift_worlds",
                "dark_fantasy_portal_old_threshold",
                "dark_fantasy_portal_shadow_gate",
                "dark_fantasy_portal_oblivion_threshold",
                "dark_fantasy_portal_world_wound",
                "dark_fantasy_portal_trail_dark",
                "dark_fantasy_portal_black_crossing",
                "dark_fantasy_portal_far_gate",
                "dark_fantasy_portal_nameless_gate",
                "dark_fantasy_portal_grim_crossing",
                "dark_fantasy_portal_trace_beyond",
                "dark_fantasy_portal_wanderer_threshold",
                "dark_fantasy_portal_silent_gate");
        addDarkDominant(patterns, PlaceType.PORTAL_LANDMARK, portalConstraints("nether"),
                "dark_fantasy_portal_ashen_gate",
                "dark_fantasy_portal_underworld_gate",
                "dark_fantasy_portal_lava_gate",
                "dark_fantasy_portal_basalt_crossing",
                "dark_fantasy_portal_blackstone_threshold",
                "dark_fantasy_portal_path_ash",
                "dark_fantasy_portal_nether_gate");
        addDarkDominant(patterns, PlaceType.PORTAL_LANDMARK, portalConstraints("end"),
                "dark_fantasy_portal_last_threshold",
                "dark_fantasy_portal_void_gate",
                "dark_fantasy_portal_pale_crossing",
                "dark_fantasy_portal_edge_threshold",
                "dark_fantasy_portal_eye_gate",
                "dark_fantasy_portal_path_void",
                "dark_fantasy_portal_white_gate",
                "dark_fantasy_portal_silent_threshold");

        addDarkPatterns(patterns, PlaceType.GENERAL_LANDMARK,
                "dark_fantasy_general_gloom_trail",
                "dark_fantasy_general_old_trace",
                "dark_fantasy_general_grim_rest",
                "dark_fantasy_general_quiet_mark",
                "dark_fantasy_general_forgotten_path",
                "dark_fantasy_general_nameless_path",
                "dark_fantasy_general_dark_stop",
                "dark_fantasy_general_wanderer_trace",
                "dark_fantasy_general_old_place",
                "dark_fantasy_general_remembered_shadow");
        addDarkBiome(patterns, "forest",
                "dark_fantasy_general_dark_canopies",
                "dark_fantasy_general_forest_shadow",
                "dark_fantasy_general_old_forest_trace");
        addDarkBiome(patterns, "desert",
                "dark_fantasy_general_dead_sand_trace",
                "dark_fantasy_general_sand_shadow",
                "dark_fantasy_general_dry_wind_path");
        addDarkBiome(patterns, "snowy",
                "dark_fantasy_general_cold_trail",
                "dark_fantasy_general_dead_snow_trace",
                "dark_fantasy_general_white_mark");
        addDarkBiome(patterns, "swamp",
                "dark_fantasy_general_mire_trail",
                "dark_fantasy_general_swamp_shadow",
                "dark_fantasy_general_mire_trace");
        addDarkBiome(patterns, "plains",
                "dark_fantasy_general_grey_trail",
                "dark_fantasy_general_grass_trace",
                "dark_fantasy_general_empty_meadow");

        addDarkPatterns(patterns, PlaceType.SETTLEMENT,
                "dark_fantasy_settlement_old_refuge",
                "dark_fantasy_settlement_dark_hearth",
                "dark_fantasy_settlement_shelter_ash",
                "dark_fantasy_settlement_lived_shadow",
                "dark_fantasy_settlement_ember_rest",
                "dark_fantasy_settlement_house_clouds",
                "dark_fantasy_settlement_last_fire",
                "dark_fantasy_settlement_quiet_haven",
                "dark_fantasy_settlement_settler_trace",
                "dark_fantasy_settlement_cold_hearth",
                "dark_fantasy_settlement_ashen_hearth",
                "dark_fantasy_settlement_shadow_haven",
                "dark_fantasy_settlement_last_light_house",
                "dark_fantasy_settlement_ember_shelter",
                "dark_fantasy_settlement_grim_home",
                "dark_fantasy_settlement_old_haven",
                "dark_fantasy_settlement_shadow_settlement",
                "dark_fantasy_settlement_quiet_hearth",
                "dark_fantasy_settlement_cold_shelter",
                "dark_fantasy_settlement_living_trace",
                "dark_fantasy_settlement_roof_dark",
                "dark_fantasy_settlement_night_camp");

        addDarkPatterns(patterns, PlaceType.FIRST_DISCOVERY,
                "dark_fantasy_first_revelation",
                "dark_fantasy_first_trace",
                "dark_fantasy_first_discovery_threshold",
                "dark_fantasy_first_light_place",
                "dark_fantasy_first_shadow_find",
                "dark_fantasy_first_new_legend",
                "dark_fantasy_first_beginning_trace",
                "dark_fantasy_first_path_memory",
                "dark_fantasy_first_sign",
                "dark_fantasy_first_shadow_discovery",
                "dark_fantasy_first_new_path_trace",
                "dark_fantasy_first_secret_threshold",
                "dark_fantasy_first_forgotten_discovery",
                "dark_fantasy_first_black_find",
                "dark_fantasy_first_world_scar",
                "dark_fantasy_first_secret_place",
                "dark_fantasy_first_sign_trail",
                "dark_fantasy_first_new_shadow",
                "dark_fantasy_first_awakening",
                "dark_fantasy_first_path_mark",
                "dark_fantasy_first_early_threshold",
                "dark_fantasy_first_border");
        addDarkExactFirst(patterns, "world:first_stronghold_found",
                "dark_fantasy_first_stronghold_citadel",
                "dark_fantasy_first_stronghold_eye_halls",
                "dark_fantasy_first_stronghold_hidden_threshold",
                "dark_fantasy_first_stronghold_eye_citadel",
                "dark_fantasy_first_stronghold_dark_halls",
                "dark_fantasy_first_stronghold_stone_threshold",
                "dark_fantasy_first_stronghold_path_eye",
                "dark_fantasy_first_stronghold_forgotten_citadel",
                "dark_fantasy_first_stronghold_edge_fortress",
                "dark_fantasy_first_stronghold_last_path_halls",
                "dark_fantasy_first_stronghold_black_halls",
                "dark_fantasy_first_stronghold_eye_threshold",
                "dark_fantasy_first_stronghold_old_stone",
                "dark_fantasy_first_stronghold_hidden_eye");
        addDarkExactFirst(patterns, "world:first_diamond_ore_mined",
                "dark_fantasy_first_diamond",
                "dark_fantasy_first_cold_glitter",
                "dark_fantasy_first_diamond_light",
                "dark_fantasy_first_diamond_vein",
                "dark_fantasy_first_diamond_trace",
                "dark_fantasy_first_dark_glitter");
        addDarkExactFirst(patterns, "world:first_ancient_debris_mined",
                "dark_fantasy_first_ancient_debris",
                "dark_fantasy_first_relic_ash",
                "dark_fantasy_first_buried_relic",
                "dark_fantasy_first_ancient_trace",
                "dark_fantasy_first_netherite_trace",
                "dark_fantasy_first_underworld_scar");
        addDarkExactFirst(patterns, "world:first_nether_entry",
                "dark_fantasy_first_descent",
                "dark_fantasy_first_ashen_gate",
                "dark_fantasy_first_path_below",
                "dark_fantasy_first_underworld_threshold",
                "dark_fantasy_first_ashen_trace",
                "dark_fantasy_first_fire_gate",
                "dark_fantasy_first_underworld_gate",
                "dark_fantasy_first_lava_path",
                "dark_fantasy_first_ash_border",
                "dark_fantasy_first_burning_threshold");
        addDarkExactFirst(patterns, "world:first_end_entry",
                "dark_fantasy_first_last_threshold",
                "dark_fantasy_first_pale_crossing",
                "dark_fantasy_first_path_edge",
                "dark_fantasy_first_void_threshold",
                "dark_fantasy_first_void_trace",
                "dark_fantasy_first_final_silence_gate");

        addDarkPatterns(patterns, PlaceType.BOSS_SITE,
                "dark_fantasy_boss_beast_lair",
                "dark_fantasy_boss_clash_ground",
                "dark_fantasy_boss_great_fall_site",
                "dark_fantasy_boss_fallen_lair",
                "dark_fantasy_boss_strong_foe_trace",
                "dark_fantasy_boss_black_arena",
                "dark_fantasy_boss_heavy_victory",
                "dark_fantasy_boss_dark_lair",
                "dark_fantasy_boss_broken_power",
                "dark_fantasy_boss_old_fear",
                "dark_fantasy_boss_old_dread_site",
                "dark_fantasy_boss_fallen_arena",
                "dark_fantasy_boss_shadow_lair",
                "dark_fantasy_boss_black_victory",
                "dark_fantasy_boss_great_beast_trace",
                "dark_fantasy_boss_dark_power_place",
                "dark_fantasy_boss_heavy_clash_ground",
                "dark_fantasy_boss_blood_stone",
                "dark_fantasy_boss_old_lair",
                "dark_fantasy_boss_beast_threshold",
                "dark_fantasy_boss_black_victory_trace",
                "dark_fantasy_boss_fallen_power_site");
        addDarkDominant(patterns, PlaceType.BOSS_SITE, bossConstraints("minecraft:ender_dragon"),
                "dark_fantasy_boss_dragonfall",
                "dark_fantasy_boss_dragon_fall",
                "dark_fantasy_boss_fallen_dragon_threshold",
                "dark_fantasy_boss_fallen_dragon_throne",
                "dark_fantasy_boss_dragonfall_ground",
                "dark_fantasy_boss_dragon_rest",
                "dark_fantasy_boss_dragon_trace",
                "dark_fantasy_boss_fallen_dragon_edge",
                "dark_fantasy_boss_dragon_ash",
                "dark_fantasy_boss_dragon_void",
                "dark_fantasy_boss_dragon_bone",
                "dark_fantasy_boss_void_dragon_throne",
                "dark_fantasy_boss_last_wing_shadow",
                "dark_fantasy_boss_dragon_black_rest");
        addDarkDominant(patterns, PlaceType.BOSS_SITE, bossConstraints("minecraft:wither"),
                "dark_fantasy_boss_skullfall_hollow",
                "dark_fantasy_boss_witherfall",
                "dark_fantasy_boss_wither_fall",
                "dark_fantasy_boss_blackened_ground",
                "dark_fantasy_boss_black_skull_ground",
                "dark_fantasy_boss_withering_site",
                "dark_fantasy_boss_wither_trace",
                "dark_fantasy_boss_wither_ash",
                "dark_fantasy_boss_wither_shadow",
                "dark_fantasy_boss_black_hollow",
                "dark_fantasy_boss_skull_ash",
                "dark_fantasy_boss_withered_throne",
                "dark_fantasy_boss_three_skull_mark",
                "dark_fantasy_boss_decay_ground");
        addDarkDominant(patterns, PlaceType.BOSS_SITE, bossConstraints("minecraft:warden"),
                "dark_fantasy_boss_silent_deep",
                "dark_fantasy_boss_warden_silence",
                "dark_fantasy_boss_sculk_depths",
                "dark_fantasy_boss_deep_guardian_trace",
                "dark_fantasy_boss_black_silence",
                "dark_fantasy_boss_soundless_field",
                "dark_fantasy_boss_sculk_grave",
                "dark_fantasy_boss_soundless_place");
        addDarkDominant(patterns, PlaceType.BOSS_SITE, bossConstraints("minecraft:elder_guardian"),
                "dark_fantasy_boss_guardian_waters",
                "dark_fantasy_boss_temple_deep",
                "dark_fantasy_boss_elder_wave_fall",
                "dark_fantasy_boss_elder_guardian_site",
                "dark_fantasy_boss_temple_shadow",
                "dark_fantasy_boss_guardian_deep",
                "dark_fantasy_boss_elder_eye_water");

        addDarkPatterns(patterns, PlaceType.RAID_SITE,
                "dark_fantasy_raid_defense_site",
                "dark_fantasy_raid_bloodied_line",
                "dark_fantasy_raid_field_defense",
                "dark_fantasy_raid_broken_banner",
                "dark_fantasy_raid_repelled_site",
                "dark_fantasy_raid_village_shadow",
                "dark_fantasy_raid_victory_banner",
                "dark_fantasy_raid_ash");
        addDarkPatterns(patterns, PlaceType.PET_MEMORIAL,
                "dark_fantasy_pet_rest",
                "dark_fantasy_pet_memory",
                "dark_fantasy_pet_last_trail",
                "dark_fantasy_pet_quiet_trace",
                "dark_fantasy_pet_loyal_memory",
                "dark_fantasy_pet_dark_rest");
        addDarkPatterns(patterns, PlaceType.NAMED_MOB_MEMORIAL,
                "dark_fantasy_named_memory",
                "dark_fantasy_named_fall",
                "dark_fantasy_named_last_trace",
                "dark_fantasy_named_place",
                "dark_fantasy_named_dark_mark",
                "dark_fantasy_named_remembered_trace");
        addDarkPatterns(patterns, PlaceType.DIMENSION_THRESHOLD,
                "dark_fantasy_dimension_threshold_worlds",
                "dark_fantasy_dimension_dark_border",
                "dark_fantasy_dimension_worldbound_trace",
                "dark_fantasy_dimension_path_between_worlds",
                "dark_fantasy_dimension_border",
                "dark_fantasy_dimension_transition_threshold",
                "dark_fantasy_dimension_betweenworld_shadow");
        addDarkPatterns(patterns, PlaceType.CUSTOM,
                "dark_fantasy_custom_remembered_place",
                "dark_fantasy_custom_named_shadow",
                "dark_fantasy_custom_place_memory",
                "dark_fantasy_custom_old_place",
                "dark_fantasy_custom_dark_mark",
                "dark_fantasy_custom_remembered_trace",
                "dark_fantasy_custom_forgotten_place",
                "dark_fantasy_custom_dark_place",
                "dark_fantasy_custom_forgotten_trace",
                "dark_fantasy_custom_quiet_shadow",
                "dark_fantasy_custom_memory_threshold",
                "dark_fantasy_custom_black_trace",
                "dark_fantasy_custom_old_mark",
                "dark_fantasy_custom_oblivion_place",
                "dark_fantasy_custom_shadow_memory",
                "dark_fantasy_custom_trace_dark",
                "dark_fantasy_custom_nameless_mark",
                "dark_fantasy_custom_gloom_memory",
                "dark_fantasy_custom_old_shadow",
                "dark_fantasy_custom_quiet_place",
                "dark_fantasy_custom_oblivion_trace",
                "dark_fantasy_custom_black_mark",
                "dark_fantasy_custom_grim_trace",
                "dark_fantasy_custom_remembered_dark",
                "dark_fantasy_custom_forgotten_mark",
                "dark_fantasy_custom_old_threshold",
                "dark_fantasy_custom_nameless_shadow");

        return new NameDataPack(NameStyle.DARK_FANTASY.idString(), List.copyOf(patterns), smallStyleTokens());
    }

    private static void addDarkPatterns(List<NamePattern> patterns, PlaceType placeType, String... ids) {
        for (int index = 0; index < ids.length; index++) {
            patterns.add(darkPattern(ids[index], placeType, darkWeight(index), Set.of(), NameCauseConstraints.none(), "place_type"));
        }
    }

    private static void addDarkDeath(List<NamePattern> patterns, DeathSiteEnvironment environment, String... ids) {
        for (int index = 0; index < ids.length; index++) {
            patterns.add(darkPattern(ids[index], PlaceType.DEATH_SITE, darkWeight(index), Set.of(environment), NameCauseConstraints.none(), environment.idString()));
        }
    }

    private static void addDarkExactFirst(List<NamePattern> patterns, String firstDiscoveryKey, String... ids) {
        NameCauseConstraints constraints = NameCauseConstraints.builder()
                .requiredFirstDiscoveryKey(firstDiscoveryKey)
                .build();
        for (int index = 0; index < ids.length; index++) {
            patterns.add(darkPattern(ids[index], PlaceType.FIRST_DISCOVERY, darkWeight(index), Set.of(), constraints, "exact_cause"));
        }
    }

    private static void addDarkDominant(List<NamePattern> patterns, PlaceType placeType, NameCauseConstraints constraints, String... ids) {
        for (int index = 0; index < ids.length; index++) {
            patterns.add(darkPattern(ids[index], placeType, darkWeight(index), Set.of(), constraints, "dominant_target"));
        }
    }

    private static void addDarkBiome(List<NamePattern> patterns, String biomeGroup, String... ids) {
        NameCauseConstraints constraints = NameCauseConstraints.builder()
                .requiredCauseType(PlaceCauseType.VISITS)
                .requiredBiomeGroups(biomeGroup)
                .build();
        for (int index = 0; index < ids.length; index++) {
            patterns.add(darkPattern(ids[index], PlaceType.GENERAL_LANDMARK, darkWeight(index), Set.of(), constraints, "biome", biomeGroup));
        }
    }

    private static NamePattern darkPattern(
            String id,
            PlaceType placeType,
            double weight,
            Set<DeathSiteEnvironment> environments,
            NameCauseConstraints constraints,
            String... tags
    ) {
        return new NamePattern(
                id,
                "living_legends.name.pattern." + id,
                Set.of(placeType),
                environments,
                weight,
                List.of(),
                darkTags(tags),
                NameSemanticRoots.inferPatternRoot(id),
                constraints,
                Set.of()
        );
    }

    private static Set<String> darkTags(String... tags) {
        Set<String> result = new LinkedHashSet<>();
        result.add(NameStyle.DARK_FANTASY.idString());
        if (tags != null) {
            for (String tag : tags) {
                String normalized = WorldPos.optionalId(tag);
                if (!normalized.isBlank()) {
                    result.add(normalized);
                }
            }
        }
        return Set.copyOf(result);
    }

    private static double darkWeight(int index) {
        if (index < 6) {
            return 10.0;
        }
        if (index < 12) {
            return 5.0;
        }
        return 2.0;
    }

    private static NameDataPack cozySurvivalPack() {
        List<NamePattern> patterns = new ArrayList<>();

        addCozyDeath(patterns, DeathSiteEnvironment.SURFACE,
                "cozy_survival_surface_quiet_memory",
                "cozy_survival_surface_place_memory",
                "cozy_survival_surface_last_rest",
                "cozy_survival_surface_gentle_trace",
                "cozy_survival_surface_quiet_meadow",
                "cozy_survival_surface_fallen_rest",
                "cozy_survival_surface_last_trail",
                "cozy_survival_surface_soft_ground",
                "cozy_survival_surface_memory_hill",
                "cozy_survival_surface_old_trace",
                "cozy_survival_surface_meadow_memory",
                "cozy_survival_surface_warm_mark",
                "cozy_survival_surface_memory_clearing",
                "cozy_survival_surface_lantern_trace",
                "cozy_survival_surface_memory_path",
                "cozy_survival_surface_little_rest",
                "cozy_survival_surface_quiet_clearing",
                "cozy_survival_surface_roadside_trace",
                "cozy_survival_surface_flower_hill",
                "cozy_survival_surface_remembered_meadow",
                "cozy_survival_surface_warm_clearing",
                "cozy_survival_surface_last_lantern",
                "cozy_survival_surface_soft_rest",
                "cozy_survival_surface_bright_grass",
                "cozy_survival_surface_kind_trace",
                "cozy_survival_surface_old_rest",
                "cozy_survival_surface_last_steps_meadow");
        addCozyDeath(patterns, DeathSiteEnvironment.CAVE,
                "cozy_survival_cave_quiet_cave",
                "cozy_survival_cave_depths_rest",
                "cozy_survival_cave_soft_echo",
                "cozy_survival_cave_last_torch",
                "cozy_survival_cave_deep_rest",
                "cozy_survival_cave_memory",
                "cozy_survival_cave_warm_light",
                "cozy_survival_cave_old_torch",
                "cozy_survival_cave_depth_rest",
                "cozy_survival_cave_memory_echo",
                "cozy_survival_cave_warm_torch",
                "cozy_survival_cave_lantern_deep",
                "cozy_survival_cave_footpath",
                "cozy_survival_cave_quiet_torch",
                "cozy_survival_cave_stone_rest",
                "cozy_survival_cave_light",
                "cozy_survival_cave_memory_beneath",
                "cozy_survival_cave_torch_trace",
                "cozy_survival_cave_deep_trace",
                "cozy_survival_cave_little_light",
                "cozy_survival_cave_old_lantern",
                "cozy_survival_cave_path_under_stone",
                "cozy_survival_cave_rest_beneath",
                "cozy_survival_cave_bright_nook",
                "cozy_survival_cave_quiet_corner",
                "cozy_survival_cave_memory_nook");
        addCozyDeath(patterns, DeathSiteEnvironment.WATER,
                "cozy_survival_water_quiet_water",
                "cozy_survival_water_last_crossing",
                "cozy_survival_water_memory",
                "cozy_survival_water_memory_pool",
                "cozy_survival_water_quiet_shore",
                "cozy_survival_water_waterside_rest",
                "cozy_survival_water_bright_shore",
                "cozy_survival_water_soft_water",
                "cozy_survival_water_reed_trace",
                "cozy_survival_water_shore_memory",
                "cozy_survival_water_quiet_brook",
                "cozy_survival_water_waterside_path",
                "cozy_survival_water_trace",
                "cozy_survival_water_last_steps_pool",
                "cozy_survival_water_memory_shore",
                "cozy_survival_water_memory_bridge");
        addCozyDeath(patterns, DeathSiteEnvironment.MOUNTAIN,
                "cozy_survival_mountain_last_ledge",
                "cozy_survival_mountain_quiet_summit",
                "cozy_survival_mountain_memory",
                "cozy_survival_mountain_stone_trace",
                "cozy_survival_mountain_wind_rest",
                "cozy_survival_mountain_cold_rest",
                "cozy_survival_mountain_rock_trace",
                "cozy_survival_mountain_rest",
                "cozy_survival_mountain_windy_rest",
                "cozy_survival_mountain_stone_footpath",
                "cozy_survival_mountain_summit_memory",
                "cozy_survival_mountain_cold_trace",
                "cozy_survival_mountain_memory_ledge",
                "cozy_survival_mountain_wind_trail",
                "cozy_survival_mountain_old_ascent",
                "cozy_survival_mountain_bright_stone",
                "cozy_survival_mountain_rest_on_stone",
                "cozy_survival_mountain_path",
                "cozy_survival_mountain_cliffside_rest",
                "cozy_survival_mountain_remembered_ledge",
                "cozy_survival_mountain_sky_trace");
        addCozyDeath(patterns, DeathSiteEnvironment.NETHER,
                "cozy_survival_nether_ashen_rest",
                "cozy_survival_nether_warm_ash",
                "cozy_survival_nether_lava_rest",
                "cozy_survival_nether_memory",
                "cozy_survival_nether_fire_rest",
                "cozy_survival_nether_basalt_trace",
                "cozy_survival_nether_warm_threshold",
                "cozy_survival_nether_ashen_footpath",
                "cozy_survival_nether_lava_light",
                "cozy_survival_nether_coal_trace",
                "cozy_survival_nether_blackstone_rest",
                "cozy_survival_nether_warm_lava",
                "cozy_survival_nether_ash_trace",
                "cozy_survival_nether_rest",
                "cozy_survival_nether_ashen_mark",
                "cozy_survival_nether_lava_lantern");
        addCozyDeath(patterns, DeathSiteEnvironment.END,
                "cozy_survival_end_quiet_end",
                "cozy_survival_end_void_rest",
                "cozy_survival_end_pale_memory",
                "cozy_survival_end_last_threshold",
                "cozy_survival_end_soft_silence",
                "cozy_survival_end_trace",
                "cozy_survival_end_edge_rest",
                "cozy_survival_end_silence_threshold",
                "cozy_survival_end_bright_threshold",
                "cozy_survival_end_pale_footpath",
                "cozy_survival_end_quiet_trace",
                "cozy_survival_end_edge_rest_short",
                "cozy_survival_end_white_trace",
                "cozy_survival_end_silence_trail",
                "cozy_survival_end_soft_edge",
                "cozy_survival_end_star_rest");
        addCozyDeath(patterns, DeathSiteEnvironment.UNKNOWN,
                "cozy_survival_death_quiet_memory",
                "cozy_survival_death_last_rest",
                "cozy_survival_death_soft_trace",
                "cozy_survival_death_memory_place");

        addCozyPatterns(patterns, PlaceType.BATTLEFIELD,
                "cozy_survival_battle_old_clash",
                "cozy_survival_battle_field_defense",
                "cozy_survival_battle_memory",
                "cozy_survival_battle_clash_trace",
                "cozy_survival_battle_quiet_battlefield",
                "cozy_survival_battle_defense_site",
                "cozy_survival_battle_old_banner",
                "cozy_survival_battle_resolve_field",
                "cozy_survival_battle_after_trail",
                "cozy_survival_battle_remembered_clash");
        addCozyDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:zombie", "minecraft:zombie_villager", "minecraft:husk", "minecraft:drowned", "group:undead"),
                "cozy_survival_battle_undead_field",
                "cozy_survival_battle_night_clash",
                "cozy_survival_battle_dead_trace",
                "cozy_survival_battle_old_zombie_field",
                "cozy_survival_battle_night_memory",
                "cozy_survival_battle_groans_trail",
                "cozy_survival_battle_old_night_clash",
                "cozy_survival_battle_undead_trace",
                "cozy_survival_battle_after_night_trail",
                "cozy_survival_battle_defense_undead",
                "cozy_survival_battle_old_night_trace");
        addCozyDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:skeleton", "minecraft:stray", "minecraft:bogged", "minecraft:wither_skeleton", "group:skeleton"),
                "cozy_survival_battle_arrow_field",
                "cozy_survival_battle_bone_trail",
                "cozy_survival_battle_archer_trace",
                "cozy_survival_battle_old_volley",
                "cozy_survival_battle_arrow_memory",
                "cozy_survival_battle_quiet_bone_lowland",
                "cozy_survival_battle_quiet_bone_trail",
                "cozy_survival_battle_old_arrow_field",
                "cozy_survival_battle_arrow_lowland",
                "cozy_survival_battle_after_volley_trail",
                "cozy_survival_battle_remembered_bow",
                "cozy_survival_battle_old_bone_trace",
                "cozy_survival_battle_soft_lowland");
        addCozyDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:creeper", "group:explosive"),
                "cozy_survival_battle_creeper_crater",
                "cozy_survival_battle_hissing_field",
                "cozy_survival_battle_soft_crater",
                "cozy_survival_battle_blast_trace",
                "cozy_survival_battle_clash_crater",
                "cozy_survival_battle_repair_trace",
                "cozy_survival_battle_smiling_crater",
                "cozy_survival_battle_after_blast_trail",
                "cozy_survival_battle_old_crater",
                "cozy_survival_battle_repair_memory",
                "cozy_survival_battle_quiet_crater",
                "cozy_survival_battle_powder_trace",
                "cozy_survival_battle_roadside_crater");
        addCozyDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:enderman", "group:end"),
                "cozy_survival_battle_wanderer_trace",
                "cozy_survival_battle_quiet_shadow",
                "cozy_survival_battle_wanderer_trail",
                "cozy_survival_battle_shadow_memory",
                "cozy_survival_battle_twilight_trace");
        addCozyDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:blaze", "minecraft:ghast", "minecraft:piglin", "minecraft:piglin_brute", "minecraft:zombified_piglin", "minecraft:hoglin", "minecraft:zoglin", "group:nether"),
                "cozy_survival_battle_ashen_clash",
                "cozy_survival_battle_fire_field",
                "cozy_survival_battle_warm_mark",
                "cozy_survival_battle_flame_trace",
                "cozy_survival_battle_nether_trail",
                "cozy_survival_battle_ashen_field",
                "cozy_survival_battle_warm_fire_field",
                "cozy_survival_battle_ashen_trail",
                "cozy_survival_battle_nether_footpath",
                "cozy_survival_battle_rest_after_fire",
                "cozy_survival_battle_flame_light",
                "cozy_survival_battle_warm_ash_field",
                "cozy_survival_battle_coal_trace",
                "cozy_survival_battle_fire_memory");
        addCozyDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("group:illager"),
                "cozy_survival_battle_raider_field",
                "cozy_survival_battle_defense_place",
                "cozy_survival_battle_raid_mark",
                "cozy_survival_battle_old_banner_target",
                "cozy_survival_battle_raid_trace",
                "cozy_survival_battle_defender_trail");
        addCozyDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:warden", "group:boss_like"),
                "cozy_survival_battle_quiet_deep",
                "cozy_survival_battle_sculk_mark",
                "cozy_survival_battle_warden_trace",
                "cozy_survival_battle_silent_trail",
                "cozy_survival_battle_deep_field",
                "cozy_survival_battle_silence_clash",
                "cozy_survival_battle_soft_sculk",
                "cozy_survival_battle_silence_trail",
                "cozy_survival_battle_deep_rest",
                "cozy_survival_battle_silent_footpath",
                "cozy_survival_battle_depth_memory",
                "cozy_survival_battle_quiet_sculk",
                "cozy_survival_battle_silence_trace",
                "cozy_survival_battle_rest_beneath");
        addCozyDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:breeze"),
                "cozy_survival_battle_wind_field",
                "cozy_survival_battle_wind_trail",
                "cozy_survival_battle_trial_mark",
                "cozy_survival_battle_light_clash",
                "cozy_survival_battle_whirl_trace");

        addCozyPatterns(patterns, PlaceType.SLAUGHTER_FIELD,
                "cozy_survival_slaughter_shepherd_meadow",
                "cozy_survival_slaughter_herd_trace",
                "cozy_survival_slaughter_old_pasture",
                "cozy_survival_slaughter_herd_trail",
                "cozy_survival_slaughter_hunt_meadow",
                "cozy_survival_slaughter_herd_memory",
                "cozy_survival_slaughter_herd_field",
                "cozy_survival_slaughter_quiet_pasture",
                "cozy_survival_slaughter_pasture_mark",
                "cozy_survival_slaughter_meadow_loss",
                "cozy_survival_slaughter_shepherd_trail",
                "cozy_survival_slaughter_grass_trace",
                "cozy_survival_slaughter_old_pen",
                "cozy_survival_slaughter_meadow_trace",
                "cozy_survival_slaughter_flock_trail_generic",
                "cozy_survival_slaughter_meadow_memory");
        addCozyDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("minecraft:cow", "minecraft:mooshroom", "group:farm_animal"),
                "cozy_survival_slaughter_cattle_meadow",
                "cozy_survival_slaughter_cattle_trace",
                "cozy_survival_slaughter_herd_pasture",
                "cozy_survival_slaughter_quiet_pasture_cow",
                "cozy_survival_slaughter_cow_meadow",
                "cozy_survival_slaughter_herd_memory_cow",
                "cozy_survival_slaughter_milk_trail",
                "cozy_survival_slaughter_pen_trace_cow",
                "cozy_survival_slaughter_warm_meadow_cow",
                "cozy_survival_slaughter_cattle_mark");
        addCozyDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("minecraft:pig"),
                "cozy_survival_slaughter_hog_pasture",
                "cozy_survival_slaughter_pig_trace",
                "cozy_survival_slaughter_piglet_meadow",
                "cozy_survival_slaughter_quiet_pen",
                "cozy_survival_slaughter_boar_pasture",
                "cozy_survival_slaughter_pig_trail",
                "cozy_survival_slaughter_oinking_footpath",
                "cozy_survival_slaughter_warm_pen",
                "cozy_survival_slaughter_little_pig_meadow",
                "cozy_survival_slaughter_trough_trace");
        addCozyDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("minecraft:chicken"),
                "cozy_survival_slaughter_feather_yard",
                "cozy_survival_slaughter_chicken_trace",
                "cozy_survival_slaughter_chicken_meadow",
                "cozy_survival_slaughter_feather_field",
                "cozy_survival_slaughter_feather_trail",
                "cozy_survival_slaughter_quiet_coop",
                "cozy_survival_slaughter_little_coop",
                "cozy_survival_slaughter_feather_footpath",
                "cozy_survival_slaughter_roost_trace",
                "cozy_survival_slaughter_chicken_mark");
        addCozyDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("minecraft:sheep"),
                "cozy_survival_slaughter_sheep_slope",
                "cozy_survival_slaughter_wool_field",
                "cozy_survival_slaughter_flock_trace",
                "cozy_survival_slaughter_soft_meadow",
                "cozy_survival_slaughter_flock_trail");
        addCozyDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("group:civilian"),
                "cozy_survival_slaughter_tragedy_path",
                "cozy_survival_slaughter_place_loss",
                "cozy_survival_slaughter_villager_memory",
                "cozy_survival_slaughter_trader_trace",
                "cozy_survival_slaughter_quiet_square",
                "cozy_survival_slaughter_civilian_memory",
                "cozy_survival_slaughter_village_memory",
                "cozy_survival_slaughter_trader_trail",
                "cozy_survival_slaughter_remembered_market",
                "cozy_survival_slaughter_old_trade");
        addCozyDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("group:companion"),
                "cozy_survival_slaughter_quiet_memory",
                "cozy_survival_slaughter_companion_trace",
                "cozy_survival_slaughter_farewell_site",
                "cozy_survival_slaughter_remembered_trail",
                "cozy_survival_slaughter_bright_trace",
                "cozy_survival_slaughter_friend_rest");

        addCozyPatterns(patterns, PlaceType.PVP_ARENA,
                "cozy_survival_pvp_duel_ring",
                "cozy_survival_pvp_rival_arena",
                "cozy_survival_pvp_duel_site",
                "cozy_survival_pvp_honor_field",
                "cozy_survival_pvp_old_arena",
                "cozy_survival_pvp_rival_trail",
                "cozy_survival_pvp_clash_ground",
                "cozy_survival_pvp_honor_ring",
                "cozy_survival_pvp_duelist_mark",
                "cozy_survival_pvp_friends_arena");

        addCozyPatterns(patterns, PlaceType.MINING_SITE,
                "cozy_survival_mining_ore_vein",
                "cozy_survival_mining_generic_site",
                "cozy_survival_mining_deep_vein",
                "cozy_survival_mining_ore_trace",
                "cozy_survival_mining_lucky_mine",
                "cozy_survival_mining_quiet_mine",
                "cozy_survival_mining_stone_glitter");
        addCozyDominant(patterns, PlaceType.MINING_SITE, blockConstraints("minecraft:diamond_ore", "minecraft:deepslate_diamond_ore"),
                "cozy_survival_mining_diamond_vein",
                "cozy_survival_mining_glittering_vein",
                "cozy_survival_mining_deep_glitter",
                "cozy_survival_mining_diamond_trace",
                "cozy_survival_mining_lucky_vein",
                "cozy_survival_mining_fortune_vein");
        addCozyDominant(patterns, PlaceType.MINING_SITE, blockConstraints("minecraft:emerald_ore", "minecraft:deepslate_emerald_ore"),
                "cozy_survival_mining_emerald_vein",
                "cozy_survival_mining_green_glitter",
                "cozy_survival_mining_emerald_trace",
                "cozy_survival_mining_trader_vein",
                "cozy_survival_mining_lucky_layer");
        addCozyDominant(patterns, PlaceType.MINING_SITE, blockConstraints("minecraft:ancient_debris"),
                "cozy_survival_mining_ancient_debris",
                "cozy_survival_mining_buried_relic",
                "cozy_survival_mining_warm_ash",
                "cozy_survival_mining_netherite_trace",
                "cozy_survival_mining_ancient_vein",
                "cozy_survival_mining_relic_mark");

        addCozyPatterns(patterns, PlaceType.PORTAL_LANDMARK,
                "cozy_survival_portal_warm_gate",
                "cozy_survival_portal_worldbound_path",
                "cozy_survival_portal_threshold_worlds",
                "cozy_survival_portal_old_crossing",
                "cozy_survival_portal_traveler_gate",
                "cozy_survival_portal_bright_threshold",
                "cozy_survival_portal_far_crossing",
                "cozy_survival_portal_remembered_gate",
                "cozy_survival_portal_soft_crossing",
                "cozy_survival_portal_trail_between_worlds",
                "cozy_survival_portal_bright_crossing",
                "cozy_survival_portal_cozy_gate",
                "cozy_survival_portal_old_threshold",
                "cozy_survival_portal_quiet_crossing",
                "cozy_survival_portal_path_through_light",
                "cozy_survival_portal_wanderer_threshold",
                "cozy_survival_portal_soft_gate",
                "cozy_survival_portal_roadside_threshold",
                "cozy_survival_portal_trail_gate",
                "cozy_survival_portal_far_threshold");
        addCozyDominant(patterns, PlaceType.PORTAL_LANDMARK, portalConstraints("nether"),
                "cozy_survival_portal_nether_gate",
                "cozy_survival_portal_warm_crossing",
                "cozy_survival_portal_path_ash",
                "cozy_survival_portal_basalt_threshold",
                "cozy_survival_portal_fire_rest",
                "cozy_survival_portal_lava_gate",
                "cozy_survival_portal_warm_nether_gate");
        addCozyDominant(patterns, PlaceType.PORTAL_LANDMARK, portalConstraints("end"),
                "cozy_survival_portal_last_threshold",
                "cozy_survival_portal_end_gate",
                "cozy_survival_portal_quiet_edge",
                "cozy_survival_portal_endward_path",
                "cozy_survival_portal_bright_threshold_end",
                "cozy_survival_portal_eye_gate",
                "cozy_survival_portal_quiet_gate");

        addCozyPatterns(patterns, PlaceType.GENERAL_LANDMARK,
                "cozy_survival_general_old_footpath",
                "cozy_survival_general_quiet_rest",
                "cozy_survival_general_remembered_trail",
                "cozy_survival_general_many_steps",
                "cozy_survival_general_cozy_trace",
                "cozy_survival_general_old_path",
                "cozy_survival_general_soft_mark",
                "cozy_survival_general_traveler_trail",
                "cozy_survival_general_roadside_rest",
                "cozy_survival_general_sunny_trail",
                "cozy_survival_general_forest_footpath",
                "cozy_survival_general_kind_trace",
                "cozy_survival_general_little_rest",
                "cozy_survival_general_remembered_stop",
                "cozy_survival_general_old_lane",
                "cozy_survival_general_cozy_rest",
                "cozy_survival_general_grass_trace_generic",
                "cozy_survival_general_lantern_stop");
        addCozyBiome(patterns, "plains",
                "cozy_survival_general_meadow_path",
                "cozy_survival_general_plains_rest",
                "cozy_survival_general_grass_trace",
                "cozy_survival_general_sunny_meadow");
        addCozyBiome(patterns, "forest",
                "cozy_survival_general_forest_path",
                "cozy_survival_general_canopy_rest",
                "cozy_survival_general_old_forest_trace",
                "cozy_survival_general_shaded_trail");
        addCozyBiome(patterns, "desert",
                "cozy_survival_general_sand_trace",
                "cozy_survival_general_sand_rest",
                "cozy_survival_general_dry_wind_trail",
                "cozy_survival_general_warm_mark");
        addCozyBiome(patterns, "snowy",
                "cozy_survival_general_snowy_path",
                "cozy_survival_general_quiet_snowdrift",
                "cozy_survival_general_snow_trace",
                "cozy_survival_general_frost_rest");
        addCozyBiome(patterns, "swamp",
                "cozy_survival_general_swamp_path",
                "cozy_survival_general_quiet_pool",
                "cozy_survival_general_reed_trace",
                "cozy_survival_general_soft_mire");
        addCozyBiome(patterns, "cherry_grove",
                "cozy_survival_general_cherry_rest",
                "cozy_survival_general_petal_trail",
                "cozy_survival_general_pink_trace",
                "cozy_survival_general_blossom_rest");

        addCozyPatterns(patterns, PlaceType.SETTLEMENT,
                "cozy_survival_settlement_lived_place",
                "cozy_survival_settlement_old_hearth",
                "cozy_survival_settlement_warm_rest",
                "cozy_survival_settlement_hearth_home",
                "cozy_survival_settlement_quiet_haven",
                "cozy_survival_settlement_roofed_place",
                "cozy_survival_settlement_cozy_hearth",
                "cozy_survival_settlement_bright_home",
                "cozy_survival_settlement_builder_rest",
                "cozy_survival_settlement_home_mark",
                "cozy_survival_settlement_little_hearth",
                "cozy_survival_settlement_roof_light",
                "cozy_survival_settlement_roadside_cottage",
                "cozy_survival_settlement_bright_roof",
                "cozy_survival_settlement_hearth_rest",
                "cozy_survival_settlement_warm_roof",
                "cozy_survival_settlement_cozy_shelter",
                "cozy_survival_settlement_builder_trace",
                "cozy_survival_settlement_kind_hearth",
                "cozy_survival_settlement_lantern_rest",
                "cozy_survival_settlement_quiet_cottage");

        addCozyPatterns(patterns, PlaceType.FIRST_DISCOVERY,
                "cozy_survival_first_find",
                "cozy_survival_first_generic_trace",
                "cozy_survival_first_light_place",
                "cozy_survival_first_quiet_discovery",
                "cozy_survival_first_new_trail",
                "cozy_survival_first_remembered_threshold",
                "cozy_survival_first_generic_record",
                "cozy_survival_first_ray",
                "cozy_survival_first_beginning_path",
                "cozy_survival_first_bright_find",
                "cozy_survival_first_new_footpath",
                "cozy_survival_first_remembered_find",
                "cozy_survival_first_little_light",
                "cozy_survival_first_sign",
                "cozy_survival_first_quiet_threshold",
                "cozy_survival_first_trail_beginning",
                "cozy_survival_first_spark",
                "cozy_survival_first_kind_find");
        addCozyExactFirst(patterns, "world:first_stronghold_found",
                "cozy_survival_first_stronghold",
                "cozy_survival_first_eye_halls",
                "cozy_survival_first_hidden_threshold",
                "cozy_survival_first_quiet_citadel",
                "cozy_survival_first_stone_halls",
                "cozy_survival_first_path_eye",
                "cozy_survival_first_endward_fortress",
                "cozy_survival_first_citadel",
                "cozy_survival_first_lantern_halls",
                "cozy_survival_first_eye_footpath",
                "cozy_survival_first_warm_stone_halls",
                "cozy_survival_first_threshold_rest");
        addCozyExactFirst(patterns, "world:first_diamond_ore_mined",
                "cozy_survival_first_diamond",
                "cozy_survival_first_glitter",
                "cozy_survival_first_diamond_vein",
                "cozy_survival_first_bright_vein",
                "cozy_survival_first_diamond_light",
                "cozy_survival_first_lucky_find",
                "cozy_survival_first_diamond_trace",
                "cozy_survival_first_gem_lantern",
                "cozy_survival_first_shining_find",
                "cozy_survival_first_little_gem");
        addCozyExactFirst(patterns, "world:first_ancient_debris_mined",
                "cozy_survival_first_ancient_debris",
                "cozy_survival_first_buried_relic",
                "cozy_survival_first_warm_ash",
                "cozy_survival_first_netherite_trace",
                "cozy_survival_first_ancient_find",
                "cozy_survival_first_relic_ash",
                "cozy_survival_first_ancient_warmth",
                "cozy_survival_first_ash_lantern",
                "cozy_survival_first_relic_trace",
                "cozy_survival_first_warm_relic");
        addCozyExactFirst(patterns, "world:first_nether_entry",
                "cozy_survival_first_descent",
                "cozy_survival_first_ashen_gate",
                "cozy_survival_first_path_below",
                "cozy_survival_first_warm_crossing",
                "cozy_survival_first_ashen_trace",
                "cozy_survival_first_fire_gate",
                "cozy_survival_first_nether_lantern",
                "cozy_survival_first_warm_nether_gate",
                "cozy_survival_first_ash_footpath",
                "cozy_survival_first_below_threshold");
        addCozyExactFirst(patterns, "world:first_end_entry",
                "cozy_survival_first_last_threshold",
                "cozy_survival_first_pale_crossing",
                "cozy_survival_first_endward_path",
                "cozy_survival_first_quiet_edge",
                "cozy_survival_first_end_trace",
                "cozy_survival_first_silence_gate",
                "cozy_survival_first_end_lantern",
                "cozy_survival_first_pale_footpath",
                "cozy_survival_first_soft_end_threshold",
                "cozy_survival_first_quiet_end_rest");

        addCozyPatterns(patterns, PlaceType.BOSS_SITE,
                "cozy_survival_boss_victory_site",
                "cozy_survival_boss_strong_foe_trace",
                "cozy_survival_boss_old_lair",
                "cozy_survival_boss_clash_memory",
                "cozy_survival_boss_quiet_arena",
                "cozy_survival_boss_great_victory_site",
                "cozy_survival_boss_beast_rest",
                "cozy_survival_boss_victory_trace",
                "cozy_survival_boss_victory_memory",
                "cozy_survival_boss_rest_after_clash",
                "cozy_survival_boss_old_power_trace",
                "cozy_survival_boss_bright_victory_mark",
                "cozy_survival_boss_kind_victory_mark",
                "cozy_survival_boss_lantern_after_clash",
                "cozy_survival_boss_victory_rest",
                "cozy_survival_boss_warm_arena");
        addCozyDominant(patterns, PlaceType.BOSS_SITE, bossConstraints("minecraft:ender_dragon"),
                "cozy_survival_boss_dragon_rest",
                "cozy_survival_boss_dragon_trace",
                "cozy_survival_boss_dragon_memory",
                "cozy_survival_boss_dragon_threshold",
                "cozy_survival_boss_quiet_dragon_edge",
                "cozy_survival_boss_dragon_victory",
                "cozy_survival_boss_old_dragon_trace",
                "cozy_survival_boss_rest_after_flight",
                "cozy_survival_boss_bright_dragon_trace",
                "cozy_survival_boss_dragon_trail",
                "cozy_survival_boss_dragon_landing",
                "cozy_survival_boss_dragon_light");
        addCozyDominant(patterns, PlaceType.BOSS_SITE, bossConstraints("minecraft:wither"),
                "cozy_survival_boss_wither_rest",
                "cozy_survival_boss_wither_trace",
                "cozy_survival_boss_wither_memory",
                "cozy_survival_boss_wither_victory",
                "cozy_survival_boss_quiet_wither_trace",
                "cozy_survival_boss_old_wither_site",
                "cozy_survival_boss_rest_after_storm",
                "cozy_survival_boss_trace_after_clash",
                "cozy_survival_boss_quiet_wither_shadow");
        addCozyDominant(patterns, PlaceType.BOSS_SITE, bossConstraints("minecraft:warden"),
                "cozy_survival_boss_silent_deep",
                "cozy_survival_boss_warden_silence",
                "cozy_survival_boss_deep_guardian_trace",
                "cozy_survival_boss_sculk_depths",
                "cozy_survival_boss_quiet_sculk",
                "cozy_survival_boss_warden_rest",
                "cozy_survival_boss_deep_memory",
                "cozy_survival_boss_silent_rest",
                "cozy_survival_boss_old_warden_trace",
                "cozy_survival_boss_sculk_memory",
                "cozy_survival_boss_warden_rest_beneath");
        addCozyDominant(patterns, PlaceType.BOSS_SITE, bossConstraints("minecraft:elder_guardian"),
                "cozy_survival_boss_guardian_waters",
                "cozy_survival_boss_temple_deep",
                "cozy_survival_boss_elder_wave_memory",
                "cozy_survival_boss_elder_guardian_site",
                "cozy_survival_boss_quiet_temple_deep",
                "cozy_survival_boss_guardian_rest",
                "cozy_survival_boss_temple_memory",
                "cozy_survival_boss_elder_guardian_trace",
                "cozy_survival_boss_soft_temple_water",
                "cozy_survival_boss_temple_threshold",
                "cozy_survival_boss_old_temple_trace");

        addCozyPatterns(patterns, PlaceType.RAID_SITE,
                "cozy_survival_raid_defense_site",
                "cozy_survival_raid_field_defense",
                "cozy_survival_raid_victory_banner",
                "cozy_survival_raid_village_line",
                "cozy_survival_raid_saved_village",
                "cozy_survival_raid_defender_trail",
                "cozy_survival_raid_defense_memory",
                "cozy_survival_raid_village_mark");
        addCozyPatterns(patterns, PlaceType.PET_MEMORIAL,
                "cozy_survival_pet_rest",
                "cozy_survival_pet_memory",
                "cozy_survival_pet_last_trail",
                "cozy_survival_pet_bright_memory",
                "cozy_survival_pet_quiet_rest",
                "cozy_survival_pet_kind_trace",
                "cozy_survival_pet_place",
                "cozy_survival_pet_memory_nook");
        addCozyPatterns(patterns, PlaceType.NAMED_MOB_MEMORIAL,
                "cozy_survival_named_memory",
                "cozy_survival_named_last_trace",
                "cozy_survival_named_place",
                "cozy_survival_named_quiet_mark",
                "cozy_survival_named_remembered_trace",
                "cozy_survival_named_bright_memory");
        addCozyPatterns(patterns, PlaceType.DIMENSION_THRESHOLD,
                "cozy_survival_dimension_threshold_worlds",
                "cozy_survival_dimension_worldbound_trace",
                "cozy_survival_dimension_path_between_worlds",
                "cozy_survival_dimension_border",
                "cozy_survival_dimension_warm_threshold",
                "cozy_survival_dimension_bright_crossing",
                "cozy_survival_dimension_worlds_trail");
        addCozyPatterns(patterns, PlaceType.CUSTOM,
                "cozy_survival_custom_remembered_place",
                "cozy_survival_custom_named_place",
                "cozy_survival_custom_place_memory",
                "cozy_survival_custom_old_place",
                "cozy_survival_custom_quiet_mark",
                "cozy_survival_custom_remembered_trace",
                "cozy_survival_custom_cozy_place",
                "cozy_survival_custom_kind_trail",
                "cozy_survival_custom_little_trace",
                "cozy_survival_custom_quiet_corner",
                "cozy_survival_custom_warm_mark",
                "cozy_survival_custom_remembered_lane",
                "cozy_survival_custom_bright_place",
                "cozy_survival_custom_soft_trace",
                "cozy_survival_custom_old_corner",
                "cozy_survival_custom_kind_mark");

        return new NameDataPack(NameStyle.COZY_SURVIVAL.idString(), List.copyOf(patterns), smallStyleTokens());
    }

    private static void addCozyPatterns(List<NamePattern> patterns, PlaceType placeType, String... ids) {
        for (int index = 0; index < ids.length; index++) {
            patterns.add(cozyPattern(ids[index], placeType, cozyWeight(index), Set.of(), NameCauseConstraints.none(), "place_type"));
        }
    }

    private static void addCozyDeath(List<NamePattern> patterns, DeathSiteEnvironment environment, String... ids) {
        for (int index = 0; index < ids.length; index++) {
            patterns.add(cozyPattern(ids[index], PlaceType.DEATH_SITE, cozyWeight(index), Set.of(environment), NameCauseConstraints.none(), environment.idString()));
        }
    }

    private static void addCozyExactFirst(List<NamePattern> patterns, String firstDiscoveryKey, String... ids) {
        NameCauseConstraints constraints = NameCauseConstraints.builder()
                .requiredFirstDiscoveryKey(firstDiscoveryKey)
                .build();
        for (int index = 0; index < ids.length; index++) {
            patterns.add(cozyPattern(ids[index], PlaceType.FIRST_DISCOVERY, cozyWeight(index), Set.of(), constraints, "exact_cause"));
        }
    }

    private static void addCozyDominant(List<NamePattern> patterns, PlaceType placeType, NameCauseConstraints constraints, String... ids) {
        for (int index = 0; index < ids.length; index++) {
            patterns.add(cozyPattern(ids[index], placeType, cozyWeight(index), Set.of(), constraints, "dominant_target"));
        }
    }

    private static void addCozyBiome(List<NamePattern> patterns, String biomeGroup, String... ids) {
        NameCauseConstraints constraints = NameCauseConstraints.builder()
                .requiredCauseType(PlaceCauseType.VISITS)
                .requiredBiomeGroups(biomeGroup)
                .build();
        for (int index = 0; index < ids.length; index++) {
            patterns.add(cozyPattern(ids[index], PlaceType.GENERAL_LANDMARK, cozyWeight(index), Set.of(), constraints, "biome", biomeGroup));
        }
    }

    private static NamePattern cozyPattern(
            String id,
            PlaceType placeType,
            double weight,
            Set<DeathSiteEnvironment> environments,
            NameCauseConstraints constraints,
            String... tags
    ) {
        return new NamePattern(
                id,
                "living_legends.name.pattern." + id,
                Set.of(placeType),
                environments,
                weight,
                List.of(),
                cozyTags(tags),
                NameSemanticRoots.inferPatternRoot(id),
                constraints,
                Set.of()
        );
    }

    private static Set<String> cozyTags(String... tags) {
        Set<String> result = new LinkedHashSet<>();
        result.add(NameStyle.COZY_SURVIVAL.idString());
        if (tags != null) {
            for (String tag : tags) {
                String normalized = WorldPos.optionalId(tag);
                if (!normalized.isBlank()) {
                    result.add(normalized);
                }
            }
        }
        return Set.copyOf(result);
    }

    private static double cozyWeight(int index) {
        if (index < 6) {
            return 10.0;
        }
        if (index < 12) {
            return 5.0;
        }
        return 2.0;
    }

    private static NameDataPack epicMythologyPack() {
        List<NamePattern> patterns = new ArrayList<>();

        addEpicDeath(patterns, DeathSiteEnvironment.SURFACE,
                "epic_mythology_surface_field_fallen",
                "epic_mythology_surface_ground_oath",
                "epic_mythology_surface_last_path_trace",
                "epic_mythology_surface_meadow_memory",
                "epic_mythology_surface_crown_fallen",
                "epic_mythology_surface_last_rest",
                "epic_mythology_surface_field_fate",
                "epic_mythology_surface_last_steps_ground",
                "epic_mythology_surface_memory_stone",
                "epic_mythology_surface_fallen_trail",
                "epic_mythology_surface_heroes_rest",
                "epic_mythology_surface_last_oath_hill",
                "epic_mythology_surface_last_light_memory",
                "epic_mythology_surface_departed_trace",
                "epic_mythology_surface_old_saga_field",
                "epic_mythology_surface_old_saga_ground",
                "epic_mythology_surface_fallen_hill",
                "epic_mythology_surface_last_song_stone",
                "epic_mythology_surface_fate_trace",
                "epic_mythology_surface_departed_field",
                "epic_mythology_surface_remembered_ground",
                "epic_mythology_surface_last_oath_meadow",
                "epic_mythology_surface_last_light_rest",
                "epic_mythology_surface_last_star_trace",
                "epic_mythology_surface_saga_clearing",
                "epic_mythology_surface_last_song_ground",
                "epic_mythology_surface_last_path_crown",
                "epic_mythology_surface_old_oath_trail",
                "epic_mythology_surface_departed_memory");
        addEpicDeath(patterns, DeathSiteEnvironment.CAVE,
                "epic_mythology_cave_halls_depth",
                "epic_mythology_cave_oath_hollow",
                "epic_mythology_cave_echo_fallen",
                "epic_mythology_cave_deep_threshold",
                "epic_mythology_cave_last_light",
                "epic_mythology_cave_stone_memory",
                "epic_mythology_cave_throne_beneath",
                "epic_mythology_cave_path_under_stone",
                "epic_mythology_cave_lost_picks_hall",
                "epic_mythology_cave_depth_rest",
                "epic_mythology_cave_underground_saga",
                "epic_mythology_cave_fate",
                "epic_mythology_cave_last_torch_light",
                "epic_mythology_cave_memory_rift",
                "epic_mythology_cave_last_oath_hall",
                "epic_mythology_cave_stone_threshold",
                "epic_mythology_cave_path_beneath",
                "epic_mythology_cave_last_saga_echo",
                "epic_mythology_cave_underground_crown",
                "epic_mythology_cave_sunless_hall",
                "epic_mythology_cave_old_song",
                "epic_mythology_cave_memory_under_stone",
                "epic_mythology_cave_deep_saga",
                "epic_mythology_cave_depth_trace",
                "epic_mythology_cave_underground_oath_stone",
                "epic_mythology_cave_depths_throne",
                "epic_mythology_cave_threshold_under_stone",
                "epic_mythology_cave_crown");
        addEpicDeath(patterns, DeathSiteEnvironment.WATER,
                "epic_mythology_water_drowned_pool",
                "epic_mythology_water_last_crossing",
                "epic_mythology_water_oath",
                "epic_mythology_water_memory_shore",
                "epic_mythology_water_last_path_sea",
                "epic_mythology_water_quiet_crown",
                "epic_mythology_water_wave_threshold",
                "epic_mythology_water_shore_trace",
                "epic_mythology_water_wave_rest",
                "epic_mythology_water_fate_crossing",
                "epic_mythology_water_wave_crown",
                "epic_mythology_water_last_song_shore",
                "epic_mythology_water_threshold",
                "epic_mythology_water_water_trace",
                "epic_mythology_water_oath_crossing",
                "epic_mythology_water_old_saga_pool",
                "epic_mythology_water_single_wave_rest",
                "epic_mythology_water_fate_shore",
                "epic_mythology_water_memory",
                "epic_mythology_water_waves_trail",
                "epic_mythology_water_shore_stone",
                "epic_mythology_water_last_shore");
        addEpicDeath(patterns, DeathSiteEnvironment.MOUNTAIN,
                "epic_mythology_mountain_last_ledge",
                "epic_mythology_mountain_fallen_summit",
                "epic_mythology_mountain_wind_trail",
                "epic_mythology_mountain_oath_rock",
                "epic_mythology_mountain_last_step_stone",
                "epic_mythology_mountain_summit_crown",
                "epic_mythology_mountain_cloud_path",
                "epic_mythology_mountain_wind_rest",
                "epic_mythology_mountain_rock_trace",
                "epic_mythology_mountain_fate_hill",
                "epic_mythology_mountain_last_saga_cliff",
                "epic_mythology_mountain_memory",
                "epic_mythology_mountain_rock_crown",
                "epic_mythology_mountain_oath",
                "epic_mythology_mountain_cloud_trace",
                "epic_mythology_mountain_wind_stone",
                "epic_mythology_mountain_last_height_trail",
                "epic_mythology_mountain_summit_threshold",
                "epic_mythology_mountain_old_saga",
                "epic_mythology_mountain_memory_rock",
                "epic_mythology_mountain_cold_star_trace",
                "epic_mythology_mountain_cloud_rest",
                "epic_mythology_mountain_summit_fate",
                "epic_mythology_mountain_fallen_stone");
        addEpicDeath(patterns, DeathSiteEnvironment.NETHER,
                "epic_mythology_nether_ash_crown",
                "epic_mythology_nether_ashen_rest",
                "epic_mythology_nether_fire_threshold",
                "epic_mythology_nether_flame_trace",
                "epic_mythology_nether_basalt_oath",
                "epic_mythology_nether_rest",
                "epic_mythology_nether_ash_trail",
                "epic_mythology_nether_fire_memory",
                "epic_mythology_nether_ashen_saga",
                "epic_mythology_nether_lava_crown",
                "epic_mythology_nether_flame_stone",
                "epic_mythology_nether_ashen_realm_threshold");
        addEpicDeath(patterns, DeathSiteEnvironment.END,
                "epic_mythology_end_void_rest",
                "epic_mythology_end_last_threshold",
                "epic_mythology_end_pale_crown",
                "epic_mythology_end_edge_trace",
                "epic_mythology_end_last_light_silence",
                "epic_mythology_end_edge_memory",
                "epic_mythology_end_void_threshold",
                "epic_mythology_end_white_trace",
                "epic_mythology_end_void_saga",
                "epic_mythology_end_last_star_rest",
                "epic_mythology_end_edge_trail",
                "epic_mythology_end_silence_crown",
                "epic_mythology_end_last_star_threshold",
                "epic_mythology_end_pale_saga",
                "epic_mythology_end_void_trace",
                "epic_mythology_end_edge_crown",
                "epic_mythology_end_last_sky_memory",
                "epic_mythology_end_white_trail",
                "epic_mythology_end_pale_light_rest",
                "epic_mythology_end_old_saga_silence",
                "epic_mythology_end_last_threshold_trace",
                "epic_mythology_end_void_trail",
                "epic_mythology_end_last_star_memory",
                "epic_mythology_end_pale_path");
        addEpicDeath(patterns, DeathSiteEnvironment.UNKNOWN,
                "epic_mythology_death_field_fallen",
                "epic_mythology_death_ground_oath",
                "epic_mythology_death_last_path_trace",
                "epic_mythology_death_memory_stone",
                "epic_mythology_death_fallen_trail",
                "epic_mythology_death_heroes_rest",
                "epic_mythology_death_field_fate",
                "epic_mythology_death_old_saga_field",
                "epic_mythology_death_departed_trace",
                "epic_mythology_death_last_rest");

        addEpicPatterns(patterns, PlaceType.BATTLEFIELD,
                "epic_mythology_battle_oath_field",
                "epic_mythology_battle_banner",
                "epic_mythology_battle_clash_ground",
                "epic_mythology_battle_fallen_banners",
                "epic_mythology_battle_old_war_trace",
                "epic_mythology_battle_honor_ring",
                "epic_mythology_battle_fate_field",
                "epic_mythology_battle_victory_trail",
                "epic_mythology_battle_great_clash",
                "epic_mythology_battle_blades_saga",
                "epic_mythology_battle_shield_ground",
                "epic_mythology_battle_crown",
                "epic_mythology_battle_resolve_field",
                "epic_mythology_battle_old_banner");
        addEpicDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:zombie", "minecraft:zombie_villager", "minecraft:husk", "minecraft:drowned", "group:undead"),
                "epic_mythology_battle_undead_field",
                "epic_mythology_battle_dead_ground",
                "epic_mythology_battle_night_saga",
                "epic_mythology_battle_old_undead_trace",
                "epic_mythology_battle_night_oath",
                "epic_mythology_battle_groans_field",
                "epic_mythology_battle_dead_trail",
                "epic_mythology_battle_night_banner",
                "epic_mythology_battle_undead_memory",
                "epic_mythology_battle_sleepless_field");
        addEpicDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:skeleton", "minecraft:stray", "minecraft:bogged", "minecraft:wither_skeleton", "group:skeleton"),
                "epic_mythology_battle_arrow_field",
                "epic_mythology_battle_bone_crown",
                "epic_mythology_battle_fate_volley",
                "epic_mythology_battle_archer_trace",
                "epic_mythology_battle_arrow_banner",
                "epic_mythology_battle_bone_saga",
                "epic_mythology_battle_white_bones_trail",
                "epic_mythology_battle_old_volley_memory",
                "epic_mythology_battle_fate_bow",
                "epic_mythology_battle_archer_field");
        addEpicDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:creeper", "group:explosive"),
                "epic_mythology_battle_fate_crater",
                "epic_mythology_battle_creeper_crater",
                "epic_mythology_battle_hissing_field",
                "epic_mythology_battle_blast_mark",
                "epic_mythology_battle_oath_crater",
                "epic_mythology_battle_powder_trace",
                "epic_mythology_battle_rupture_field",
                "epic_mythology_battle_old_saga_crater",
                "epic_mythology_battle_thunder_mark",
                "epic_mythology_battle_after_blast_trail");
        addEpicDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:enderman", "group:end"),
                "epic_mythology_battle_shadow_field",
                "epic_mythology_battle_wanderer_trace",
                "epic_mythology_battle_void_trail",
                "epic_mythology_battle_shadow_memory",
                "epic_mythology_battle_twilight_oath",
                "epic_mythology_battle_shadow_crown",
                "epic_mythology_battle_wanderer_field",
                "epic_mythology_battle_silent_trace",
                "epic_mythology_battle_shadow_banner",
                "epic_mythology_battle_wanderer_threshold");
        addEpicDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:blaze", "minecraft:ghast", "minecraft:piglin", "minecraft:piglin_brute", "minecraft:zombified_piglin", "minecraft:hoglin", "minecraft:zoglin", "group:nether"),
                "epic_mythology_battle_ashen_clash",
                "epic_mythology_battle_flame_field",
                "epic_mythology_battle_ash_trace",
                "epic_mythology_battle_basalt_oath",
                "epic_mythology_battle_nether_ground",
                "epic_mythology_battle_flame_saga",
                "epic_mythology_battle_fire_crown",
                "epic_mythology_battle_ash_threshold",
                "epic_mythology_battle_fire_trail");
        addEpicDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("group:illager", "minecraft:pillager", "minecraft:vindicator", "minecraft:evoker", "minecraft:ravager"),
                "epic_mythology_battle_raid_banner",
                "epic_mythology_battle_raider_field",
                "epic_mythology_battle_defense_line",
                "epic_mythology_battle_defenders_saga",
                "epic_mythology_battle_victory_banner",
                "epic_mythology_battle_raid_trace",
                "epic_mythology_battle_repelled_raid_field",
                "epic_mythology_battle_village_oath",
                "epic_mythology_battle_resolve_line",
                "epic_mythology_battle_broken_raid_site");
        addEpicDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:warden", "group:boss_like"),
                "epic_mythology_battle_warden_silence",
                "epic_mythology_battle_sculk_threshold",
                "epic_mythology_battle_deep_oath",
                "epic_mythology_battle_guardian_trace",
                "epic_mythology_battle_soundless_field",
                "epic_mythology_battle_depth_rest",
                "epic_mythology_battle_sculk_saga",
                "epic_mythology_battle_silence_threshold",
                "epic_mythology_battle_soundless_ground",
                "epic_mythology_battle_warden_trail");
        addEpicDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:breeze"),
                "epic_mythology_battle_wind_field",
                "epic_mythology_battle_whirling_oath",
                "epic_mythology_battle_trial_mark",
                "epic_mythology_battle_wind_saga",
                "epic_mythology_battle_trial_hall",
                "epic_mythology_battle_whirl_trace",
                "epic_mythology_battle_light_thunder_field");

        addEpicPatterns(patterns, PlaceType.SLAUGHTER_FIELD,
                "epic_mythology_slaughter_herd_field",
                "epic_mythology_slaughter_oath_pasture",
                "epic_mythology_slaughter_hunt_trace",
                "epic_mythology_slaughter_herd_saga",
                "epic_mythology_slaughter_herd_trail",
                "epic_mythology_slaughter_old_pasture",
                "epic_mythology_slaughter_loss_meadow",
                "epic_mythology_slaughter_hunt_mark",
                "epic_mythology_slaughter_shepherd_path",
                "epic_mythology_slaughter_herd_memory",
                "epic_mythology_slaughter_fate_pasture",
                "epic_mythology_slaughter_pasture_trace");
        addEpicDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("minecraft:cow", "minecraft:mooshroom", "group:farm_animal"),
                "epic_mythology_slaughter_cattle_meadow",
                "epic_mythology_slaughter_herd_trace",
                "epic_mythology_slaughter_herd_pasture",
                "epic_mythology_slaughter_herd_saga_cow",
                "epic_mythology_slaughter_cattle_path",
                "epic_mythology_slaughter_herd_memory_cow",
                "epic_mythology_slaughter_pasture_crown",
                "epic_mythology_slaughter_cow_meadow",
                "epic_mythology_slaughter_shepherd_trace",
                "epic_mythology_slaughter_herd_field_cow");
        addEpicDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("minecraft:pig"),
                "epic_mythology_slaughter_hog_pasture",
                "epic_mythology_slaughter_pig_trace",
                "epic_mythology_slaughter_boar_pasture",
                "epic_mythology_slaughter_pig_saga",
                "epic_mythology_slaughter_boar_trail",
                "epic_mythology_slaughter_pig_path",
                "epic_mythology_slaughter_pasture_mark",
                "epic_mythology_slaughter_old_pen",
                "epic_mythology_slaughter_boar_field");
        addEpicDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("minecraft:chicken"),
                "epic_mythology_slaughter_feather_field",
                "epic_mythology_slaughter_feather_saga",
                "epic_mythology_slaughter_chicken_trace",
                "epic_mythology_slaughter_feather_trail",
                "epic_mythology_slaughter_feather_yard",
                "epic_mythology_slaughter_chicken_mark",
                "epic_mythology_slaughter_feather_banner",
                "epic_mythology_slaughter_light_trace",
                "epic_mythology_slaughter_feather_court",
                "epic_mythology_slaughter_chicken_memory");
        addEpicDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("minecraft:sheep"),
                "epic_mythology_slaughter_wool_field",
                "epic_mythology_slaughter_sheep_slope",
                "epic_mythology_slaughter_sheep_pasture",
                "epic_mythology_slaughter_flock_saga",
                "epic_mythology_slaughter_soft_meadow",
                "epic_mythology_slaughter_sheep_trail",
                "epic_mythology_slaughter_wool_memory",
                "epic_mythology_slaughter_flock_meadow",
                "epic_mythology_slaughter_shepherd_trace_sheep");
        addEpicDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("group:civilian", "minecraft:villager", "minecraft:wandering_trader"),
                "epic_mythology_slaughter_tragedy_path",
                "epic_mythology_slaughter_villager_memory",
                "epic_mythology_slaughter_trader_trace",
                "epic_mythology_slaughter_civilian_oath",
                "epic_mythology_slaughter_memory_square",
                "epic_mythology_slaughter_trader_saga",
                "epic_mythology_slaughter_village_trace",
                "epic_mythology_slaughter_market_memory",
                "epic_mythology_slaughter_loss_trail",
                "epic_mythology_slaughter_village_saga");
        addEpicDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("group:companion"),
                "epic_mythology_slaughter_companion_memory",
                "epic_mythology_slaughter_friend_trace",
                "epic_mythology_slaughter_bright_oath",
                "epic_mythology_slaughter_farewell_trail",
                "epic_mythology_slaughter_friend_rest",
                "epic_mythology_slaughter_loyal_trace",
                "epic_mythology_slaughter_loyal_memory",
                "epic_mythology_slaughter_farewell_site");

        addEpicPatterns(patterns, PlaceType.PVP_ARENA,
                "epic_mythology_pvp_honor_ring",
                "epic_mythology_pvp_oath_arena",
                "epic_mythology_pvp_rival_field",
                "epic_mythology_pvp_duelist_ring",
                "epic_mythology_pvp_duel_site",
                "epic_mythology_pvp_honor_banner",
                "epic_mythology_pvp_fate_arena",
                "epic_mythology_pvp_rival_trace",
                "epic_mythology_pvp_duelist_trail",
                "epic_mythology_pvp_clash_crown",
                "epic_mythology_pvp_old_honor_field",
                "epic_mythology_pvp_victory_ring");

        addEpicPatterns(patterns, PlaceType.MINING_SITE,
                "epic_mythology_mining_ore_vein",
                "epic_mythology_mining_fate_vein",
                "epic_mythology_mining_deep_trace",
                "epic_mythology_mining_stone_saga",
                "epic_mythology_mining_ore_trace",
                "epic_mythology_mining_memory_layer",
                "epic_mythology_mining_ore_crown",
                "epic_mythology_mining_deep_mark",
                "epic_mythology_mining_stone_path",
                "epic_mythology_mining_old_earth_vein");
        addEpicDominant(patterns, PlaceType.MINING_SITE, blockConstraints("minecraft:diamond_ore", "minecraft:deepslate_diamond_ore"),
                "epic_mythology_mining_diamond_vein",
                "epic_mythology_mining_first_light_vein",
                "epic_mythology_mining_diamond_crown",
                "epic_mythology_mining_diamond_trace",
                "epic_mythology_mining_deep_glitter",
                "epic_mythology_mining_diamond_layer",
                "epic_mythology_mining_fate_vein_diamond",
                "epic_mythology_mining_bright_vein",
                "epic_mythology_mining_diamond_path",
                "epic_mythology_mining_light_stone");
        addEpicDominant(patterns, PlaceType.MINING_SITE, blockConstraints("minecraft:emerald_ore", "minecraft:deepslate_emerald_ore"),
                "epic_mythology_mining_emerald_vein",
                "epic_mythology_mining_green_crown",
                "epic_mythology_mining_emerald_trace",
                "epic_mythology_mining_trader_vein",
                "epic_mythology_mining_green_layer",
                "epic_mythology_mining_emerald_trail",
                "epic_mythology_mining_trade_stone",
                "epic_mythology_mining_bargain_vein");
        addEpicDominant(patterns, PlaceType.MINING_SITE, blockConstraints("minecraft:ancient_debris"),
                "epic_mythology_mining_ancient_relic",
                "epic_mythology_mining_buried_relic",
                "epic_mythology_mining_netherite_trace",
                "epic_mythology_mining_ash_scar",
                "epic_mythology_mining_ancients_vein",
                "epic_mythology_mining_relic_mark",
                "epic_mythology_mining_saga_shard",
                "epic_mythology_mining_ashen_layer",
                "epic_mythology_mining_nether_stone",
                "epic_mythology_mining_ancient_fire_trace");

        addEpicPatterns(patterns, PlaceType.PORTAL_LANDMARK,
                "epic_mythology_portal_world_threshold",
                "epic_mythology_portal_fate_gate",
                "epic_mythology_portal_worldgate",
                "epic_mythology_portal_between_worlds_trail",
                "epic_mythology_portal_star_threshold",
                "epic_mythology_portal_wanderer_gate",
                "epic_mythology_portal_betweenworld_trace",
                "epic_mythology_portal_saga_path",
                "epic_mythology_portal_crossing_crown",
                "epic_mythology_portal_old_gate",
                "epic_mythology_portal_oath_threshold",
                "epic_mythology_portal_traveler_gate",
                "epic_mythology_portal_old_saga_gate",
                "epic_mythology_portal_fate_threshold",
                "epic_mythology_portal_star_gate",
                "epic_mythology_portal_betweenworld_crown",
                "epic_mythology_portal_stars_path",
                "epic_mythology_portal_traveler_threshold",
                "epic_mythology_portal_oath_gate",
                "epic_mythology_portal_betweenworld_trail",
                "epic_mythology_portal_old_world_threshold",
                "epic_mythology_portal_beyond_trace",
                "epic_mythology_portal_ancient_path_gate",
                "epic_mythology_portal_star_oath_threshold",
                "epic_mythology_portal_skies_path",
                "epic_mythology_portal_traveler_crown");
        addEpicDominant(patterns, PlaceType.PORTAL_LANDMARK, portalConstraints("nether"),
                "epic_mythology_portal_ashen_gate",
                "epic_mythology_portal_ashen_realm_gate",
                "epic_mythology_portal_fire_threshold",
                "epic_mythology_portal_lava_crown",
                "epic_mythology_portal_ash_path",
                "epic_mythology_portal_basalt_threshold",
                "epic_mythology_portal_nether_gate",
                "epic_mythology_portal_flame_trace",
                "epic_mythology_portal_fire_threshold_short",
                "epic_mythology_portal_flame_gate");
        addEpicDominant(patterns, PlaceType.PORTAL_LANDMARK, portalConstraints("end"),
                "epic_mythology_portal_last_threshold",
                "epic_mythology_portal_edge_gate",
                "epic_mythology_portal_void_gate",
                "epic_mythology_portal_pale_crossing",
                "epic_mythology_portal_star_threshold_end",
                "epic_mythology_portal_edge_path",
                "epic_mythology_portal_eye_gate",
                "epic_mythology_portal_void_crown",
                "epic_mythology_portal_silent_threshold",
                "epic_mythology_portal_last_star_trace");

        addEpicPatterns(patterns, PlaceType.GENERAL_LANDMARK,
                "epic_mythology_general_saga_trail",
                "epic_mythology_general_old_path",
                "epic_mythology_general_wanderer_trace",
                "epic_mythology_general_remembered_trail",
                "epic_mythology_general_old_song_place",
                "epic_mythology_general_traveler_path",
                "epic_mythology_general_waystone",
                "epic_mythology_general_ancient_road_trace",
                "epic_mythology_general_oath_trail",
                "epic_mythology_general_old_rest",
                "epic_mythology_general_star_trail",
                "epic_mythology_general_sky_path",
                "epic_mythology_general_ancient_trail",
                "epic_mythology_general_old_song_trace",
                "epic_mythology_general_wanderer_stone",
                "epic_mythology_general_sky_trail",
                "epic_mythology_general_old_steps_path",
                "epic_mythology_general_saga_rest",
                "epic_mythology_general_traveler_trace",
                "epic_mythology_general_old_oath_place",
                "epic_mythology_general_starry_night_path",
                "epic_mythology_general_ancient_path_trail",
                "epic_mythology_general_old_waystone",
                "epic_mythology_general_sun_trace",
                "epic_mythology_general_remembered_road",
                "epic_mythology_general_old_road_place");
        addEpicBiome(patterns, "plains",
                "epic_mythology_general_plains_saga",
                "epic_mythology_general_grass_trace",
                "epic_mythology_general_old_song_meadow",
                "epic_mythology_general_plains_sky_path");
        addEpicBiome(patterns, "forest",
                "epic_mythology_general_canopy_trail",
                "epic_mythology_general_forest_saga",
                "epic_mythology_general_old_forest_trace",
                "epic_mythology_general_memory_grove");
        addEpicBiome(patterns, "desert",
                "epic_mythology_general_sand_trace",
                "epic_mythology_general_sand_saga",
                "epic_mythology_general_dry_wind_trail",
                "epic_mythology_general_desert_stone");
        addEpicBiome(patterns, "snowy",
                "epic_mythology_general_snow_saga",
                "epic_mythology_general_snow_trace",
                "epic_mythology_general_cold_star_trail",
                "epic_mythology_general_white_rest");
        addEpicBiome(patterns, "swamp",
                "epic_mythology_general_swamp_saga",
                "epic_mythology_general_reed_trail",
                "epic_mythology_general_mire_trace",
                "epic_mythology_general_memory_pool");
        addEpicBiome(patterns, "cherry_grove",
                "epic_mythology_general_petal_saga",
                "epic_mythology_general_flower_trail",
                "epic_mythology_general_pink_trace",
                "epic_mythology_general_blossom_rest");

        addEpicPatterns(patterns, PlaceType.SETTLEMENT,
                "epic_mythology_settlement_old_hearth",
                "epic_mythology_settlement_house_oath",
                "epic_mythology_settlement_lived_place",
                "epic_mythology_settlement_warm_rest",
                "epic_mythology_settlement_traveler_hearth",
                "epic_mythology_settlement_roof_light",
                "epic_mythology_settlement_settled_trace",
                "epic_mythology_settlement_star_house",
                "epic_mythology_settlement_little_hearth",
                "epic_mythology_settlement_bright_home",
                "epic_mythology_settlement_founders_rest",
                "epic_mythology_settlement_roof_oath",
                "epic_mythology_settlement_old_saga_house",
                "epic_mythology_settlement_first_fire_place",
                "epic_mythology_settlement_founders_hearth",
                "epic_mythology_settlement_first_fire_house",
                "epic_mythology_settlement_old_saga_haven",
                "epic_mythology_settlement_oath_roof",
                "epic_mythology_settlement_bright_hearth",
                "epic_mythology_settlement_old_path_settlement",
                "epic_mythology_settlement_sky_house",
                "epic_mythology_settlement_star_rest",
                "epic_mythology_settlement_hearth_oath",
                "epic_mythology_settlement_founders_trace");

        addEpicPatterns(patterns, PlaceType.FIRST_DISCOVERY,
                "epic_mythology_first_omen",
                "epic_mythology_first_trace_generic",
                "epic_mythology_first_discovery_threshold",
                "epic_mythology_first_light_place",
                "epic_mythology_first_find",
                "epic_mythology_first_beginning_trace",
                "epic_mythology_first_new_saga",
                "epic_mythology_first_ray",
                "epic_mythology_first_path_oath",
                "epic_mythology_first_path_memory",
                "epic_mythology_first_opened_threshold",
                "epic_mythology_first_saga_beginning",
                "epic_mythology_first_world_sign",
                "epic_mythology_first_saga_threshold",
                "epic_mythology_first_light_trace",
                "epic_mythology_first_old_song_beginning",
                "epic_mythology_first_path_star",
                "epic_mythology_first_step_memory",
                "epic_mythology_first_omen_path",
                "epic_mythology_first_find_crown",
                "epic_mythology_first_waystone",
                "epic_mythology_first_threshold_light",
                "epic_mythology_first_oath_trace",
                "epic_mythology_first_opened_road",
                "epic_mythology_first_world_threshold",
                "epic_mythology_first_saga_spark");
        addEpicExactFirst(patterns, "world:first_stronghold_found",
                "epic_mythology_first_citadel",
                "epic_mythology_first_eye_halls",
                "epic_mythology_first_hidden_threshold",
                "epic_mythology_first_eye_citadel",
                "epic_mythology_first_stone_halls",
                "epic_mythology_first_eye_path",
                "epic_mythology_first_endward_fortress",
                "epic_mythology_first_last_path_halls",
                "epic_mythology_first_eye_threshold",
                "epic_mythology_first_path_citadel",
                "epic_mythology_first_ancient_citadel",
                "epic_mythology_first_last_threshold_path");
        addEpicExactFirst(patterns, "world:first_diamond_ore_mined",
                "epic_mythology_first_diamond",
                "epic_mythology_first_diamond_light",
                "epic_mythology_first_diamond_vein",
                "epic_mythology_first_glitter",
                "epic_mythology_first_diamond_crown",
                "epic_mythology_first_diamond_trace",
                "epic_mythology_first_fortune_stone",
                "epic_mythology_first_light_vein",
                "epic_mythology_first_bright_find",
                "epic_mythology_first_earth_spark");
        addEpicExactFirst(patterns, "world:first_ancient_debris_mined",
                "epic_mythology_first_ancient_debris",
                "epic_mythology_first_buried_relic",
                "epic_mythology_first_netherite_trace",
                "epic_mythology_first_ash_relic",
                "epic_mythology_first_ashen_realm_scar",
                "epic_mythology_first_ancient_find",
                "epic_mythology_first_saga_shard",
                "epic_mythology_first_ancient_fire_trace",
                "epic_mythology_first_saga_debris",
                "epic_mythology_first_nether_relic");
        addEpicExactFirst(patterns, "world:first_nether_entry",
                "epic_mythology_first_descent",
                "epic_mythology_first_ashen_gate",
                "epic_mythology_first_path_below",
                "epic_mythology_first_ashen_realm_threshold",
                "epic_mythology_first_fire_gate",
                "epic_mythology_first_ashen_trace",
                "epic_mythology_first_nether_path",
                "epic_mythology_first_fire_oath",
                "epic_mythology_first_flame_threshold",
                "epic_mythology_first_descent_trace");
        addEpicExactFirst(patterns, "world:first_end_entry",
                "epic_mythology_first_last_threshold",
                "epic_mythology_first_pale_crossing",
                "epic_mythology_first_edge_path",
                "epic_mythology_first_void_threshold",
                "epic_mythology_first_final_silence_gate",
                "epic_mythology_first_last_star_trace",
                "epic_mythology_first_path_to_edge",
                "epic_mythology_first_void_trace",
                "epic_mythology_first_last_light_threshold",
                "epic_mythology_first_pale_path_crown");

        addEpicPatterns(patterns, PlaceType.BOSS_SITE,
                "epic_mythology_boss_great_fall_site",
                "epic_mythology_boss_fallen_power_throne",
                "epic_mythology_boss_saga_arena",
                "epic_mythology_boss_great_foe_trace",
                "epic_mythology_boss_victory_ground",
                "epic_mythology_boss_victory_crown",
                "epic_mythology_boss_old_lair",
                "epic_mythology_boss_beast_threshold",
                "epic_mythology_boss_great_clash_field",
                "epic_mythology_boss_old_dread_site",
                "epic_mythology_boss_victory_oath_trace",
                "epic_mythology_boss_victory_saga");
        addEpicDominant(patterns, PlaceType.BOSS_SITE, bossConstraints("minecraft:ender_dragon"),
                "epic_mythology_boss_dragonfall",
                "epic_mythology_boss_dragon_fall",
                "epic_mythology_boss_fallen_dragon_throne",
                "epic_mythology_boss_dragon_crown",
                "epic_mythology_boss_fallen_dragon_threshold",
                "epic_mythology_boss_end_dragon_trace",
                "epic_mythology_boss_dragon_saga",
                "epic_mythology_boss_end_dragon_rest",
                "epic_mythology_boss_dragon_void",
                "epic_mythology_boss_fallen_dragon_edge",
                "epic_mythology_boss_edge_throne",
                "epic_mythology_boss_void_crown");
        addEpicDominant(patterns, PlaceType.BOSS_SITE, bossConstraints("minecraft:wither"),
                "epic_mythology_boss_witherfall",
                "epic_mythology_boss_wither_fall",
                "epic_mythology_boss_skullfall_hollow",
                "epic_mythology_boss_wither_crown",
                "epic_mythology_boss_black_skull_ground",
                "epic_mythology_boss_wither_trace",
                "epic_mythology_boss_wither_throne",
                "epic_mythology_boss_withering_saga",
                "epic_mythology_boss_wither_ash",
                "epic_mythology_boss_black_oath",
                "epic_mythology_boss_withering_site",
                "epic_mythology_boss_skull_threshold");
        addEpicDominant(patterns, PlaceType.BOSS_SITE, bossConstraints("minecraft:warden"),
                "epic_mythology_boss_warden_silence",
                "epic_mythology_boss_silent_deep",
                "epic_mythology_boss_sculk_throne",
                "epic_mythology_boss_deep_guardian_trace",
                "epic_mythology_boss_silence_threshold",
                "epic_mythology_boss_sculk_saga",
                "epic_mythology_boss_silence_crown",
                "epic_mythology_boss_deep_oath",
                "epic_mythology_boss_depths_throne",
                "epic_mythology_boss_warden_rest");
        addEpicDominant(patterns, PlaceType.BOSS_SITE, bossConstraints("minecraft:elder_guardian"),
                "epic_mythology_boss_guardian_waters",
                "epic_mythology_boss_temple_deep",
                "epic_mythology_boss_elder_wave_fall",
                "epic_mythology_boss_elder_guardian_throne",
                "epic_mythology_boss_elder_eye_trace",
                "epic_mythology_boss_temple_saga",
                "epic_mythology_boss_wave_crown",
                "epic_mythology_boss_guardian_rest",
                "epic_mythology_boss_temple_threshold");

        addEpicPatterns(patterns, PlaceType.RAID_SITE,
                "epic_mythology_raid_victory_banner",
                "epic_mythology_raid_defense_site",
                "epic_mythology_raid_village_line",
                "epic_mythology_raid_defender_field",
                "epic_mythology_raid_defense_oath",
                "epic_mythology_raid_village_saga",
                "epic_mythology_raid_resolve_banner",
                "epic_mythology_raid_repelled_trace",
                "epic_mythology_raid_victory_line",
                "epic_mythology_raid_defender_memory");
        addEpicPatterns(patterns, PlaceType.PET_MEMORIAL,
                "epic_mythology_pet_companion_memory",
                "epic_mythology_pet_bright_oath",
                "epic_mythology_pet_last_trail",
                "epic_mythology_pet_loyal_trace",
                "epic_mythology_pet_rest",
                "epic_mythology_pet_star",
                "epic_mythology_pet_saga",
                "epic_mythology_pet_loyal_trail");
        addEpicPatterns(patterns, PlaceType.NAMED_MOB_MEMORIAL,
                "epic_mythology_named_memory",
                "epic_mythology_named_fall",
                "epic_mythology_named_trace",
                "epic_mythology_named_saga",
                "epic_mythology_named_place",
                "epic_mythology_named_last_mark",
                "epic_mythology_named_remembered_trace",
                "epic_mythology_named_name");
        addEpicPatterns(patterns, PlaceType.DIMENSION_THRESHOLD,
                "epic_mythology_dimension_world_threshold",
                "epic_mythology_dimension_border",
                "epic_mythology_dimension_worldbound_trace",
                "epic_mythology_dimension_between_worlds_path",
                "epic_mythology_dimension_crossing_crown",
                "epic_mythology_dimension_world_oath",
                "epic_mythology_dimension_star_threshold",
                "epic_mythology_dimension_saga_path",
                "epic_mythology_dimension_fate_border",
                "epic_mythology_dimension_world_trail");
        addEpicPatterns(patterns, PlaceType.CUSTOM,
                "epic_mythology_custom_remembered_place",
                "epic_mythology_custom_named_place",
                "epic_mythology_custom_memory_place",
                "epic_mythology_custom_old_trace",
                "epic_mythology_custom_saga_trail",
                "epic_mythology_custom_remembered_threshold",
                "epic_mythology_custom_old_song_place",
                "epic_mythology_custom_star_mark",
                "epic_mythology_custom_traveler_trace",
                "epic_mythology_custom_old_path",
                "epic_mythology_custom_star_trace",
                "epic_mythology_custom_old_road_memory",
                "epic_mythology_custom_named_path",
                "epic_mythology_custom_saga_mark",
                "epic_mythology_custom_ancient_mark",
                "epic_mythology_custom_memory_stone",
                "epic_mythology_custom_old_path_trace",
                "epic_mythology_custom_memory_threshold",
                "epic_mythology_custom_traveler_trail",
                "epic_mythology_custom_sky_sign",
                "epic_mythology_custom_old_crown",
                "epic_mythology_custom_oath_place");

        return new NameDataPack(NameStyle.EPIC_MYTHOLOGY.idString(), List.copyOf(patterns), smallStyleTokens());
    }

    private static void addEpicPatterns(List<NamePattern> patterns, PlaceType placeType, String... ids) {
        for (int index = 0; index < ids.length; index++) {
            patterns.add(epicPattern(ids[index], placeType, epicWeight(index), Set.of(), NameCauseConstraints.none(), "place_type"));
        }
    }

    private static void addEpicDeath(List<NamePattern> patterns, DeathSiteEnvironment environment, String... ids) {
        for (int index = 0; index < ids.length; index++) {
            patterns.add(epicPattern(ids[index], PlaceType.DEATH_SITE, epicWeight(index), Set.of(environment), NameCauseConstraints.none(), environment.idString()));
        }
    }

    private static void addEpicExactFirst(List<NamePattern> patterns, String firstDiscoveryKey, String... ids) {
        NameCauseConstraints constraints = NameCauseConstraints.builder()
                .requiredFirstDiscoveryKey(firstDiscoveryKey)
                .build();
        for (int index = 0; index < ids.length; index++) {
            patterns.add(epicPattern(ids[index], PlaceType.FIRST_DISCOVERY, epicWeight(index), Set.of(), constraints, "exact_cause"));
        }
    }

    private static void addEpicDominant(List<NamePattern> patterns, PlaceType placeType, NameCauseConstraints constraints, String... ids) {
        for (int index = 0; index < ids.length; index++) {
            patterns.add(epicPattern(ids[index], placeType, epicWeight(index), Set.of(), constraints, "dominant_target"));
        }
    }

    private static void addEpicBiome(List<NamePattern> patterns, String biomeGroup, String... ids) {
        NameCauseConstraints constraints = NameCauseConstraints.builder()
                .requiredCauseType(PlaceCauseType.VISITS)
                .requiredBiomeGroups(biomeGroup)
                .build();
        for (int index = 0; index < ids.length; index++) {
            patterns.add(epicPattern(ids[index], PlaceType.GENERAL_LANDMARK, epicWeight(index), Set.of(), constraints, "biome", biomeGroup));
        }
    }

    private static NamePattern epicPattern(
            String id,
            PlaceType placeType,
            double weight,
            Set<DeathSiteEnvironment> environments,
            NameCauseConstraints constraints,
            String... tags
    ) {
        return new NamePattern(
                id,
                "living_legends.name.pattern." + id,
                Set.of(placeType),
                environments,
                weight,
                List.of(),
                epicTags(tags),
                NameSemanticRoots.inferPatternRoot(id),
                constraints,
                Set.of()
        );
    }

    private static Set<String> epicTags(String... tags) {
        Set<String> result = new LinkedHashSet<>();
        result.add(NameStyle.EPIC_MYTHOLOGY.idString());
        if (tags != null) {
            for (String tag : tags) {
                String normalized = WorldPos.optionalId(tag);
                if (!normalized.isBlank()) {
                    result.add(normalized);
                }
            }
        }
        return Set.copyOf(result);
    }

    private static double epicWeight(int index) {
        if (index < 6) {
            return 10.0;
        }
        if (index < 12) {
            return 5.0;
        }
        return 2.0;
    }

    private static NameDataPack funnyCommunityPack() {
        List<NamePattern> patterns = new ArrayList<>();

        addFunnyDeath(patterns, DeathSiteEnvironment.SURFACE, ids("""
                funny_community_surface_oops_field
                funny_community_surface_skill_issue_zone
                funny_community_surface_respawn_site
                funny_community_surface_regret_clearing
                funny_community_surface_armor_minus
                funny_community_surface_error_trace
                funny_community_surface_last_checkpoint
                funny_community_surface_pain_happened
                funny_community_surface_almost_made_it
                funny_community_surface_minus_hp_meadow
                funny_community_surface_almost_site
                funny_community_surface_respawn_trail
                funny_community_surface_minus_loot_field
                funny_community_surface_legendary_fail
                funny_community_surface_mistake_hill
                funny_community_surface_return_point
                funny_community_surface_panic_monument
                funny_community_surface_awkward_zone
                funny_community_surface_no_shield_trace
                funny_community_surface_i_can_do_it_meadow
                funny_community_surface_bed_check_failed
                funny_community_surface_tutorial_skip
                funny_community_surface_dramatic_respawn
                funny_community_surface_inventory_panic
                funny_community_surface_wrong_button
                funny_community_surface_helmet_optional
                funny_community_surface_tiny_mistake
                funny_community_surface_chat_laughed
                """));
        addFunnyDeath(patterns, DeathSiteEnvironment.CAVE, ids("""
                funny_community_cave_minus_pick
                funny_community_cave_oops_hollow
                funny_community_cave_regret_tunnel
                funny_community_cave_no_exit_mine
                funny_community_cave_torch_did_not_help
                funny_community_cave_greed_site
                funny_community_cave_panic_depths
                funny_community_cave_respawn
                funny_community_cave_last_torch
                funny_community_cave_minus_inventory
                funny_community_cave_skill_issue_hollow
                funny_community_cave_downward_and_wrong
                funny_community_cave_miner_fail
                funny_community_cave_stone_oops
                funny_community_cave_quiet_scream
                funny_community_cave_regret_hole
                funny_community_cave_planless_descent
                funny_community_cave_just_a_bit_more
                funny_community_cave_deep_cringe
                funny_community_cave_minus_diamonds
                funny_community_cave_gravity_meeting
                funny_community_cave_torch_budget_cut
                funny_community_cave_no_way_up
                funny_community_cave_mob_snack
                funny_community_cave_greed_checkpoint
                funny_community_cave_pickaxe_tears
                funny_community_cave_wrong_turn
                funny_community_cave_inventory_goodbye
                """));
        addFunnyDeath(patterns, DeathSiteEnvironment.WATER, ids("""
                funny_community_water_glub_pool
                funny_community_water_drowning_site
                funny_community_water_minus_air
                funny_community_water_crossing_failed
                funny_community_water_won
                funny_community_water_quiet_glub
                funny_community_water_too_late_shore
                funny_community_water_regret_lake
                funny_community_water_short_swim
                funny_community_water_bridge_needed
                funny_community_water_underwater_fail
                funny_community_water_wet_panic_trace
                funny_community_water_minus_loot_pool
                funny_community_water_boat_not_found
                funny_community_water_bubble_panic
                funny_community_water_swim_lesson
                funny_community_water_soggy_checkpoint
                """));
        addFunnyDeath(patterns, DeathSiteEnvironment.MOUNTAIN, ids("""
                funny_community_mountain_regret_cliff
                funny_community_mountain_jump_failed
                funny_community_mountain_minus_knees
                funny_community_mountain_mistakes
                funny_community_mountain_last_ledge
                funny_community_mountain_i_can_fly_summit
                funny_community_mountain_no_bucket_site
                funny_community_mountain_panic_slope
                funny_community_mountain_freefall_trace
                funny_community_mountain_fail_stone
                funny_community_mountain_height_won
                funny_community_mountain_minus_loot_ledge
                funny_community_mountain_too_high_trail
                funny_community_mountain_bad_parkour_hill
                funny_community_mountain_bucket_forgotten
                funny_community_mountain_no_parachute
                funny_community_mountain_ledge_confidence
                funny_community_mountain_landing_problem
                """));
        addFunnyDeath(patterns, DeathSiteEnvironment.NETHER, ids("""
                funny_community_nether_said_no
                funny_community_nether_lava_won
                funny_community_nether_ashen_fail
                funny_community_nether_minus_netherite
                funny_community_nether_hot_respawn
                funny_community_nether_bridge_too_much
                funny_community_nether_basalt_oops
                funny_community_nether_shame_portal
                funny_community_nether_lava_panic
                funny_community_nether_no_fire_res_site
                funny_community_nether_ash_regret
                funny_community_nether_toasted_trace
                funny_community_nether_bridge_nowhere
                funny_community_nether_fire_res_home
                funny_community_nether_piglin_tax
                funny_community_nether_bridge_regret
                funny_community_nether_hot_decision
                """));
        addFunnyDeath(patterns, DeathSiteEnvironment.END, ids("""
                funny_community_end_void_ate_it
                funny_community_end_did_not_forgive
                funny_community_end_final_oops
                funny_community_end_minus_ender_pearl
                funny_community_end_fail_threshold
                funny_community_end_flight_nowhere
                funny_community_end_regret_edge
                funny_community_end_void_trace
                funny_community_end_no_water_site
                funny_community_end_final_screen
                funny_community_end_pale_cringe
                funny_community_end_minus_loot_trail
                funny_community_end_oops_void
                funny_community_end_wrong_pearl
                funny_community_end_pearl_math
                funny_community_end_void_receipt
                funny_community_end_dragon_judged
                funny_community_end_platform_panic
                """));

        addFunnyPatterns(patterns, PlaceType.BATTLEFIELD, ids("""
                funny_community_battle_brawl_field
                funny_community_battle_mess_zone
                funny_community_battle_question_clash
                funny_community_battle_minus_armor_field
                funny_community_battle_button_mash_site
                funny_community_battle_after_fight_trail
                funny_community_battle_panic_banner
                funny_community_battle_regret_arena
                funny_community_battle_almost_field
                funny_community_battle_old_scuffle
                funny_community_battle_combo_trace
                funny_community_battle_i_carry_zone
                funny_community_battle_friendly_fire_field
                funny_community_battle_pain_circle
                """));
        addFunnyDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:zombie", "minecraft:zombie_villager", "minecraft:husk", "minecraft:drowned", "group:undead"), ids("""
                funny_community_battle_zombie_queue_field
                funny_community_battle_night_cringe
                funny_community_battle_dead_pile_on
                funny_community_battle_groan_trace
                funny_community_battle_minus_brains_zone
                funny_community_battle_undead_trail
                funny_community_battle_too_many_site
                funny_community_battle_night_crowd
                funny_community_battle_rotten_question_field
                funny_community_battle_zombie_party
                """));
        addFunnyDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:skeleton", "minecraft:stray", "minecraft:bogged", "minecraft:wither_skeleton", "group:skeleton"), ids("""
                funny_community_battle_arrows_face_field
                funny_community_battle_bone_range
                funny_community_battle_skeleton_snipe
                funny_community_battle_archer_trace
                funny_community_battle_minus_shield_zone
                funny_community_battle_from_where_field
                funny_community_battle_bone_barrage
                funny_community_battle_arrow_trail
                funny_community_battle_unfair_bow_site
                funny_community_battle_white_volley
                """));
        addFunnyDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:creeper", "group:explosive"), ids("""
                funny_community_battle_regret_crater
                funny_community_battle_oops_crater
                funny_community_battle_sudden_repair_field
                funny_community_battle_hissing_cringe
                funny_community_battle_minus_house_site
                funny_community_battle_explosive_question
                funny_community_battle_didnt_hear_crater
                funny_community_battle_powder_trace
                funny_community_battle_repair_field
                funny_community_battle_surprise_crater
                funny_community_battle_creeper_betrayal
                funny_community_battle_after_boom_trail
                """));
        addFunnyDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:enderman", "group:end"), ids("""
                funny_community_battle_dont_look_field
                funny_community_battle_wanderer_trace
                funny_community_battle_aggro_stare_trail
                funny_community_battle_bad_look_site
                funny_community_battle_shadow_regret
                funny_community_battle_i_didnt_look_zone
                funny_community_battle_pearl_no_help
                funny_community_battle_teleport_field
                funny_community_battle_quiet_cringe
                funny_community_battle_ender_question
                """));
        addFunnyDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:blaze", "minecraft:ghast", "minecraft:piglin", "minecraft:piglin_brute", "minecraft:zombified_piglin", "minecraft:hoglin", "minecraft:zoglin", "group:nether"), ids("""
                funny_community_battle_hot_brawl
                funny_community_battle_ashen_betrayal
                funny_community_battle_minus_fire_res_field
                funny_community_battle_nether_brawl
                funny_community_battle_flame_trace
                funny_community_battle_where_ghast_zone
                funny_community_battle_blaze_question
                funny_community_battle_piglin_disapproved
                funny_community_battle_no_gold_site
                funny_community_battle_lava_cringe
                """));
        addFunnyDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("group:illager", "minecraft:pillager", "minecraft:vindicator", "minecraft:evoker", "minecraft:ravager"), ids("""
                funny_community_battle_raid_went_wrong
                funny_community_battle_raider_field
                funny_community_battle_panic_banner_illager
                funny_community_battle_village_help
                funny_community_battle_raid_trace
                funny_community_battle_minus_totem_site
                funny_community_battle_patrol_cringe
                funny_community_battle_where_golem_field
                funny_community_battle_raid_questions
                funny_community_battle_village_brawl
                """));
        addFunnyDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:warden", "group:deep_dark", "group:boss_like"), ids("""
                funny_community_battle_big_silent_question
                funny_community_battle_warden_shhh
                funny_community_battle_minus_sound_zone
                funny_community_battle_sculk_cringe
                funny_community_battle_panic_trace
                funny_community_battle_dont_make_noise_site
                funny_community_battle_big_silent_one
                funny_community_battle_deep_oops
                funny_community_battle_wool_trail
                funny_community_battle_soundless_field
                """));
        addFunnyDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:breeze", "group:wind"), ids("""
                funny_community_battle_wind_betrayed
                funny_community_battle_blow_off_field
                funny_community_battle_whirl_cringe
                funny_community_battle_trial_trace
                funny_community_battle_light_brawl
                funny_community_battle_jump_field
                funny_community_battle_trial_mark
                """));

        addFunnyPatterns(patterns, PlaceType.SLAUGHTER_FIELD, ids("""
                funny_community_slaughter_question_pasture
                funny_community_slaughter_awkward_field
                funny_community_slaughter_herd_trace
                funny_community_slaughter_regret_meadow
                funny_community_slaughter_shepherd_cringe
                funny_community_slaughter_herd_trail
                funny_community_slaughter_had_to_site
                funny_community_slaughter_farmer_incident
                funny_community_slaughter_minus_morale_meadow
                funny_community_slaughter_oops_field
                funny_community_slaughter_consequence_pasture
                funny_community_slaughter_trouble_trail
                """));
        addFunnyDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("minecraft:cow", "minecraft:mooshroom", "group:cattle"), ids("""
                funny_community_slaughter_cow_question
                funny_community_slaughter_moo_meadow
                funny_community_slaughter_cattle_trace
                funny_community_slaughter_milky_cringe_pasture
                funny_community_slaughter_cow_betrayal
                funny_community_slaughter_moo_site
                funny_community_slaughter_herd_field
                funny_community_slaughter_sigh_pasture
                funny_community_slaughter_beefy_thoughts_meadow
                funny_community_slaughter_cow_incident
                """));
        addFunnyDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("minecraft:pig", "group:pig"), ids("""
                funny_community_slaughter_pig_incident
                funny_community_slaughter_oink_meadow
                funny_community_slaughter_pig_regret_pasture
                funny_community_slaughter_pig_trace
                funny_community_slaughter_pig_question
                funny_community_slaughter_awkward_pen
                funny_community_slaughter_piglet_cringe
                funny_community_slaughter_oink_site
                funny_community_slaughter_boar_pasture
                funny_community_slaughter_trough_trail
                """));
        addFunnyDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("minecraft:chicken", "group:chicken"), ids("""
                funny_community_slaughter_chicken_incident
                funny_community_slaughter_feather_cringe
                funny_community_slaughter_chicken_trace
                funny_community_slaughter_minus_feathers_yard
                funny_community_slaughter_chicken_panic
                funny_community_slaughter_cluck_field
                funny_community_slaughter_feather_trail
                funny_community_slaughter_cluck_site
                funny_community_slaughter_regret_coop
                funny_community_slaughter_feather_question
                """));
        addFunnyDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("minecraft:sheep", "group:sheep"), ids("""
                funny_community_slaughter_sheep_question
                funny_community_slaughter_wool_cringe
                funny_community_slaughter_flock_trace
                funny_community_slaughter_baa_field
                funny_community_slaughter_minus_wool_meadow
                funny_community_slaughter_sheep_betrayal
                funny_community_slaughter_baa_site
                funny_community_slaughter_sigh_pasture_sheep
                funny_community_slaughter_flock_trail
                funny_community_slaughter_wool_incident
                """));
        addFunnyDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("group:civilian", "minecraft:villager", "minecraft:wandering_trader"), ids("""
                funny_community_slaughter_awkward_trail
                funny_community_slaughter_villager_incident
                funny_community_slaughter_regret_square
                funny_community_slaughter_trader_trace
                funny_community_slaughter_minus_discounts_site
                funny_community_slaughter_question_market
                funny_community_slaughter_trader_stayed
                funny_community_slaughter_civilian_cringe
                funny_community_slaughter_hmm_trail
                funny_community_slaughter_hrrm_square
                """));
        addFunnyDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("group:companion"), ids("""
                funny_community_slaughter_awkward_memory
                funny_community_slaughter_friend_trace
                funny_community_slaughter_sorry_site
                funny_community_slaughter_quiet_cringe
                funny_community_slaughter_remembered_awkwardness
                funny_community_slaughter_regret_trail
                funny_community_slaughter_companion_trace
                funny_community_slaughter_no_comment_site
                """));

        addFunnyPatterns(patterns, PlaceType.PVP_ARENA, ids("""
                funny_community_pvp_skill_issue_arena
                funny_community_pvp_rival_ring
                funny_community_pvp_minus_ego_field
                funny_community_pvp_backpedal_site
                funny_community_pvp_duel_cringe
                funny_community_pvp_it_lagged_arena
                funny_community_pvp_rival_trace
                funny_community_pvp_i_lagged_ring
                funny_community_pvp_fair_random_field
                funny_community_pvp_minus_friendship_zone
                funny_community_pvp_argument_arena
                funny_community_pvp_rematch_site
                funny_community_pvp_salt_ring
                funny_community_pvp_one_more_field
                """));

        addFunnyPatterns(patterns, PlaceType.MINING_SITE, ids("""
                funny_community_mining_digger_vein
                funny_community_mining_regret_mine
                funny_community_mining_ore_trace
                funny_community_mining_greed_site
                funny_community_mining_ore_question
                funny_community_mining_luck_pocket
                funny_community_mining_deep_flex
                funny_community_mining_pickaxe_trail
                funny_community_mining_miner_cringe
                funny_community_mining_stone_temptation
                """));
        addFunnyDominant(patterns, PlaceType.MINING_SITE, blockConstraints("minecraft:diamond_ore", "minecraft:deepslate_diamond_ore"), ids("""
                funny_community_mining_diamond_oops
                funny_community_mining_greed_vein
                funny_community_mining_shiny_cringe
                funny_community_mining_diamond_trace
                funny_community_mining_fortune_vein
                funny_community_mining_one_more_block_site
                funny_community_mining_diamond_betrayal
                funny_community_mining_miner_joy
                funny_community_mining_regret_glitter
                funny_community_mining_luck_pocket_diamond
                """));
        addFunnyDominant(patterns, PlaceType.MINING_SITE, blockConstraints("minecraft:emerald_ore", "minecraft:deepslate_emerald_ore"), ids("""
                funny_community_mining_emerald_question
                funny_community_mining_trade_vein
                funny_community_mining_emerald_trace
                funny_community_mining_green_temptation
                funny_community_mining_trader_joy
                funny_community_mining_discount_site
                funny_community_mining_green_betrayal
                funny_community_mining_villager_vein
                """));
        addFunnyDominant(patterns, PlaceType.MINING_SITE, blockConstraints("minecraft:ancient_debris"), ids("""
                funny_community_mining_netherite_copium
                funny_community_mining_ancient_oops
                funny_community_mining_netherite_trace
                funny_community_mining_ashen_greed
                funny_community_mining_just_a_bit_more_vein
                funny_community_mining_happy_debris
                funny_community_mining_buried_flex
                funny_community_mining_ancient_cringe
                funny_community_mining_minus_beds_site
                funny_community_mining_netherite_dream
                """));

        addFunnyPatterns(patterns, PlaceType.PORTAL_LANDMARK, ids("""
                funny_community_portal_wrong_way_gate
                funny_community_portal_questions
                funny_community_portal_betweenworld_cringe
                funny_community_portal_where_threshold
                funny_community_portal_bug_trail
                funny_community_portal_doubt_gate
                funny_community_portal_regret_crossing
                funny_community_portal_old_teleport
                funny_community_portal_ill_be_quick_path
                funny_community_portal_betrayal
                funny_community_portal_far_oops
                funny_community_portal_confusion_gate
                funny_community_portal_loading_site
                funny_community_portal_ping_pong_threshold
                funny_community_portal_regret_lobby
                funny_community_portal_wrong_dimension
                funny_community_portal_speedrun_excuse
                funny_community_portal_return_later
                """));
        addFunnyDominant(patterns, PlaceType.PORTAL_LANDMARK, portalConstraints("nether"), ids("""
                funny_community_portal_nether_gate
                funny_community_portal_heat_gate
                funny_community_portal_minus_eyebrows
                funny_community_portal_ashen_oops
                funny_community_portal_lava_crossing
                funny_community_portal_problem_path
                funny_community_portal_no_fire_res_gate
                funny_community_portal_admin_chat
                funny_community_portal_basalt_cringe
                funny_community_portal_just_a_minute_gate
                """));
        addFunnyDominant(patterns, PlaceType.PORTAL_LANDMARK, portalConstraints("end"), ids("""
                funny_community_portal_end_gate
                funny_community_portal_final_threshold
                funny_community_portal_thats_it_gate
                funny_community_portal_void_crossing
                funny_community_portal_minus_pearls
                funny_community_portal_dragon_path
                funny_community_portal_final_cringe
                funny_community_portal_last_panic_gate
                funny_community_portal_no_way_back_threshold
                funny_community_portal_eye_mistake
                """));

        addFunnyPatterns(patterns, PlaceType.GENERAL_LANDMARK, ids("""
                funny_community_general_back_forth_trail
                funny_community_general_many_steps_place
                funny_community_general_people_trace
                funny_community_general_old_path
                funny_community_general_habit_path
                funny_community_general_local_highway
                funny_community_general_i_was_here_site
                funny_community_general_server_trail
                funny_community_general_remembered_stomping
                funny_community_general_traveler_trace
                funny_community_general_jogging_zone
                funny_community_general_busy_path
                funny_community_general_no_reason_path
                funny_community_general_people_trail
                funny_community_general_afk_loop
                funny_community_general_everyone_passed
                funny_community_general_boot_stomping
                funny_community_general_shortcut_maybe
                """));
        addFunnyBiome(patterns, "plains", ids("""
                funny_community_general_meadow_highway
                funny_community_general_grass_trail
                funny_community_general_field_trace
                funny_community_general_plains_stomping
                """));
        addFunnyBiome(patterns, "forest", ids("""
                funny_community_general_forest_path
                funny_community_general_canopy_trail
                funny_community_general_forest_trace
                funny_community_general_forest_zigzag
                """));
        addFunnyBiome(patterns, "desert", ids("""
                funny_community_general_sandy_stomping
                funny_community_general_sand_trace
                funny_community_general_no_water_trail
                funny_community_general_desert_zigzag
                """));
        addFunnyBiome(patterns, "snowy", ids("""
                funny_community_general_snowy_stomping
                funny_community_general_snow_trace
                funny_community_general_frost_path
                funny_community_general_frozen_feet_trail
                """));
        addFunnyBiome(patterns, "swamp", ids("""
                funny_community_general_swampy_plop
                funny_community_general_goo_trail
                funny_community_general_reed_trace
                funny_community_general_soggy_path
                """));
        addFunnyBiome(patterns, "cherry_grove", ids("""
                funny_community_general_petal_trail
                funny_community_general_pink_trace
                funny_community_general_screenshot_rest
                funny_community_general_blossom_trail
                """));

        addFunnyPatterns(patterns, PlaceType.SETTLEMENT, ids("""
                funny_community_settlement_chaos_house
                funny_community_settlement_almost_base
                funny_community_settlement_chaos_hearth
                funny_community_settlement_warm_mess
                funny_community_settlement_questions
                funny_community_settlement_people_house
                funny_community_settlement_builder_rest
                funny_community_settlement_chest_site
                funny_community_settlement_roof_cringe
                funny_community_settlement_finish_later_camp
                funny_community_settlement_cozy_mess
                funny_community_settlement_too_many_chests
                funny_community_settlement_signpost
                funny_community_settlement_old_storage
                funny_community_settlement_doorless_house
                funny_community_settlement_chest_maze
                funny_community_settlement_temporary_forever
                funny_community_settlement_signs_everywhere
                """));

        addFunnyPatterns(patterns, PlaceType.FIRST_DISCOVERY, ids("""
                funny_community_first_whoa
                funny_community_first_find
                funny_community_first_flex
                funny_community_first_new_question
                funny_community_first_aha_site
                funny_community_first_trace
                funny_community_first_chaos_beginning
                funny_community_first_record
                funny_community_first_legend_started
                funny_community_first_screenshot
                funny_community_first_world_nice
                funny_community_first_decent_one
                funny_community_first_chat_screenshot
                funny_community_first_server_clapped
                funny_community_first_almost_world_first
                funny_community_first_new_flex_point
                funny_community_first_rare_aha
                funny_community_first_map_got_bigger
                """));
        addFunnyExactFirst(patterns, "world:first_stronghold_found", ids("""
                funny_community_first_stronghold
                funny_community_first_eye_halls
                funny_community_first_finally_fortress
                funny_community_first_hidden_threshold
                funny_community_first_eye_delivered
                funny_community_first_stronghold_found
                funny_community_first_now_what_halls
                funny_community_first_almost_portal
                funny_community_first_chaos_fortress
                funny_community_first_eye_spam_site
                """));
        addFunnyExactFirst(patterns, "world:first_diamond_ore_mined", ids("""
                funny_community_first_diamond
                funny_community_first_glitter
                funny_community_first_diamond_flex
                funny_community_first_not_iron_site
                funny_community_first_nice
                funny_community_first_lucky_vein
                funny_community_first_lucky_find
                funny_community_first_luck_glitter
                funny_community_first_screenshot_site
                funny_community_first_diamond_whoa
                """));
        addFunnyExactFirst(patterns, "world:first_ancient_debris_mined", ids("""
                funny_community_first_netherite_copium
                funny_community_first_ancient_oops
                funny_community_first_debris
                funny_community_first_minus_beds_site
                funny_community_first_ashen_flex
                funny_community_first_netherite_whoa
                funny_community_first_ancient_joy
                funny_community_first_debris_hope
                funny_community_first_one_more_site
                funny_community_first_chaos_relic
                """));
        addFunnyExactFirst(patterns, "world:first_nether_entry", ids("""
                funny_community_first_descent
                funny_community_first_nether_gate
                funny_community_first_oh_hot
                funny_community_first_problem_path
                funny_community_first_ashen_start
                funny_community_first_minute_gate
                funny_community_first_nether_cringe
                funny_community_first_where_fortress_site
                funny_community_first_ashen_trace
                funny_community_first_chaos_entrance
                """));
        addFunnyExactFirst(patterns, "world:first_end_entry", ids("""
                funny_community_first_last_threshold
                funny_community_first_end_oops
                funny_community_first_finale_entrance
                funny_community_first_thats_it_site
                funny_community_first_dragon_threshold
                funny_community_first_end_started
                funny_community_first_void_path
                funny_community_first_final_chaos_gate
                funny_community_first_end_trace
                funny_community_first_minus_pearls_site
                """));

        addFunnyPatterns(patterns, PlaceType.BOSS_SITE, ids("""
                funny_community_boss_big_brawl_site
                funny_community_boss_we_did_it_arena
                funny_community_boss_big_question_trace
                funny_community_boss_victory_scream_site
                funny_community_boss_bossfight_zone
                funny_community_boss_almost_lair
                funny_community_boss_brawl_memory
                funny_community_boss_chaos_arena
                funny_community_boss_again_site
                funny_community_boss_victory_trace
                funny_community_boss_cringe_field
                funny_community_boss_triumph_point
                funny_community_boss_big_health_bar
                funny_community_boss_group_photo_later
                funny_community_boss_teamwork_maybe
                funny_community_boss_loot_discussion
                funny_community_boss_panic_meeting
                funny_community_boss_victory_noise
                """));
        addFunnyDominant(patterns, PlaceType.BOSS_SITE, bossConstraints("minecraft:ender_dragon"), ids("""
                funny_community_boss_dragon_fell
                funny_community_boss_dragonfall
                funny_community_boss_minus_dragon_site
                funny_community_boss_gg_arena
                funny_community_boss_dragon_trace
                funny_community_boss_victory_threshold
                funny_community_boss_final_flex
                funny_community_boss_victory_over_dragon
                funny_community_boss_we_did_it_throne
                funny_community_boss_dragon_oops
                funny_community_boss_final_cringe
                funny_community_boss_ender_chaos_arena
                """));
        addFunnyDominant(patterns, PlaceType.BOSS_SITE, bossConstraints("minecraft:wither"), ids("""
                funny_community_boss_wither_couldnt
                funny_community_boss_witherfall
                funny_community_boss_minus_wither_site
                funny_community_boss_wither_trace
                funny_community_boss_skull_arena
                funny_community_boss_wither_cringe
                funny_community_boss_why_do_this_site
                funny_community_boss_victory_hollow
                funny_community_boss_gg_wither_point
                funny_community_boss_wither_tired
                funny_community_boss_bossfight_trace
                funny_community_boss_anti_skull_site
                """));
        addFunnyDominant(patterns, PlaceType.BOSS_SITE, bossConstraints("minecraft:warden"), ids("""
                funny_community_boss_warden_no_forgive
                funny_community_boss_big_silent_one
                funny_community_boss_shhh_site
                funny_community_boss_bad_decision_trace
                funny_community_boss_sculk_oops
                funny_community_boss_silent_cringe
                funny_community_boss_minus_sound_site
                funny_community_boss_wool_trail
                funny_community_boss_panic_arena
                funny_community_boss_warden_question
                """));
        addFunnyDominant(patterns, PlaceType.BOSS_SITE, bossConstraints("minecraft:elder_guardian"), ids("""
                funny_community_boss_guardian_tired
                funny_community_boss_minus_fish_site
                funny_community_boss_temple_question
                funny_community_boss_elder_guardian_trace
                funny_community_boss_water_said_no
                funny_community_boss_where_sponge_site
                funny_community_boss_deep_cringe
                funny_community_boss_guardian_waters
                funny_community_boss_temple_chaos
                funny_community_boss_old_fish_boss
                """));

        addFunnyPatterns(patterns, PlaceType.RAID_SITE, ids("""
                funny_community_raid_failed
                funny_community_raid_village_survived
                funny_community_raid_defender_field
                funny_community_raid_we_did_it_banner
                funny_community_raid_minus_raid_site
                funny_community_raid_panic_line
                funny_community_raid_golem_pretended
                funny_community_raid_hero_trail
                funny_community_raid_village_brawl
                funny_community_raid_where_bell_site
                """));
        addFunnyPatterns(patterns, PlaceType.PET_MEMORIAL, ids("""
                funny_community_pet_memory
                funny_community_pet_rest
                funny_community_pet_trail
                funny_community_pet_bright_trace
                funny_community_pet_thank_you_site
                funny_community_pet_kind_trace
                funny_community_pet_memory_nook
                funny_community_pet_loyal_trace
                funny_community_pet_remembered_oops
                funny_community_pet_quiet_rest
                """));
        addFunnyPatterns(patterns, PlaceType.NAMED_MOB_MEMORIAL, ids("""
                funny_community_named_memory
                funny_community_named_fall
                funny_community_named_trace
                funny_community_named_place
                funny_community_named_last_oops
                funny_community_named_remembered_trace
                funny_community_named_legend
                funny_community_named_point
                funny_community_named_well_site
                funny_community_named_name
                """));
        addFunnyPatterns(patterns, PlaceType.DIMENSION_THRESHOLD, ids("""
                funny_community_dimension_where_threshold
                funny_community_dimension_betweenworld_oops
                funny_community_dimension_chaos_path
                funny_community_dimension_question_border
                funny_community_dimension_ping_pong_gate
                funny_community_dimension_trail
                funny_community_dimension_loading_threshold
                funny_community_dimension_world_trace
                funny_community_dimension_transition_zone
                funny_community_dimension_ill_be_back_site
                """));
        addFunnyPatterns(patterns, PlaceType.CUSTOM, ids("""
                funny_community_custom_remembered_place
                funny_community_custom_chaos_site
                funny_community_custom_named_oops
                funny_community_custom_old_trace
                funny_community_custom_question_point
                funny_community_custom_do_not_touch_site
                funny_community_custom_remembered_cringe
                funny_community_custom_friendly_betrayal
                funny_community_custom_people_mark
                funny_community_custom_old_joke
                funny_community_custom_local_meme
                funny_community_custom_why_here
                funny_community_custom_suspicious_marker
                funny_community_custom_someones_bit
                funny_community_custom_chat_named_it
                funny_community_custom_legacy_oops
                """));

        return new NameDataPack(NameStyle.FUNNY_COMMUNITY.idString(), List.copyOf(patterns), smallStyleTokens());
    }

    private static String[] ids(String block) {
        if (block == null || block.isBlank()) {
            return new String[0];
        }
        return block.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toArray(String[]::new);
    }

    private static void addFunnyPatterns(List<NamePattern> patterns, PlaceType placeType, String... ids) {
        for (int index = 0; index < ids.length; index++) {
            patterns.add(funnyPattern(ids[index], placeType, funnyWeight(index), Set.of(), NameCauseConstraints.none(), "place_type"));
        }
    }

    private static void addFunnyDeath(List<NamePattern> patterns, DeathSiteEnvironment environment, String... ids) {
        for (int index = 0; index < ids.length; index++) {
            patterns.add(funnyPattern(ids[index], PlaceType.DEATH_SITE, funnyWeight(index), Set.of(environment), NameCauseConstraints.none(), environment.idString()));
        }
    }

    private static void addFunnyExactFirst(List<NamePattern> patterns, String firstDiscoveryKey, String... ids) {
        NameCauseConstraints constraints = NameCauseConstraints.builder()
                .requiredFirstDiscoveryKey(firstDiscoveryKey)
                .build();
        for (int index = 0; index < ids.length; index++) {
            patterns.add(funnyPattern(ids[index], PlaceType.FIRST_DISCOVERY, funnyWeight(index), Set.of(), constraints, "exact_cause"));
        }
    }

    private static void addFunnyDominant(List<NamePattern> patterns, PlaceType placeType, NameCauseConstraints constraints, String... ids) {
        for (int index = 0; index < ids.length; index++) {
            patterns.add(funnyPattern(ids[index], placeType, funnyWeight(index), Set.of(), constraints, "dominant_target"));
        }
    }

    private static void addFunnyBiome(List<NamePattern> patterns, String biomeGroup, String... ids) {
        NameCauseConstraints constraints = NameCauseConstraints.builder()
                .requiredCauseType(PlaceCauseType.VISITS)
                .requiredBiomeGroups(biomeGroup)
                .build();
        for (int index = 0; index < ids.length; index++) {
            patterns.add(funnyPattern(ids[index], PlaceType.GENERAL_LANDMARK, funnyWeight(index), Set.of(), constraints, "biome", biomeGroup));
        }
    }

    private static NamePattern funnyPattern(
            String id,
            PlaceType placeType,
            double weight,
            Set<DeathSiteEnvironment> environments,
            NameCauseConstraints constraints,
            String... tags
    ) {
        return new NamePattern(
                id,
                "living_legends.name.pattern." + id,
                Set.of(placeType),
                environments,
                weight,
                List.of(),
                funnyTags(tags),
                NameSemanticRoots.inferPatternRoot(id),
                constraints,
                Set.of()
        );
    }

    private static Set<String> funnyTags(String... tags) {
        Set<String> result = new LinkedHashSet<>();
        result.add(NameStyle.FUNNY_COMMUNITY.idString());
        result.add("funny");
        result.add("community");
        result.add("smp");
        if (tags != null) {
            for (String tag : tags) {
                String normalized = WorldPos.optionalId(tag);
                if (!normalized.isBlank()) {
                    result.add(normalized);
                }
            }
        }
        return Set.copyOf(result);
    }

    private static double funnyWeight(int index) {
        if (index < 5) {
            return 10.0;
        }
        if (index < 10) {
            return 6.0;
        }
        if (index < 14) {
            return 4.0;
        }
        return 2.0;
    }

    private static NameDataPack neutralServerPack() {
        List<NamePattern> patterns = new ArrayList<>();

        addNeutralDeath(patterns, DeathSiteEnvironment.SURFACE,
                "neutral_server_surface_death_site",
                "neutral_server_surface_fallen_site",
                "neutral_server_surface_remembered_ground",
                "neutral_server_surface_ground_loss",
                "neutral_server_surface_last_steps",
                "neutral_server_surface_fallen_memorial",
                "neutral_server_surface_quiet_marker",
                "neutral_server_surface_remembered_field",
                "neutral_server_surface_memorial_marker",
                "neutral_server_surface_loss_marker",
                "neutral_server_surface_old_memorial",
                "neutral_server_surface_last_place",
                "neutral_server_surface_silent_ground",
                "neutral_server_surface_memory_field",
                "neutral_server_surface_fallen_ground",
                "neutral_server_surface_death_marker",
                "neutral_server_surface_lost_steps",
                "neutral_server_surface_remembrance_site",
                "neutral_server_surface_grief_marker",
                "neutral_server_surface_farewell_ground",
                "neutral_server_surface_still_field",
                "neutral_server_surface_solemn_site",
                "neutral_server_surface_lost_ground",
                "neutral_server_surface_memorial_field",
                "neutral_server_surface_last_trace",
                "neutral_server_surface_death_field",
                "neutral_server_surface_loss_site",
                "neutral_server_surface_remembered_steps",
                "neutral_server_surface_quiet_field",
                "neutral_server_surface_marked_loss");
        addNeutralDeath(patterns, DeathSiteEnvironment.CAVE,
                "neutral_server_cave_memorial",
                "neutral_server_cave_death_site",
                "neutral_server_cave_depths_memorial",
                "neutral_server_cave_deep_marker",
                "neutral_server_cave_site_loss",
                "neutral_server_cave_memory_hollow",
                "neutral_server_cave_quiet_cave",
                "neutral_server_cave_depth_marker",
                "neutral_server_cave_fallen_depths",
                "neutral_server_cave_last_tunnel");
        addNeutralDeath(patterns, DeathSiteEnvironment.WATER,
                "neutral_server_water_drowned_site",
                "neutral_server_water_memorial",
                "neutral_server_water_last_crossing",
                "neutral_server_water_quiet_water",
                "neutral_server_water_waterside_death",
                "neutral_server_water_drowned_marker",
                "neutral_server_water_lost_crossing",
                "neutral_server_water_shore_memorial");
        addNeutralDeath(patterns, DeathSiteEnvironment.MOUNTAIN,
                "neutral_server_mountain_memorial",
                "neutral_server_mountain_last_ledge",
                "neutral_server_mountain_fall_site",
                "neutral_server_mountain_summit_memory",
                "neutral_server_mountain_stone_marker",
                "neutral_server_mountain_high_marker",
                "neutral_server_mountain_cliff_memory",
                "neutral_server_mountain_last_slope");
        addNeutralDeath(patterns, DeathSiteEnvironment.NETHER,
                "neutral_server_nether_ashen_memorial",
                "neutral_server_nether_death_site",
                "neutral_server_nether_lava_marker",
                "neutral_server_nether_fire_memory",
                "neutral_server_nether_ash_marker",
                "neutral_server_nether_blackstone_memorial",
                "neutral_server_nether_burning_site");
        addNeutralDeath(patterns, DeathSiteEnvironment.END,
                "neutral_server_end_void_memorial",
                "neutral_server_end_death_site",
                "neutral_server_end_pale_marker",
                "neutral_server_end_last_threshold",
                "neutral_server_end_quiet_edge",
                "neutral_server_end_silent_marker",
                "neutral_server_end_void_site",
                "neutral_server_end_pale_memorial");
        addNeutralDeath(patterns, DeathSiteEnvironment.UNKNOWN,
                "neutral_server_death_unknown_site",
                "neutral_server_death_unknown_memorial",
                "neutral_server_death_unknown_marker",
                "neutral_server_death_unknown_loss",
                "neutral_server_death_unknown_last_trace",
                "neutral_server_death_unknown_remembered");

        addNeutralPatterns(patterns, PlaceType.BATTLEFIELD,
                "neutral_server_battle_site",
                "neutral_server_battlefield_clear",
                "neutral_server_clash_site",
                "neutral_server_battle_marker",
                "neutral_server_conflict_field",
                "neutral_server_combat_zone",
                "neutral_server_clash_memory",
                "neutral_server_fighting_ground",
                "neutral_server_combat_marker",
                "neutral_server_battle_trace",
                "neutral_server_contested_field",
                "neutral_server_old_battle_site");
        addNeutralDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:zombie", "minecraft:zombie_villager", "minecraft:husk", "minecraft:drowned", "group:undead"),
                "neutral_server_battle_undead_site",
                "neutral_server_battle_field_dead",
                "neutral_server_battle_zombie_field",
                "neutral_server_battle_night_clash",
                "neutral_server_battle_dead_marker",
                "neutral_server_battle_undead_marker",
                "neutral_server_battle_lost_dead",
                "neutral_server_battle_dead_field");
        addNeutralDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:skeleton", "minecraft:stray", "minecraft:bogged", "minecraft:wither_skeleton", "group:skeleton"),
                "neutral_server_battle_bone_marker",
                "neutral_server_battle_arrow_field",
                "neutral_server_battle_archer_site",
                "neutral_server_battle_bone_lowland",
                "neutral_server_battle_arrow_trace",
                "neutral_server_battle_skeleton_marker",
                "neutral_server_battle_bone_field",
                "neutral_server_battle_arrow_clash");
        addNeutralDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:creeper", "group:explosive"),
                "neutral_server_battle_creeper_crater",
                "neutral_server_battle_blast_marker",
                "neutral_server_battle_explosion_site",
                "neutral_server_battle_clash_crater",
                "neutral_server_battle_hissing_field",
                "neutral_server_battle_blast_field",
                "neutral_server_battle_crater_marker",
                "neutral_server_battle_creeper_site");
        addNeutralDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:enderman", "group:end"),
                "neutral_server_battle_shadow_site",
                "neutral_server_battle_wanderer_field",
                "neutral_server_battle_shadow_marker",
                "neutral_server_battle_clash_shadow",
                "neutral_server_battle_end_marker",
                "neutral_server_battle_shadow_trace");
        addNeutralDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:blaze", "minecraft:ghast", "minecraft:piglin", "minecraft:piglin_brute", "minecraft:zombified_piglin", "minecraft:hoglin", "minecraft:zoglin", "group:nether"),
                "neutral_server_battle_ashen_clash",
                "neutral_server_battle_fire_site",
                "neutral_server_battle_nether_marker",
                "neutral_server_battle_fire_field",
                "neutral_server_battle_ash_field",
                "neutral_server_battle_nether_clash",
                "neutral_server_battle_flame_marker",
                "neutral_server_battle_field_flame",
                "neutral_server_battle_fire_trace",
                "neutral_server_battle_ashen_marker");
        addNeutralDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("group:illager"),
                "neutral_server_battle_raider_site",
                "neutral_server_battle_raid_marker",
                "neutral_server_battle_ambush_field",
                "neutral_server_battle_clash_banner",
                "neutral_server_battle_illager_marker",
                "neutral_server_battle_raider_field");
        addNeutralDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:warden", "group:boss_like"),
                "neutral_server_battle_silent_clash",
                "neutral_server_battle_sculk_marker",
                "neutral_server_battle_deep_field",
                "neutral_server_battle_warden_site",
                "neutral_server_battle_deep_marker");
        addNeutralDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("minecraft:breeze"),
                "neutral_server_battle_wind_clash",
                "neutral_server_battle_trial_marker",
                "neutral_server_battle_breeze_site",
                "neutral_server_battle_wind_field");
        addNeutralDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("group:aquatic"),
                "neutral_server_battle_aquatic_site",
                "neutral_server_battle_guardian_marker",
                "neutral_server_battle_tide_field");
        addNeutralDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("group:arthropod"),
                "neutral_server_battle_webbed_site",
                "neutral_server_battle_spider_marker",
                "neutral_server_battle_web_field");
        addNeutralDominant(patterns, PlaceType.BATTLEFIELD, mobConstraints("group:slime"),
                "neutral_server_battle_slime_field",
                "neutral_server_battle_slime_marker",
                "neutral_server_battle_soft_clash");

        addNeutralPatterns(patterns, PlaceType.SLAUGHTER_FIELD,
                "neutral_server_slaughter_hunting_ground",
                "neutral_server_slaughter_butchered_pasture",
                "neutral_server_slaughter_herd_field",
                "neutral_server_slaughter_herd_trace",
                "neutral_server_slaughter_old_pasture",
                "neutral_server_slaughter_site",
                "neutral_server_slaughter_hunt_marker",
                "neutral_server_slaughter_pasture_marker",
                "neutral_server_slaughter_herd_marker",
                "neutral_server_slaughter_old_herd_path");
        addNeutralDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("minecraft:cow", "minecraft:mooshroom", "group:farm_animal"),
                "neutral_server_slaughter_cattle_pasture",
                "neutral_server_slaughter_herd_pasture",
                "neutral_server_slaughter_cattle_trace",
                "neutral_server_slaughter_herd_site",
                "neutral_server_slaughter_cow_marker",
                "neutral_server_slaughter_cattle_field");
        addNeutralDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("minecraft:pig"),
                "neutral_server_slaughter_hog_pasture",
                "neutral_server_slaughter_pig_trace",
                "neutral_server_slaughter_boar_pasture",
                "neutral_server_slaughter_pig_marker",
                "neutral_server_slaughter_hog_field",
                "neutral_server_slaughter_pig_site");
        addNeutralDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("minecraft:chicken"),
                "neutral_server_slaughter_feather_yard",
                "neutral_server_slaughter_chicken_trace",
                "neutral_server_slaughter_chicken_marker",
                "neutral_server_slaughter_feather_field",
                "neutral_server_slaughter_chicken_site",
                "neutral_server_slaughter_feather_trace");
        addNeutralDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("minecraft:sheep"),
                "neutral_server_slaughter_sheep_hill",
                "neutral_server_slaughter_wool_field",
                "neutral_server_slaughter_flock_trace",
                "neutral_server_slaughter_sheep_pasture",
                "neutral_server_slaughter_wool_marker",
                "neutral_server_slaughter_flock_site");
        addNeutralDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("group:civilian"),
                "neutral_server_slaughter_tragedy_path",
                "neutral_server_slaughter_place_loss",
                "neutral_server_slaughter_villager_memory",
                "neutral_server_slaughter_civilian_marker",
                "neutral_server_slaughter_trader_trace",
                "neutral_server_slaughter_civilian_trace");
        addNeutralDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("group:companion"),
                "neutral_server_slaughter_quiet_memory",
                "neutral_server_slaughter_companion_trace",
                "neutral_server_slaughter_farewell_site",
                "neutral_server_slaughter_remembered_trail",
                "neutral_server_slaughter_companion_marker");
        addNeutralDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("group:aquatic"),
                "neutral_server_slaughter_water_trace",
                "neutral_server_slaughter_catch_site",
                "neutral_server_slaughter_quiet_shore");
        addNeutralDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("group:guardian_construct"),
                "neutral_server_slaughter_guard_marker",
                "neutral_server_slaughter_broken_guard");
        addNeutralDominant(patterns, PlaceType.SLAUGHTER_FIELD, mobConstraints("group:wild_animal"),
                "neutral_server_slaughter_wild_trace",
                "neutral_server_slaughter_wild_marker");

        addNeutralPatterns(patterns, PlaceType.PVP_ARENA,
                "neutral_server_pvp_duel_arena",
                "neutral_server_pvp_rival_ring",
                "neutral_server_pvp_duel_site",
                "neutral_server_pvp_clash_ground",
                "neutral_server_pvp_honor_arena",
                "neutral_server_pvp_blood_ring",
                "neutral_server_pvp_rival_field",
                "neutral_server_pvp_duelist_marker",
                "neutral_server_pvp_combat_ring",
                "neutral_server_pvp_challenge_field");

        addNeutralPatterns(patterns, PlaceType.MINING_SITE,
                "neutral_server_mining_ore_site",
                "neutral_server_mining_ore_vein",
                "neutral_server_mining_site_clear",
                "neutral_server_mining_deep_vein",
                "neutral_server_mining_ore_trace",
                "neutral_server_mining_quarry_marker",
                "neutral_server_mining_rich_vein",
                "neutral_server_mining_deep_marker");
        addNeutralDominant(patterns, PlaceType.MINING_SITE, blockConstraints("minecraft:diamond_ore", "minecraft:deepslate_diamond_ore"),
                "neutral_server_mining_diamond_vein",
                "neutral_server_mining_deepslate_diamond_vein",
                "neutral_server_mining_diamond_layer",
                "neutral_server_mining_diamond_trace",
                "neutral_server_mining_diamond_ore_marker",
                "neutral_server_mining_diamond_site",
                "neutral_server_mining_diamond_deposit",
                "neutral_server_mining_diamond_cut");
        addNeutralDominant(patterns, PlaceType.MINING_SITE, blockConstraints("minecraft:emerald_ore", "minecraft:deepslate_emerald_ore"),
                "neutral_server_mining_emerald_vein",
                "neutral_server_mining_emerald_layer",
                "neutral_server_mining_emerald_trace",
                "neutral_server_mining_green_vein",
                "neutral_server_mining_emerald_marker",
                "neutral_server_mining_emerald_site");
        addNeutralDominant(patterns, PlaceType.MINING_SITE, blockConstraints("minecraft:ancient_debris"),
                "neutral_server_mining_ancient_debris",
                "neutral_server_mining_buried_relic",
                "neutral_server_mining_netherite_trace",
                "neutral_server_mining_ancient_layer",
                "neutral_server_mining_relic_vein",
                "neutral_server_mining_debris_marker",
                "neutral_server_mining_netherite_marker");

        addNeutralPatterns(patterns, PlaceType.PORTAL_LANDMARK,
                "neutral_server_portal_marker_clear",
                "neutral_server_portal_worldgate",
                "neutral_server_portal_threshold_worlds",
                "neutral_server_portal_old_crossing",
                "neutral_server_portal_crossing_gate",
                "neutral_server_portal_worldbound_trace",
                "neutral_server_portal_path_between_worlds",
                "neutral_server_portal_remembered_threshold",
                "neutral_server_portal_transition_site",
                "neutral_server_portal_crossing_marker",
                "neutral_server_portal_way_marker",
                "neutral_server_portal_old_gate",
                "neutral_server_portal_dimensional_marker",
                "neutral_server_portal_stone_crossing",
                "neutral_server_portal_travel_point",
                "neutral_server_portal_realm_crossing",
                "neutral_server_portal_passage_site",
                "neutral_server_portal_memory_gate",
                "neutral_server_portal_known_crossing",
                "neutral_server_portal_path_marker",
                "neutral_server_portal_quiet_threshold",
                "neutral_server_portal_far_crossing",
                "neutral_server_portal_traveler_gate",
                "neutral_server_portal_wanderer_gate",
                "neutral_server_portal_portal_trace");
        addNeutralDominant(patterns, PlaceType.PORTAL_LANDMARK, portalConstraints("nether"),
                "neutral_server_portal_nether_gate",
                "neutral_server_portal_ashen_gate",
                "neutral_server_portal_nether_threshold",
                "neutral_server_portal_fire_crossing",
                "neutral_server_portal_path_ash",
                "neutral_server_portal_basalt_threshold",
                "neutral_server_portal_lava_gate",
                "neutral_server_portal_nether_marker",
                "neutral_server_portal_lower_world_crossing",
                "neutral_server_portal_ash_marker",
                "neutral_server_portal_nether_crossing",
                "neutral_server_portal_fire_marker");
        addNeutralDominant(patterns, PlaceType.PORTAL_LANDMARK, portalConstraints("end"),
                "neutral_server_portal_end_gate",
                "neutral_server_portal_last_threshold",
                "neutral_server_portal_pale_crossing",
                "neutral_server_portal_void_threshold",
                "neutral_server_portal_eye_gate",
                "neutral_server_portal_endward_path",
                "neutral_server_portal_end_marker",
                "neutral_server_portal_pale_marker",
                "neutral_server_portal_void_crossing",
                "neutral_server_portal_end_threshold");

        addNeutralPatterns(patterns, PlaceType.GENERAL_LANDMARK,
                "neutral_server_general_remembered_trail",
                "neutral_server_general_many_steps",
                "neutral_server_general_old_path",
                "neutral_server_general_marked_place",
                "neutral_server_general_memory_marker",
                "neutral_server_general_old_rest",
                "neutral_server_general_notable_trace",
                "neutral_server_general_crossing_place",
                "neutral_server_general_known_path",
                "neutral_server_general_common_trail",
                "neutral_server_general_travel_marker",
                "neutral_server_general_waypoint",
                "neutral_server_general_old_marker",
                "neutral_server_general_quiet_stop",
                "neutral_server_general_worn_path",
                "neutral_server_general_frequent_trail",
                "neutral_server_general_server_marker",
                "neutral_server_general_remembered_stop",
                "neutral_server_general_simple_landmark",
                "neutral_server_general_trail_marker",
                "neutral_server_general_quiet_trail",
                "neutral_server_general_old_trace",
                "neutral_server_general_road_marker",
                "neutral_server_general_remembered_path",
                "neutral_server_general_stopping_place",
                "neutral_server_general_traveler_trace",
                "neutral_server_general_old_place",
                "neutral_server_general_way_marker");
        addNeutralBiome(patterns, "plains",
                "neutral_server_general_plains_trail",
                "neutral_server_general_plains_trace",
                "neutral_server_general_plains_marker");
        addNeutralBiome(patterns, "forest",
                "neutral_server_general_forest_trail",
                "neutral_server_general_canopy_trace",
                "neutral_server_general_forest_marker");
        addNeutralBiome(patterns, "desert",
                "neutral_server_general_sand_trace",
                "neutral_server_general_desert_marker",
                "neutral_server_general_sand_trail");
        addNeutralBiome(patterns, "snowy",
                "neutral_server_general_snow_trail",
                "neutral_server_general_trace_snow",
                "neutral_server_general_cold_marker");
        addNeutralBiome(patterns, "swamp",
                "neutral_server_general_swamp_trail",
                "neutral_server_general_mire_marker",
                "neutral_server_general_swamp_trace");
        addNeutralBiome(patterns, "mountain", "neutral_server_general_mountain_path", "neutral_server_general_highland_marker");
        addNeutralBiome(patterns, "jungle", "neutral_server_general_jungle_trail", "neutral_server_general_jungle_marker");
        addNeutralBiome(patterns, "badlands", "neutral_server_general_badlands_marker", "neutral_server_general_badlands_trace");
        addNeutralBiome(patterns, "ocean", "neutral_server_general_sea_marker", "neutral_server_general_coastal_trace");
        addNeutralBiome(patterns, "nether", "neutral_server_general_nether_marker", "neutral_server_general_nether_trace");
        addNeutralBiome(patterns, "end", "neutral_server_general_end_marker", "neutral_server_general_end_trace");

        addNeutralPatterns(patterns, PlaceType.SETTLEMENT,
                "neutral_server_settlement_lived_in_place",
                "neutral_server_settlement_old_hearth",
                "neutral_server_settlement_resting_place",
                "neutral_server_settlement_warm_rest",
                "neutral_server_settlement_old_shelter",
                "neutral_server_settlement_settled_trace",
                "neutral_server_settlement_roofed_place",
                "neutral_server_settlement_home_marker",
                "neutral_server_settlement_hearth_memory",
                "neutral_server_settlement_builder_rest",
                "neutral_server_settlement_safe_place",
                "neutral_server_settlement_camp_marker",
                "neutral_server_settlement_worked_ground",
                "neutral_server_settlement_sleeping_place",
                "neutral_server_settlement_known_home",
                "neutral_server_settlement_common_hearth",
                "neutral_server_settlement_old_roof",
                "neutral_server_settlement_living_marker",
                "neutral_server_settlement_shelter_trace",
                "neutral_server_settlement_rest_marker");

        addNeutralPatterns(patterns, PlaceType.FIRST_DISCOVERY,
                "neutral_server_first_discovery_clear",
                "neutral_server_first_find",
                "neutral_server_first_trace_clear",
                "neutral_server_first_remembered_threshold",
                "neutral_server_first_beginning_site",
                "neutral_server_first_record_clear",
                "neutral_server_first_new_path",
                "neutral_server_first_opened_path",
                "neutral_server_first_mark",
                "neutral_server_first_known_find",
                "neutral_server_first_memory",
                "neutral_server_first_noted_place",
                "neutral_server_first_opening",
                "neutral_server_first_path_marker",
                "neutral_server_first_discovery_site",
                "neutral_server_first_world_record",
                "neutral_server_first_landmark",
                "neutral_server_first_found_place",
                "neutral_server_first_early_trace",
                "neutral_server_first_new_record",
                "neutral_server_first_remembered_place",
                "neutral_server_first_memory_record",
                "neutral_server_first_remembered_trace",
                "neutral_server_first_memory_place");
        addNeutralExactFirst(patterns, "world:first_stronghold_found",
                "neutral_server_first_stronghold_citadel",
                "neutral_server_first_stronghold_first",
                "neutral_server_first_stronghold_eye_halls",
                "neutral_server_first_stronghold_hidden_threshold",
                "neutral_server_first_stronghold_eye_citadel",
                "neutral_server_first_stronghold_eye_threshold",
                "neutral_server_first_stronghold_stone_halls",
                "neutral_server_first_stronghold_path_eye",
                "neutral_server_first_stronghold_found_halls",
                "neutral_server_first_stronghold_endward_site",
                "neutral_server_first_stronghold_record",
                "neutral_server_first_stronghold_marker");
        addNeutralExactFirst(patterns, "world:first_diamond_ore_mined",
                "neutral_server_first_diamond",
                "neutral_server_first_diamond_vein",
                "neutral_server_first_glitter",
                "neutral_server_first_diamond_light",
                "neutral_server_first_diamond_trace",
                "neutral_server_first_diamond_marker",
                "neutral_server_first_diamond_find",
                "neutral_server_first_diamond_record");
        addNeutralExactFirst(patterns, "world:first_ancient_debris_mined",
                "neutral_server_first_ancient_debris",
                "neutral_server_first_buried_relic",
                "neutral_server_first_ancient_trace",
                "neutral_server_first_netherite_trace",
                "neutral_server_first_debris_marker",
                "neutral_server_first_ancient_record");
        addNeutralExactFirst(patterns, "world:first_nether_entry",
                "neutral_server_first_nether_descent",
                "neutral_server_first_nether_gate",
                "neutral_server_first_path_below",
                "neutral_server_first_ashen_gate",
                "neutral_server_first_nether_marker",
                "neutral_server_first_lower_world_path");
        addNeutralExactFirst(patterns, "world:first_end_entry",
                "neutral_server_first_end_last_threshold",
                "neutral_server_first_end_pale_crossing",
                "neutral_server_first_endward_path",
                "neutral_server_first_end_threshold",
                "neutral_server_first_end_marker",
                "neutral_server_first_end_record");
        addNeutralExactFirst(patterns, "world:first_ender_dragon_killed",
                "neutral_server_first_dragonfall",
                "neutral_server_first_dragon_record",
                "neutral_server_first_end_victory");

        addNeutralPatterns(patterns, PlaceType.BOSS_SITE,
                "neutral_server_boss_strong_foe_site",
                "neutral_server_boss_beast_lair",
                "neutral_server_boss_great_clash_site",
                "neutral_server_boss_fallen_lair",
                "neutral_server_boss_clash_ground",
                "neutral_server_boss_strong_foe_trace",
                "neutral_server_boss_major_threat_site",
                "neutral_server_boss_old_lair",
                "neutral_server_boss_heavy_marker",
                "neutral_server_boss_final_ground",
                "neutral_server_boss_victory_site",
                "neutral_server_boss_marked_lair",
                "neutral_server_boss_danger_site",
                "neutral_server_boss_hard_won_ground",
                "neutral_server_boss_lost_lair",
                "neutral_server_boss_defeated_site",
                "neutral_server_boss_memory_ground",
                "neutral_server_boss_large_clash",
                "neutral_server_boss_silent_lair",
                "neutral_server_boss_fallen_foe_site");
        addNeutralDominant(patterns, PlaceType.BOSS_SITE, bossConstraints("minecraft:ender_dragon"),
                "neutral_server_boss_dragonfall",
                "neutral_server_boss_dragon_fall",
                "neutral_server_boss_dragonfall_site",
                "neutral_server_boss_dragon_trace",
                "neutral_server_boss_dragon_rest",
                "neutral_server_boss_fallen_dragon_threshold",
                "neutral_server_boss_ender_dragon_site",
                "neutral_server_boss_dragon_marker",
                "neutral_server_boss_dragon_ground",
                "neutral_server_boss_end_dragon_fall");
        addNeutralDominant(patterns, PlaceType.BOSS_SITE, bossConstraints("minecraft:wither"),
                "neutral_server_boss_witherfall",
                "neutral_server_boss_wither_fall",
                "neutral_server_boss_wither_trace",
                "neutral_server_boss_withering_site",
                "neutral_server_boss_wither_marker",
                "neutral_server_boss_wither_shadow",
                "neutral_server_boss_wither_ground");
        addNeutralDominant(patterns, PlaceType.BOSS_SITE, bossConstraints("minecraft:warden"),
                "neutral_server_boss_silent_deep",
                "neutral_server_boss_sculk_depths",
                "neutral_server_boss_warden_silence",
                "neutral_server_boss_deep_guardian_trace",
                "neutral_server_boss_warden_site",
                "neutral_server_boss_sculk_marker");
        addNeutralDominant(patterns, PlaceType.BOSS_SITE, bossConstraints("minecraft:elder_guardian"),
                "neutral_server_boss_guardian_waters",
                "neutral_server_boss_temple_deep",
                "neutral_server_boss_elder_wave_fall",
                "neutral_server_boss_elder_guardian_site",
                "neutral_server_boss_ocean_guardian_marker",
                "neutral_server_boss_temple_marker");

        addNeutralPatterns(patterns, PlaceType.RAID_SITE,
                "neutral_server_raid_defense_site",
                "neutral_server_raid_field_defense",
                "neutral_server_raid_victory_banner",
                "neutral_server_raid_village_line",
                "neutral_server_raid_repelled_site",
                "neutral_server_raid_marker",
                "neutral_server_raid_defended_place",
                "neutral_server_raid_guarded_line");
        addNeutralPatterns(patterns, PlaceType.PET_MEMORIAL,
                "neutral_server_pet_quiet_memory",
                "neutral_server_pet_companion_trace",
                "neutral_server_pet_farewell_site",
                "neutral_server_pet_remembered_trail",
                "neutral_server_pet_loyal_memory",
                "neutral_server_pet_gentle_marker");
        addNeutralPatterns(patterns, PlaceType.NAMED_MOB_MEMORIAL,
                "neutral_server_named_memory",
                "neutral_server_named_fall",
                "neutral_server_named_last_trace",
                "neutral_server_named_place",
                "neutral_server_named_remembered_trace",
                "neutral_server_named_marker");
        addNeutralPatterns(patterns, PlaceType.DIMENSION_THRESHOLD,
                "neutral_server_dimension_threshold_worlds",
                "neutral_server_dimension_worldbound_trace",
                "neutral_server_dimension_path_between_worlds",
                "neutral_server_dimension_border",
                "neutral_server_dimension_transition_site",
                "neutral_server_dimension_crossing_marker");
        addNeutralPatterns(patterns, PlaceType.CUSTOM,
                "neutral_server_custom_remembered_place",
                "neutral_server_custom_named_place",
                "neutral_server_custom_place_memory",
                "neutral_server_custom_old_place",
                "neutral_server_custom_memory_marker",
                "neutral_server_custom_marked_place");

        return new NameDataPack(NameStyle.NEUTRAL_SERVER.idString(), List.copyOf(patterns), smallStyleTokens());
    }

    private static void addNeutralPatterns(List<NamePattern> patterns, PlaceType placeType, String... ids) {
        for (int index = 0; index < ids.length; index++) {
            patterns.add(neutralPattern(ids[index], placeType, neutralWeight(index), Set.of(), NameCauseConstraints.none(), "place_type"));
        }
    }

    private static void addNeutralDeath(List<NamePattern> patterns, DeathSiteEnvironment environment, String... ids) {
        for (int index = 0; index < ids.length; index++) {
            patterns.add(neutralPattern(ids[index], PlaceType.DEATH_SITE, neutralWeight(index), Set.of(environment), NameCauseConstraints.none(), environment.idString()));
        }
    }

    private static void addNeutralExactFirst(List<NamePattern> patterns, String firstDiscoveryKey, String... ids) {
        NameCauseConstraints constraints = NameCauseConstraints.builder()
                .requiredFirstDiscoveryKey(firstDiscoveryKey)
                .build();
        for (int index = 0; index < ids.length; index++) {
            patterns.add(neutralPattern(ids[index], PlaceType.FIRST_DISCOVERY, neutralWeight(index), Set.of(), constraints, "exact_cause"));
        }
    }

    private static void addNeutralDominant(List<NamePattern> patterns, PlaceType placeType, NameCauseConstraints constraints, String... ids) {
        for (int index = 0; index < ids.length; index++) {
            patterns.add(neutralPattern(ids[index], placeType, neutralWeight(index), Set.of(), constraints, "dominant_target"));
        }
    }

    private static void addNeutralBiome(List<NamePattern> patterns, String biomeGroup, String... ids) {
        NameCauseConstraints constraints = NameCauseConstraints.builder()
                .requiredCauseType(PlaceCauseType.VISITS)
                .requiredBiomeGroups(biomeGroup)
                .build();
        for (int index = 0; index < ids.length; index++) {
            patterns.add(neutralPattern(ids[index], PlaceType.GENERAL_LANDMARK, neutralWeight(index), Set.of(), constraints, "biome", biomeGroup));
        }
    }

    private static NamePattern neutralPattern(
            String id,
            PlaceType placeType,
            double weight,
            Set<DeathSiteEnvironment> environments,
            NameCauseConstraints constraints,
            String... tags
    ) {
        return new NamePattern(
                id,
                "living_legends.name.pattern." + id,
                Set.of(placeType),
                environments,
                weight,
                List.of(),
                neutralTags(tags),
                NameSemanticRoots.inferPatternRoot(id),
                constraints,
                Set.of()
        );
    }

    private static Set<String> neutralTags(String... tags) {
        Set<String> result = new LinkedHashSet<>();
        result.add(NameStyle.NEUTRAL_SERVER.idString());
        if (tags != null) {
            for (String tag : tags) {
                String normalized = WorldPos.optionalId(tag);
                if (!normalized.isBlank()) {
                    result.add(normalized);
                }
            }
        }
        return Set.copyOf(result);
    }

    private static double neutralWeight(int index) {
        if (index < 8) {
            return 10.0;
        }
        if (index < 16) {
            return 5.0;
        }
        return 2.0;
    }

    private static NameCauseConstraints mobConstraints(String... mobTypes) {
        return NameCauseConstraints.builder()
                .requiredDominantMobTypes(mobTypes)
                .build();
    }

    private static NameCauseConstraints blockConstraints(String... blockIds) {
        return NameCauseConstraints.builder()
                .requiredDominantValuableBlocks(blockIds)
                .build();
    }

    private static NameCauseConstraints portalConstraints(String portalType) {
        return NameCauseConstraints.builder()
                .requiredPortalType(portalType)
                .build();
    }

    private static NameCauseConstraints bossConstraints(String bossId) {
        return NameCauseConstraints.builder()
                .requiredBossId(bossId)
                .build();
    }

    private static final class Holder {
        private static final NameDataPack DEFAULT_PACK = new NameDataPack(
                DEFAULT_STYLE_ID,
                List.of(
                        deathPatternWithSlot("death_site_cave", DeathSiteEnvironment.CAVE, 1.0, "death_subject", NameTokenForm.GENITIVE),
                        deathPatternWithSlot("death_site_cave_depths", DeathSiteEnvironment.CAVE, 0.9, "death_subject", NameTokenForm.GENITIVE),
                        deathPatternWithSlot("death_site_cave_echo", DeathSiteEnvironment.CAVE, 0.7, "echo", NameTokenForm.GENITIVE),
                        deathPatternWithSlot("death_site_surface", DeathSiteEnvironment.SURFACE, 1.0, "death_subject", NameTokenForm.GENITIVE),
                        deathPatternWithSlot("death_site_surface_ground", DeathSiteEnvironment.SURFACE, 0.9, "death_subject", NameTokenForm.GENITIVE),
                        deathPatternWithSlot("death_site_surface_meadow", DeathSiteEnvironment.SURFACE, 0.7, "death_subject", NameTokenForm.GENITIVE),
                        deathPattern("death_site_water", DeathSiteEnvironment.WATER, 1.0, "death_subject", "water_place"),
                        deathPatternWithSlot("death_site_water_waters", DeathSiteEnvironment.WATER, 0.9, "death_subject", NameTokenForm.GENITIVE),
                        deathPatternWithSlot("death_site_water_shore", DeathSiteEnvironment.WATER, 0.7, "water_place", NameTokenForm.GENITIVE),
                        deathPattern("death_site_mountain", DeathSiteEnvironment.MOUNTAIN, 1.0, "death_subject", "mountain_place"),
                        deathPatternWithSlot("death_site_mountain_heights", DeathSiteEnvironment.MOUNTAIN, 0.9, "death_subject", NameTokenForm.GENITIVE),
                        deathPatternWithSlot("death_site_mountain_peak", DeathSiteEnvironment.MOUNTAIN, 0.7, "mountain_place", NameTokenForm.GENITIVE),
                        deathPattern("death_site_nether", DeathSiteEnvironment.NETHER, 1.0, "death_subject", "nether_place"),
                        deathPatternWithSlot("death_site_nether_ashes", DeathSiteEnvironment.NETHER, 0.9, "death_subject", NameTokenForm.GENITIVE),
                        deathPatternWithSlot("death_site_nether_embers", DeathSiteEnvironment.NETHER, 0.7, "nether_place", NameTokenForm.GENITIVE),
                        deathPattern("death_site_end", DeathSiteEnvironment.END, 1.0, "death_subject", "end_place"),
                        deathPatternWithSlot("death_site_end_void", DeathSiteEnvironment.END, 0.9, "death_subject", NameTokenForm.GENITIVE),
                        deathPatternWithSlot("death_site_end_chorus", DeathSiteEnvironment.END, 0.7, "end_place", NameTokenForm.GENITIVE),
                        deathPatternWithSlot("death_site_unknown", DeathSiteEnvironment.UNKNOWN, 1.0, "death_subject", NameTokenForm.GENITIVE),
                        deathPatternWithSlot("death_site_unknown_memory", DeathSiteEnvironment.UNKNOWN, 0.7, "death_subject", NameTokenForm.GENITIVE),
                        deathPattern("va_death_surface_field_fallen", DeathSiteEnvironment.SURFACE, 1.3),
                        deathPattern("va_death_surface_last_steps", DeathSiteEnvironment.SURFACE, 1.2),
                        deathPattern("va_death_surface_place_loss", DeathSiteEnvironment.SURFACE, 1.1),
                        deathPattern("va_death_surface_fallen_ground", DeathSiteEnvironment.SURFACE, 1.0),
                        deathPattern("va_death_surface_warning_stone", DeathSiteEnvironment.SURFACE, 0.8),
                        deathPattern("va_death_surface_quiet_marker", DeathSiteEnvironment.SURFACE, 0.7),
                        deathPattern("va_death_surface_last_path", DeathSiteEnvironment.SURFACE, 1.0),
                        deathPattern("va_death_surface_silent_ground", DeathSiteEnvironment.SURFACE, 0.95),
                        deathPattern("va_death_surface_old_loss", DeathSiteEnvironment.SURFACE, 0.9),
                        deathPattern("va_death_surface_memory_stone", DeathSiteEnvironment.SURFACE, 0.9),
                        deathPattern("va_death_surface_fallen_meadow", DeathSiteEnvironment.SURFACE, 0.9),
                        deathPattern("va_death_surface_lone_marker", DeathSiteEnvironment.SURFACE, 0.85),
                        deathPattern("va_death_surface_sorrow_field", DeathSiteEnvironment.SURFACE, 0.85),
                        deathPattern("va_death_surface_last_trace", DeathSiteEnvironment.SURFACE, 0.85),
                        deathPattern("va_death_surface_remembered_ground", DeathSiteEnvironment.SURFACE, 0.8),
                        deathPattern("va_death_surface_grass_rest", DeathSiteEnvironment.SURFACE, 0.8),
                        deathPattern("va_death_surface_still_field", DeathSiteEnvironment.SURFACE, 0.8),
                        deathPattern("va_death_surface_warning_field", DeathSiteEnvironment.SURFACE, 0.75),
                        deathPattern("va_death_surface_memory_ground", DeathSiteEnvironment.SURFACE, 0.75),
                        deathPattern("va_death_surface_fallen_steps", DeathSiteEnvironment.SURFACE, 0.75),
                        deathPattern("va_death_surface_quiet_grass", DeathSiteEnvironment.SURFACE, 0.7),
                        deathPattern("va_death_surface_old_marker", DeathSiteEnvironment.SURFACE, 0.7),
                        deathPattern("va_death_surface_lost_path", DeathSiteEnvironment.SURFACE, 0.7),
                        deathPattern("va_death_surface_last_meadow", DeathSiteEnvironment.SURFACE, 0.65),
                        deathPattern("va_death_surface_silent_steps", DeathSiteEnvironment.SURFACE, 0.65),
                        deathPattern("va_death_surface_farewell_ground", DeathSiteEnvironment.SURFACE, 0.65),
                        deathPattern("va_death_surface_marker_loss", DeathSiteEnvironment.SURFACE, 0.6),
                        deathPattern("va_death_cave_hollow_fallen", DeathSiteEnvironment.CAVE, 1.3),
                        deathPattern("va_death_cave_lost_picks", DeathSiteEnvironment.CAVE, 1.2),
                        deathPattern("va_death_cave_last_breath", DeathSiteEnvironment.CAVE, 1.1),
                        deathPattern("va_death_cave_dark_echo", DeathSiteEnvironment.CAVE, 1.0),
                        deathPattern("va_death_cave_silent_hollow", DeathSiteEnvironment.CAVE, 0.8),
                        deathPattern("va_death_cave_deep_rest", DeathSiteEnvironment.CAVE, 0.7),
                        deathPattern("va_death_water_drowned_place", DeathSiteEnvironment.WATER, 1.3),
                        deathPattern("va_death_water_final_crossing", DeathSiteEnvironment.WATER, 1.2),
                        deathPattern("va_death_water_deep_water", DeathSiteEnvironment.WATER, 1.0),
                        deathPattern("va_death_water_last_ford", DeathSiteEnvironment.WATER, 0.9),
                        deathPattern("va_death_water_quiet_current", DeathSiteEnvironment.WATER, 0.8),
                        deathPattern("va_death_mountain_last_ledge", DeathSiteEnvironment.MOUNTAIN, 1.3),
                        deathPattern("va_death_mountain_fallen_ascent", DeathSiteEnvironment.MOUNTAIN, 1.2),
                        deathPattern("va_death_mountain_cold_wind", DeathSiteEnvironment.MOUNTAIN, 1.0),
                        deathPattern("va_death_mountain_stone_rest", DeathSiteEnvironment.MOUNTAIN, 0.9),
                        deathPattern("va_death_mountain_high_fall", DeathSiteEnvironment.MOUNTAIN, 0.8),
                        deathPattern("va_death_nether_ashen_rest", DeathSiteEnvironment.NETHER, 1.3),
                        deathPattern("va_death_nether_basalt_rest", DeathSiteEnvironment.NETHER, 1.1),
                        deathPattern("va_death_nether_blackstone_rest", DeathSiteEnvironment.NETHER, 1.0),
                        deathPattern("va_death_nether_lava_grave", DeathSiteEnvironment.NETHER, 0.9),
                        deathPattern("va_death_nether_fire_mark", DeathSiteEnvironment.NETHER, 0.8),
                        deathPattern("va_death_end_void_rest", DeathSiteEnvironment.END, 1.3),
                        deathPattern("va_death_end_pale_hollow", DeathSiteEnvironment.END, 1.1),
                        deathPattern("va_death_end_last_threshold", DeathSiteEnvironment.END, 1.0),
                        deathPattern("va_death_end_silent_void", DeathSiteEnvironment.END, 0.9),
                        deathPattern("va_death_end_pale_stone", DeathSiteEnvironment.END, 0.8),
                        deathPattern("va_death_unknown_remembered_place", DeathSiteEnvironment.UNKNOWN, 1.0),
                        deathPattern("va_death_unknown_place_loss", DeathSiteEnvironment.UNKNOWN, 0.9),
                        deathPattern("va_death_unknown_old_warning", DeathSiteEnvironment.UNKNOWN, 0.7),
                        deathCausePattern("va_death_cause_fall_cliff", DeathSiteEnvironment.MOUNTAIN, "fall", 1.4),
                        deathCausePattern("va_death_cause_fall_ledge", DeathSiteEnvironment.SURFACE, "fall", 1.1),
                        deathCausePattern("va_death_cause_lava_ashen", DeathSiteEnvironment.NETHER, "lava", 1.4),
                        deathCausePattern("va_death_cause_fire_rest", DeathSiteEnvironment.NETHER, "fire", 1.2),
                        deathCausePattern("va_death_cause_drowning_crossing", DeathSiteEnvironment.WATER, "drowning", 1.4),
                        deathCausePattern("va_death_cause_void_rest", DeathSiteEnvironment.END, "void", 1.4),
                        pattern("battlefield", PlaceType.BATTLEFIELD, 1.0, "battle_subject", "battle_place"),
                        patternWithSlot("battlefield_clash", PlaceType.BATTLEFIELD, 0.9, "battle_subject", NameTokenForm.GENITIVE),
                        patternWithSlots("battlefield_banner", PlaceType.BATTLEFIELD, 0.7,
                                slot("token_1", "battle_subject", NameTokenForm.ADJECTIVE_NEUT),
                                slot("token_2", "battle_symbol_neut", NameTokenForm.BASE)),
                        patternWithSlots("battlefield_symbol_masc", PlaceType.BATTLEFIELD, 0.65,
                                slot("token_1", "battle_subject", NameTokenForm.ADJECTIVE_MASC),
                                slot("token_2", "battle_symbol_masc", NameTokenForm.BASE)),
                        patternWithSlot("battlefield_bannerfall", PlaceType.BATTLEFIELD, 0.8, "battle_symbol", NameTokenForm.GENITIVE),
                        dominantTargetPattern("battlefield_zombie_deadfield", PlaceType.BATTLEFIELD, 1.2,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:zombie").build()),
                        dominantTargetPattern("battlefield_zombie_field_dead", PlaceType.BATTLEFIELD, 1.0,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:zombie").build()),
                        dominantTargetPattern("battlefield_skeleton_bonefield", PlaceType.BATTLEFIELD, 1.2,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:skeleton").build()),
                        dominantTargetPattern("battlefield_skeleton_arrowfall", PlaceType.BATTLEFIELD, 1.0,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:skeleton").build()),
                        dominantTargetPattern("battlefield_skeleton_bone_lowland", PlaceType.BATTLEFIELD, 0.9,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:skeleton").build()),
                        dominantTargetPattern("battlefield_creeper_crater", PlaceType.BATTLEFIELD, 1.2,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:creeper").build()),
                        dominantTargetPattern("battlefield_creeper_hissing_field", PlaceType.BATTLEFIELD, 1.0,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:creeper").build()),
                        dominantTargetPattern("battlefield_creeper_blastmark", PlaceType.BATTLEFIELD, 0.9,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:creeper").build()),
                        dominantTargetPattern("battlefield_blaze_field", PlaceType.BATTLEFIELD, 1.2,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:blaze").build()),
                        dominantTargetPattern("battlefield_blaze_flame_field", PlaceType.BATTLEFIELD, 1.0,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:blaze").build()),
                        dominantTargetPattern("battlefield_enderman_shadowfield", PlaceType.BATTLEFIELD, 1.1,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:enderman").build()),
                        dominantTargetPattern("battlefield_warden_silent_dark", PlaceType.BATTLEFIELD, 1.1,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:warden").build()),
                        dominantTargetPattern("battlefield_breeze_trial_winds", PlaceType.BATTLEFIELD, 1.1,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:breeze").build()),
                        pattern("va_battle_echo_field", PlaceType.BATTLEFIELD, 0.9),
                        pattern("va_battle_fallen_banner", PlaceType.BATTLEFIELD, 0.8),
                        pattern("va_battle_bloodied_ground", PlaceType.BATTLEFIELD, 0.7),
                        pattern("va_battle_old_skirmish", PlaceType.BATTLEFIELD, 0.7),
                        dominantTargetPattern("va_battle_zombie_dead_lowland", PlaceType.BATTLEFIELD, 1.1,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:zombie", "minecraft:zombie_villager", "minecraft:husk", "minecraft:drowned").build()),
                        dominantTargetPattern("va_battle_zombie_restless_field", PlaceType.BATTLEFIELD, 1.0,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:zombie", "minecraft:zombie_villager", "minecraft:husk", "minecraft:drowned").build()),
                        dominantTargetPattern("va_battle_skeleton_arrow_ground", PlaceType.BATTLEFIELD, 1.1,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:skeleton", "minecraft:stray", "minecraft:bogged", "minecraft:wither_skeleton").build()),
                        dominantTargetPattern("va_battle_skeleton_bone_hollow", PlaceType.BATTLEFIELD, 1.0,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:skeleton", "minecraft:stray", "minecraft:bogged", "minecraft:wither_skeleton").build()),
                        dominantTargetPattern("va_battle_creeper_blastfield", PlaceType.BATTLEFIELD, 1.1,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:creeper").build()),
                        dominantTargetPattern("va_battle_creeper_spark_crater", PlaceType.BATTLEFIELD, 1.0,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:creeper").build()),
                        dominantTargetPattern("va_battle_enderman_shadow_field", PlaceType.BATTLEFIELD, 1.1,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:enderman").build()),
                        dominantTargetPattern("va_battle_enderman_long_shadow", PlaceType.BATTLEFIELD, 0.9,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:enderman").build()),
                        dominantTargetPattern("va_battle_blaze_ashen_arena", PlaceType.BATTLEFIELD, 1.1,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:blaze").build()),
                        dominantTargetPattern("va_battle_blaze_fire_ground", PlaceType.BATTLEFIELD, 1.0,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:blaze").build()),
                        dominantTargetPattern("va_battle_pillager_raider_field", PlaceType.BATTLEFIELD, 1.1,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:pillager", "minecraft:vindicator", "minecraft:evoker", "minecraft:ravager", "minecraft:vex").build()),
                        dominantTargetPattern("va_battle_pillager_banner_field", PlaceType.BATTLEFIELD, 1.0,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:pillager", "minecraft:vindicator", "minecraft:evoker", "minecraft:ravager", "minecraft:vex").build()),
                        dominantTargetPattern("va_battle_spider_web_hollow", PlaceType.BATTLEFIELD, 0.9,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:arthropod").build()),
                        dominantTargetPattern("va_battle_slime_lowland", PlaceType.BATTLEFIELD, 0.9,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:slime").build()),
                        dominantTargetPattern("va_battle_aquatic_tide_field", PlaceType.BATTLEFIELD, 0.9,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:aquatic").build()),
                        dominantTargetPattern("va_battle_warden_sculk_depths", PlaceType.BATTLEFIELD, 1.0,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:warden", "group:boss_like").build()),
                        dominantTargetPattern("va_battle_breeze_windfield", PlaceType.BATTLEFIELD, 1.0,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:breeze").build()),
                        dominantTargetPattern("battlefield_group_undead", PlaceType.BATTLEFIELD, 0.85,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:undead").build()),
                        dominantTargetPattern("battlefield_group_nether", PlaceType.BATTLEFIELD, 0.85,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:nether").build()),
                        dominantTargetPattern("battlefield_group_end", PlaceType.BATTLEFIELD, 0.85,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:end").build()),
                        dominantTargetPattern("battlefield_group_illager", PlaceType.BATTLEFIELD, 0.85,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:illager").build()),
                        dominantTargetPattern("battlefield_group_aquatic", PlaceType.BATTLEFIELD, 0.8,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:aquatic").build()),
                        dominantTargetPattern("battlefield_group_arthropod", PlaceType.BATTLEFIELD, 0.8,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:arthropod").build()),
                        dominantTargetPattern("battlefield_group_slime", PlaceType.BATTLEFIELD, 0.8,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:slime").build()),
                        dominantTargetPattern("battlefield_group_flying", PlaceType.BATTLEFIELD, 0.75,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:flying").build()),
                        dominantTargetPattern("battlefield_group_magic", PlaceType.BATTLEFIELD, 0.75,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:magic").build()),
                        dominantTargetPattern("battlefield_group_boss_like", PlaceType.BATTLEFIELD, 0.75,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:boss_like").build()),
                        patternWithSlot("slaughter_field", PlaceType.SLAUGHTER_FIELD, 1.0, "slaughter_subject", NameTokenForm.GENITIVE),
                        patternWithSlot("slaughter_pasture", PlaceType.SLAUGHTER_FIELD, 0.9, "slaughter_subject", NameTokenForm.GENITIVE),
                        patternWithSlots("slaughter_trampled", PlaceType.SLAUGHTER_FIELD, 0.25,
                                new NamePatternSlot("token_1", "slaughter_subject", NameTokenForm.GENITIVE, Set.of("trampled"))),
                        dominantTargetPattern("slaughter_cow_butchered_pasture", PlaceType.SLAUGHTER_FIELD, 1.2,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:cow").build()),
                        dominantTargetPattern("slaughter_cow_field_herd", PlaceType.SLAUGHTER_FIELD, 1.0,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:cow").build()),
                        dominantTargetPattern("slaughter_cow_herd_pasture", PlaceType.SLAUGHTER_FIELD, 0.9,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:cow").build()),
                        dominantTargetPattern("slaughter_chicken_featherfall", PlaceType.SLAUGHTER_FIELD, 1.2,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:chicken").build()),
                        dominantTargetPattern("slaughter_chicken_feather_yard", PlaceType.SLAUGHTER_FIELD, 1.0,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:chicken").build()),
                        dominantTargetPattern("slaughter_pig_hog_pasture", PlaceType.SLAUGHTER_FIELD, 1.2,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:pig").build()),
                        dominantTargetPattern("slaughter_pig_field_hogs", PlaceType.SLAUGHTER_FIELD, 1.0,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:pig").build()),
                        dominantTargetPattern("slaughter_sheep_wool_field", PlaceType.SLAUGHTER_FIELD, 1.2,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:sheep").build()),
                        dominantTargetPattern("slaughter_sheep_field_flock", PlaceType.SLAUGHTER_FIELD, 1.0,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:sheep").build()),
                        pattern("va_slaughter_old_pasture", PlaceType.SLAUGHTER_FIELD, 0.9),
                        pattern("va_slaughter_hunt_field", PlaceType.SLAUGHTER_FIELD, 0.8),
                        pattern("va_slaughter_herd_trail", PlaceType.SLAUGHTER_FIELD, 0.7),
                        pattern("va_slaughter_hunt_pasture", PlaceType.SLAUGHTER_FIELD, 0.9),
                        pattern("va_slaughter_butchered_pasture", PlaceType.SLAUGHTER_FIELD, 0.85),
                        pattern("va_slaughter_field_slaughter", PlaceType.SLAUGHTER_FIELD, 0.8),
                        pattern("va_slaughter_herd_trace", PlaceType.SLAUGHTER_FIELD, 0.75),
                        pattern("va_slaughter_trampled_meadow", PlaceType.SLAUGHTER_FIELD, 0.75),
                        pattern("va_slaughter_herd_meadow", PlaceType.SLAUGHTER_FIELD, 0.7),
                        pattern("va_slaughter_hunting_ground", PlaceType.SLAUGHTER_FIELD, 0.7),
                        pattern("va_slaughter_shepherd_trace", PlaceType.SLAUGHTER_FIELD, 0.65),
                        pattern("va_slaughter_trampled_field", PlaceType.SLAUGHTER_FIELD, 0.65),
                        dominantTargetPattern("va_slaughter_cow_cattle_meadow", PlaceType.SLAUGHTER_FIELD, 1.1,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:cow", "minecraft:mooshroom").build()),
                        dominantTargetPattern("va_slaughter_cow_herd_field", PlaceType.SLAUGHTER_FIELD, 1.0,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:cow", "minecraft:mooshroom").build()),
                        dominantTargetPattern("va_slaughter_pig_hog_field", PlaceType.SLAUGHTER_FIELD, 1.1,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:pig").build()),
                        dominantTargetPattern("va_slaughter_pig_pasture_trace", PlaceType.SLAUGHTER_FIELD, 0.9,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:pig").build()),
                        dominantTargetPattern("va_slaughter_chicken_feather_field", PlaceType.SLAUGHTER_FIELD, 1.1,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:chicken").build()),
                        dominantTargetPattern("va_slaughter_chicken_little_feathers", PlaceType.SLAUGHTER_FIELD, 0.9,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:chicken").build()),
                        dominantTargetPattern("va_slaughter_sheep_flock_hill", PlaceType.SLAUGHTER_FIELD, 1.1,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:sheep").build()),
                        dominantTargetPattern("va_slaughter_sheep_wool_slope", PlaceType.SLAUGHTER_FIELD, 0.9,
                                NameCauseConstraints.builder().requiredDominantMobTypes("minecraft:sheep").build()),
                        dominantTargetPattern("va_slaughter_fish_quiet_water", PlaceType.SLAUGHTER_FIELD, 1.0,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:aquatic").build()),
                        dominantTargetPattern("va_slaughter_fish_catchwater", PlaceType.SLAUGHTER_FIELD, 0.9,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:aquatic").build()),
                        dominantTargetPattern("va_slaughter_villager_tragedy_path", PlaceType.SLAUGHTER_FIELD, 1.1,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:civilian").build()),
                        dominantTargetPattern("va_slaughter_villager_place_loss", PlaceType.SLAUGHTER_FIELD, 1.0,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:civilian").build()),
                        dominantTargetPattern("va_slaughter_companion_soft_rest", PlaceType.SLAUGHTER_FIELD, 1.0,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:companion").build()),
                        dominantTargetPattern("va_slaughter_golem_broken_guard", PlaceType.SLAUGHTER_FIELD, 0.9,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:guardian_construct").build()),
                        dominantTargetPattern("va_slaughter_wild_trail", PlaceType.SLAUGHTER_FIELD, 0.8,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:wild_animal").build()),
                        dominantTargetPattern("slaughter_group_farm_animal", PlaceType.SLAUGHTER_FIELD, 0.85,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:farm_animal").build()),
                        dominantTargetPattern("slaughter_group_mount", PlaceType.SLAUGHTER_FIELD, 0.8,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:mount").build()),
                        dominantTargetPattern("slaughter_group_aquatic", PlaceType.SLAUGHTER_FIELD, 0.8,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:aquatic").build()),
                        dominantTargetPattern("slaughter_group_companion", PlaceType.SLAUGHTER_FIELD, 0.8,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:companion").build()),
                        dominantTargetPattern("slaughter_group_civilian", PlaceType.SLAUGHTER_FIELD, 0.8,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:civilian").build()),
                        dominantTargetPattern("slaughter_group_guardian_construct", PlaceType.SLAUGHTER_FIELD, 0.75,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:guardian_construct").build()),
                        dominantTargetPattern("slaughter_group_ambient", PlaceType.SLAUGHTER_FIELD, 0.7,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:ambient").build()),
                        dominantTargetPattern("slaughter_group_wild_animal", PlaceType.SLAUGHTER_FIELD, 0.75,
                                NameCauseConstraints.builder().requiredDominantMobTypes("group:wild_animal").build()),
                        patternWithSlot("pvp_arena", PlaceType.PVP_ARENA, 1.0, "pvp_subject", NameTokenForm.ADJECTIVE_FEM),
                        patternWithSlot("pvp_ring", PlaceType.PVP_ARENA, 0.9, "combatant", NameTokenForm.GENITIVE),
                        patternWithSlots("pvp_challenge", PlaceType.PVP_ARENA, 0.7,
                                slot("token_1", "pvp_subject", NameTokenForm.ADJECTIVE_FEM),
                                slot("token_2", "pvp_place", NameTokenForm.BASE)),
                        patternWithSlot("pvp_duelist_ring", PlaceType.PVP_ARENA, 0.9, "combatant", NameTokenForm.GENITIVE),
                        patternWithSlot("pvp_rival_ground", PlaceType.PVP_ARENA, 0.8, "combatant", NameTokenForm.GENITIVE),
                        patternWithSlots("pvp_champion_arena", PlaceType.PVP_ARENA, 0.6,
                                slot("token_1", "champion", NameTokenForm.ADJECTIVE_FEM),
                                slot("token_2", "pvp_place", NameTokenForm.BASE)),
                        pattern("va_pvp_duelist_ring", PlaceType.PVP_ARENA, 1.2),
                        pattern("va_pvp_rivals_arena", PlaceType.PVP_ARENA, 1.1),
                        pattern("va_pvp_field_honor", PlaceType.PVP_ARENA, 1.0),
                        pattern("va_pvp_duel_ground", PlaceType.PVP_ARENA, 0.9),
                        pattern("va_pvp_blood_ring", PlaceType.PVP_ARENA, 0.8),
                        pattern("va_pvp_clash_ground", PlaceType.PVP_ARENA, 0.8),
                        pattern("portal_landmark", PlaceType.PORTAL_LANDMARK, 1.0, "portal_subject", "place_noun"),
                        patternWithSlot("portal_gate", PlaceType.PORTAL_LANDMARK, 0.9, "portal_subject", NameTokenForm.GENITIVE),
                        patternWithSlots("portal_waystone", PlaceType.PORTAL_LANDMARK, 0.7,
                                slot("token_1", "portal_subject", NameTokenForm.ADJECTIVE_MASC),
                                slot("token_2", "place_noun", NameTokenForm.BASE)),
                        dominantTargetPattern("portal_nether_ashen_gate", PlaceType.PORTAL_LANDMARK, 1.2,
                                NameCauseConstraints.builder().requiredPortalType("nether").build()),
                        dominantTargetPattern("portal_nether_gate", PlaceType.PORTAL_LANDMARK, 1.0,
                                NameCauseConstraints.builder().requiredPortalType("nether").build()),
                        dominantTargetPattern("va_portal_nether_fire_crossing", PlaceType.PORTAL_LANDMARK, 1.0,
                                NameCauseConstraints.builder().requiredPortalType("nether").build()),
                        dominantTargetPattern("va_portal_nether_lower_world_gate", PlaceType.PORTAL_LANDMARK, 0.9,
                                NameCauseConstraints.builder().requiredPortalType("nether").build()),
                        dominantTargetPattern("va_portal_nether_return_gate", PlaceType.PORTAL_LANDMARK, 0.8,
                                NameCauseConstraints.builder().requiredPortalType("nether").build()),
                        dominantTargetPattern("va_portal_nether_threshold", PlaceType.PORTAL_LANDMARK, 0.9,
                                NameCauseConstraints.builder().requiredPortalType("nether").build()),
                        dominantTargetPattern("va_portal_nether_path_ash", PlaceType.PORTAL_LANDMARK, 0.85,
                                NameCauseConstraints.builder().requiredPortalType("nether").build()),
                        dominantTargetPattern("va_portal_nether_lava_gate", PlaceType.PORTAL_LANDMARK, 0.8,
                                NameCauseConstraints.builder().requiredPortalType("nether").build()),
                        dominantTargetPattern("va_portal_nether_basalt_threshold", PlaceType.PORTAL_LANDMARK, 0.75,
                                NameCauseConstraints.builder().requiredPortalType("nether").build()),
                        dominantTargetPattern("va_portal_nether_crossing", PlaceType.PORTAL_LANDMARK, 0.75,
                                NameCauseConstraints.builder().requiredPortalType("nether").build()),
                        dominantTargetPattern("portal_end_last_threshold", PlaceType.PORTAL_LANDMARK, 1.2,
                                NameCauseConstraints.builder().requiredPortalType("end").build()),
                        dominantTargetPattern("portal_end_endward_gate", PlaceType.PORTAL_LANDMARK, 1.0,
                                NameCauseConstraints.builder().requiredPortalType("end").build()),
                        dominantTargetPattern("portal_end_pale_crossing", PlaceType.PORTAL_LANDMARK, 0.9,
                                NameCauseConstraints.builder().requiredPortalType("end").build()),
                        dominantTargetPattern("va_portal_end_void_threshold", PlaceType.PORTAL_LANDMARK, 1.0,
                                NameCauseConstraints.builder().requiredPortalType("end").build()),
                        dominantTargetPattern("va_portal_end_eye_gate", PlaceType.PORTAL_LANDMARK, 0.8,
                                NameCauseConstraints.builder().requiredPortalType("end").build()),
                        causeTypePattern("va_portal_worldgate", PlaceType.PORTAL_LANDMARK, 0.8, PlaceCauseType.PORTAL_USAGE),
                        causeTypePattern("va_portal_old_crossing", PlaceType.PORTAL_LANDMARK, 0.75, PlaceCauseType.PORTAL_USAGE),
                        causeTypePattern("va_portal_threshold_worlds", PlaceType.PORTAL_LANDMARK, 0.75, PlaceCauseType.PORTAL_USAGE),
                        causeTypePattern("va_portal_crossing_gate", PlaceType.PORTAL_LANDMARK, 0.7, PlaceCauseType.PORTAL_USAGE),
                        causeTypePattern("va_portal_worldbound_trace", PlaceType.PORTAL_LANDMARK, 0.7, PlaceCauseType.PORTAL_USAGE),
                        causeTypePattern("va_portal_path_between_worlds", PlaceType.PORTAL_LANDMARK, 0.65, PlaceCauseType.PORTAL_USAGE),
                        causeTypePattern("va_portal_old_gate", PlaceType.PORTAL_LANDMARK, 0.65, PlaceCauseType.PORTAL_USAGE),
                        causeTypePattern("va_portal_stone_crossing", PlaceType.PORTAL_LANDMARK, 0.6, PlaceCauseType.PORTAL_USAGE),
                        causeTypePattern("va_portal_known_threshold", PlaceType.PORTAL_LANDMARK, 0.6, PlaceCauseType.PORTAL_USAGE),
                        causeTypePattern("va_portal_far_crossing", PlaceType.PORTAL_LANDMARK, 0.55, PlaceCauseType.PORTAL_USAGE),
                        causeTypePattern("va_portal_wayfarer_gate", PlaceType.PORTAL_LANDMARK, 0.55, PlaceCauseType.PORTAL_USAGE),
                        causeTypePattern("va_portal_realm_path", PlaceType.PORTAL_LANDMARK, 0.5, PlaceCauseType.PORTAL_USAGE),
                        causeTypePattern("va_portal_quiet_threshold", PlaceType.PORTAL_LANDMARK, 0.5, PlaceCauseType.PORTAL_USAGE),
                        causeTypePattern("va_portal_world_marker", PlaceType.PORTAL_LANDMARK, 0.45, PlaceCauseType.PORTAL_USAGE),
                        causeTypePattern("va_portal_between_crossing", PlaceType.PORTAL_LANDMARK, 0.45, PlaceCauseType.PORTAL_USAGE),
                        causeTypePattern("va_portal_worn_threshold", PlaceType.PORTAL_LANDMARK, 0.45, PlaceCauseType.PORTAL_USAGE),
                        causeTypePattern("va_portal_travelers_gate", PlaceType.PORTAL_LANDMARK, 0.4, PlaceCauseType.PORTAL_USAGE),
                        causeTypePattern("va_portal_old_world_path", PlaceType.PORTAL_LANDMARK, 0.4, PlaceCauseType.PORTAL_USAGE),
                        causeTypePattern("va_portal_distant_gate", PlaceType.PORTAL_LANDMARK, 0.35, PlaceCauseType.PORTAL_USAGE),
                        causeTypePattern("va_portal_remembered_crossing", PlaceType.PORTAL_LANDMARK, 0.35, PlaceCauseType.PORTAL_USAGE),
                        causeTypePattern("va_portal_between_worlds", PlaceType.PORTAL_LANDMARK, 0.25, PlaceCauseType.PORTAL_USAGE),
                        pattern("general_landmark", PlaceType.GENERAL_LANDMARK, 1.0, "memory_subject", "place_noun"),
                        patternWithSlot("general_remembered_stone", PlaceType.GENERAL_LANDMARK, 0.9, "memory_subject", NameTokenForm.GENITIVE),
                        patternWithSlot("general_crossing", PlaceType.GENERAL_LANDMARK, 0.7, "place_noun", NameTokenForm.GENITIVE),
                        causeTypePattern("general_many_steps", PlaceType.GENERAL_LANDMARK, 1.1, PlaceCauseType.VISITS),
                        causeTypePattern("general_old_crossing", PlaceType.GENERAL_LANDMARK, 1.0, PlaceCauseType.VISITS),
                        causeTypePattern("va_general_old_trail", PlaceType.GENERAL_LANDMARK, 1.0, PlaceCauseType.VISITS),
                        causeTypePattern("va_general_old_rest", PlaceType.GENERAL_LANDMARK, 0.9, PlaceCauseType.VISITS),
                        causeTypePattern("va_general_marked_place", PlaceType.GENERAL_LANDMARK, 0.8, PlaceCauseType.VISITS),
                        causeTypePattern("va_general_quiet_path", PlaceType.GENERAL_LANDMARK, 0.7, PlaceCauseType.VISITS),
                        biomeGeneralPattern("general_biome_plains_crossing", "plains", 1.3),
                        biomeGeneralPattern("va_general_biome_plains_steps", "plains", 1.0),
                        biomeGeneralPattern("general_biome_forest_trail", "forest", 1.3),
                        biomeGeneralPattern("va_general_biome_forest_canopy_path", "forest", 1.1),
                        biomeGeneralPattern("general_biome_birch_forest_trail", "birch_forest", 1.2),
                        biomeGeneralPattern("va_general_biome_birch_rest", "birch_forest", 0.9),
                        biomeGeneralPattern("general_biome_dark_forest_marker", "dark_forest", 1.2),
                        biomeGeneralPattern("va_general_biome_dark_forest_path", "dark_forest", 0.9),
                        biomeGeneralPattern("general_biome_taiga_trail", "taiga", 1.2),
                        biomeGeneralPattern("va_general_biome_taiga_marker", "taiga", 0.9),
                        biomeGeneralPattern("general_biome_snowy_crossing", "snowy", 1.2),
                        biomeGeneralPattern("va_general_biome_snowy_marker", "snowy", 0.9),
                        biomeGeneralPattern("general_biome_desert_marker", "desert", 1.3),
                        biomeGeneralPattern("va_general_biome_desert_tracks", "desert", 1.0),
                        biomeGeneralPattern("general_biome_savanna_marker", "savanna", 1.1),
                        biomeGeneralPattern("va_general_biome_savanna_trail", "savanna", 0.9),
                        biomeGeneralPattern("general_biome_jungle_path", "jungle", 1.1),
                        biomeGeneralPattern("va_general_biome_jungle_steps", "jungle", 0.9),
                        biomeGeneralPattern("general_biome_swamp_path", "swamp", 1.2),
                        biomeGeneralPattern("va_general_biome_swamp_marker", "swamp", 0.9),
                        biomeGeneralPattern("general_biome_mangrove_path", "mangrove_swamp", 1.1),
                        biomeGeneralPattern("va_general_biome_mangrove_roots", "mangrove_swamp", 0.8),
                        biomeGeneralPattern("general_biome_mountain_marker", "mountain", 1.1),
                        biomeGeneralPattern("va_general_biome_mountain_trail", "mountain", 0.9),
                        biomeGeneralPattern("general_biome_meadow_rest", "meadow", 1.1),
                        biomeGeneralPattern("va_general_biome_meadow_path", "meadow", 0.9),
                        biomeGeneralPattern("general_biome_cherry_grove_rest", "cherry_grove", 1.2),
                        biomeGeneralPattern("va_general_biome_cherry_path", "cherry_grove", 0.9),
                        biomeGeneralPattern("general_biome_river_crossing", "river", 1.1),
                        biomeGeneralPattern("va_general_biome_river_steps", "river", 0.8),
                        biomeGeneralPattern("general_biome_beach_marker", "beach", 1.1),
                        biomeGeneralPattern("va_general_biome_coastal_trail", "beach", 0.9),
                        biomeGeneralPattern("general_biome_ocean_marker", "ocean", 1.1),
                        biomeGeneralPattern("va_general_biome_ocean_watch", "ocean", 0.8),
                        biomeGeneralPattern("general_biome_badlands_marker", "badlands", 1.2),
                        biomeGeneralPattern("va_general_biome_badlands_trail", "badlands", 0.9),
                        biomeGeneralPattern("general_biome_underground_marker", "cave_or_underground", 1.0),
                        biomeGeneralPattern("va_general_biome_underground_path", "cave_or_underground", 0.8),
                        biomeGeneralPattern("general_biome_nether_marker", "nether", 1.0),
                        biomeGeneralPattern("va_general_biome_nether_trace", "nether", 0.8),
                        biomeGeneralPattern("general_biome_end_marker", "end", 1.0),
                        biomeGeneralPattern("va_general_biome_end_trace", "end", 0.8),
                        pattern("mining_site", PlaceType.MINING_SITE, 1.0, "mining_subject", "mining_place"),
                        patternWithSlot("mining_quarry", PlaceType.MINING_SITE, 0.9, "mining_subject", NameTokenForm.GENITIVE),
                        patternWithSlots("mining_deep_vein", PlaceType.MINING_SITE, 0.7,
                                slot("token_1", "mining_subject", NameTokenForm.ADJECTIVE_FEM),
                                slot("token_2", "mining_place", NameTokenForm.BASE)),
                        dominantTargetPattern("mining_diamond_vein", PlaceType.MINING_SITE, 1.2,
                                NameCauseConstraints.builder().requiredDominantValuableBlocks("minecraft:diamond_ore", "minecraft:deepslate_diamond_ore").build()),
                        dominantTargetPattern("mining_diamond_gem_lode", PlaceType.MINING_SITE, 1.0,
                                NameCauseConstraints.builder().requiredDominantValuableBlocks("minecraft:diamond_ore", "minecraft:deepslate_diamond_ore").build()),
                        dominantTargetPattern("mining_diamond_glitter", PlaceType.MINING_SITE, 0.9,
                                NameCauseConstraints.builder().requiredDominantValuableBlocks("minecraft:diamond_ore", "minecraft:deepslate_diamond_ore").build()),
                        dominantTargetPattern("va_mining_diamond_deepslate_vein", PlaceType.MINING_SITE, 1.0,
                                NameCauseConstraints.builder().requiredDominantValuableBlocks("minecraft:diamond_ore", "minecraft:deepslate_diamond_ore").build()),
                        dominantTargetPattern("va_mining_diamond_fortune_vein", PlaceType.MINING_SITE, 0.9,
                                NameCauseConstraints.builder().requiredDominantValuableBlocks("minecraft:diamond_ore", "minecraft:deepslate_diamond_ore").build()),
                        dominantTargetPattern("mining_ancient_scar", PlaceType.MINING_SITE, 1.2,
                                NameCauseConstraints.builder().requiredDominantValuableBlocks("minecraft:ancient_debris").build()),
                        dominantTargetPattern("va_mining_ancient_relic", PlaceType.MINING_SITE, 1.1,
                                NameCauseConstraints.builder().requiredDominantValuableBlocks("minecraft:ancient_debris").build()),
                        dominantTargetPattern("va_mining_buried_relic", PlaceType.MINING_SITE, 1.0,
                                NameCauseConstraints.builder().requiredDominantValuableBlocks("minecraft:ancient_debris").build()),
                        dominantTargetPattern("va_mining_netherite_trace", PlaceType.MINING_SITE, 0.8,
                                NameCauseConstraints.builder().requiredDominantValuableBlocks("minecraft:ancient_debris").build()),
                        dominantTargetPattern("va_mining_emerald_vein", PlaceType.MINING_SITE, 1.1,
                                NameCauseConstraints.builder().requiredDominantValuableBlocks("minecraft:emerald_ore", "minecraft:deepslate_emerald_ore").build()),
                        dominantTargetPattern("va_mining_emerald_lode", PlaceType.MINING_SITE, 1.0,
                                NameCauseConstraints.builder().requiredDominantValuableBlocks("minecraft:emerald_ore", "minecraft:deepslate_emerald_ore").build()),
                        pattern("va_mining_ore_site", PlaceType.MINING_SITE, 0.8),
                        pattern("va_mining_deep_vein", PlaceType.MINING_SITE, 0.7),
                        exactFirstDiscoveryPattern("first_stronghold_first", "world:first_stronghold_found", 1.0),
                        exactFirstDiscoveryPattern("first_stronghold_eye_halls", "world:first_stronghold_found", 0.9),
                        exactFirstDiscoveryPattern("first_stronghold_hidden_threshold", "world:first_stronghold_found", 0.8),
                        exactFirstDiscoveryPattern("first_stronghold_endward_halls", "world:first_stronghold_found", 0.8),
                        exactFirstDiscoveryPattern("va_first_stronghold_citadel", "world:first_stronghold_found", 0.9),
                        exactFirstDiscoveryPattern("va_first_stronghold_eye_citadel", "world:first_stronghold_found", 0.8),
                        exactFirstDiscoveryPattern("va_first_stronghold_eye_threshold", "world:first_stronghold_found", 0.75),
                        exactFirstDiscoveryPattern("va_first_stronghold_forgotten_halls", "world:first_stronghold_found", 0.7),
                        exactFirstDiscoveryPattern("va_first_stronghold_path_eye", "world:first_stronghold_found", 0.7),
                        exactFirstDiscoveryPattern("va_first_stronghold_stone_halls", "world:first_stronghold_found", 0.65),
                        exactFirstDiscoveryPattern("first_diamond", "world:first_diamond_ore_mined", 1.0),
                        exactFirstDiscoveryPattern("first_diamond_vein", "world:first_diamond_ore_mined", 0.9),
                        exactFirstDiscoveryPattern("first_diamond_glitter", "world:first_diamond_ore_mined", 0.8),
                        exactFirstDiscoveryPattern("va_first_diamond_first_light", "world:first_diamond_ore_mined", 0.7),
                        exactFirstDiscoveryPattern("first_ancient_debris", "world:first_ancient_debris_mined", 1.0),
                        exactFirstDiscoveryPattern("first_buried_relic", "world:first_ancient_debris_mined", 0.9),
                        exactFirstDiscoveryPattern("va_first_ancient_scar", "world:first_ancient_debris_mined", 0.8),
                        exactFirstDiscoveryPattern("va_first_netherite_trace", "world:first_ancient_debris_mined", 0.7),
                        exactFirstDiscoveryPattern("first_nether_descent", "world:first_nether_entry", 1.0),
                        exactFirstDiscoveryPattern("first_ashen_gate", "world:first_nether_entry", 0.9),
                        exactFirstDiscoveryPattern("va_first_nether_path_below", "world:first_nether_entry", 0.8),
                        exactFirstDiscoveryPattern("va_first_nether_gate", "world:first_nether_entry", 0.8),
                        exactFirstDiscoveryPattern("first_end_threshold", "world:first_end_entry", 1.0),
                        exactFirstDiscoveryPattern("first_pale_crossing", "world:first_end_entry", 0.9),
                        exactFirstDiscoveryPattern("va_first_endward_path", "world:first_end_entry", 0.8),
                        exactFirstDiscoveryPattern("va_first_edge_threshold", "world:first_end_entry", 0.7),
                        exactFirstDiscoveryPattern("first_dragonfall", "world:first_ender_dragon_killed", 1.0),
                        exactFirstDiscoveryPattern("first_void_victory", "world:first_ender_dragon_killed", 0.9),
                        exactFirstDiscoveryPattern("va_first_dragon_victory", "world:first_ender_dragon_killed", 0.8),
                        pattern("first_discovery", PlaceType.FIRST_DISCOVERY, 1.0, "discovery_subject", "discovery_place"),
                        patternWithSlot("first_discovery_footstep", PlaceType.FIRST_DISCOVERY, 0.9, "discovery_subject", NameTokenForm.GENITIVE),
                        patternWithSlot("first_discovery_light", PlaceType.FIRST_DISCOVERY, 0.7, "discovery_subject", NameTokenForm.GENITIVE),
                        pattern("va_first_generic_discovery", PlaceType.FIRST_DISCOVERY, 1.0),
                        pattern("va_first_generic_trace", PlaceType.FIRST_DISCOVERY, 0.9),
                        pattern("va_first_generic_first_light_place", PlaceType.FIRST_DISCOVERY, 0.85),
                        pattern("va_first_generic_remembered_threshold", PlaceType.FIRST_DISCOVERY, 0.8),
                        pattern("va_first_generic_path_trace", PlaceType.FIRST_DISCOVERY, 0.8),
                        pattern("va_first_generic_first_find", PlaceType.FIRST_DISCOVERY, 0.75),
                        pattern("va_first_generic_old_record", PlaceType.FIRST_DISCOVERY, 0.7),
                        pattern("va_first_generic_new_legend", PlaceType.FIRST_DISCOVERY, 0.7),
                        pattern("va_first_generic_first_mark", PlaceType.FIRST_DISCOVERY, 0.65),
                        pattern("va_first_generic_beginning", PlaceType.FIRST_DISCOVERY, 0.65),
                        pattern("va_first_generic_opening", PlaceType.FIRST_DISCOVERY, 0.6),
                        pattern("va_first_generic_first_memory", PlaceType.FIRST_DISCOVERY, 0.6),
                        pattern("va_first_generic_early_trace", PlaceType.FIRST_DISCOVERY, 0.55),
                        pattern("va_first_generic_unseen_path", PlaceType.FIRST_DISCOVERY, 0.55),
                        pattern("va_first_generic_first_story", PlaceType.FIRST_DISCOVERY, 0.5),
                        pattern("va_first_generic_opened_way", PlaceType.FIRST_DISCOVERY, 0.5),
                        pattern("va_first_generic_new_threshold", PlaceType.FIRST_DISCOVERY, 0.45),
                        pattern("va_first_generic_memory_trace", PlaceType.FIRST_DISCOVERY, 0.45),
                        pattern("boss_site", PlaceType.BOSS_SITE, 0.4, "boss_subject", "battle_place"),
                        patternWithSlot("boss_lair", PlaceType.BOSS_SITE, 0.3, "boss_subject", NameTokenForm.GENITIVE),
                        patternWithSlot("boss_fall", PlaceType.BOSS_SITE, 0.2, "boss_subject", NameTokenForm.GENITIVE),
                        dominantTargetPattern("boss_witherfall", PlaceType.BOSS_SITE, 1.2,
                                NameCauseConstraints.builder().requiredBossId("minecraft:wither").build()),
                        dominantTargetPattern("boss_wither_fall", PlaceType.BOSS_SITE, 1.0,
                                NameCauseConstraints.builder().requiredBossId("minecraft:wither").build()),
                        dominantTargetPattern("boss_wither_blackened_ground", PlaceType.BOSS_SITE, 0.9,
                                NameCauseConstraints.builder().requiredBossId("minecraft:wither").build()),
                        dominantTargetPattern("boss_wither_skullfall_hollow", PlaceType.BOSS_SITE, 0.8,
                                NameCauseConstraints.builder().requiredBossId("minecraft:wither").build()),
                        dominantTargetPattern("va_boss_wither_black_skull", PlaceType.BOSS_SITE, 0.8,
                                NameCauseConstraints.builder().requiredBossId("minecraft:wither").build()),
                        dominantTargetPattern("va_boss_wither_shadow", PlaceType.BOSS_SITE, 0.8,
                                NameCauseConstraints.builder().requiredBossId("minecraft:wither").build()),
                        dominantTargetPattern("va_boss_wither_black_hollow", PlaceType.BOSS_SITE, 0.75,
                                NameCauseConstraints.builder().requiredBossId("minecraft:wither").build()),
                        dominantTargetPattern("va_boss_wither_ash", PlaceType.BOSS_SITE, 0.75,
                                NameCauseConstraints.builder().requiredBossId("minecraft:wither").build()),
                        dominantTargetPattern("va_boss_wither_withering_ground", PlaceType.BOSS_SITE, 0.7,
                                NameCauseConstraints.builder().requiredBossId("minecraft:wither").build()),
                        dominantTargetPattern("va_boss_wither_trace", PlaceType.BOSS_SITE, 0.7,
                                NameCauseConstraints.builder().requiredBossId("minecraft:wither").build()),
                        dominantTargetPattern("boss_dragonfall", PlaceType.BOSS_SITE, 1.2,
                                NameCauseConstraints.builder().requiredBossId("minecraft:ender_dragon").build()),
                        dominantTargetPattern("boss_ender_dragon_fall", PlaceType.BOSS_SITE, 1.0,
                                NameCauseConstraints.builder().requiredBossId("minecraft:ender_dragon").build()),
                        dominantTargetPattern("va_boss_dragon_perch", PlaceType.BOSS_SITE, 0.9,
                                NameCauseConstraints.builder().requiredBossId("minecraft:ender_dragon").build()),
                        dominantTargetPattern("va_boss_dragon_ground", PlaceType.BOSS_SITE, 0.8,
                                NameCauseConstraints.builder().requiredBossId("minecraft:ender_dragon").build()),
                        dominantTargetPattern("va_boss_dragon_trace", PlaceType.BOSS_SITE, 0.8,
                                NameCauseConstraints.builder().requiredBossId("minecraft:ender_dragon").build()),
                        dominantTargetPattern("va_boss_dragon_rest", PlaceType.BOSS_SITE, 0.75,
                                NameCauseConstraints.builder().requiredBossId("minecraft:ender_dragon").build()),
                        dominantTargetPattern("va_boss_dragon_ground_dragonfall", PlaceType.BOSS_SITE, 0.75,
                                NameCauseConstraints.builder().requiredBossId("minecraft:ender_dragon").build()),
                        dominantTargetPattern("va_boss_dragon_edge", PlaceType.BOSS_SITE, 0.7,
                                NameCauseConstraints.builder().requiredBossId("minecraft:ender_dragon").build()),
                        dominantTargetPattern("va_boss_dragon_ash", PlaceType.BOSS_SITE, 0.65,
                                NameCauseConstraints.builder().requiredBossId("minecraft:ender_dragon").build()),
                        dominantTargetPattern("va_boss_dragon_threshold", PlaceType.BOSS_SITE, 0.65,
                                NameCauseConstraints.builder().requiredBossId("minecraft:ender_dragon").build()),
                        dominantTargetPattern("boss_warden_silence", PlaceType.BOSS_SITE, 0.9,
                                NameCauseConstraints.builder().requiredBossId("minecraft:warden").build()),
                        dominantTargetPattern("va_boss_warden_sculk_depths", PlaceType.BOSS_SITE, 1.0,
                                NameCauseConstraints.builder().requiredBossId("minecraft:warden").build()),
                        dominantTargetPattern("va_boss_warden_deep_trace", PlaceType.BOSS_SITE, 0.8,
                                NameCauseConstraints.builder().requiredBossId("minecraft:warden").build()),
                        dominantTargetPattern("boss_elder_guardian_tide", PlaceType.BOSS_SITE, 0.9,
                                NameCauseConstraints.builder().requiredBossId("minecraft:elder_guardian").build()),
                        dominantTargetPattern("va_boss_elder_guardian_waters", PlaceType.BOSS_SITE, 1.0,
                                NameCauseConstraints.builder().requiredBossId("minecraft:elder_guardian").build()),
                        dominantTargetPattern("va_boss_elder_temple_deep", PlaceType.BOSS_SITE, 0.8,
                                NameCauseConstraints.builder().requiredBossId("minecraft:elder_guardian").build()),
                        pattern("va_boss_generic_beast_lair", PlaceType.BOSS_SITE, 1.0),
                        pattern("va_boss_generic_great_clash", PlaceType.BOSS_SITE, 0.9),
                        pattern("va_boss_generic_fallen_lair", PlaceType.BOSS_SITE, 0.85),
                        pattern("va_boss_generic_strong_foe_trace", PlaceType.BOSS_SITE, 0.8),
                        pattern("va_boss_generic_clash_ground", PlaceType.BOSS_SITE, 0.75),
                        pattern("va_boss_generic_beast_mark", PlaceType.BOSS_SITE, 0.7),
                        pattern("va_boss_generic_old_lair", PlaceType.BOSS_SITE, 0.7),
                        pattern("va_boss_generic_great_fall", PlaceType.BOSS_SITE, 0.65),
                        pattern("va_boss_generic_deep_mark", PlaceType.BOSS_SITE, 0.65),
                        pattern("va_boss_generic_broken_ground", PlaceType.BOSS_SITE, 0.6),
                        pattern("va_boss_generic_foe_rest", PlaceType.BOSS_SITE, 0.6),
                        pattern("va_boss_generic_last_stand", PlaceType.BOSS_SITE, 0.55),
                        pattern("va_boss_generic_hard_won_ground", PlaceType.BOSS_SITE, 0.55),
                        pattern("va_boss_generic_scarred_lair", PlaceType.BOSS_SITE, 0.5),
                        pattern("va_boss_generic_legend_ground", PlaceType.BOSS_SITE, 0.5),
                        pattern("pet_memorial", PlaceType.PET_MEMORIAL, 1.0, "creature_subject", "memory_place"),
                        patternWithSlot("pet_rest", PlaceType.PET_MEMORIAL, 0.9, "creature_subject", NameTokenForm.GENITIVE),
                        patternWithSlot("pet_collars", PlaceType.PET_MEMORIAL, 0.7, "creature_subject", NameTokenForm.GENITIVE),
                        pattern("va_pet_loyal_rest", PlaceType.PET_MEMORIAL, 1.0),
                        pattern("va_pet_last_trail", PlaceType.PET_MEMORIAL, 0.9),
                        pattern("va_pet_bright_memory", PlaceType.PET_MEMORIAL, 0.8),
                        pattern("named_mob_memorial", PlaceType.NAMED_MOB_MEMORIAL, 1.0, "creature_subject", "memory_place"),
                        patternWithSlot("named_mob_trace", PlaceType.NAMED_MOB_MEMORIAL, 0.9, "creature_subject", NameTokenForm.GENITIVE),
                        patternWithSlot("named_mob_rest", PlaceType.NAMED_MOB_MEMORIAL, 0.7, "creature_subject", NameTokenForm.GENITIVE),
                        pattern("va_named_mob_memory", PlaceType.NAMED_MOB_MEMORIAL, 1.0),
                        pattern("va_named_mob_last_trace", PlaceType.NAMED_MOB_MEMORIAL, 0.9),
                        pattern("va_named_mob_fall", PlaceType.NAMED_MOB_MEMORIAL, 0.8),
                        pattern("raid_site", PlaceType.RAID_SITE, 1.0, "battle_subject", "settlement_place"),
                        patternWithSlot("raid_stand", PlaceType.RAID_SITE, 0.9, "battle_subject", NameTokenForm.GENITIVE),
                        patternWithSlot("raid_victory", PlaceType.RAID_SITE, 0.7, "battle_subject", NameTokenForm.GENITIVE),
                        pattern("va_raid_defense_ground", PlaceType.RAID_SITE, 1.1),
                        pattern("va_raid_field_defense", PlaceType.RAID_SITE, 1.0),
                        pattern("va_raid_victory_banner", PlaceType.RAID_SITE, 0.9),
                        pattern("va_raid_village_line", PlaceType.RAID_SITE, 0.8),
                        pattern("va_raid_repelled_site", PlaceType.RAID_SITE, 0.8),
                        pattern("dimension_threshold", PlaceType.DIMENSION_THRESHOLD, 1.0, "portal_subject", "discovery_place"),
                        patternWithSlot("dimension_gate", PlaceType.DIMENSION_THRESHOLD, 0.9, "portal_subject", NameTokenForm.GENITIVE),
                        patternWithSlot("dimension_crossing", PlaceType.DIMENSION_THRESHOLD, 0.7, "discovery_place", NameTokenForm.GENITIVE),
                        pattern("va_dimension_threshold_worlds", PlaceType.DIMENSION_THRESHOLD, 1.0),
                        pattern("va_dimension_worldbound_trace", PlaceType.DIMENSION_THRESHOLD, 0.9),
                        pattern("va_dimension_between_worlds", PlaceType.DIMENSION_THRESHOLD, 0.8),
                        pattern("va_dimension_border", PlaceType.DIMENSION_THRESHOLD, 0.7),
                        patternWithSlot("camp", PlaceType.CAMP, 1.0, "place_noun", NameTokenForm.GENITIVE),
                        patternWithSlot("settlement", PlaceType.SETTLEMENT, 1.0, "settlement_place", NameTokenForm.GENITIVE),
                        pattern("va_settlement_lived_in_place", PlaceType.SETTLEMENT, 1.1),
                        pattern("va_settlement_old_hearth", PlaceType.SETTLEMENT, 1.0),
                        pattern("va_settlement_warm_rest", PlaceType.SETTLEMENT, 0.9),
                        pattern("va_settlement_hearthstead", PlaceType.SETTLEMENT, 0.9),
                        pattern("va_settlement_old_shelter", PlaceType.SETTLEMENT, 0.8),
                        pattern("va_settlement_roofed_rest", PlaceType.SETTLEMENT, 0.8),
                        pattern("va_settlement_builder_rest", PlaceType.SETTLEMENT, 0.7),
                        patternWithSlot("grave", PlaceType.GRAVE, 1.0, "death_subject", NameTokenForm.GENITIVE),
                        patternWithSlot("landmark", PlaceType.LANDMARK, 1.0, "place_noun", NameTokenForm.GENITIVE),
                        patternWithSlot("mine", PlaceType.MINE, 1.0, "mining_subject", NameTokenForm.GENITIVE),
                        patternWithSlot("farm", PlaceType.FARM, 1.0, "place_noun", NameTokenForm.GENITIVE),
                        patternWithSlot("workshop", PlaceType.WORKSHOP, 1.0, "place_noun", NameTokenForm.GENITIVE),
                        patternWithSlot("road", PlaceType.ROAD, 1.0, "place_noun", NameTokenForm.GENITIVE),
                        patternWithSlot("portal_site", PlaceType.PORTAL_SITE, 1.0, "portal_subject", NameTokenForm.GENITIVE),
                        patternWithSlot("custom", PlaceType.CUSTOM, 0.1, "memory_subject", NameTokenForm.GENITIVE),
                        pattern("va_custom_remembered_place", PlaceType.CUSTOM, 1.0),
                        pattern("va_custom_named_place", PlaceType.CUSTOM, 0.9),
                        pattern("va_custom_place_memory", PlaceType.CUSTOM, 0.8),
                        pattern("va_custom_old_place", PlaceType.CUSTOM, 0.7),
                        pattern("va_custom_old_memory", PlaceType.CUSTOM, 0.75),
                        pattern("va_custom_old_trace", PlaceType.CUSTOM, 0.75),
                        pattern("va_custom_remembered_trace", PlaceType.CUSTOM, 0.7)
                ),
                List.of(
                        token("fallen", "death", "memory", "generic", "death_subject", "memory_subject"),
                        token("last_breath", "death", "memory", "death_subject", "memory_subject"),
                        token("deep", "cave", "mining", "place_noun", "cave_place", "mining_place"),
                        token("cavern", "cave", "terrain", "place_noun", "cave_place"),
                        token("field", "surface", "field", "terrain", "place_noun", "battle_place", "settlement_place"),
                        token("crossroads", "surface", "landmark", "threshold", "place_noun", "discovery_place"),
                        token("drowned", "water", "death", "death_subject", "creature_subject"),
                        token("silent_water", "water", "landmark", "place_noun", "water_place"),
                        token("high_peak", "mountain", "terrain", "place_noun", "mountain_place"),
                        token("thin_air", "mountain", "death", "abstract_quality", "mountain_place"),
                        token("ashen", "nether", "death", "abstract_quality", "nether_place"),
                        token("blackstone", "nether", "terrain", "place_noun", "nether_place"),
                        token("void", "end", "dimension", "place_noun", "end_place", "portal_subject"),
                        token("chorus", "end", "landmark", "place_noun", "end_place"),
                        token("clash", "battle", "raid", "battle_subject", "abstract_quality", "pvp_subject"),
                        token("valor", "battle", "memory", "battle_subject", "memory_subject", "abstract_quality"),
                        token("slaughter", "slaughter", "battle", "slaughter_subject"),
                        token("hunt", "slaughter", "battle", "slaughter_subject"),
                        token("pens", "slaughter", "field", "place_noun", "slaughter_place"),
                        token("rift", "portal", "dimension", "portal_subject", "place_noun"),
                        token("gate", "portal", "threshold", "portal_subject", "place_noun"),
                        token("old_stone", "landmark", "generic", "place_noun", "memory_place"),
                        token("remembered", "memory", "generic", "memory_place", "abstract_quality"),
                        token("vein", "mining", "ore", "mining_subject", "mining_place"),
                        token("diamond", "ore", "discovery", "mining_subject", "discovery_subject"),
                        token("first_step", "discovery", "threshold", "discovery_place"),
                        token("dawn", "discovery", "landmark", "discovery_subject", "abstract_quality"),
                        constrainedToken("dragon",
                                NameCauseConstraints.builder()
                                        .allowedBossIds("minecraft:ender_dragon")
                                        .allowedEntityIds("minecraft:ender_dragon")
                                        .allowedDominantMobTypes("minecraft:ender_dragon")
                                        .allowedFirstDiscoveryKeys("world:first_ender_dragon_killed")
                                        .build(),
                                "boss", "battle", "boss_subject", "battle_subject"),
                        constrainedToken("wither",
                                NameCauseConstraints.builder()
                                        .allowedBossIds("minecraft:wither")
                                        .allowedEntityIds("minecraft:wither")
                                        .allowedDominantMobTypes("minecraft:wither")
                                        .build(),
                                "boss", "death", "boss_subject"),
                        token("companion", "pet", "memory", "creature_subject", "memory_subject"),
                        token("loyal", "pet", "memory", "creature_subject", "memory_subject", "abstract_quality"),
                        token("named_one", "creature", "memory", "creature_subject", "memory_subject"),
                        token("strange_beast", "creature", "battle", "creature_subject", "boss_subject"),
                        token("raid_bell", "raid", "settlement", "battle_symbol", "battle_symbol_masc", "settlement_place", "place_noun"),
                        token("defense", "raid", "battle", "battle_subject", "abstract_quality"),
                        token("threshold", "threshold", "dimension", "portal_subject", "discovery_place", "place_noun"),
                        token("far_world", "dimension", "landmark", "portal_subject", "discovery_place", "place_noun"),
                        token("echoes", "echo", "memory", "generic", "memory_subject", "abstract_quality"),
                        token("bones", "death", "memory", "death_subject", "memory_subject"),
                        token("shadow", "death", "generic", "death_subject", "memory_subject", "abstract_quality"),
                        token("roots", "surface", "field", "terrain", "place_noun", "surface_place"),
                        token("meadow", "surface", "field", "terrain", "place_noun", "surface_place"),
                        token("shore", "water", "landmark", "place_noun", "water_place"),
                        token("summit", "mountain", "terrain", "place_noun", "mountain_place"),
                        token("ember", "nether", "death", "place_noun", "nether_place"),
                        token("soul_sand", "nether", "terrain", "place_noun", "nether_place"),
                        token("obsidian", "portal", "terrain", "portal_subject", "place_noun"),
                        token("ender_eye", "end", "dimension", "portal_subject", "discovery_subject"),
                        token("banner", "banner", "battle", "raid", "battle_symbol", "battle_symbol_neut"),
                        token("bannerfall", "bannerfall", "battle", "battle_symbol", "battle_subject"),
                        token("shield", "battle", "raid", "battle_symbol", "battle_symbol_masc", "battle_subject"),
                        token("duel", "duel", "battle", "pvp", "pvp_subject"),
                        token("duelist", "duel", "pvp", "combatant", "pvp_subject"),
                        token("rival", "duel", "pvp", "combatant", "pvp_subject"),
                        token("champion", "pvp", "combatant", "champion", "pvp_subject", "battle_subject"),
                        token("challenge", "duel", "pvp", "pvp_subject"),
                        token("vendetta", "duel", "pvp", "pvp_subject"),
                        token("ring", "arena", "pvp_place", "place_noun"),
                        token("arena", "arena", "battle", "pvp_place", "place_noun", "battle_place"),
                        token("pasture", "slaughter", "field", "place_noun", "slaughter_place"),
                        token("trampled", "slaughter", "field", "slaughter_subject"),
                        token("quarry", "mining", "ore", "mining_subject", "mining_place"),
                        token("emerald", "ore", "discovery", "mining_subject", "discovery_subject"),
                        token("ancient_debris", "ore", "nether", "mining_subject", "discovery_subject"),
                        token("waystone", "landmark", "threshold", "place_noun", "discovery_place", "portal_subject"),
                        token("campfire", "camp", "memory", "place_noun", "memory_place"),
                        token("hearth", "settlement", "memory", "place_noun", "memory_place", "settlement_place"),
                        token("haven", "settlement", "landmark", "place_noun", "settlement_place"),
                        token("footstep", "discovery", "threshold", "discovery_place"),
                        token("first_light", "discovery", "landmark", "discovery_place"),
                        token("lair", "boss", "place", "place_noun", "boss_place", "battle_place"),
                        token("fall", "battle", "battle_subject"),
                        token("rest", "pet", "creature", "memory", "creature_subject", "memory_place"),
                        token("collar", "pet", "memory", "creature_subject"),
                        token("trace", "creature", "memory", "creature_subject", "memory_subject"),
                        token("victory", "raid", "battle", "battle_subject", "abstract_quality"),
                        token("crossing", "road", "threshold", "landmark", "place_noun", "discovery_place"),
                        token("marker", "landmark", "memory", "place_noun", "memory_place"),
                        token("rows", "field", "farm", "place_noun"),
                        token("hammer", "workshop", "memory", "place_noun"),
                        token("bench", "workshop", "landmark", "place_noun"),
                        token("trail", "road", "landmark", "place_noun"),
                        token("bridge", "road", "landmark", "place_noun"),
                        token("lost", "death", "memory", "lost_subject", "death_subject", "memory_subject"),
                        token("blood", "death", "battle", "blood_subject", "death_subject", "battle_subject"),
                        token("red", "battle", "abstract_quality", "battle_subject"),
                        token("portal", "portal", "dimension", "portal_subject"),
                        token("old", "memory", "abstract_quality", "memory_subject"),
                        token("forgotten", "memory", "abstract_quality", "memory_subject"),
                        token("mine", "mining", "mining_place", "place_noun", "mining_subject"),
                        token("ore", "ore", "mining", "mining_subject"),
                        token("discovery", "discovery", "memory", "discovery_subject"),
                        weightedToken("memory", 0.5, "memory", "generic", "memory_place")
                )
        );
        private static final Map<String, NameDataPack> PACKS = packs();

        private static Map<String, NameDataPack> packs() {
            Map<String, NameDataPack> packs = new LinkedHashMap<>();
            packs.put(DEFAULT_PACK.styleId(), withContentCompatPatterns(withHighFrequencyCauseVariants(BuiltInNamePolishData.apply(DEFAULT_PACK))));
            packs.put(NameStyle.DARK_FANTASY.idString(), withContentCompatPatterns(withHighFrequencyCauseVariants(BuiltInNamePolishData.apply(darkFantasyPack()))));
            packs.put(NameStyle.COZY_SURVIVAL.idString(), withContentCompatPatterns(withHighFrequencyCauseVariants(BuiltInNamePolishData.apply(cozySurvivalPack()))));
            packs.put(NameStyle.EPIC_MYTHOLOGY.idString(), withContentCompatPatterns(withHighFrequencyCauseVariants(BuiltInNamePolishData.apply(epicMythologyPack()))));
            packs.put(NameStyle.NEUTRAL_SERVER.idString(), withContentCompatPatterns(withHighFrequencyCauseVariants(BuiltInNamePolishData.apply(neutralServerPack()))));
            packs.put(NameStyle.FUNNY_COMMUNITY.idString(), withContentCompatPatterns(withHighFrequencyCauseVariants(BuiltInNamePolishData.apply(funnyCommunityPack()))));
            return Map.copyOf(packs);
        }
    }
}
