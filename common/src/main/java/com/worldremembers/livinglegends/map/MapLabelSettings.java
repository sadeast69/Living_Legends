package com.worldremembers.livinglegends.map;

import java.io.Serializable;

public record MapLabelSettings(
        boolean enabled,
        boolean showGeneralLandmarks,
        boolean showTooltips,
        boolean showCoordinatesInTooltip,
        boolean showDimensionInTooltip,
        boolean showPlaceTypeInTooltip
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public static MapLabelSettings disabled() {
        return new MapLabelSettings(false, false, false, false, false, false);
    }
}
