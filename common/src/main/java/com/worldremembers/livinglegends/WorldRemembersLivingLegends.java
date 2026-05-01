package com.worldremembers.livinglegends;

import com.worldremembers.livinglegends.config.LivingLegendsConfig;
import com.worldremembers.livinglegends.config.LivingLegendsConfigLoadResult;
import com.worldremembers.livinglegends.config.LivingLegendsConfigManager;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

public final class WorldRemembersLivingLegends {
    public static final String SERIES_NAME = "World Remembers";
    public static final String MOD_NAME = "World Remembers: Living Legends";
    public static final String MOD_ID = "living_legends";
    public static final String REGISTRY_NAMESPACE = MOD_ID;
    public static final String CONFIG_ROOT = "config/world_remembers/living_legends/";

    private static final String INITIALIZED_MESSAGE = MOD_NAME + " initialized";

    private WorldRemembersLivingLegends() {
    }

    public static void init(Consumer<String> logger) {
        Objects.requireNonNull(logger, "logger").accept(INITIALIZED_MESSAGE);
    }

    public static LivingLegendsConfig config() {
        return LivingLegendsConfigManager.current();
    }

    public static LivingLegendsConfigLoadResult loadConfigOnServerStart(
            Path gameRoot,
            Consumer<String> infoLogger,
            Consumer<String> warningLogger
    ) {
        return LivingLegendsConfigManager.loadOnServerStart(gameRoot, infoLogger, warningLogger);
    }

    public static LivingLegendsConfigLoadResult reloadConfig(
            Path gameRoot,
            Consumer<String> infoLogger,
            Consumer<String> warningLogger
    ) {
        return LivingLegendsConfigManager.reload(gameRoot, infoLogger, warningLogger);
    }
}
