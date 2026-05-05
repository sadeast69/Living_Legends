package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.PlaceType;
import com.worldremembers.livinglegends.map.MapDestinationDescriptor;
import com.worldremembers.livinglegends.map.MapIntegrationSettings;
import com.worldremembers.livinglegends.map.MapPlaceDescriptor;
import com.worldremembers.livinglegends.map.NoopPlaceMapIntegration;
import com.worldremembers.livinglegends.map.PlaceMapIntegration;
import com.worldremembers.livinglegends.map.PlaceMapIntegration.DestinationUpdateResult;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FabricMapIntegrationClient {
    private static final String JOURNEY_MAP_PROVIDER_ID = "journeymap";
    private static final String XAERO_PROVIDER_ID = "xaero";
    private static final String FTB_CHUNKS_PROVIDER_ID = "ftbchunks";
    private static final int DESTINATION_CHECK_INTERVAL_TICKS = 10;
    private static final Map<String, MapPlaceDescriptor> PLACE_LABELS = new LinkedHashMap<>();
    private static final Map<String, MapDestinationDescriptor> DESTINATIONS = new LinkedHashMap<>();
    private static final Map<String, PlaceMapIntegration> INTEGRATIONS = new LinkedHashMap<>();
    private static PlaceMapIntegration integration = NoopPlaceMapIntegration.INSTANCE;
    private static MapIntegrationSettings settings = MapIntegrationSettings.disabled();
    private static final boolean JOURNEY_MAP_INSTALLED = detectJourneyMap();
    private static final boolean XAERO_MINIMAP_INSTALLED = detectXaeroMinimap();
    private static final boolean FTB_CHUNKS_INSTALLED = detectFtbChunks();
    private static Logger logger;
    private static boolean builtInRegistrationAttempted;
    private static int destinationTickCounter;

    private FabricMapIntegrationClient() {
    }

    static void registerBuiltInIntegrations(Logger logger) {
        FabricMapIntegrationClient.logger = logger;
        refreshBuiltInIntegrations();
    }

    private static synchronized void refreshBuiltInIntegrations() {
        builtInRegistrationAttempted = true;
        if (XAERO_MINIMAP_INSTALLED && !INTEGRATIONS.containsKey(XAERO_PROVIDER_ID)) {
            try {
                installIntegration(new XaeroFabricIntegration(logger));
            } catch (RuntimeException | LinkageError exception) {
                if (logger != null) {
                    logger.warn("Could not initialize Xaero map integration: " + exception.getMessage());
                }
            }
        }
        if (FTB_CHUNKS_INSTALLED && !INTEGRATIONS.containsKey(FTB_CHUNKS_PROVIDER_ID)) {
            try {
                installIntegration(new FtbChunksFabricIntegration(logger));
            } catch (RuntimeException | LinkageError exception) {
                if (logger != null) {
                    logger.warn("Could not initialize FTB Chunks map integration: " + exception.getMessage());
                }
            }
        }
    }

    static synchronized void installIntegration(PlaceMapIntegration newIntegration) {
        if (newIntegration == null) {
            return;
        }
        String providerId = newIntegration.providerId() == null ? "" : newIntegration.providerId().trim();
        if (providerId.isBlank() || NoopPlaceMapIntegration.INSTANCE.providerId().equals(providerId)) {
            return;
        }
        INTEGRATIONS.put(providerId, newIntegration);
        integration = compositeIntegration();
        pushLabels();
        pushDestinations();
        refreshCurrentJournal();
    }

    static void handle(MapIntegrationS2CPayload payload) {
        if (payload == null) {
            return;
        }
        settings = payload.settings() == null ? MapIntegrationSettings.disabled() : payload.settings();
        refreshBuiltInIntegrations();
        PLACE_LABELS.clear();
        for (MapPlaceDescriptor place : payload.places()) {
            MapPlaceDescriptor resolved = resolveClientText(place);
            if (!resolved.placeId().isBlank()) {
                PLACE_LABELS.put(resolved.placeId(), resolved);
            }
        }
        if (!destinationsAvailable()) {
            clearDestinations();
        }
        pushLabels();
        refreshCurrentJournal();
    }

    static void handle(MapDestinationS2CPayload payload) {
        if (payload == null) {
            return;
        }
        if (payload.action() == MapDestinationS2CPayload.Action.REMOVE) {
            removeDestination(payload.placeId());
        } else {
            refreshDestination(payload.place());
        }
        refreshCurrentJournal();
    }

    private static void refreshCurrentJournal() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof WorldJournalScreen journal) {
            journal.refreshMapIntegrationControls();
        }
    }

    static void tick(MinecraftClient client) {
        if (client == null || DESTINATIONS.isEmpty()) {
            return;
        }
        destinationTickCounter++;
        if (destinationTickCounter % DESTINATION_CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        pruneRemovedDestinations();
        if (client.player == null || client.world == null || DESTINATIONS.isEmpty()) {
            return;
        }
        String dimension = client.world.getRegistryKey().getValue().toString();
        BlockPos pos = client.player.getBlockPos();
        List<String> reached = new ArrayList<>();
        for (MapDestinationDescriptor destination : DESTINATIONS.values()) {
            if (destination == null || !dimension.equals(destination.dimensionId())) {
                continue;
            }
            int clearDistance = Math.max(1, settings.destinations().fallbackClearDistanceBlocks());
            if (settings.destinations().clearWhenEnteringPlaceRadius()) {
                clearDistance = Math.max(clearDistance, destination.radius());
            }
            long dx = (long) pos.getX() - destination.centerX();
            long dz = (long) pos.getZ() - destination.centerZ();
            if (dx * dx + dz * dz <= (long) clearDistance * clearDistance) {
                reached.add(destination.placeId());
            }
        }
        for (String placeId : reached) {
            removeDestination(placeId);
        }
    }

    static boolean destinationButtonVisible() {
        return destinationUiAvailable();
    }

    static boolean destinationsAvailable() {
        return destinationUiAvailable();
    }

    static boolean destinationUiAvailable() {
        refreshBuiltInIntegrations();
        return settings.destinationsEnabled() && hasUiDestinationProvider();
    }

    static boolean destinationRuntimeReady() {
        refreshBuiltInIntegrations();
        return settings.destinationsEnabled() && hasRuntimeReadyDestinationProvider();
    }

    static void refreshDestinationUiState() {
        refreshBuiltInIntegrations();
    }

    static boolean isDestinationActive(String placeId) {
        pruneRemovedDestinations();
        return DESTINATIONS.containsKey(placeId == null ? "" : placeId);
    }

    static void toggleDestination(MapPlaceDescriptor place) {
        MapPlaceDescriptor resolved = resolveClientText(place);
        if (resolved.placeId().isBlank() || !destinationsAvailable()) {
            return;
        }
        if (isDestinationActive(resolved.placeId())) {
            removeDestination(resolved.placeId());
            return;
        }
        addDestination(MapDestinationDescriptor.fromPlace(resolved));
    }

    static void refreshDestination(MapPlaceDescriptor place) {
        MapPlaceDescriptor resolved = resolveClientText(place);
        if (resolved.placeId().isBlank() || !DESTINATIONS.containsKey(resolved.placeId())) {
            return;
        }
        if (!destinationsAvailable()) {
            clearDestinations();
            return;
        }
        MapDestinationDescriptor destination = MapDestinationDescriptor.fromPlace(resolved);
        DestinationUpdateResult result = integration.addOrUpdateDestinationResult(destination, settings);
        if (result.success()) {
            DESTINATIONS.put(destination.placeId(), destination);
        } else {
            DESTINATIONS.remove(destination.placeId());
        }
    }

    private static void addDestination(MapDestinationDescriptor destination) {
        if (destination == null || destination.placeId().isBlank() || !destinationsAvailable()) {
            return;
        }
        if (settings.destinations().onlyOneActiveDestination()) {
            clearDestinations();
        }
        DestinationUpdateResult result = integration.addOrUpdateDestinationResult(destination, settings);
        if (result.success()) {
            DESTINATIONS.put(destination.placeId(), destination);
        } else {
            DESTINATIONS.remove(destination.placeId());
            notifyDestinationFailure(result);
        }
    }

    private static void removeDestination(String placeId) {
        String id = placeId == null ? "" : placeId;
        if (id.isBlank()) {
            return;
        }
        if (integration.removeDestinationResult(id)) {
            DESTINATIONS.remove(id);
        }
    }

    private static void clearDestinations() {
        DESTINATIONS.clear();
        integration.clearDestinations();
    }

    private static void pushLabels() {
        if (!settings.placeLabelsEnabled() || !integration.available()) {
            integration.clearPlaceLabels();
            return;
        }
        integration.replacePlaceLabels(PLACE_LABELS.values(), settings);
    }

    private static void pushDestinations() {
        if (!destinationsAvailable()) {
            return;
        }
        for (MapDestinationDescriptor destination : DESTINATIONS.values()) {
            integration.addOrUpdateDestinationResult(destination, settings);
        }
    }

    private static void notifyDestinationFailure(DestinationUpdateResult result) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(Text.translatable("living_legends.journal.error.destination_failed"));
        }
    }

    private static void pruneRemovedDestinations() {
        if (DESTINATIONS.isEmpty() || !integration.available()) {
            return;
        }
        Iterator<Map.Entry<String, MapDestinationDescriptor>> iterator = DESTINATIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, MapDestinationDescriptor> entry = iterator.next();
            if (!integration.hasDestination(entry.getKey())) {
                iterator.remove();
            }
        }
    }

    private static MapPlaceDescriptor resolveClientText(MapPlaceDescriptor place) {
        if (place == null) {
            return new MapPlaceDescriptor("", "", PlaceType.UNKNOWN, "", 0, 64, 0, 0, false, "", null, "", "");
        }
        String displayName = place.manualName()
                ? place.manualNameText()
                : NameResolver.resolve(place.nameRecipe()).getString();
        if (displayName == null
                || displayName.isBlank()
                || displayName.startsWith("living_legends.name.")) {
            displayName = place.serverResolvedFallbackName();
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = place.placeId();
        }
        return place.withClientText(displayName, buildTooltip(place, displayName));
    }

    private static String buildTooltip(MapPlaceDescriptor place, String displayName) {
        if (!settings.placeLabels().showTooltips()) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        lines.add(displayName);
        String placeType = Text.translatable(placeTypeKey(place.placeType())).getString();
        if (!placeType.isBlank()) {
            lines.add(placeType);
        }
        String summary = journalFlavor(place);
        if (!summary.isBlank()) {
            lines.add(summary);
        }
        return String.join("\n", lines);
    }

    private static String journalFlavor(MapPlaceDescriptor place) {
        String type = journalFlavorType(place == null ? "" : place.placeType().idString());
        String key = "living_legends.journal.flavor." + type;
        String variants = Text.translatable(key).getString();
        if (variants == null || variants.isBlank() || variants.equals(key)) {
            variants = Text.translatable("living_legends.journal.flavor.remembered").getString();
        }
        return selectFlavorVariant(variants, place == null ? 0 : place.placeId().hashCode());
    }

    private static String journalFlavorType(String type) {
        return switch (type == null ? "" : type.toLowerCase(java.util.Locale.ROOT)) {
            case "death_site",
                 "battlefield",
                 "slaughter_field",
                 "pvp_arena",
                 "mining_site",
                 "portal_landmark",
                 "general_landmark",
                 "settlement",
                 "first_discovery",
                 "boss_site",
                 "pet_memorial",
                 "named_mob_memorial",
                 "raid_site",
                 "dimension_threshold" -> type.toLowerCase(java.util.Locale.ROOT);
            default -> "custom";
        };
    }

    private static String selectFlavorVariant(String variants, int seed) {
        if (variants == null || variants.isBlank()) {
            return "";
        }
        int count = 1;
        for (int index = 0; index < variants.length(); index++) {
            if (variants.charAt(index) == '|') {
                count++;
            }
        }
        int target = Math.floorMod(seed, count);
        int start = 0;
        int current = 0;
        for (int index = 0; index <= variants.length(); index++) {
            if (index == variants.length() || variants.charAt(index) == '|') {
                if (current == target) {
                    String value = variants.substring(start, index).trim();
                    return value.isBlank() ? "" : value;
                }
                current++;
                start = index + 1;
            }
        }
        return "";
    }

    private static String placeTypeKey(PlaceType type) {
        PlaceType resolved = type == null ? PlaceType.CUSTOM : type;
        return "living_legends.place_type." + resolved.idString();
    }

    private static boolean detectJourneyMap() {
        try {
            return FabricLoader.getInstance().isModLoaded("journeymap");
        } catch (RuntimeException | LinkageError exception) {
            return false;
        }
    }

    private static boolean detectXaeroMinimap() {
        try {
            return FabricLoader.getInstance().isModLoaded("xaerominimap");
        } catch (RuntimeException | LinkageError exception) {
            return false;
        }
    }

    private static boolean detectFtbChunks() {
        try {
            return FabricLoader.getInstance().isModLoaded("ftbchunks");
        } catch (RuntimeException | LinkageError exception) {
            return false;
        }
    }

    private static boolean hasUiDestinationProvider() {
        return destinationProviderUiAvailable(JOURNEY_MAP_PROVIDER_ID, JOURNEY_MAP_INSTALLED)
                || destinationProviderUiAvailable(XAERO_PROVIDER_ID, XAERO_MINIMAP_INSTALLED)
                || destinationProviderUiAvailable(FTB_CHUNKS_PROVIDER_ID, FTB_CHUNKS_INSTALLED);
    }

    private static boolean hasRuntimeReadyDestinationProvider() {
        return destinationProviderRuntimeReady(JOURNEY_MAP_PROVIDER_ID, JOURNEY_MAP_INSTALLED)
                || destinationProviderRuntimeReady(XAERO_PROVIDER_ID, XAERO_MINIMAP_INSTALLED)
                || destinationProviderRuntimeReady(FTB_CHUNKS_PROVIDER_ID, FTB_CHUNKS_INSTALLED);
    }

    private static boolean destinationProviderUiAvailable(String providerId, boolean modInstalled) {
        if (!modInstalled || !settings.destinationsEnabledFor(providerId)) {
            return false;
        }
        PlaceMapIntegration provider = INTEGRATIONS.get(providerId);
        return provider != null && provider.supportsDestinations() && provider.available();
    }

    private static boolean destinationProviderRuntimeReady(String providerId, boolean modInstalled) {
        if (!destinationProviderUiAvailable(providerId, modInstalled)) {
            return false;
        }
        PlaceMapIntegration provider = INTEGRATIONS.get(providerId);
        return provider != null && provider.destinationRuntimeReady();
    }

    private static PlaceMapIntegration compositeIntegration() {
        if (INTEGRATIONS.isEmpty()) {
            return NoopPlaceMapIntegration.INSTANCE;
        }
        if (INTEGRATIONS.size() == 1) {
            return INTEGRATIONS.values().iterator().next();
        }
        return new FabricCompositePlaceMapIntegration(INTEGRATIONS.values());
    }
}
