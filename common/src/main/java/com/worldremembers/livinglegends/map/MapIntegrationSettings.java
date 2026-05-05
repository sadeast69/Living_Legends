package com.worldremembers.livinglegends.map;

import com.worldremembers.livinglegends.config.LivingLegendsConfig;

import java.io.Serializable;

public record MapIntegrationSettings(
        boolean enabled,
        boolean journeyMapEnabled,
        boolean xaeroMapEnabled,
        boolean ftbChunksMapEnabled,
        MapLabelSettings placeLabels,
        MapDestinationSettings destinations
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public MapIntegrationSettings {
        placeLabels = placeLabels == null ? MapLabelSettings.disabled() : placeLabels;
        destinations = destinations == null ? MapDestinationSettings.disabled() : destinations;
    }

    public MapIntegrationSettings(
            boolean enabled,
            boolean journeyMapEnabled,
            MapLabelSettings placeLabels,
            MapDestinationSettings destinations
    ) {
        this(enabled, journeyMapEnabled, false, false, placeLabels, destinations);
    }

    public MapIntegrationSettings(
            boolean enabled,
            boolean journeyMapEnabled,
            boolean xaeroMapEnabled,
            MapLabelSettings placeLabels,
            MapDestinationSettings destinations
    ) {
        this(enabled, journeyMapEnabled, xaeroMapEnabled, false, placeLabels, destinations);
    }

    public static MapIntegrationSettings disabled() {
        return new MapIntegrationSettings(
                false,
                false,
                false,
                false,
                MapLabelSettings.disabled(),
                MapDestinationSettings.disabled()
        );
    }

    public static MapIntegrationSettings fromConfig(LivingLegendsConfig config) {
        if (config == null || config.mapIntegration == null) {
            return disabled();
        }
        LivingLegendsConfig.MapIntegration map = config.mapIntegration;
        LivingLegendsConfig.MapIntegration.PlaceLabels labels = map.placeLabels == null
                ? new LivingLegendsConfig.MapIntegration.PlaceLabels()
                : map.placeLabels;
        LivingLegendsConfig.MapIntegration.Destinations destinations = map.destinations == null
                ? new LivingLegendsConfig.MapIntegration.Destinations()
                : map.destinations;
        return new MapIntegrationSettings(
                map.enabled,
                map.journeyMap != null && map.journeyMap.enabled,
                map.xaero != null && map.xaero.enabled,
                map.ftbChunks != null && map.ftbChunks.enabled,
                new MapLabelSettings(
                        labels.enabled,
                        labels.showGeneralLandmarks,
                        labels.showTooltips,
                        labels.showCoordinatesInTooltip,
                        labels.showDimensionInTooltip,
                        labels.showPlaceTypeInTooltip
                ),
                new MapDestinationSettings(
                        destinations.enabled,
                        destinations.onlyOneActiveDestination,
                        destinations.clearWhenEnteringPlaceRadius,
                        destinations.fallbackClearDistanceBlocks
                )
        );
    }

    public boolean placeLabelsEnabled() {
        return enabled && journeyMapEnabled && placeLabels.enabled();
    }

    public boolean destinationsEnabled() {
        return enabled && anyDestinationProviderEnabled() && destinations.enabled();
    }

    public boolean placeLabelsEnabledFor(String providerId) {
        String id = providerId == null ? "" : providerId.trim();
        return enabled
                && "journeymap".equals(id)
                && journeyMapEnabled
                && placeLabels.enabled();
    }

    public boolean destinationsEnabledFor(String providerId) {
        return enabled && providerEnabled(providerId) && destinations.enabled();
    }

    private boolean anyDestinationProviderEnabled() {
        return journeyMapEnabled || xaeroMapEnabled || ftbChunksMapEnabled;
    }

    private boolean providerEnabled(String providerId) {
        return switch (providerId == null ? "" : providerId.trim()) {
            case "journeymap" -> journeyMapEnabled;
            case "xaero" -> xaeroMapEnabled;
            case "ftbchunks" -> ftbChunksMapEnabled;
            default -> false;
        };
    }
}
