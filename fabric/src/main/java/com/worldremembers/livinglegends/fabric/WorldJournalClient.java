package com.worldremembers.livinglegends.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

final class WorldJournalClient {
    private WorldJournalClient() {
    }

    static void handle(WorldJournalS2CPayload payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || payload == null) {
            return;
        }

        if (payload.action() == WorldJournalS2CPayload.Action.ERROR) {
            Text message = payload.messageKey().isBlank()
                    ? Text.literal(payload.messageText())
                    : Text.translatable(payload.messageKey());
            if (client.inGameHud != null) {
                client.inGameHud.getChatHud().addMessage(message);
            }
            return;
        }

        WorldJournalScreen screen = client.currentScreen instanceof WorldJournalScreen journal
                ? journal
                : new WorldJournalScreen(client.currentScreen);
        if (!(client.currentScreen instanceof WorldJournalScreen)) {
            client.setScreen(screen);
        }
        screen.apply(payload);
    }

    static void send(WorldJournalC2SPayload payload) {
        try {
            ClientPlayNetworking.send(payload);
        } catch (RuntimeException ignored) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.inGameHud != null) {
                client.inGameHud.getChatHud().addMessage(Text.translatable("living_legends.journal.error.network"));
            }
        }
    }
}
