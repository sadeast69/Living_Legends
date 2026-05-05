package com.worldremembers.livinglegends.map;

import java.util.Collection;

public final class NoopPlaceMapIntegration implements PlaceMapIntegration {
    public static final NoopPlaceMapIntegration INSTANCE = new NoopPlaceMapIntegration();

    private NoopPlaceMapIntegration() {
    }

    @Override
    public String providerId() {
        return "none";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public boolean supportsPlaceLabels() {
        return false;
    }

    @Override
    public boolean supportsDestinations() {
        return false;
    }

    @Override
    public void replacePlaceLabels(Collection<MapPlaceDescriptor> places, MapIntegrationSettings settings) {
    }

    @Override
    public void clearPlaceLabels() {
    }

    @Override
    public void addOrUpdateDestination(MapDestinationDescriptor destination, MapIntegrationSettings settings) {
    }

    @Override
    public DestinationUpdateResult addOrUpdateDestinationResult(
            MapDestinationDescriptor destination,
            MapIntegrationSettings settings
    ) {
        return DestinationUpdateResult.PROVIDER_UNAVAILABLE;
    }

    @Override
    public void removeDestination(String placeId) {
    }

    @Override
    public boolean removeDestinationResult(String placeId) {
        return true;
    }

    @Override
    public void clearDestinations() {
    }

    @Override
    public boolean hasDestination(String placeId) {
        return false;
    }
}
