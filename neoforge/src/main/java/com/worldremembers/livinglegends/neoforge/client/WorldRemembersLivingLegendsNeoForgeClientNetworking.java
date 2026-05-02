package com.worldremembers.livinglegends.neoforge.client;

import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import com.worldremembers.livinglegends.neoforge.WorldRemembersLivingLegendsNeoForgeClientBridge;
import com.worldremembers.livinglegends.neoforge.network.PlaceTitleS2CPayload;
import com.worldremembers.livinglegends.neoforge.network.WorldJournalS2CPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

public final class WorldRemembersLivingLegendsNeoForgeClientNetworking {
    private static volatile boolean registered;

    private WorldRemembersLivingLegendsNeoForgeClientNetworking() {
    }

    public static void register(Logger logger) {
        if (registered) {
            return;
        }
        registered = true;
        WorldRemembersLivingLegendsNeoForgeClientConfig.load(logger);
        WorldRemembersLivingLegendsNeoForgeClientBridge.setPlaceTitleHandler(WorldRemembersLivingLegendsNeoForgeClientState::handlePlaceTitle);
        WorldRemembersLivingLegendsNeoForgeClientBridge.setWorldJournalHandler(WorldRemembersLivingLegendsNeoForgeClientState::handleWorldJournal);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> PlaceTitleOverlayRenderer.tick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) -> PlaceTitleOverlayRenderer.render(
                event.getGuiGraphics(),
                event.getPartialTick().getGameTimeDeltaPartialTick(false)
        ));
        if (logger != null) {
            logger.info(WorldRemembersLivingLegends.MOD_ID + " NeoForge client title overlay hooks registered");
        }
    }

    public static void handlePlaceTitle(PlaceTitleS2CPayload payload) {
        WorldRemembersLivingLegendsNeoForgeClientState.handlePlaceTitle(payload);
    }

    public static void handleWorldJournal(WorldJournalS2CPayload payload) {
        WorldRemembersLivingLegendsNeoForgeClientState.handleWorldJournal(payload);
    }

    public static PlaceTitleS2CPayload lastPlaceTitlePayload() {
        return WorldRemembersLivingLegendsNeoForgeClientState.lastPlaceTitlePayload();
    }

    public static WorldJournalS2CPayload lastWorldJournalPayload() {
        return WorldRemembersLivingLegendsNeoForgeClientState.lastWorldJournalPayload();
    }
}
