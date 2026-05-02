package com.worldremembers.livinglegends.neoforge.client;

import com.worldremembers.livinglegends.neoforge.network.WorldJournalC2SPayload;
import com.worldremembers.livinglegends.neoforge.network.WorldJournalS2CPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

final class WorldJournalClient {
    private WorldJournalClient() {
    }

    static void handle(WorldJournalS2CPayload payload) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || payload == null) {
            return;
        }
        if (!client.isSameThread()) {
            client.execute(() -> handle(payload));
            return;
        }

        WorldJournalScreen screen = client.screen instanceof WorldJournalScreen journal
                ? journal
                : new WorldJournalScreen(client.screen);
        if (!(client.screen instanceof WorldJournalScreen)) {
            client.setScreen(screen);
        }
        screen.apply(payload);
    }

    static void send(WorldJournalC2SPayload payload) {
        try {
            PacketDistributor.sendToServer(payload);
        } catch (RuntimeException ignored) {
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.gui != null) {
                client.gui.getChat().addMessage(Component.translatable("living_legends.journal.error.network"));
            }
        }
    }
}
