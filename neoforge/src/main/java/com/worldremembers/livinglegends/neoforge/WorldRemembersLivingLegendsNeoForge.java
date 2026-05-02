package com.worldremembers.livinglegends.neoforge;

import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(WorldRemembersLivingLegends.MOD_ID)
public final class WorldRemembersLivingLegendsNeoForge {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldRemembersLivingLegends.MOD_ID);

    public WorldRemembersLivingLegendsNeoForge(IEventBus modEventBus) {
        WorldRemembersLivingLegends.init(LOGGER::info);
        WorldRemembersLivingLegendsNeoForgeNetworking.register(modEventBus, LOGGER);
        WorldRemembersLivingLegendsNeoForgeClientLoader.initializeIfClient(LOGGER);
        WorldRemembersLivingLegendsNeoForgeItems.register(modEventBus, LOGGER);
        WorldRemembersLivingLegendsNeoForgeCommands.register(LOGGER);
        WorldRemembersCompatDataLoader.register(LOGGER);
        WorldRemembersLivingLegendsNeoForgeEvents.register(LOGGER);
    }
}
