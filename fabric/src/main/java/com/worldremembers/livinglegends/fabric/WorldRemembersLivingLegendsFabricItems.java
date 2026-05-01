package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

final class WorldRemembersLivingLegendsFabricItems {
    static final Identifier WORLD_JOURNAL_ID = Identifier.of(WorldRemembersLivingLegends.MOD_ID, "world_journal");
    static final Item WORLD_JOURNAL = new WorldJournalItem(new Item.Settings().maxCount(1));
    private static final RegistryKey<ItemGroup> TOOLS_AND_UTILITIES =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of("minecraft", "tools_and_utilities"));

    private static boolean registered;

    private WorldRemembersLivingLegendsFabricItems() {
    }

    static synchronized void register(Logger logger) {
        if (registered) {
            return;
        }
        Registry.register(Registries.ITEM, WORLD_JOURNAL_ID, WORLD_JOURNAL);
        ItemGroupEvents.modifyEntriesEvent(TOOLS_AND_UTILITIES).register(entries -> entries.add(WORLD_JOURNAL));
        registered = true;
        if (logger != null) {
            logger.info("Registered World Journal item");
        }
    }
}
