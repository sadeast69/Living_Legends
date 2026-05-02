package com.worldremembers.livinglegends.neoforge;

import com.worldremembers.livinglegends.CompatThemeJson;
import com.worldremembers.livinglegends.WorldRemembersCompatRegistries;
import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import com.worldremembers.livinglegends.config.SimpleJson;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
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
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> event.addListener(new Listener(logger)));
    }

    private static void load(ResourceManager manager, Logger logger) {
        CompatThemeJson.MutableData data = CompatThemeJson.MutableData.empty();
        List<String> warnings = new ArrayList<>();
        Map<ResourceLocation, Resource> resources = manager.listResources(
                ROOT,
                location -> location.getPath().endsWith(".json")
        );

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation id = entry.getKey();
            String folder = folder(id.getPath());
            if (folder.isBlank()) {
                continue;
            }
            try (Reader reader = entry.getValue().openAsReader()) {
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

    private static final class Listener extends SimplePreparableReloadListener<ResourceManager> {
        private final Logger logger;

        private Listener(Logger logger) {
            this.logger = logger;
        }

        @Override
        protected ResourceManager prepare(ResourceManager manager, ProfilerFiller profiler) {
            return manager;
        }

        @Override
        protected void apply(ResourceManager manager, ResourceManager resourceManager, ProfilerFiller profiler) {
            load(manager, logger);
        }
    }
}
