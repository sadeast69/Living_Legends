package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.CompatThemeJson;
import com.worldremembers.livinglegends.WorldRemembersCompatRegistries;
import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import com.worldremembers.livinglegends.config.SimpleJson;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class WorldRemembersCompatDataLoader {
    private static final String ROOT = "world_remembers";

    private WorldRemembersCompatDataLoader() {
    }

    static void register(Logger logger) {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return Identifier.of(WorldRemembersLivingLegends.MOD_ID, "compat_registries");
            }

            @Override
            public void reload(ResourceManager manager) {
                load(manager, logger);
            }
        });
    }

    private static void load(ResourceManager manager, Logger logger) {
        CompatThemeJson.MutableData data = CompatThemeJson.MutableData.empty();
        List<String> warnings = new ArrayList<>();
        Map<Identifier, Resource> resources = manager.findResources(
                ROOT,
                identifier -> identifier.getPath().endsWith(".json")
        );

        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier id = entry.getKey();
            String folder = folder(id.getPath());
            if (folder.isBlank()) {
                continue;
            }
            try (Reader reader = entry.getValue().getReader()) {
                Map<String, Object> root = SimpleJson.parseObject(reader);
                int index = 0;
                for (Map<String, Object> object : CompatThemeJson.entries(root)) {
                    String sourceId = id + (index == 0 ? "" : "#" + index);
                    CompatThemeJson.addEntry(folder, sourceId, object, data, warnings);
                    index++;
                }
            } catch (Exception exception) {
                warnings.add("Could not load compat registry " + id + ": "
                        + exception.getClass().getSimpleName() + ": " + exception.getMessage());
            }
        }

        WorldRemembersCompatRegistries.reloadDatapack(data.toLoadedData(warnings));
        if (logger != null) {
            logger.info("World Remembers compat registries loaded: "
                    + WorldRemembersCompatRegistries.summaryLine());
            if (WorldRemembersLivingLegends.config().debug.enabled) {
                for (String warning : warnings) {
                    logger.warn("World Remembers compat registry warning: " + warning);
                }
            } else if (!warnings.isEmpty()) {
                logger.warn("World Remembers compat registries loaded with " + warnings.size()
                        + " warnings; enable debug logging for details.");
            }
        }
    }

    private static String folder(String path) {
        if (path == null || !path.startsWith(ROOT + "/")) {
            return "";
        }
        String tail = path.substring((ROOT + "/").length());
        int slash = tail.indexOf('/');
        return slash <= 0 ? "" : tail.substring(0, slash);
    }
}
