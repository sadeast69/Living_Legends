package com.worldremembers.livinglegends;

import java.io.Serializable;

public record BiomeMetadata(
        String biomeId,
        String dominantBiomeId,
        String biomeGroup,
        String biomeTheme,
        String biomeSource
) implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String UNKNOWN = "unknown";

    public BiomeMetadata {
        biomeId = normalize(biomeId);
        dominantBiomeId = normalize(dominantBiomeId);
        biomeGroup = normalize(biomeGroup);
        biomeTheme = normalize(biomeTheme);
        biomeSource = normalize(biomeSource);
        if (biomeId.isBlank()) {
            biomeId = UNKNOWN;
        }
        if (dominantBiomeId.isBlank()) {
            dominantBiomeId = biomeId;
        }
        if (biomeGroup.isBlank()) {
            biomeGroup = UNKNOWN;
        }
        if (biomeTheme.isBlank()) {
            biomeTheme = biomeGroup;
        }
        if (biomeSource.isBlank()) {
            biomeSource = "fallback";
        }
    }

    public static BiomeMetadata unknown() {
        return new BiomeMetadata(UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, "fallback");
    }

    public static BiomeMetadata fromDimensionFallback(String dimensionId) {
        String dimension = WorldPos.optionalId(dimensionId);
        if ("minecraft:the_nether".equals(dimension)) {
            return new BiomeMetadata(UNKNOWN, UNKNOWN, "nether", "nether", "dimension_fallback");
        }
        if ("minecraft:the_end".equals(dimension)) {
            return new BiomeMetadata(UNKNOWN, UNKNOWN, "end", "end", "dimension_fallback");
        }
        return unknown();
    }

    public boolean known() {
        return !UNKNOWN.equals(biomeGroup) || !UNKNOWN.equals(biomeId);
    }

    private static String normalize(String value) {
        return WorldPos.optionalId(value);
    }
}
