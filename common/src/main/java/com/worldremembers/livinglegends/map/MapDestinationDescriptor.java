package com.worldremembers.livinglegends.map;

import com.worldremembers.livinglegends.PlaceType;
import com.worldremembers.livinglegends.WorldPos;

import java.io.Serializable;

public record MapDestinationDescriptor(
        String destinationId,
        String placeId,
        String displayName,
        PlaceType placeType,
        String dimensionId,
        int centerX,
        int centerY,
        int centerZ,
        int radius,
        String tooltip
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public MapDestinationDescriptor {
        destinationId = clean(destinationId);
        placeId = clean(placeId);
        displayName = displayName == null ? "" : displayName.trim();
        placeType = placeType == null ? PlaceType.UNKNOWN : placeType;
        dimensionId = clean(dimensionId);
        radius = Math.max(0, radius);
        tooltip = tooltip == null ? "" : tooltip.trim();
    }

    public static MapDestinationDescriptor fromPlace(MapPlaceDescriptor place) {
        return new MapDestinationDescriptor(
                "living_legends:destination/" + (place == null ? "" : place.placeId()),
                place == null ? "" : place.placeId(),
                place == null ? "" : place.displayName(),
                place == null ? PlaceType.UNKNOWN : place.placeType(),
                place == null ? "" : place.dimensionId(),
                place == null ? 0 : place.centerX(),
                place == null ? 64 : place.centerY(),
                place == null ? 0 : place.centerZ(),
                place == null ? 0 : place.radius(),
                place == null ? "" : place.tooltip()
        );
    }

    public WorldPos center() {
        return new WorldPos(dimensionId.isBlank() ? "minecraft:overworld" : dimensionId, centerX, centerY, centerZ);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
