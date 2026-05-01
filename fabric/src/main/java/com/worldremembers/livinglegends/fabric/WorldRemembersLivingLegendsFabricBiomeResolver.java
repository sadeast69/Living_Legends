package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.BiomeMetadata;
import com.worldremembers.livinglegends.BiomeThemeResolver;
import com.worldremembers.livinglegends.config.LivingLegendsConfig;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.slf4j.Logger;

final class WorldRemembersLivingLegendsFabricBiomeResolver {
    private WorldRemembersLivingLegendsFabricBiomeResolver() {
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

        Identifier dimensionIdentifier = Identifier.tryParse(dimensionId);
        if (dimensionIdentifier == null) {
            return BiomeMetadata.fromDimensionFallback(dimensionId);
        }

        ServerWorld world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, dimensionIdentifier));
        if (world == null) {
            return BiomeMetadata.fromDimensionFallback(dimensionId);
        }

        try {
            RegistryEntry<Biome> entry = world.getBiome(new BlockPos(x, y, z));
            String biomeId = entry.getKey()
                    .map(key -> key.getValue().toString())
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
