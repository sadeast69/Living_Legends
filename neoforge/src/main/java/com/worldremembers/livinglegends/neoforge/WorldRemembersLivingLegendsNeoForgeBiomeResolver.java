package com.worldremembers.livinglegends.neoforge;

import com.worldremembers.livinglegends.BiomeMetadata;
import com.worldremembers.livinglegends.BiomeThemeResolver;
import com.worldremembers.livinglegends.config.LivingLegendsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.slf4j.Logger;

final class WorldRemembersLivingLegendsNeoForgeBiomeResolver {
    private WorldRemembersLivingLegendsNeoForgeBiomeResolver() {
    }

    static BiomeMetadata resolve(
            Object serverObject,
            String dimensionId,
            int x,
            int y,
            int z,
            LivingLegendsConfig config,
            Logger logger
    ) {
        if (!(serverObject instanceof MinecraftServer server)) {
            return BiomeMetadata.fromDimensionFallback(dimensionId);
        }

        ResourceLocation dimensionLocation = ResourceLocation.tryParse(dimensionId);
        if (dimensionLocation == null) {
            return BiomeMetadata.fromDimensionFallback(dimensionId);
        }

        ServerLevel world = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionLocation));
        if (world == null) {
            return BiomeMetadata.fromDimensionFallback(dimensionId);
        }

        try {
            Holder<Biome> entry = world.getBiome(new BlockPos(x, y, z));
            String biomeId = entry.unwrapKey()
                    .map(ResourceKey::location)
                    .map(ResourceLocation::toString)
                    .orElse(BiomeMetadata.UNKNOWN);
            return BiomeThemeResolver.resolve(dimensionId, biomeId, config, "center_position");
        } catch (RuntimeException exception) {
            if (config != null && config.debug != null && config.debug.enabled && logger != null) {
                logger.warn("World Remembers biome lookup failed; using fallback"
                        + " dimension=" + dimensionId
                        + " position=" + x + "," + y + "," + z
                        + " error=" + exception.getClass().getSimpleName() + ": " + exception.getMessage());
            }
            return BiomeMetadata.fromDimensionFallback(dimensionId);
        }
    }
}
