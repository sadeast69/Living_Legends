package com.worldremembers.livinglegends.map;

import java.io.Serializable;

public record MapDestinationSettings(
        boolean enabled,
        boolean onlyOneActiveDestination,
        boolean clearWhenEnteringPlaceRadius,
        int fallbackClearDistanceBlocks
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public MapDestinationSettings {
        fallbackClearDistanceBlocks = Math.max(1, fallbackClearDistanceBlocks);
    }

    public static MapDestinationSettings disabled() {
        return new MapDestinationSettings(false, true, true, 16);
    }
}
