package com.worldremembers.livinglegends;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class VanillaMobThemeRegistry {
    private static final Map<String, MobTheme> THEMES = buildThemes();
    private static final Set<String> RELEVANT_VANILLA_ENTITY_IDS = buildRelevantIds();

    private VanillaMobThemeRegistry() {
    }

    public static MobTheme lookup(String entityId) {
        String normalized = normalize(entityId);
        if (normalized.isBlank()) {
            return MobTheme.empty("");
        }
        WorldRemembersCompatRegistries.CompatLookup<CompatThemeDefinitions.MobThemeDefinition> compat =
                WorldRemembersCompatRegistries.mobTheme(normalized);
        if (compat.matched() && compat.definition().priority() > CompatThemeDefinitions.BUILTIN_PRIORITY) {
            return fromCompat(compat.definition());
        }
        return THEMES.getOrDefault(normalized, MobTheme.empty(normalized));
    }

    public static List<MobTheme> allThemes() {
        return List.copyOf(THEMES.values());
    }

    public static boolean matchesMobKey(String entityId, Set<String> requiredKeys) {
        return matchPriority(entityId, requiredKeys, PlaceType.UNKNOWN) > 0;
    }

    public static int matchPriority(String entityId, Set<String> requiredKeys, PlaceType placeType) {
        String normalized = normalize(entityId);
        if (normalized.isBlank() || requiredKeys == null || requiredKeys.isEmpty()) {
            return 0;
        }
        Set<String> keys = normalizedSet(requiredKeys);
        if (keys.contains(normalized)) {
            return 500;
        }
        return lookup(normalized).matchPriority(keys, placeType);
    }

    public static List<String> missingRelevantMappings() {
        List<String> missing = new ArrayList<>();
        for (String entityId : RELEVANT_VANILLA_ENTITY_IDS) {
            MobTheme theme = THEMES.get(entityId);
            if (theme == null || theme.primaryGroup().isBlank()) {
                missing.add(entityId);
            }
        }
        return List.copyOf(missing);
    }

    private static Map<String, MobTheme> buildThemes() {
        Map<String, MobTheme> result = new LinkedHashMap<>();

        add(result, theme("minecraft:zombie", "undead", "zombie", "undead", roles("battlefield"), "zombie", "", "", "undead", "melee"));
        add(result, theme("minecraft:zombie_villager", "undead", "zombie_villager", "undead", roles("battlefield", "civilian"), "undead", "", "", "undead", "civilian"));
        add(result, theme("minecraft:husk", "undead", "husk", "undead", roles("battlefield"), "undead", "", "", "desert", "melee"));
        add(result, theme("minecraft:drowned", "undead", "drowned", "undead", roles("battlefield"), "undead", "", "", "aquatic", "melee"));
        add(result, theme("minecraft:skeleton", "skeleton", "skeleton", "undead", roles("battlefield"), "skeleton", "", "", "undead", "ranged"));
        add(result, theme("minecraft:stray", "skeleton", "stray", "undead", roles("battlefield"), "skeleton", "", "", "undead", "ranged", "frozen"));
        add(result, theme("minecraft:bogged", "skeleton", "bogged", "undead", roles("battlefield"), "skeleton", "", "", "undead", "ranged", "swamp"));
        add(result, theme("minecraft:wither_skeleton", "skeleton", "wither_skeleton", "undead", roles("battlefield"), "skeleton", "", "", "undead", "nether", "ranged"));
        add(result, theme("minecraft:creeper", "explosive", "creeper", "explosion", roles("battlefield"), "creeper", "", "", "explosive"));
        add(result, theme("minecraft:spider", "arthropod", "spider", "arthropod", roles("battlefield"), "arthropod", "", "", "melee"));
        add(result, theme("minecraft:cave_spider", "arthropod", "cave_spider", "arthropod", roles("battlefield"), "arthropod", "", "", "cave", "melee"));
        add(result, theme("minecraft:enderman", "end", "enderman", "end", roles("battlefield"), "enderman", "", "", "shadow"));
        add(result, theme("minecraft:endermite", "end", "endermite", "end", roles("battlefield"), "end", "", "", "arthropod"));
        add(result, theme("minecraft:silverfish", "arthropod", "silverfish", "arthropod", roles("battlefield"), "arthropod", "", "", "stone"));
        add(result, theme("minecraft:slime", "slime", "slime", "slime", roles("battlefield"), "slime", "", "", "melee"));
        add(result, theme("minecraft:magma_cube", "slime", "magma_cube", "slime", roles("battlefield"), "slime", "", "", "nether", "fire"));
        add(result, theme("minecraft:witch", "magic", "witch", "magic", roles("battlefield"), "magic", "", "", "ranged"));
        add(result, theme("minecraft:phantom", "flying", "phantom", "flying", roles("battlefield"), "flying", "", "", "undead"));
        add(result, theme("minecraft:blaze", "nether", "blaze", "nether", roles("battlefield"), "nether", "", "", "fire", "flying"));
        add(result, theme("minecraft:ghast", "nether", "ghast", "nether", roles("battlefield"), "nether", "", "", "flying"));
        add(result, theme("minecraft:guardian", "aquatic", "guardian", "aquatic", roles("battlefield"), "aquatic", "", "", "ranged"));
        add(result, theme("minecraft:elder_guardian", "boss_like", "elder_guardian", "aquatic", roles("battlefield", "boss"), "aquatic", "", "elder_guardian", "aquatic"));
        add(result, theme("minecraft:shulker", "end", "shulker", "end", roles("battlefield"), "end", "", "", "ranged"));
        add(result, theme("minecraft:vindicator", "illager", "vindicator", "illager", roles("battlefield", "raid"), "illager", "", "", "melee"));
        add(result, theme("minecraft:evoker", "illager", "evoker", "illager", roles("battlefield", "raid"), "illager", "", "", "magic"));
        add(result, theme("minecraft:pillager", "illager", "pillager", "illager", roles("battlefield", "raid"), "illager", "", "", "ranged"));
        add(result, theme("minecraft:ravager", "illager", "ravager", "illager", roles("battlefield", "raid"), "illager", "", "", "beast"));
        add(result, theme("minecraft:vex", "illager", "vex", "illager", roles("battlefield", "raid"), "illager", "", "", "flying", "magic"));
        add(result, theme("minecraft:warden", "boss_like", "warden", "deep_dark", roles("battlefield", "boss"), "warden", "", "warden", "deep_dark"));
        add(result, theme("minecraft:breeze", "trial", "breeze", "wind", roles("battlefield"), "breeze", "", "", "wind"));
        add(result, theme("minecraft:piglin", "nether", "piglin", "nether", roles("battlefield"), "nether", "", "", "piglin"));
        add(result, theme("minecraft:piglin_brute", "nether", "piglin_brute", "nether", roles("battlefield"), "nether", "", "", "piglin", "boss_like"));
        add(result, theme("minecraft:zombified_piglin", "undead", "zombified_piglin", "undead", roles("battlefield"), "undead", "", "", "nether", "piglin"));
        add(result, theme("minecraft:hoglin", "nether", "hoglin", "nether", roles("battlefield"), "nether", "", "", "beast"));
        add(result, theme("minecraft:zoglin", "undead", "zoglin", "undead", roles("battlefield"), "undead", "", "", "nether", "beast"));

        add(result, theme("minecraft:ender_dragon", "boss_like", "ender_dragon", "dragon", roles("boss", "battlefield"), "boss_like", "", "ender_dragon", "end", "dragon"));
        add(result, theme("minecraft:wither", "boss_like", "wither", "wither", roles("boss", "battlefield"), "boss_like", "", "wither", "undead"));

        add(result, theme("minecraft:cow", "farm_animal", "cow", "herd", roles("slaughter"), "", "cow", "", "pasture"));
        add(result, theme("minecraft:mooshroom", "farm_animal", "mooshroom", "herd", roles("slaughter"), "", "cow", "", "pasture", "mushroom"));
        add(result, theme("minecraft:pig", "farm_animal", "pig", "hog", roles("slaughter"), "", "pig", "", "pasture"));
        add(result, theme("minecraft:sheep", "farm_animal", "sheep", "flock", roles("slaughter"), "", "sheep", "", "wool"));
        add(result, theme("minecraft:chicken", "farm_animal", "chicken", "flock", roles("slaughter"), "", "chicken", "", "feather"));
        add(result, theme("minecraft:rabbit", "farm_animal", "rabbit", "small_animal", roles("slaughter"), "", "farm_animal", "", "wild_animal"));
        add(result, theme("minecraft:horse", "mount", "horse", "mount", roles("slaughter", "memorial"), "", "mount", "", "companion"));
        add(result, theme("minecraft:donkey", "mount", "donkey", "mount", roles("slaughter", "memorial"), "", "mount", "", "companion"));
        add(result, theme("minecraft:mule", "mount", "mule", "mount", roles("slaughter", "memorial"), "", "mount", "", "companion"));
        add(result, theme("minecraft:llama", "mount", "llama", "mount", roles("slaughter", "memorial"), "", "mount", "", "companion"));
        add(result, theme("minecraft:trader_llama", "mount", "trader_llama", "mount", roles("slaughter", "memorial"), "", "mount", "", "companion"));
        add(result, theme("minecraft:camel", "mount", "camel", "mount", roles("slaughter", "memorial"), "", "mount", "", "companion"));
        add(result, theme("minecraft:goat", "farm_animal", "goat", "herd", roles("slaughter"), "", "farm_animal", "", "mountain"));
        add(result, theme("minecraft:turtle", "aquatic", "turtle", "aquatic", roles("slaughter", "memorial"), "", "aquatic", "", "wild_animal"));
        add(result, theme("minecraft:armadillo", "wild_animal", "armadillo", "wild_animal", roles("slaughter"), "", "wild_animal", "", "ambient"));
        add(result, theme("minecraft:frog", "wild_animal", "frog", "wild_animal", roles("slaughter"), "", "wild_animal", "", "aquatic"));
        add(result, theme("minecraft:tadpole", "aquatic", "tadpole", "aquatic", roles("slaughter"), "", "aquatic", "", "wild_animal"));
        add(result, theme("minecraft:sniffer", "wild_animal", "sniffer", "wild_animal", roles("slaughter"), "", "wild_animal", "", "ancient"));
        add(result, theme("minecraft:bat", "ambient", "bat", "ambient", roles("slaughter"), "", "ambient", "", "flying"));
        add(result, theme("minecraft:squid", "aquatic", "squid", "aquatic", roles("slaughter"), "", "aquatic", "", "catch"));
        add(result, theme("minecraft:glow_squid", "aquatic", "glow_squid", "aquatic", roles("slaughter"), "", "aquatic", "", "catch"));
        add(result, theme("minecraft:cod", "aquatic", "cod", "aquatic", roles("slaughter"), "", "aquatic", "", "fish", "catch"));
        add(result, theme("minecraft:salmon", "aquatic", "salmon", "aquatic", roles("slaughter"), "", "aquatic", "", "fish", "catch"));
        add(result, theme("minecraft:tropical_fish", "aquatic", "tropical_fish", "aquatic", roles("slaughter"), "", "aquatic", "", "fish", "catch"));
        add(result, theme("minecraft:pufferfish", "aquatic", "pufferfish", "aquatic", roles("slaughter"), "", "aquatic", "", "fish", "catch"));
        add(result, theme("minecraft:allay", "companion", "allay", "companion", roles("slaughter", "memorial"), "", "companion", "", "spirit"));
        add(result, theme("minecraft:axolotl", "companion", "axolotl", "companion", roles("slaughter", "memorial"), "", "companion", "", "aquatic"));
        add(result, theme("minecraft:cat", "companion", "cat", "companion", roles("slaughter", "memorial"), "", "companion", "", "pet"));
        add(result, theme("minecraft:ocelot", "wild_animal", "ocelot", "wild_animal", roles("slaughter"), "", "wild_animal", "", "cat"));
        add(result, theme("minecraft:parrot", "companion", "parrot", "companion", roles("slaughter", "memorial"), "", "companion", "", "pet", "flying"));
        add(result, theme("minecraft:fox", "wild_animal", "fox", "wild_animal", roles("slaughter"), "", "wild_animal", "", "ambient"));
        add(result, theme("minecraft:panda", "wild_animal", "panda", "wild_animal", roles("slaughter"), "", "wild_animal", "", "ambient"));
        add(result, theme("minecraft:polar_bear", "wild_animal", "polar_bear", "wild_animal", roles("slaughter"), "", "wild_animal", "", "bear"));
        add(result, theme("minecraft:wolf", "companion", "wolf", "companion", roles("slaughter", "memorial"), "", "companion", "", "pet"));
        add(result, theme("minecraft:bee", "wild_animal", "bee", "wild_animal", roles("slaughter"), "", "wild_animal", "", "flying"));
        add(result, theme("minecraft:dolphin", "aquatic", "dolphin", "aquatic", roles("slaughter", "memorial"), "", "aquatic", "", "companion"));
        add(result, theme("minecraft:villager", "civilian", "villager", "civilian", roles("slaughter", "memorial"), "", "civilian", "", "settlement"));
        add(result, theme("minecraft:wandering_trader", "civilian", "wandering_trader", "civilian", roles("slaughter", "memorial"), "", "civilian", "", "settlement"));
        add(result, theme("minecraft:snow_golem", "guardian_construct", "snow_golem", "construct", roles("slaughter", "memorial"), "", "guardian_construct", "", "settlement"));
        add(result, theme("minecraft:iron_golem", "guardian_construct", "iron_golem", "construct", roles("slaughter", "memorial"), "", "guardian_construct", "", "settlement"));

        return Collections.unmodifiableMap(result);
    }

    private static Set<String> buildRelevantIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(THEMES.keySet()));
    }

    private static MobTheme theme(
            String entityId,
            String primaryGroup,
            String semanticRoot,
            String rootGroup,
            Set<String> nameRoles,
            String battlefieldTheme,
            String slaughterTheme,
            String bossTheme,
            String memorialTheme,
            String... secondaryGroups
    ) {
        return new MobTheme(
                entityId,
                primaryGroup,
                Set.of(secondaryGroups),
                semanticRoot,
                rootGroup,
                nameRoles,
                battlefieldTheme,
                slaughterTheme,
                bossTheme,
                memorialTheme
        );
    }

    private static Set<String> roles(String... roles) {
        return Set.of(roles);
    }

    private static void add(Map<String, MobTheme> target, MobTheme theme) {
        target.put(theme.entityId(), theme);
    }

    private static MobTheme fromCompat(CompatThemeDefinitions.MobThemeDefinition definition) {
        String role = normalize(definition.combatRole());
        String group = definition.mobGroup().isBlank() ? definition.mobTheme() : definition.mobGroup();
        Set<String> roles = switch (role) {
            case "passive" -> roles("slaughter");
            case "boss" -> roles("boss", "battlefield");
            case "companion" -> roles("memorial", "slaughter");
            case "neutral", "hostile" -> roles("battlefield");
            default -> definition.placeType() == PlaceType.SLAUGHTER_FIELD ? roles("slaughter") : roles("battlefield");
        };
        String battlefield = "passive".equals(role) ? "" : definition.mobTheme();
        String slaughter = "passive".equals(role) || definition.placeType() == PlaceType.SLAUGHTER_FIELD
                ? definition.mobTheme()
                : "";
        String boss = "boss".equals(role) || definition.placeType() == PlaceType.BOSS_SITE ? definition.mobTheme() : "";
        String memorial = "companion".equals(role) ? definition.mobTheme() : "";
        return new MobTheme(
                definition.entity(),
                group,
                Set.of(definition.mobTheme()),
                definition.mobTheme(),
                group,
                roles,
                battlefield,
                slaughter,
                boss,
                memorial
        );
    }

    private static String normalize(String value) {
        return WorldPos.optionalId(value).toLowerCase(java.util.Locale.ROOT);
    }

    private static Set<String> normalizedSet(Set<String> values) {
        Set<String> result = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                String normalized = normalize(value);
                if (!normalized.isBlank()) {
                    result.add(normalized);
                }
            }
        }
        return result;
    }

    public record MobTheme(
            String entityId,
            String primaryGroup,
            Set<String> secondaryGroups,
            String semanticRoot,
            String rootGroup,
            Set<String> nameRoles,
            String battlefieldTheme,
            String slaughterTheme,
            String bossTheme,
            String memorialTheme
    ) implements Serializable {
        private static final long serialVersionUID = 1L;

        public MobTheme {
            entityId = normalize(entityId);
            primaryGroup = normalize(primaryGroup);
            secondaryGroups = Collections.unmodifiableSet(normalizedSet(secondaryGroups));
            semanticRoot = NameSemanticRoots.normalize(semanticRoot);
            rootGroup = NameSemanticRoots.normalize(rootGroup);
            nameRoles = Collections.unmodifiableSet(normalizedSet(nameRoles));
            battlefieldTheme = normalize(battlefieldTheme);
            slaughterTheme = normalize(slaughterTheme);
            bossTheme = normalize(bossTheme);
            memorialTheme = normalize(memorialTheme);
        }

        public static MobTheme empty(String entityId) {
            String normalized = normalize(entityId);
            return new MobTheme(normalized, "", Set.of(), normalized, "", Set.of(), "", "", "", "");
        }

        public boolean hasMapping() {
            return !primaryGroup.isBlank();
        }

        public boolean matchesAny(Set<String> requiredKeys) {
            return matchPriority(requiredKeys, PlaceType.UNKNOWN) > 0;
        }

        public int matchPriority(Set<String> requiredKeys, PlaceType placeType) {
            if (requiredKeys == null || requiredKeys.isEmpty()) {
                return 0;
            }
            Set<String> keys = normalizedSet(requiredKeys);
            if (keys.contains(entityId)) {
                return 500;
            }
            String selectedTheme = themeFor(placeType);
            if (containsThemeKey(keys, selectedTheme)) {
                return 400;
            }
            if (containsGroupKey(keys, primaryGroup)) {
                return 300;
            }
            for (String group : secondaryGroups) {
                if (containsGroupKey(keys, group)) {
                    return 200;
                }
            }
            if (containsRootKey(keys, semanticRoot) || containsRootKey(keys, rootGroup)) {
                return 100;
            }
            for (String role : nameRoles) {
                if (keys.contains(role)) {
                    return 100;
                }
            }
            if (containsThemeKey(keys, battlefieldTheme)
                    || containsThemeKey(keys, slaughterTheme)
                    || containsThemeKey(keys, bossTheme)
                    || containsThemeKey(keys, memorialTheme)) {
                return 90;
            }
            return 0;
        }

        public Set<String> namingKeys() {
            Set<String> result = new LinkedHashSet<>();
            addKey(result, entityId);
            addGroupKey(result, primaryGroup);
            for (String group : secondaryGroups) {
                addGroupKey(result, group);
            }
            addRootKey(result, semanticRoot);
            addRootKey(result, rootGroup);
            addThemeKey(result, battlefieldTheme);
            addThemeKey(result, slaughterTheme);
            addThemeKey(result, bossTheme);
            addThemeKey(result, memorialTheme);
            result.addAll(nameRoles);
            return Collections.unmodifiableSet(result);
        }

        public String debugString() {
            return "entityId=" + entityId
                    + " primaryGroup=" + primaryGroup
                    + " secondaryGroups=" + secondaryGroups
                    + " semanticRoot=" + semanticRoot
                    + " rootGroup=" + rootGroup
                    + " roles=" + nameRoles
                    + " battlefieldTheme=" + battlefieldTheme
                    + " slaughterTheme=" + slaughterTheme
                    + " bossTheme=" + bossTheme
                    + " memorialTheme=" + memorialTheme
                    + " fallbackKeys=" + namingKeys();
        }

        private static Set<String> normalizedSet(Set<String> values) {
            Set<String> result = new LinkedHashSet<>();
            if (values != null) {
                for (String value : values) {
                    String normalized = normalize(value);
                    if (!normalized.isBlank()) {
                        result.add(normalized);
                    }
                }
            }
            return result;
        }

        private String themeFor(PlaceType placeType) {
            PlaceType resolvedType = placeType == null ? PlaceType.UNKNOWN : placeType;
            return switch (resolvedType) {
                case BATTLEFIELD, PVP_ARENA -> battlefieldTheme;
                case SLAUGHTER_FIELD -> slaughterTheme;
                case BOSS_SITE -> bossTheme;
                case PET_MEMORIAL, NAMED_MOB_MEMORIAL -> memorialTheme;
                default -> "";
            };
        }

        private static boolean containsGroupKey(Set<String> keys, String group) {
            String normalized = normalize(group);
            return !normalized.isBlank() && (keys.contains(normalized) || keys.contains("group:" + normalized));
        }

        private static boolean containsThemeKey(Set<String> keys, String theme) {
            String normalized = normalize(theme);
            return !normalized.isBlank() && (keys.contains(normalized) || keys.contains("theme:" + normalized));
        }

        private static boolean containsRootKey(Set<String> keys, String root) {
            String normalized = NameSemanticRoots.normalize(root);
            return !NameSemanticRoots.UNKNOWN.equals(normalized) && (keys.contains(normalized) || keys.contains("root:" + normalized));
        }

        private static void addGroupKey(Set<String> target, String group) {
            String normalized = normalize(group);
            if (!normalized.isBlank()) {
                target.add(normalized);
                target.add("group:" + normalized);
            }
        }

        private static void addRootKey(Set<String> target, String root) {
            String normalized = NameSemanticRoots.normalize(root);
            if (!NameSemanticRoots.UNKNOWN.equals(normalized)) {
                target.add(normalized);
                target.add("root:" + normalized);
            }
        }

        private static void addThemeKey(Set<String> target, String theme) {
            String normalized = normalize(theme);
            if (!normalized.isBlank()) {
                target.add(normalized);
                target.add("theme:" + normalized);
            }
        }

        private static void addKey(Set<String> target, String key) {
            String normalized = normalize(key);
            if (!normalized.isBlank()) {
                target.add(normalized);
            }
        }
    }
}
