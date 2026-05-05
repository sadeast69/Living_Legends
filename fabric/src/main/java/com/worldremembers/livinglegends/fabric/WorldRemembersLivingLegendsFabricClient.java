package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WorldRemembersLivingLegendsFabricClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldRemembersLivingLegends.MOD_ID + "-client");

    @Override
    public void onInitializeClient() {
        PlaceTitleS2CPayload.registerType();
        WorldJournalC2SPayload.registerType();
        WorldJournalS2CPayload.registerType();
        MapIntegrationS2CPayload.registerType();
        MapDestinationS2CPayload.registerType();
        WorldRemembersLivingLegendsFabricClientConfig.load(LOGGER);
        FabricMapIntegrationClient.registerBuiltInIntegrations(LOGGER);
        ClientPlayNetworking.registerGlobalReceiver(PlaceTitleS2CPayload.ID, (payload, context) ->
                context.client().execute(() -> PlaceTitleOverlayRenderer.show(payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(WorldJournalS2CPayload.ID, (payload, context) ->
                context.client().execute(() -> WorldJournalClient.handle(payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(MapIntegrationS2CPayload.ID, (payload, context) ->
                context.client().execute(() -> FabricMapIntegrationClient.handle(payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(MapDestinationS2CPayload.ID, (payload, context) ->
                context.client().execute(() -> FabricMapIntegrationClient.handle(payload))
        );
        ClientTickEvents.END_CLIENT_TICK.register(PlaceTitleOverlayRenderer::tick);
        ClientTickEvents.END_CLIENT_TICK.register(FabricMapIntegrationClient::tick);
        HudRenderCallback.EVENT.register((drawContext, tickCounter) ->
                PlaceTitleOverlayRenderer.render(drawContext, tickCounter.getTickDelta(false))
        );
    }

    public static void openTitleOverlaySettings(Screen parent) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(new PlaceTitleOverlayConfigScreen(parent));
        }
    }
}
