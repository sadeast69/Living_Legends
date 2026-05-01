package com.worldremembers.livinglegends;

import com.worldremembers.livinglegends.config.LivingLegendsConfig;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public final class NameStyleSelector {
    private NameStyleSelector() {
    }

    public static String selectStyle(PlaceCluster cluster, LivingLegendsConfig config, long seed) {
        LivingLegendsConfig.Naming naming = config == null ? LivingLegendsConfig.defaults().naming : config.naming;
        if (naming == null) {
            naming = LivingLegendsConfig.defaults().naming;
        }

        String defaultStyle = BuiltInNameData.builtInStyleId(naming.defaultStyle);
        List<String> enabledStyles = naming.normalizedEnabledStyles();
        if (enabledStyles.isEmpty()) {
            enabledStyles = List.of(defaultStyle);
        }

        return switch (NameStyleSelectionMode.fromId(naming.styleSelectionMode)) {
            case DEFAULT_ONLY -> defaultStyle;
            case RANDOM_ENABLED -> randomEnabled(cluster, enabledStyles, seed);
            case WEIGHTED -> weighted(cluster, enabledStyles, naming.styleWeights, defaultStyle, seed);
            case PER_PLACE_TYPE -> perPlaceType(cluster, naming, defaultStyle, enabledStyles);
        };
    }

    private static String randomEnabled(PlaceCluster cluster, List<String> enabledStyles, long seed) {
        if (enabledStyles.size() <= 1) {
            return enabledStyles.get(0);
        }
        Random random = new Random(mixedSeed(cluster, seed, 0x5EED_57A1L));
        return enabledStyles.get(random.nextInt(enabledStyles.size()));
    }

    private static String weighted(
            PlaceCluster cluster,
            List<String> enabledStyles,
            Map<String, Integer> styleWeights,
            String defaultStyle,
            long seed
    ) {
        int totalWeight = 0;
        for (String style : enabledStyles) {
            totalWeight += Math.max(0, styleWeights == null ? 0 : styleWeights.getOrDefault(style, 0));
        }
        if (totalWeight <= 0) {
            return defaultStyle;
        }

        Random random = new Random(mixedSeed(cluster, seed, 0x51A7_E123L));
        int selected = random.nextInt(totalWeight);
        int cursor = 0;
        for (String style : enabledStyles) {
            cursor += Math.max(0, styleWeights.getOrDefault(style, 0));
            if (selected < cursor) {
                return style;
            }
        }
        return defaultStyle;
    }

    private static String perPlaceType(
            PlaceCluster cluster,
            LivingLegendsConfig.Naming naming,
            String defaultStyle,
            List<String> enabledStyles
    ) {
        if (!naming.allowPerPlaceTypeStyleOverrides || cluster == null || cluster.placeType() == null) {
            return defaultStyle;
        }

        String override = naming.placeTypeStyleOverrides.get(cluster.placeType().idString());
        if (override == null || override.isBlank()) {
            override = naming.placeTypeStyleOverrides.get(cluster.placeType().name());
        }
        if (override == null || override.isBlank()) {
            return defaultStyle;
        }

        String style = BuiltInNameData.builtInStyleId(override);
        return enabledStyles.contains(style) ? style : defaultStyle;
    }

    private static long mixedSeed(PlaceCluster cluster, long seed, long salt) {
        int clusterHash = cluster == null ? 0 : Objects.hash(
                cluster.dimensionId(),
                cluster.centerX(),
                cluster.centerY(),
                cluster.centerZ(),
                cluster.placeType()
        );
        return seed ^ salt ^ clusterHash;
    }
}
