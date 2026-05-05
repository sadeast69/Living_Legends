package com.worldremembers.livinglegends.neoforge;

import com.worldremembers.livinglegends.BuiltInNameData;
import com.worldremembers.livinglegends.NameDataDiagnostics;
import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import com.worldremembers.livinglegends.config.SimpleJson;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod(WorldRemembersLivingLegends.MOD_ID)
public final class WorldRemembersLivingLegendsNeoForge {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldRemembersLivingLegends.MOD_ID);

    public WorldRemembersLivingLegendsNeoForge(IEventBus modEventBus) {
        WorldRemembersLivingLegends.init(LOGGER::info);
        logNameDataWarnings();
        WorldRemembersLivingLegendsNeoForgeNetworking.register(modEventBus, LOGGER);
        NeoForgeMapIntegrationManager.register(LOGGER);
        WorldRemembersLivingLegendsNeoForgeClientLoader.initializeIfClient(LOGGER);
        WorldRemembersLivingLegendsNeoForgeItems.register(modEventBus, LOGGER);
        WorldRemembersLivingLegendsNeoForgeCommands.register(LOGGER);
        WorldRemembersCompatDataLoader.register(LOGGER);
        WorldRemembersLivingLegendsNeoForgeEvents.register(LOGGER);
    }

    private static void logNameDataWarnings() {
        logNameDataWarnings("en_us");
        logNameDataWarnings("ru_ru");
    }

    private static void logNameDataWarnings(String locale) {
        Map<String, String> translations = loadLangValues(locale);
        if (translations.isEmpty()) {
            LOGGER.warn("Could not validate built-in name translations for locale " + locale + "; lang keys unavailable");
            return;
        }

        for (var pack : BuiltInNameData.allPacks()) {
            for (String warning : NameDataDiagnostics.validate(pack, translations.keySet())) {
                LOGGER.warn("Name data warning [" + locale + "][" + pack.styleId() + "]: " + warning);
            }
            for (String warning : NameDataDiagnostics.validateTranslationValues(pack, translations)) {
                LOGGER.warn("Name data warning [" + locale + "][" + pack.styleId() + "]: " + warning);
            }
        }
    }

    private static Map<String, String> loadLangValues(String locale) {
        String path = "assets/" + WorldRemembersLivingLegends.MOD_ID + "/lang/" + locale + ".json";
        try (InputStream stream = WorldRemembersLivingLegendsNeoForge.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return Map.of();
            }

            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                Map<String, Object> raw = SimpleJson.parseObject(reader);
                Map<String, String> values = new LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : raw.entrySet()) {
                    if (entry.getValue() instanceof String value) {
                        values.put(entry.getKey(), value);
                    }
                }
                return values;
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not read built-in name lang file " + path + ": " + exception.getMessage());
            return Map.of();
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Could not parse built-in name lang file " + path + ": " + exception.getMessage());
            return Map.of();
        }
    }
}
