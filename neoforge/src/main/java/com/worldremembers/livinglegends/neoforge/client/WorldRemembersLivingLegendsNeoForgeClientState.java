package com.worldremembers.livinglegends.neoforge.client;

import com.worldremembers.livinglegends.NameRecipe;
import com.worldremembers.livinglegends.neoforge.network.PlaceTitleS2CPayload;
import com.worldremembers.livinglegends.neoforge.network.WorldJournalS2CPayload;
import net.minecraft.network.chat.Component;

public final class WorldRemembersLivingLegendsNeoForgeClientState {
    private static volatile PlaceTitleS2CPayload lastPlaceTitlePayload;
    private static volatile WorldJournalS2CPayload lastWorldJournalPayload;

    private WorldRemembersLivingLegendsNeoForgeClientState() {
    }

    static void handlePlaceTitle(PlaceTitleS2CPayload payload) {
        lastPlaceTitlePayload = payload;
        if (payload == null || payload.reason() == PlaceTitleS2CPayload.Reason.CLEAR) {
            PlaceTitleOverlayRenderer.clear();
            return;
        }
        PlaceTitleOverlayRenderer.show(payload);
    }

    static void handleWorldJournal(WorldJournalS2CPayload payload) {
        lastWorldJournalPayload = payload;
        WorldJournalClient.handle(payload);
    }

    static Component titleFor(PlaceTitleS2CPayload payload) {
        if (payload == null) {
            return Component.empty();
        }
        if (payload.manualName()) {
            String manual = payload.manualNameText();
            return Component.literal(manual == null ? "" : manual);
        }

        NameRecipe recipe = payload.nameRecipe();
        Component resolved = WorldRemembersLivingLegendsNeoForgeClientNameResolver.resolve(recipe);
        if (resolved != null && !resolved.getString().isBlank()) {
            return resolved;
        }
        if (payload.serverResolvedFallbackName() != null && !payload.serverResolvedFallbackName().isBlank()) {
            return Component.literal(payload.serverResolvedFallbackName());
        }
        return Component.literal(payload.placeId() == null ? "" : payload.placeId());
    }

    static Component subtitleFor(PlaceTitleS2CPayload payload) {
        if (payload == null || payload.placeType() == null || payload.placeType().isBlank()) {
            return Component.empty();
        }
        return Component.translatable("living_legends.place_type." + payload.placeType());
    }

    public static PlaceTitleS2CPayload lastPlaceTitlePayload() {
        return lastPlaceTitlePayload;
    }

    public static WorldJournalS2CPayload lastWorldJournalPayload() {
        return lastWorldJournalPayload;
    }
}
