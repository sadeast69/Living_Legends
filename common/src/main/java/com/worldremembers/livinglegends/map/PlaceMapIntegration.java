package com.worldremembers.livinglegends.map;

import java.util.Collection;

public interface PlaceMapIntegration {
    enum DestinationUpdateResult {
        SUCCESS,
        PROVIDER_UNAVAILABLE,
        DESTINATIONS_DISABLED,
        INVALID_DESTINATION,
        PROVIDER_NOT_READY,
        WAYPOINT_SET_UNAVAILABLE,
        WAYPOINT_CREATE_FAILED;

        public boolean success() {
            return this == SUCCESS;
        }
    }

    String providerId();

    boolean available();

    default boolean supportsPlaceLabels() {
        return true;
    }

    default boolean supportsDestinations() {
        return true;
    }

    default boolean destinationRuntimeReady() {
        return supportsDestinations() && available();
    }

    void replacePlaceLabels(Collection<MapPlaceDescriptor> places, MapIntegrationSettings settings);

    void clearPlaceLabels();

    void addOrUpdateDestination(MapDestinationDescriptor destination, MapIntegrationSettings settings);

    default DestinationUpdateResult addOrUpdateDestinationResult(
            MapDestinationDescriptor destination,
            MapIntegrationSettings settings
    ) {
        addOrUpdateDestination(destination, settings);
        if (destination == null || destination.placeId().isBlank()) {
            return DestinationUpdateResult.INVALID_DESTINATION;
        }
        return hasDestination(destination.placeId())
                ? DestinationUpdateResult.SUCCESS
                : DestinationUpdateResult.WAYPOINT_CREATE_FAILED;
    }

    void removeDestination(String placeId);

    default boolean removeDestinationResult(String placeId) {
        removeDestination(placeId);
        return placeId == null || placeId.isBlank() || !hasDestination(placeId);
    }

    void clearDestinations();

    boolean hasDestination(String placeId);
}
