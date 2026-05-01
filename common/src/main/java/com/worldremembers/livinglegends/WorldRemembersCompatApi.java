package com.worldremembers.livinglegends;

import com.worldremembers.livinglegends.CompatThemeDefinitions.BiomeThemeDefinition;
import com.worldremembers.livinglegends.CompatThemeDefinitions.BlockThemeDefinition;
import com.worldremembers.livinglegends.CompatThemeDefinitions.BossThemeDefinition;
import com.worldremembers.livinglegends.CompatThemeDefinitions.DimensionThemeDefinition;
import com.worldremembers.livinglegends.CompatThemeDefinitions.MobThemeDefinition;
import com.worldremembers.livinglegends.CompatThemeDefinitions.StructureThemeDefinition;

/**
 * Internal experimental compat surface for small loader-side modules.
 * This API is intentionally string-id based and may change before a stable public compat release.
 */
public final class WorldRemembersCompatApi {
    private WorldRemembersCompatApi() {
    }

    public static void registerBossTheme(BossThemeDefinition definition) {
        WorldRemembersCompatRegistries.registerBossTheme(definition);
    }

    public static void registerStructureTheme(StructureThemeDefinition definition) {
        WorldRemembersCompatRegistries.registerStructureTheme(definition);
    }

    public static void registerBiomeTheme(BiomeThemeDefinition definition) {
        WorldRemembersCompatRegistries.registerBiomeTheme(definition);
    }

    public static void registerMobTheme(MobThemeDefinition definition) {
        WorldRemembersCompatRegistries.registerMobTheme(definition);
    }

    public static void registerBlockTheme(BlockThemeDefinition definition) {
        WorldRemembersCompatRegistries.registerBlockTheme(definition);
    }

    public static void registerDimensionTheme(DimensionThemeDefinition definition) {
        WorldRemembersCompatRegistries.registerDimensionTheme(definition);
    }

    public static WorldRemembersCompatRegistries.CompatLookup<BossThemeDefinition> getBossTheme(String entityId) {
        return WorldRemembersCompatRegistries.bossTheme(entityId);
    }

    public static WorldRemembersCompatRegistries.CompatLookup<StructureThemeDefinition> getStructureTheme(String structureId) {
        return WorldRemembersCompatRegistries.structureTheme(structureId);
    }

    public static WorldRemembersCompatRegistries.CompatLookup<BiomeThemeDefinition> getBiomeTheme(String biomeId) {
        return WorldRemembersCompatRegistries.biomeTheme(biomeId);
    }

    public static WorldRemembersCompatRegistries.CompatLookup<MobThemeDefinition> getMobTheme(String entityId) {
        return WorldRemembersCompatRegistries.mobTheme(entityId);
    }

    public static WorldRemembersCompatRegistries.CompatLookup<BlockThemeDefinition> getBlockTheme(String blockId) {
        return WorldRemembersCompatRegistries.blockTheme(blockId);
    }

    public static WorldRemembersCompatRegistries.CompatLookup<DimensionThemeDefinition> getDimensionTheme(String dimensionId) {
        return WorldRemembersCompatRegistries.dimensionTheme(dimensionId);
    }

    public static boolean isBoss(String entityId) {
        return WorldRemembersCompatRegistries.isBoss(entityId);
    }

    public static boolean isValuableBlock(String blockId) {
        return WorldRemembersCompatRegistries.isValuableBlock(blockId);
    }
}
