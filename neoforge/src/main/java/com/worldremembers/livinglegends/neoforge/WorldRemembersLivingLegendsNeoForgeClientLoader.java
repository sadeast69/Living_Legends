package com.worldremembers.livinglegends.neoforge;

import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

import java.lang.reflect.Method;

final class WorldRemembersLivingLegendsNeoForgeClientLoader {
    private static final String CLIENT_NETWORKING_CLASS =
            "com.worldremembers.livinglegends.neoforge.client.WorldRemembersLivingLegendsNeoForgeClientNetworking";

    private WorldRemembersLivingLegendsNeoForgeClientLoader() {
    }

    static void initializeIfClient(Logger logger) {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        try {
            Class<?> clientClass = Class.forName(CLIENT_NETWORKING_CLASS);
            Method register = clientClass.getDeclaredMethod("register", Logger.class);
            register.invoke(null, logger);
        } catch (ReflectiveOperationException exception) {
            if (logger != null) {
                logger.warn("Could not initialize World Remembers NeoForge client hooks: " + exception.getMessage());
            }
        }
    }
}
