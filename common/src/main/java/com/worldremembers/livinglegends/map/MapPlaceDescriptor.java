package com.worldremembers.livinglegends.map;

import com.worldremembers.livinglegends.NameRecipe;
import com.worldremembers.livinglegends.PlaceType;
import com.worldremembers.livinglegends.WorldPos;
import com.worldremembers.livinglegends.visual.PlaceVisualTheme;
import com.worldremembers.livinglegends.visual.PlaceVisualThemeResolver;

import java.io.Serializable;

public record MapPlaceDescriptor(
        String placeId,
        String displayName,
        PlaceType placeType,
        String dimensionId,
        int centerX,
        int centerY,
        int centerZ,
        int radius,
        boolean manualName,
        String manualNameText,
        NameRecipe nameRecipe,
        String serverResolvedFallbackName,
        String tooltip,
        PlaceVisualTheme visualTheme
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public MapPlaceDescriptor {
        placeId = clean(placeId);
        displayName = clean(displayName);
        placeType = placeType == null ? PlaceType.UNKNOWN : placeType;
        dimensionId = clean(dimensionId);
        radius = Math.max(0, radius);
        manualNameText = clean(manualNameText);
        nameRecipe = nameRecipe == null ? NameRecipe.empty() : nameRecipe;
        serverResolvedFallbackName = clean(serverResolvedFallbackName);
        tooltip = cleanMultiline(tooltip);
        visualTheme = visualTheme == null
                ? PlaceVisualThemeResolver.resolve(placeType, dimensionId, "", "")
                : visualTheme;
    }

    public MapPlaceDescriptor(
            String placeId,
            String displayName,
            PlaceType placeType,
            String dimensionId,
            int centerX,
            int centerY,
            int centerZ,
            int radius,
            boolean manualName,
            String manualNameText,
            NameRecipe nameRecipe,
            String serverResolvedFallbackName,
            String tooltip
    ) {
        this(
                placeId,
                displayName,
                placeType,
                dimensionId,
                centerX,
                centerY,
                centerZ,
                radius,
                manualName,
                manualNameText,
                nameRecipe,
                serverResolvedFallbackName,
                tooltip,
                PlaceVisualThemeResolver.resolve(placeType, dimensionId, "", "")
        );
    }

    public WorldPos center() {
        return new WorldPos(dimensionId.isBlank() ? "minecraft:overworld" : dimensionId, centerX, centerY, centerZ);
    }

    public MapPlaceDescriptor withClientText(String resolvedDisplayName, String resolvedTooltip) {
        return new MapPlaceDescriptor(
                placeId,
                resolvedDisplayName,
                placeType,
                dimensionId,
                centerX,
                centerY,
                centerZ,
                radius,
                manualName,
                manualNameText,
                nameRecipe,
                serverResolvedFallbackName,
                resolvedTooltip,
                visualTheme
        );
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String cleanMultiline(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (String line : value.split("\\R")) {
            String cleaned = clean(line);
            if (cleaned.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append('\n');
            }
            result.append(cleaned);
        }
        return result.toString();
    }
}
