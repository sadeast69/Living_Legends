package com.worldremembers.livinglegends.neoforge;

import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

public final class WorldRemembersLivingLegendsNeoForgeItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WorldRemembersLivingLegends.MOD_ID);
    public static final DeferredItem<Item> WORLD_JOURNAL = ITEMS.register(
            "world_journal",
            () -> new WorldJournalItem(new Item.Properties().stacksTo(1))
    );

    private WorldRemembersLivingLegendsNeoForgeItems() {
    }

    public static void register(IEventBus modEventBus, Logger logger) {
        ITEMS.register(modEventBus);
        modEventBus.addListener(WorldRemembersLivingLegendsNeoForgeItems::addCreativeTabItems);
        if (logger != null) {
            logger.info("Registered World Journal item");
        }
    }

    private static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(WORLD_JOURNAL);
        }
    }
}
