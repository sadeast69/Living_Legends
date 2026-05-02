package com.worldremembers.livinglegends.neoforge;

import com.worldremembers.livinglegends.neoforge.network.PlaceTitleS2CPayload;
import com.worldremembers.livinglegends.neoforge.network.WorldJournalC2SPayload;
import com.worldremembers.livinglegends.neoforge.network.WorldJournalS2CPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

public final class WorldRemembersLivingLegendsNeoForgeNetworking {
    private static volatile boolean registered;

    private WorldRemembersLivingLegendsNeoForgeNetworking() {
    }

    public static void register(IEventBus modEventBus, Logger logger) {
        WorldJournalService.configure(logger);
        modEventBus.addListener((RegisterPayloadHandlersEvent event) -> registerPayloads(event, logger));
    }

    public static boolean networkingRegistered() {
        return registered;
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event, Logger logger) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                PlaceTitleS2CPayload.TYPE,
                PlaceTitleS2CPayload.STREAM_CODEC,
                (payload, context) -> handlePlaceTitle(payload)
        );
        registrar.playToClient(
                WorldJournalS2CPayload.TYPE,
                WorldJournalS2CPayload.STREAM_CODEC,
                (payload, context) -> handleWorldJournal(payload)
        );
        registrar.playToServer(
                WorldJournalC2SPayload.TYPE,
                WorldJournalC2SPayload.STREAM_CODEC,
                (payload, context) -> {
                    Player player = context.player();
                    if (player instanceof ServerPlayer serverPlayer) {
                        WorldJournalService.handle(serverPlayer, payload);
                    }
                }
        );
        registered = true;
        if (logger != null) {
            logger.info("World Remembers NeoForge networking payloads registered");
        }
    }

    private static void handlePlaceTitle(PlaceTitleS2CPayload payload) {
        WorldRemembersLivingLegendsNeoForgeClientBridge.handlePlaceTitle(payload);
    }

    private static void handleWorldJournal(WorldJournalS2CPayload payload) {
        WorldRemembersLivingLegendsNeoForgeClientBridge.handleWorldJournal(payload);
    }
}
