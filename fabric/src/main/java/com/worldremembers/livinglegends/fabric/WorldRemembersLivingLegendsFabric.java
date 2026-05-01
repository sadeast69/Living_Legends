package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.BuiltInNameData;
import com.worldremembers.livinglegends.NameDataDiagnostics;
import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WorldRemembersLivingLegendsFabric implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldRemembersLivingLegends.MOD_ID);
    private static final Pattern JSON_KEY = Pattern.compile("\"([^\"]+)\"\\s*:");

    @Override
    public void onInitialize() {
        WorldRemembersLivingLegends.init(LOGGER::info);
        logNameDataWarnings();
        WorldRemembersLivingLegendsFabricItems.register(LOGGER);
        WorldRemembersLivingLegendsFabricPlaceTitles.registerNetworking(LOGGER);
        WorldJournalService.registerNetworking(LOGGER);
        WorldRemembersCompatDataLoader.register(LOGGER);
        WorldRemembersLivingLegendsFabricEvents.register(LOGGER);
        WorldRemembersLivingLegendsFabricCommands.register(LOGGER);

        if (!registerServerStartingConfigLoad()) {
            LOGGER.warn("Could not register Fabric server-start config hook; loading config during mod initialization");
            loadConfig();
        }
        registerServerStartedStateLoad();
    }

    private static boolean registerServerStartingConfigLoad() {
        try {
            Class<?> eventsClass = Class.forName("net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents");
            Object event = eventsClass.getField("SERVER_STARTING").get(null);
            Class<?> listenerClass = Class.forName("net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents$ServerStarting");
            Object listener = Proxy.newProxyInstance(
                    listenerClass.getClassLoader(),
                    new Class<?>[]{listenerClass},
                    serverStartingHandler()
            );
            Method register = event.getClass().getMethod("register", Object.class);
            register.setAccessible(true);
            register.invoke(event, listener);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("Failed to register Fabric server-start config hook: " + exception.getClass().getSimpleName()
                    + ": " + exception.getMessage());
            return false;
        }
    }

    private static InvocationHandler serverStartingHandler() {
        return (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return handleObjectMethod(proxy, method, args);
            }

            if ("onServerStarting".equals(method.getName())) {
                loadConfig();
            }

            return null;
        };
    }

    private static boolean registerServerStartedStateLoad() {
        try {
            Class<?> eventsClass = Class.forName("net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents");
            Object event = eventsClass.getField("SERVER_STARTED").get(null);
            Class<?> listenerClass = Class.forName("net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents$ServerStarted");
            Object listener = Proxy.newProxyInstance(
                    listenerClass.getClassLoader(),
                    new Class<?>[]{listenerClass},
                    serverStartedHandler()
            );
            Method register = event.getClass().getMethod("register", Object.class);
            register.setAccessible(true);
            register.invoke(event, listener);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("Failed to register Fabric server-started storage hook: " + exception.getClass().getSimpleName()
                    + ": " + exception.getMessage());
            return false;
        }
    }

    private static InvocationHandler serverStartedHandler() {
        return (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return handleObjectMethod(proxy, method, args);
            }

            if ("onServerStarted".equals(method.getName())) {
                WorldRemembersLivingLegendsFabricStorage.initializeServer(args == null ? null : args[0], LOGGER);
            }

            return null;
        };
    }

    private static Object handleObjectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> WorldRemembersLivingLegends.MOD_ID + " server-start config hook";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> null;
        };
    }

    private static void loadConfig() {
        WorldRemembersLivingLegends.loadConfigOnServerStart(
                resolveGameDir(),
                LOGGER::info,
                LOGGER::warn
        );
    }

    private static void logNameDataWarnings() {
        logNameDataWarnings("en_us");
        logNameDataWarnings("ru_ru");
    }

    private static void logNameDataWarnings(String locale) {
        Set<String> keys = loadLangKeys(locale);
        if (keys.isEmpty()) {
            LOGGER.warn("Could not validate built-in name translations for locale " + locale + "; lang keys unavailable");
            return;
        }

        for (var pack : BuiltInNameData.allPacks()) {
            for (String warning : NameDataDiagnostics.validate(pack, keys)) {
                LOGGER.warn("Name data warning [" + locale + "][" + pack.styleId() + "]: " + warning);
            }
        }
    }

    private static Set<String> loadLangKeys(String locale) {
        String path = "assets/" + WorldRemembersLivingLegends.MOD_ID + "/lang/" + locale + ".json";
        try (InputStream stream = WorldRemembersLivingLegendsFabric.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return Set.of();
            }

            Set<String> keys = new HashSet<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = JSON_KEY.matcher(line);
                    if (matcher.find()) {
                        keys.add(matcher.group(1));
                    }
                }
            }
            return keys;
        } catch (IOException exception) {
            LOGGER.warn("Could not read built-in name lang file " + path + ": " + exception.getMessage());
            return Set.of();
        }
    }

    private static Path resolveGameDir() {
        try {
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = loaderClass.getMethod("getInstance").invoke(null);
            Object gameDir = loaderClass.getMethod("getGameDir").invoke(loader);

            if (gameDir instanceof Path path) {
                return path;
            }

            LOGGER.warn("FabricLoader.getGameDir() returned unexpected value; using current directory for config");
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("Could not resolve Fabric game directory; using current directory for config: "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }

        return Path.of(".");
    }
}
