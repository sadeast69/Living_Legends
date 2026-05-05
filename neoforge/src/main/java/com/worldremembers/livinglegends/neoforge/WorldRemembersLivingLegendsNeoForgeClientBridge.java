package com.worldremembers.livinglegends.neoforge;

import com.worldremembers.livinglegends.neoforge.network.MapIntegrationS2CPayload;
import com.worldremembers.livinglegends.neoforge.network.MapDestinationS2CPayload;
import com.worldremembers.livinglegends.neoforge.network.PlaceTitleS2CPayload;
import com.worldremembers.livinglegends.neoforge.network.WorldJournalS2CPayload;

import java.util.function.Consumer;

public final class WorldRemembersLivingLegendsNeoForgeClientBridge {
    private static Consumer<PlaceTitleS2CPayload> placeTitleHandler = payload -> {
    };
    private static Consumer<WorldJournalS2CPayload> worldJournalHandler = payload -> {
    };
    private static Consumer<MapIntegrationS2CPayload> mapIntegrationHandler = payload -> {
    };
    private static Consumer<MapDestinationS2CPayload> mapDestinationHandler = payload -> {
    };

    private WorldRemembersLivingLegendsNeoForgeClientBridge() {
    }

    public static void setPlaceTitleHandler(Consumer<PlaceTitleS2CPayload> handler) {
        placeTitleHandler = handler == null ? payload -> {
        } : handler;
    }

    public static void setWorldJournalHandler(Consumer<WorldJournalS2CPayload> handler) {
        worldJournalHandler = handler == null ? payload -> {
        } : handler;
    }

    public static void setMapIntegrationHandler(Consumer<MapIntegrationS2CPayload> handler) {
        mapIntegrationHandler = handler == null ? payload -> {
        } : handler;
    }

    public static void setMapDestinationHandler(Consumer<MapDestinationS2CPayload> handler) {
        mapDestinationHandler = handler == null ? payload -> {
        } : handler;
    }

    public static void handlePlaceTitle(PlaceTitleS2CPayload payload) {
        placeTitleHandler.accept(payload);
    }

    public static void handleWorldJournal(WorldJournalS2CPayload payload) {
        worldJournalHandler.accept(payload);
    }

    public static void handleMapIntegration(MapIntegrationS2CPayload payload) {
        mapIntegrationHandler.accept(payload);
    }

    public static void handleMapDestination(MapDestinationS2CPayload payload) {
        mapDestinationHandler.accept(payload);
    }
}
