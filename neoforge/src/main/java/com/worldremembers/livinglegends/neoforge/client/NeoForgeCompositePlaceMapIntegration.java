package com.worldremembers.livinglegends.neoforge.client;

import com.worldremembers.livinglegends.map.MapDestinationDescriptor;
import com.worldremembers.livinglegends.map.MapIntegrationSettings;
import com.worldremembers.livinglegends.map.MapPlaceDescriptor;
import com.worldremembers.livinglegends.map.PlaceMapIntegration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class NeoForgeCompositePlaceMapIntegration implements PlaceMapIntegration {
    private final List<PlaceMapIntegration> integrations;

    NeoForgeCompositePlaceMapIntegration(Collection<PlaceMapIntegration> integrations) {
        List<PlaceMapIntegration> resolved = new ArrayList<>();
        if (integrations != null) {
            for (PlaceMapIntegration integration : integrations) {
                if (integration != null) {
                    resolved.add(integration);
                }
            }
        }
        this.integrations = List.copyOf(resolved);
    }

    @Override
    public String providerId() {
        StringBuilder builder = new StringBuilder();
        for (PlaceMapIntegration integration : integrations) {
            if (builder.length() > 0) {
                builder.append('+');
            }
            builder.append(integration.providerId());
        }
        return builder.toString();
    }

    @Override
    public boolean available() {
        for (PlaceMapIntegration integration : integrations) {
            if (integration.available()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean supportsPlaceLabels() {
        for (PlaceMapIntegration integration : integrations) {
            if (integration.supportsPlaceLabels()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean supportsDestinations() {
        for (PlaceMapIntegration integration : integrations) {
            if (integration.supportsDestinations()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean destinationRuntimeReady() {
        for (PlaceMapIntegration integration : integrations) {
            if (integration.supportsDestinations() && integration.destinationRuntimeReady()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void replacePlaceLabels(Collection<MapPlaceDescriptor> places, MapIntegrationSettings settings) {
        for (PlaceMapIntegration integration : integrations) {
            if (integration.supportsPlaceLabels()) {
                integration.replacePlaceLabels(places, settings);
            }
        }
    }

    @Override
    public void clearPlaceLabels() {
        for (PlaceMapIntegration integration : integrations) {
            if (integration.supportsPlaceLabels()) {
                integration.clearPlaceLabels();
            }
        }
    }

    @Override
    public void addOrUpdateDestination(MapDestinationDescriptor destination, MapIntegrationSettings settings) {
        for (PlaceMapIntegration integration : integrations) {
            if (integration.supportsDestinations()) {
                integration.addOrUpdateDestination(destination, settings);
            }
        }
    }

    @Override
    public DestinationUpdateResult addOrUpdateDestinationResult(
            MapDestinationDescriptor destination,
            MapIntegrationSettings settings
    ) {
        DestinationUpdateResult fallback = DestinationUpdateResult.PROVIDER_UNAVAILABLE;
        boolean attempted = false;
        boolean anySuccess = false;
        for (PlaceMapIntegration integration : integrations) {
            if (integration.supportsDestinations()) {
                attempted = true;
                DestinationUpdateResult result = integration.addOrUpdateDestinationResult(destination, settings);
                if (result.success()) {
                    anySuccess = true;
                } else {
                    fallback = result;
                }
            }
        }
        if (anySuccess) {
            return DestinationUpdateResult.SUCCESS;
        }
        return attempted ? fallback : DestinationUpdateResult.PROVIDER_UNAVAILABLE;
    }

    @Override
    public void removeDestination(String placeId) {
        for (PlaceMapIntegration integration : integrations) {
            if (integration.supportsDestinations()) {
                integration.removeDestination(placeId);
            }
        }
    }

    @Override
    public boolean removeDestinationResult(String placeId) {
        boolean removed = true;
        for (PlaceMapIntegration integration : integrations) {
            if (integration.supportsDestinations()) {
                removed &= integration.removeDestinationResult(placeId);
            }
        }
        return removed;
    }

    @Override
    public void clearDestinations() {
        for (PlaceMapIntegration integration : integrations) {
            if (integration.supportsDestinations()) {
                integration.clearDestinations();
            }
        }
    }

    @Override
    public boolean hasDestination(String placeId) {
        for (PlaceMapIntegration integration : integrations) {
            if (integration.supportsDestinations() && integration.hasDestination(placeId)) {
                return true;
            }
        }
        return false;
    }
}
