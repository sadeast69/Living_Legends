package com.worldremembers.livinglegends.neoforge;

import com.worldremembers.livinglegends.neoforge.network.PlaceTitleS2CPayload;
import com.worldremembers.livinglegends.neoforge.network.WorldJournalS2CPayload;

import java.util.function.Consumer;

public final class WorldRemembersLivingLegendsNeoForgeClientBridge {
    private static Consumer<PlaceTitleS2CPayload> placeTitleHandler = payload -> {
    };
    private static Consumer<WorldJournalS2CPayload> worldJournalHandler = payload -> {
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

    public static void handlePlaceTitle(PlaceTitleS2CPayload payload) {
        placeTitleHandler.accept(payload);
    }

    public static void handleWorldJournal(WorldJournalS2CPayload payload) {
        worldJournalHandler.accept(payload);
    }
}
