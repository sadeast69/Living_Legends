package com.worldremembers.livinglegends;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.StringJoiner;

public final class NameGenerator {
    private static final Set<String> STRUCTURAL_TAUTOLOGY_ROOTS = Set.of(
            "shore",
            "crossing",
            "path",
            "trail",
            "rest",
            "stone",
            "mark",
            "gate",
            "threshold",
            "field",
            "place",
            "trace",
            "ember",
            "ash",
            "flame",
            "lava",
            "basalt",
            "blackstone",
            "shadow",
            "scar",
            "wound",
            "rift"
    );

    private static final int MAX_DUPLICATE_AVOIDANCE_ATTEMPTS = 48;
    private static final String[] SHARED_RUNTIME_MEMORIAL_PATTERN_KEYS = {
            "living_legends.name.pattern.runtime_memory",
            "living_legends.name.pattern.runtime_trace",
            "living_legends.name.pattern.runtime_name",
            "living_legends.name.pattern.runtime_place",
            "living_legends.name.pattern.runtime_remembering",
            "living_legends.name.pattern.runtime_last_trace",
            "living_legends.name.pattern.runtime_quiet_memory"
    };
    private static final Map<String, String[]> STYLE_RUNTIME_MEMORIAL_PATTERN_KEYS = Map.of(
            "vanilla_adventure", new String[] {
                    "living_legends.name.pattern.va_runtime_memory",
                    "living_legends.name.pattern.va_runtime_trace",
                    "living_legends.name.pattern.va_runtime_last_trace",
                    "living_legends.name.pattern.va_runtime_quiet_mark",
                    "living_legends.name.pattern.va_runtime_rest",
                    "living_legends.name.pattern.va_runtime_memory_place"
            },
            "neutral_server", new String[] {
                    "living_legends.name.pattern.ns_runtime_memorial",
                    "living_legends.name.pattern.ns_runtime_record",
                    "living_legends.name.pattern.ns_runtime_memory_mark",
                    "living_legends.name.pattern.ns_runtime_trace",
                    "living_legends.name.pattern.ns_runtime_memory_place",
                    "living_legends.name.pattern.ns_runtime_creature_memory"
            },
            "dark_fantasy", new String[] {
                    "living_legends.name.pattern.df_runtime_quiet_shadow",
                    "living_legends.name.pattern.df_runtime_last_scar",
                    "living_legends.name.pattern.df_runtime_grim_memory",
                    "living_legends.name.pattern.df_runtime_silent_trace",
                    "living_legends.name.pattern.df_runtime_shadow_rest",
                    "living_legends.name.pattern.df_runtime_loss_mark"
            },
            "cozy_survival", new String[] {
                    "living_legends.name.pattern.cs_runtime_kind_memory",
                    "living_legends.name.pattern.cs_runtime_quiet_rest",
                    "living_legends.name.pattern.cs_runtime_bright_trace",
                    "living_legends.name.pattern.cs_runtime_memory_nook",
                    "living_legends.name.pattern.cs_runtime_warm_memory",
                    "living_legends.name.pattern.cs_runtime_remembered_here"
            },
            "epic_mythology", new String[] {
                    "living_legends.name.pattern.em_runtime_saga",
                    "living_legends.name.pattern.em_runtime_memory_oath",
                    "living_legends.name.pattern.em_runtime_memory_crown",
                    "living_legends.name.pattern.em_runtime_chronicle",
                    "living_legends.name.pattern.em_runtime_legend_trace",
                    "living_legends.name.pattern.em_runtime_star_memory"
            },
            "funny_community", new String[] {
                    "living_legends.name.pattern.fc_runtime_remembered_oops",
                    "living_legends.name.pattern.fc_runtime_sorry_site",
                    "living_legends.name.pattern.fc_runtime_friend_trace",
                    "living_legends.name.pattern.fc_runtime_chat_memory",
                    "living_legends.name.pattern.fc_runtime_quiet_respect",
                    "living_legends.name.pattern.fc_runtime_server_legend"
            }
    );

    private NameGenerator() {
    }

    public static NameRecipe generate(
            PlaceCluster cluster,
            PlaceType placeType,
            DeathSiteEnvironment environment,
            long seed
    ) {
        return generate(cluster, placeType, environment, seed, BuiltInNameData.defaultPack(), List.of());
    }

    public static NameRecipe generate(
            PlaceCluster cluster,
            PlaceType placeType,
            DeathSiteEnvironment environment,
            long seed,
            NameDataPack nameData,
            Iterable<NameRecipe> nearbyExistingRecipes
    ) {
        return generate(cluster, placeType, environment, seed, nameData, nearbyExistingRecipes, null);
    }

    public static NameRecipe generate(
            PlaceCluster cluster,
            PlaceType placeType,
            DeathSiteEnvironment environment,
            long seed,
            NameDataPack nameData,
            Iterable<NameRecipe> nearbyExistingRecipes,
            NameGenerationDiagnostics diagnostics
    ) {
        NameDataPack resolvedData = nameData == null ? BuiltInNameData.defaultPack() : nameData;
        PlaceType resolvedType = placeType == null && cluster != null ? cluster.placeType() : placeType;
        resolvedType = resolvedType == null ? PlaceType.UNKNOWN : resolvedType;
        DeathSiteEnvironment resolvedEnvironment = environment == null && cluster != null ? cluster.environment() : environment;
        resolvedEnvironment = resolvedEnvironment == null ? DeathSiteEnvironment.UNKNOWN : resolvedEnvironment;
        NameContext context = NameContext.from(cluster, resolvedType, resolvedEnvironment, resolvedData.styleId());
        return generate(cluster, context, seed, resolvedData, nearbyExistingRecipes, diagnostics);
    }

    public static NameRecipe generate(
            PlaceCluster cluster,
            NameContext context,
            long seed,
            NameDataPack nameData,
            Iterable<NameRecipe> nearbyExistingRecipes,
            NameGenerationDiagnostics diagnostics
    ) {
        NameDataPack resolvedData = nameData == null ? BuiltInNameData.defaultPack() : nameData;
        NameContext resolvedContext = context == null
                ? NameContext.from(cluster, cluster == null ? PlaceType.UNKNOWN : cluster.placeType(), cluster == null ? DeathSiteEnvironment.UNKNOWN : cluster.environment(), resolvedData.styleId())
                : context;
        PlaceType resolvedType = resolvedContext.placeType();
        DeathSiteEnvironment resolvedEnvironment = resolvedContext.environment();
        Set<String> existingSignatures = recipeSignatures(nearbyExistingRecipes);

        NameRecipe bestRecipe = NameRecipe.empty();
        for (int attempt = 0; attempt < MAX_DUPLICATE_AVOIDANCE_ATTEMPTS; attempt++) {
            NameRecipe recipe = generateAttempt(
                    cluster,
                    resolvedType,
                    resolvedEnvironment,
                    seed + attempt * 31L,
                    resolvedData,
                    diagnostics,
                    true,
                    resolvedContext
            );
            bestRecipe = recipe;
            if (!existingSignatures.contains(recipe.recipeSignature())) {
                if (diagnostics != null) {
                    diagnostics.generated();
                }
                return recipe;
            }
            if (diagnostics != null) {
                diagnostics.duplicateAvoided();
            }
        }
        if (diagnostics != null) {
            diagnostics.generated();
        }
        return bestRecipe;
    }

    public static NameRecipe generateAttempt(
            PlaceCluster cluster,
            PlaceType placeType,
            DeathSiteEnvironment environment,
            long seed,
            NameDataPack nameData
    ) {
        NameDataPack resolvedData = nameData == null ? BuiltInNameData.defaultPack() : nameData;
        return generateAttempt(cluster, placeType, environment, seed, resolvedData, null, true, NameContext.from(cluster, placeType, environment, resolvedData.styleId()));
    }

    public static NameRecipe generate(
            NameContext context,
            long seed,
            NameDataPack nameData,
            Iterable<NameRecipe> nearbyExistingRecipes,
            NameGenerationDiagnostics diagnostics
    ) {
        NameContext resolvedContext = context == null
                ? NameContext.from(PlaceType.UNKNOWN, DeathSiteEnvironment.UNKNOWN, PlaceCause.unknown(), BuiltInNameData.DEFAULT_STYLE_ID)
                : context;
        NameDataPack resolvedData = nameData == null ? BuiltInNameData.defaultPack() : nameData;
        Set<String> existingSignatures = recipeSignatures(nearbyExistingRecipes);
        NameRecipe bestRecipe = NameRecipe.empty();
        for (int attempt = 0; attempt < MAX_DUPLICATE_AVOIDANCE_ATTEMPTS; attempt++) {
            NameRecipe recipe = generateAttempt(
                    null,
                    resolvedContext.placeType(),
                    resolvedContext.environment(),
                    seed + attempt * 31L,
                    resolvedData,
                    diagnostics,
                    true,
                    resolvedContext
            );
            bestRecipe = recipe;
            if (!existingSignatures.contains(recipe.recipeSignature())) {
                if (diagnostics != null) {
                    diagnostics.generated();
                }
                return recipe;
            }
            if (diagnostics != null) {
                diagnostics.duplicateAvoided();
            }
        }
        if (diagnostics != null) {
            diagnostics.generated();
        }
        return bestRecipe;
    }

    private static NameRecipe generateAttempt(
            PlaceCluster cluster,
            PlaceType placeType,
            DeathSiteEnvironment environment,
            long seed,
            NameDataPack nameData,
            NameGenerationDiagnostics diagnostics,
            boolean allowVanillaFallback,
            NameContext context
    ) {
        NameDataPack resolvedData = nameData == null ? BuiltInNameData.defaultPack() : nameData;
        NameRecipe runtimeMemorialRecipe = runtimeMemorialRecipe(context, resolvedData.styleId(), seed);
        if (runtimeMemorialRecipe != null) {
            if (diagnostics != null) {
                diagnostics.selectedPatternSource(NamePatternSource.EXACT_CAUSE);
            }
            return runtimeMemorialRecipe;
        }

        long mixedSeed = mixedSeed(cluster, placeType, environment, seed);
        Random random = new Random(mixedSeed);
        for (NamePatternSource source : selectionOrder()) {
            NameRecipe recipe = generateFromSource(
                    resolvedData,
                    placeType,
                    environment,
                    context,
                    seed,
                    random,
                    diagnostics,
                    source
            );
            if (recipe != null) {
                return recipe;
            }

        }

        if (allowVanillaFallback && !BuiltInNameData.DEFAULT_STYLE_ID.equals(resolvedData.styleId())) {
            return generateAttempt(
                    cluster,
                    placeType,
                    environment,
                    seed,
                    BuiltInNameData.defaultPack(),
                    diagnostics,
                    false,
                    context
            );
        }

        return NameRecipe.empty();
    }

    private static NameRecipe runtimeMemorialRecipe(NameContext context, String styleId, long seed) {
        if (context == null) {
            return null;
        }
        String runtimeName = switch (context.placeType()) {
            case PET_MEMORIAL -> context.petName();
            case NAMED_MOB_MEMORIAL -> context.namedMobName();
            default -> "";
        };
        String literalToken = NameRecipe.literalToken(runtimeName);
        if (literalToken.isBlank()) {
            return null;
        }

        String resolvedStyleId = WorldPos.optionalId(styleId);
        if (resolvedStyleId.isBlank()) {
            resolvedStyleId = BuiltInNameData.DEFAULT_STYLE_ID;
        }
        String[] patternKeys = runtimeMemorialPatternKeys(resolvedStyleId);
        int patternIndex = Math.floorMod(seed, patternKeys.length);
        String patternKey = patternKeys[patternIndex];
        return new NameRecipe(
                resolvedStyleId,
                patternKey,
                List.of(literalToken),
                List.of(NameTokenForm.BASE),
                seed,
                runtimeFallbackName(patternKey, runtimeName)
        );
    }

    private static String[] runtimeMemorialPatternKeys(String styleId) {
        String[] patternKeys = STYLE_RUNTIME_MEMORIAL_PATTERN_KEYS.get(styleId);
        if (patternKeys == null || patternKeys.length == 0) {
            return SHARED_RUNTIME_MEMORIAL_PATTERN_KEYS;
        }
        return patternKeys;
    }

    public static boolean isRuntimeMemorialPatternKey(String styleId, String patternKey) {
        String key = WorldPos.optionalId(patternKey);
        if (key.isBlank()) {
            return false;
        }
        for (String sharedKey : SHARED_RUNTIME_MEMORIAL_PATTERN_KEYS) {
            if (sharedKey.equals(key)) {
                return true;
            }
        }

        String resolvedStyleId = WorldPos.optionalId(styleId);
        if (!resolvedStyleId.isBlank()) {
            for (String styleKey : runtimeMemorialPatternKeys(resolvedStyleId)) {
                if (styleKey.equals(key)) {
                    return true;
                }
            }
            return false;
        }

        for (String[] styleKeys : STYLE_RUNTIME_MEMORIAL_PATTERN_KEYS.values()) {
            for (String styleKey : styleKeys) {
                if (styleKey.equals(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String runtimeFallbackName(String patternKey, String runtimeName) {
        String name = RuntimeNameFormatter.sanitize(runtimeName);
        if (name.isBlank()) {
            return "";
        }
        return switch (patternKey) {
            case "living_legends.name.pattern.runtime_trace" -> "Trace of " + name;
            case "living_legends.name.pattern.runtime_name" -> "Name of " + name;
            case "living_legends.name.pattern.runtime_place" -> "Place of " + name;
            case "living_legends.name.pattern.runtime_remembering" -> "Remembering " + name;
            case "living_legends.name.pattern.runtime_last_trace" -> "Last Trace of " + name;
            case "living_legends.name.pattern.runtime_quiet_memory" -> "Quiet Memory of " + name;
            case "living_legends.name.pattern.va_runtime_trace" -> "Trace of " + name;
            case "living_legends.name.pattern.va_runtime_last_trace" -> "Last Trace of " + name;
            case "living_legends.name.pattern.va_runtime_quiet_mark" -> "Quiet Mark of " + name;
            case "living_legends.name.pattern.va_runtime_rest" -> "Rest of " + name;
            case "living_legends.name.pattern.va_runtime_memory_place" -> "Place of Memory: " + name;
            case "living_legends.name.pattern.ns_runtime_memorial" -> "Memorial of " + name;
            case "living_legends.name.pattern.ns_runtime_record" -> "Record of " + name;
            case "living_legends.name.pattern.ns_runtime_memory_mark" -> "Memory Mark of " + name;
            case "living_legends.name.pattern.ns_runtime_trace" -> "Trace of " + name;
            case "living_legends.name.pattern.ns_runtime_memory_place" -> "Place of Memory: " + name;
            case "living_legends.name.pattern.ns_runtime_creature_memory" -> "Creature Memory: " + name;
            case "living_legends.name.pattern.df_runtime_quiet_shadow" -> "Quiet Shadow of " + name;
            case "living_legends.name.pattern.df_runtime_last_scar" -> "Last Scar of " + name;
            case "living_legends.name.pattern.df_runtime_grim_memory" -> "Grim Memory of " + name;
            case "living_legends.name.pattern.df_runtime_silent_trace" -> "Trace in Silence: " + name;
            case "living_legends.name.pattern.df_runtime_shadow_rest" -> "Shadow Rest of " + name;
            case "living_legends.name.pattern.df_runtime_loss_mark" -> "Mark of Loss: " + name;
            case "living_legends.name.pattern.cs_runtime_kind_memory" -> "Kind Memory of " + name;
            case "living_legends.name.pattern.cs_runtime_quiet_rest" -> "Quiet Rest of " + name;
            case "living_legends.name.pattern.cs_runtime_bright_trace" -> "Bright Trace of " + name;
            case "living_legends.name.pattern.cs_runtime_memory_nook" -> "Memory Nook of " + name;
            case "living_legends.name.pattern.cs_runtime_warm_memory" -> "Warm Memory of " + name;
            case "living_legends.name.pattern.cs_runtime_remembered_here" -> "Remembered Here: " + name;
            case "living_legends.name.pattern.em_runtime_saga" -> "Saga of " + name;
            case "living_legends.name.pattern.em_runtime_memory_oath" -> "Oath of Memory: " + name;
            case "living_legends.name.pattern.em_runtime_memory_crown" -> "Crown of Memory: " + name;
            case "living_legends.name.pattern.em_runtime_chronicle" -> "Chronicle of " + name;
            case "living_legends.name.pattern.em_runtime_legend_trace" -> "Trace of Legend: " + name;
            case "living_legends.name.pattern.em_runtime_star_memory" -> "Memory of Stars: " + name;
            case "living_legends.name.pattern.fc_runtime_remembered_oops" -> "Remembered Oops: " + name;
            case "living_legends.name.pattern.fc_runtime_sorry_site" -> "\"Sorry\" Site: " + name;
            case "living_legends.name.pattern.fc_runtime_friend_trace" -> "Friend Trace: " + name;
            case "living_legends.name.pattern.fc_runtime_chat_memory" -> "Chat Remembers " + name;
            case "living_legends.name.pattern.fc_runtime_quiet_respect" -> "Quiet Respect: " + name;
            case "living_legends.name.pattern.fc_runtime_server_legend" -> "Server Legend: " + name;
            default -> "Memory of " + name;
        };
    }

    private static NameRecipe generateFromSource(
            NameDataPack nameData,
            PlaceType placeType,
            DeathSiteEnvironment environment,
            NameContext context,
            long seed,
            Random random,
            NameGenerationDiagnostics diagnostics,
            NamePatternSource source
    ) {
        List<NamePattern> patterns = weightedOrder(
                compatiblePatterns(nameData, placeType, environment, context, source, diagnostics),
                random
        );
        if (source == NamePatternSource.SAFE_FALLBACK) {
            if (diagnostics != null) {
                diagnostics.selectedPatternSource(NamePatternSource.SAFE_FALLBACK);
            }
            return new NameRecipe(
                    nameData == null ? BuiltInNameData.DEFAULT_STYLE_ID : nameData.styleId(),
                    "living_legends.name.pattern.safe_fallback",
                    List.of(),
                    List.of(),
                    seed,
                    "Remembered Place"
            );
        }
        for (NamePattern pattern : patterns) {
            NameRecipe recipe = tryPattern(nameData, pattern, context, seed, random, diagnostics);
            if (recipe != null) {
                if (diagnostics != null) {
                    diagnostics.selectedPatternSource(source);
                }
                return recipe;
            }
        }
        return null;
    }

    private static NameRecipe tryPattern(
            NameDataPack nameData,
            NamePattern pattern,
            NameContext context,
            long seed,
            Random random,
            NameGenerationDiagnostics diagnostics
    ) {
        List<String> tokenIds = new ArrayList<>();
        List<NameTokenForm> forms = new ArrayList<>();
        Set<String> selectedSemanticRoots = new LinkedHashSet<>();
        for (NamePatternSlot slot : pattern.slots()) {
            List<NameToken> tokens = compatibleTokens(nameData, pattern, slot, context, tokenIds, selectedSemanticRoots, diagnostics);
            NameToken token = weightedPick(tokens, random);
            if (token == null) {
                if (diagnostics != null) {
                    diagnostics.rejectRoleMismatch(pattern, slot);
                }
                return null;
            }
            tokenIds.add(token.id());
            forms.add(slot.requiredForm());
            selectedSemanticRoots.addAll(NameSemanticRoots.conflictRoots(token.semanticRoot()));
        }

        return new NameRecipe(
                nameData.styleId(),
                pattern.translationKey(),
                tokenIds,
                forms,
                seed,
                fallbackName(pattern, tokenIds)
        );
    }

    public static String debugRecipe(NameRecipe recipe) {
        if (recipe == null) {
            return "NameRecipe{empty}";
        }
        String fallback = recipe.fallbackResolvedName().isBlank()
                ? ""
                : ", fallbackResolvedName=" + recipe.fallbackResolvedName();
        return "NameRecipe{style=" + recipe.styleId()
                + ", patternKey=" + recipe.patternKey()
                + ", tokens=" + recipe.selectedTokenIds()
                + ", forms=" + recipe.requestedTokenForms()
                + ", seed=" + recipe.seed()
                + ", signature=" + recipe.recipeSignature()
                + fallback
                + "}";
    }

    private static List<NamePattern> compatiblePatterns(
            NameDataPack nameData,
            PlaceType placeType,
            DeathSiteEnvironment environment,
            NameContext context,
            NamePatternSource source,
            NameGenerationDiagnostics diagnostics
    ) {
        List<NamePattern> result = new ArrayList<>();
        for (NamePattern pattern : nameData.patterns()) {
            if (!pattern.supports(placeType, environment)) {
                continue;
            }
            if (pattern.sourceFor(environment) != source) {
                continue;
            }
            if (!pattern.causeConstraints().matchesPattern(context)) {
                if (diagnostics != null) {
                    if (source == NamePatternSource.DOMINANT_TARGET) {
                        diagnostics.rejectTargetMismatch(pattern, null, null);
                    } else {
                        diagnostics.rejectCauseMismatch(pattern, null, null);
                    }
                }
                continue;
            }
            result.add(pattern);
        }
        if (source == NamePatternSource.DOMINANT_TARGET) {
            int highestPriority = 0;
            for (NamePattern pattern : result) {
                highestPriority = Math.max(highestPriority, pattern.causeConstraints().dominantTargetPriority(context));
            }
            if (highestPriority > 0) {
                int selectedPriority = highestPriority;
                result = result.stream()
                        .filter(pattern -> pattern.causeConstraints().dominantTargetPriority(context) == selectedPriority)
                        .toList();
            }
        }
        return result.stream()
                .sorted(Comparator.comparing(NamePattern::id))
                .toList();
    }

    private static List<NameToken> compatibleTokens(
            NameDataPack nameData,
            NamePattern pattern,
            NamePatternSlot slot,
            NameContext context,
            List<String> selectedTokenIds,
            Set<String> selectedSemanticRoots,
            NameGenerationDiagnostics diagnostics
    ) {
        List<NameToken> result = new ArrayList<>();
        for (NameToken token : nameData.tokens()) {
            if (!token.hasTag(slot.requiredTag())) {
                continue;
            }
            if (pattern.forbiddenRoles().contains(slot.requiredTag()) || pattern.forbiddenRoles().stream().anyMatch(token.tags()::contains)) {
                if (diagnostics != null) {
                    diagnostics.rejectRoleMismatch(pattern, slot);
                }
                continue;
            }
            if (!token.supportsCause(context)) {
                if (diagnostics != null) {
                    diagnostics.rejectCauseMismatch(pattern, slot, token);
                }
                continue;
            }
            if (!environmentAllowsToken(token, context)) {
                if (diagnostics != null) {
                    diagnostics.rejectCauseMismatch(pattern, slot, token);
                }
                continue;
            }
            if (!token.supportsForm(slot.requiredForm())) {
                if (diagnostics != null) {
                    diagnostics.rejectMissingForm(pattern, slot, token);
                }
                continue;
            }
            if (selectedTokenIds != null && selectedTokenIds.contains(token.id())) {
                continue;
            }
            if (tautologicalToken(pattern, slot, token, selectedSemanticRoots)) {
                if (diagnostics != null) {
                    diagnostics.rejectTautology(pattern, slot, token, forbiddenRoots(pattern, slot));
                }
                continue;
            }
            result.add(token);
        }
        return result.stream()
                .sorted(Comparator.comparing(NameToken::id))
                .toList();
    }

    private static boolean environmentAllowsToken(NameToken token, NameContext context) {
        if (token == null || context == null) {
            return true;
        }

        boolean isWaterDeathToken = "drowned".equals(token.id())
                || (token.hasTag("water") && token.hasTag("death_subject"));
        if (!isWaterDeathToken) {
            return true;
        }

        String deathCause = context.deathCause();
        boolean drowningCause = deathCause.contains("drown") || deathCause.contains("water");
        return context.environment() == DeathSiteEnvironment.WATER || drowningCause;
    }

    private static List<NamePatternSource> selectionOrder() {
        return List.of(
                NamePatternSource.EXACT_CAUSE,
                NamePatternSource.DOMINANT_TARGET,
                NamePatternSource.CAUSE_TYPE,
                NamePatternSource.PLACE_TYPE_ENVIRONMENT,
                NamePatternSource.PLACE_TYPE_GENERIC,
                NamePatternSource.SAFE_FALLBACK
        );
    }

    private static boolean tautologicalToken(
            NamePattern pattern,
            NamePatternSlot slot,
            NameToken token,
            Set<String> selectedSemanticRoots
    ) {
        if (pattern == null || token == null) {
            return false;
        }

        Set<String> tokenRoots = NameSemanticRoots.conflictRoots(token.semanticRoot());
        Set<String> forbiddenRoots = forbiddenRoots(pattern, slot);
        if (intersects(forbiddenRoots, tokenRoots)) {
            return true;
        }
        if (selectedSemanticRoots != null && intersects(selectedSemanticRoots, tokenRoots)) {
            return true;
        }
        return false;
    }

    private static Set<String> forbiddenRoots(NamePattern pattern, NamePatternSlot slot) {
        Set<String> forbidden = new LinkedHashSet<>();
        if (slot != null) {
            for (String root : slot.forbiddenSemanticRoots()) {
                forbidden.addAll(NameSemanticRoots.conflictRoots(root));
            }
        }

        String id = pattern == null ? "" : pattern.id();
        String root = pattern == null ? NameSemanticRoots.UNKNOWN : pattern.semanticRoot();
        addStructuralRootConflicts(forbidden, root);
        addStructuralRootConflicts(forbidden, NameSemanticRoots.inferPatternRoot(id));
        if ("custom".equals(id) || id.endsWith("_memory") || id.contains("remembered")) {
            forbidden.add("memory");
        }
        if ("first".equals(root) || id.contains("first_discovery")) {
            forbidden.add("first");
            forbidden.add("discovery");
        }
        if (id.contains("gate")) {
            forbidden.add("gate");
        }
        if (id.contains("rest")) {
            forbidden.add("rest");
        }
        return forbidden;
    }

    private static void addStructuralRootConflicts(Set<String> roots, String root) {
        for (String conflictRoot : NameSemanticRoots.conflictRoots(root)) {
            if (STRUCTURAL_TAUTOLOGY_ROOTS.contains(conflictRoot)) {
                roots.add(conflictRoot);
            }
        }
    }

    private static boolean intersects(Set<String> left, Set<String> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return false;
        }
        for (String value : right) {
            if (left.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private static <T> List<T> weightedOrder(List<T> values, Random random) {
        List<T> remaining = new ArrayList<>(values == null ? List.of() : values);
        List<T> result = new ArrayList<>();
        while (!remaining.isEmpty()) {
            T selected = weightedPick(remaining, random);
            result.add(selected);
            remaining.remove(selected);
        }
        return result;
    }

    private static <T> T weightedPick(List<T> values, Random random) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        double totalWeight = 0.0;
        for (T value : values) {
            totalWeight += weight(value);
        }
        if (totalWeight <= 0.0) {
            return values.get(random.nextInt(values.size()));
        }

        double selected = random.nextDouble() * totalWeight;
        double cursor = 0.0;
        for (T value : values) {
            cursor += weight(value);
            if (selected <= cursor) {
                return value;
            }
        }
        return values.get(values.size() - 1);
    }

    private static double weight(Object value) {
        if (value instanceof NamePattern pattern) {
            return Math.max(0.0, pattern.weight());
        }
        if (value instanceof NameToken token) {
            return Math.max(0.0, token.weight());
        }
        return 1.0;
    }

    private static Set<String> recipeSignatures(Iterable<NameRecipe> recipes) {
        Set<String> signatures = new HashSet<>();
        if (recipes == null) {
            return signatures;
        }
        for (NameRecipe recipe : recipes) {
            if (recipe != null) {
                signatures.add(recipe.recipeSignature());
            }
        }
        return signatures;
    }

    private static long mixedSeed(
            PlaceCluster cluster,
            PlaceType placeType,
            DeathSiteEnvironment environment,
            long seed
    ) {
        int clusterHash = cluster == null ? 0 : Objects.hash(
                cluster.dimensionId(),
                cluster.centerX(),
                cluster.centerY(),
                cluster.centerZ(),
                cluster.placeType()
        );
        return seed
                ^ ((long) Objects.hashCode(placeType) << 32)
                ^ Objects.hashCode(environment)
                ^ clusterHash;
    }

    private static String fallbackName(NamePattern pattern, List<String> tokenIds) {
        StringJoiner joiner = new StringJoiner(" ");
        for (String tokenId : tokenIds) {
            joiner.add(tokenId.replace('_', ' '));
        }
        String tokens = joiner.toString();
        return tokens.isBlank() ? pattern.id().replace('_', ' ') : tokens;
    }

    public static List<NameRecipe> nearbyRecipes(WorldMemoryStorageData data, PlaceCluster cluster) {
        return nearbyRecipes(data, cluster, Math.max(64, cluster == null ? 64 : cluster.radius() * 2));
    }

    public static List<NameRecipe> nearbyRecipes(WorldMemoryStorageData data, PlaceCluster cluster, int radiusBlocks) {
        if (data == null || cluster == null) {
            return List.of();
        }

        int radius = Math.max(0, radiusBlocks);
        if (radius == 0) {
            return List.of();
        }

        List<NameRecipe> recipes = new ArrayList<>();
        for (NamedPlace place : data.namedPlaces()) {
            if (!sameDimension(place, cluster) || place.nameRecipe() == null) {
                continue;
            }
            if (nearCluster(place, cluster, radius)) {
                recipes.add(place.nameRecipe());
            }
        }
        return recipes;
    }

    private static boolean sameDimension(NamedPlace place, PlaceCluster cluster) {
        return place.bounds().dimensionId().equals(cluster.dimensionId());
    }

    private static boolean nearCluster(NamedPlace place, PlaceCluster cluster, int radiusBlocks) {
        PlaceBounds bounds = place.bounds();
        int centerX = (bounds.minX() + bounds.maxX()) / 2;
        int centerZ = (bounds.minZ() + bounds.maxZ()) / 2;
        long dx = centerX - cluster.centerX();
        long dz = centerZ - cluster.centerZ();
        long radius = Math.max(0L, radiusBlocks);
        return dx * dx + dz * dz <= radius * radius;
    }

    public static Map<String, NameToken> tokenMap(NameDataPack dataPack) {
        NameDataPack resolvedData = dataPack == null ? BuiltInNameData.defaultPack() : dataPack;
        return resolvedData.tokenMap();
    }
}
