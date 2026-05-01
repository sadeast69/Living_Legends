package com.worldremembers.livinglegends.config;

import java.nio.file.Path;

public record LivingLegendsConfigLoadResult(
        LivingLegendsConfig config,
        Path configPath,
        Path namePacksPath,
        Path exportsPath,
        boolean createdDefaultConfig,
        boolean usedDefaultConfig,
        boolean malformedConfig,
        Path backupPath,
        String message
) {
}
