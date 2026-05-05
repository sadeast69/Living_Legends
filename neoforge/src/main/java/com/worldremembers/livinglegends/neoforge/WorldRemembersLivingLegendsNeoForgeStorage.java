package com.worldremembers.livinglegends.neoforge;

import com.worldremembers.livinglegends.BuiltInNameData;
import com.worldremembers.livinglegends.CandidateDecayEngine;
import com.worldremembers.livinglegends.CauseContextResolver;
import com.worldremembers.livinglegends.ChunkMemoryStats;
import com.worldremembers.livinglegends.DeathSiteEnvironment;
import com.worldremembers.livinglegends.DeletedPlaceMarker;
import com.worldremembers.livinglegends.DirtyChunkScoreQueue;
import com.worldremembers.livinglegends.EventCollector;
import com.worldremembers.livinglegends.EventType;
import com.worldremembers.livinglegends.NameContext;
import com.worldremembers.livinglegends.NameDataDiagnostics;
import com.worldremembers.livinglegends.NameDataPack;
import com.worldremembers.livinglegends.NameGenerationDiagnostics;
import com.worldremembers.livinglegends.NameGenerator;
import com.worldremembers.livinglegends.NamePatternSource;
import com.worldremembers.livinglegends.NameRecipe;
import com.worldremembers.livinglegends.NameTextSafety;
import com.worldremembers.livinglegends.NameTokenForm;
import com.worldremembers.livinglegends.NamedPlace;
import com.worldremembers.livinglegends.PlaceBounds;
import com.worldremembers.livinglegends.PlaceCause;
import com.worldremembers.livinglegends.PlaceCauseType;
import com.worldremembers.livinglegends.PlaceCluster;
import com.worldremembers.livinglegends.PlaceGenerationLimits;
import com.worldremembers.livinglegends.PlaceRarity;
import com.worldremembers.livinglegends.PlaceSpacingRules;
import com.worldremembers.livinglegends.PlaceStats;
import com.worldremembers.livinglegends.PlaceType;
import com.worldremembers.livinglegends.ScoreEngine;
import com.worldremembers.livinglegends.WorldMemoryDataValidator;
import com.worldremembers.livinglegends.WorldMemoryEvent;
import com.worldremembers.livinglegends.WorldMemoryStorageData;
import com.worldremembers.livinglegends.WorldRemembersDataVersions;
import com.worldremembers.livinglegends.WorldPos;
import com.worldremembers.livinglegends.WorldRemembersCompatRegistries;
import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import com.worldremembers.livinglegends.config.LivingLegendsConfig;
import com.worldremembers.livinglegends.config.LivingLegendsConfigManager;
import com.worldremembers.livinglegends.config.SimpleJson;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class WorldRemembersLivingLegendsNeoForgeStorage {
    public static final String STATE_ID = "world_remembers_living_legends_world_memory";

    private static final Set<Integer> LOGGED_LOADED_STATES = Collections.synchronizedSet(new HashSet<>());
    private static final List<String> REQUIRED_STYLE_IDS = List.of(
            "vanilla_adventure",
            "neutral_server",
            "dark_fantasy",
            "cozy_survival",
            "epic_mythology",
            "funny_community"
    );
    private static volatile boolean warningLogged;

    private WorldRemembersLivingLegendsNeoForgeStorage() {
    }

    public static void initializeServer(Object server, Logger logger) {
        ServerLevel overworld = overworld(server);
        if (overworld == null) {
            warnOnce(logger, "Could not initialize World Remembers state: server overworld is unavailable");
            return;
        }

        state(overworld, logger);
    }

    public static void recordEvent(Object world, WorldMemoryEvent event, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null || event == null) {
            return;
        }

        EventCollector.collect(
                event,
                new PersistentEventStore(state.data(), state.scoreQueue(), state, logger),
                logger == null ? null : logger::info
        );
    }

    public static void processDirtyScoreQueue(Object server, Logger logger) {
        ServerLevel overworld = overworld(server);
        WorldRemembersNeoForgeSavedData state = state(overworld, logger);
        if (overworld == null || state == null) {
            return;
        }

        MinecraftServer minecraftServer = overworld.getServer();
        LivingLegendsConfig config = WorldRemembersLivingLegends.config();
        DirtyChunkScoreQueue.ProcessResult result = state.scoreQueue().process(
                state.data(),
                config,
                gameTime(overworld),
                logger == null ? null : logger::info,
                (dimensionId, x, y, z) -> WorldRemembersLivingLegendsNeoForgeBiomeResolver.resolve(
                        minecraftServer,
                        dimensionId,
                        x,
                        y,
                        z,
                        config,
                        logger
                ),
                place -> onGeneratedPlace(minecraftServer, place, logger)
        );
        if (result.namedPlaceChanges() > 0) {
            WorldRemembersLivingLegendsNeoForgePlaceTitles.invalidateIndex();
            NeoForgeMapIntegrationManager.syncAllFromWorld(minecraftServer, logger);
            markDirty(state, "place_generation", logger);
        }
    }

    public static void processCandidateDecay(Object server, Logger logger) {
        ServerLevel overworld = overworld(server);
        WorldRemembersNeoForgeSavedData state = state(overworld, logger);
        if (overworld == null || state == null) {
            return;
        }

        LivingLegendsConfig config = WorldRemembersLivingLegends.config();
        if (config == null || config.candidateDecay == null || !config.candidateDecay.enabled) {
            return;
        }

        long currentGameTime = gameTime(overworld);
        if (!state.shouldRunCandidateDecay(currentGameTime, config.candidateDecay.intervalTicks)) {
            return;
        }

        CandidateDecayEngine.DecaySummary summary = CandidateDecayEngine.run(state.data(), config, currentGameTime, 0L);
        state.recordCandidateDecayCheck(currentGameTime);
        if (summary.decayed() > 0) {
            markDirty(state, "candidate_decay", logger);
        }
        if (logger != null && config.candidateDecay.debugLogging) {
            logger.info(summary.logLine());
        }
    }

    public static List<NamedPlace> places(Object world, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        return state == null ? List.of() : state.data().namedPlaces();
    }

    public static NamedPlace place(Object world, String placeId, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        return state == null ? null : state.data().namedPlace(placeId);
    }

    public static boolean upsertPlace(Object world, NamedPlace place, String reason, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null || place == null) {
            return false;
        }

        boolean changed = state.data().upsertNamedPlace(place);
        if (changed) {
            WorldRemembersLivingLegendsNeoForgePlaceTitles.invalidateIndex();
            NeoForgeMapIntegrationManager.syncAllFromWorld(world, logger);
            markDirty(state, reason == null || reason.isBlank() ? "upsert_place" : reason, logger);
        }
        return changed;
    }

    public static boolean deletePlace(Object world, String placeId, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null) {
            return false;
        }

        NamedPlace place = state.data().namedPlace(placeId);
        if (place == null) {
            return false;
        }

        state.data().recordDeletedPlaceMarker(DeletedPlaceMarker.fromPlace(place, gameTime(world)));
        boolean changed = state.data().deleteNamedPlace(placeId);
        if (changed) {
            state.data().removeDiscoveredPlaceFromAllPlayers(placeId);
            WorldRemembersLivingLegendsNeoForgePlaceTitles.invalidateIndex();
            NeoForgeMapIntegrationManager.syncAllFromWorld(world, logger);
            NeoForgeMapIntegrationManager.removeDestinationFromWorld(world, placeId, logger);
            markDirty(state, "delete_place " + placeId, logger);
        }
        return changed;
    }

    public static List<DeletedPlaceMarker> deletedPlaceMarkers(Object world, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        return state == null ? List.of() : state.data().deletedPlaceMarkers();
    }

    public static String clearDeletedPlaceMarker(Object world, String markerId, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null) {
            return "World Remembers deleted marker clear failed: storage unavailable";
        }

        boolean changed = state.data().removeDeletedPlaceMarker(markerId);
        if (changed) {
            markDirty(state, "clear_deleted_marker " + markerId, logger);
        }
        return changed
                ? "World Remembers cleared deleted marker " + markerId
                : "World Remembers deleted marker not found: " + markerId;
    }

    public static String clearAllDeletedPlaceMarkers(Object world, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null) {
            return "World Remembers deleted marker clear failed: storage unavailable";
        }

        int cleared = state.data().clearDeletedPlaceMarkers();
        if (cleared > 0) {
            markDirty(state, "clear_all_deleted_markers", logger);
        }
        return "World Remembers cleared " + cleared + " deleted place markers";
    }

    public static Set<String> discoveredPlaces(Object world, String playerId, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        return state == null ? Set.of() : state.data().discoveredPlaceIds(playerId);
    }

    public static boolean hasWorldFirstDiscovery(Object world, String discoveryId, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        return state != null && state.data().hasWorldFirstDiscovery(discoveryId);
    }

    public static String debugChunk(Object world, String dimensionId, int chunkX, int chunkZ, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null) {
            return "World Remembers chunk storage is unavailable";
        }

        String key = WorldMemoryStorageData.chunkStatsKey(dimensionId, chunkX, chunkZ);
        ChunkMemoryStats stats = state.data().chunkStatsByKey().get(key);
        if (stats == null) {
            return "World Remembers chunk " + key + " events=0 visits=0 deaths=0 combat=0 build=0 score=0.0 lastGameTime=0";
        }

        return "World Remembers chunk " + key
                + " events=" + stats.eventCount()
                + " visits=" + stats.visitCount()
                + " deaths=" + stats.deathCount()
                + " combat=" + stats.combatEventCount()
                + " build=" + stats.buildEventCount()
                + " score=" + stats.basicScore()
                + " lastGameTime=" + stats.lastEventGameTime();
    }

    public static String debugScore(Object world, String dimensionId, int chunkX, int chunkZ, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null) {
            return "World Remembers score storage is unavailable";
        }

        String key = WorldMemoryStorageData.chunkStatsKey(dimensionId, chunkX, chunkZ);
        ChunkMemoryStats stats = state.data().chunkStatsByKey().get(key);
        if (stats == null) {
            stats = ChunkMemoryStats.empty(dimensionId, chunkX, chunkZ);
        }

        StringBuilder message = new StringBuilder();
        message.append("World Remembers score ")
                .append(dimensionId)
                .append("@")
                .append(chunkX)
                .append(",")
                .append(chunkZ);
        for (ScoreEngine.ScoreEvaluation evaluation : ScoreEngine.evaluateChunk(
                stats,
                state.data(),
                WorldRemembersLivingLegends.config()
        )) {
            message.append('\n').append(ScoreEngine.formatCommandLine(evaluation));
        }
        return message.toString();
    }

    public static String candidateDecayStatus() {
        return CandidateDecayEngine.status(WorldRemembersLivingLegends.config());
    }

    public static String runCandidateDecayNow(Object world, long simulatedTicks, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null) {
            return "Candidate decay run failed: storage unavailable";
        }

        CandidateDecayEngine.DecaySummary summary = CandidateDecayEngine.run(
                state.data(),
                WorldRemembersLivingLegends.config(),
                gameTime(world),
                Math.max(0L, simulatedTicks)
        );
        if (summary.decayed() > 0) {
            markDirty(state, "candidate_decay_debug_run", logger);
        }
        if (logger != null && WorldRemembersLivingLegends.config().candidateDecay.debugLogging) {
            logger.info(summary.logLine());
        }
        return summary.logLine();
    }

    public static String debugCandidateDecayChunk(
            Object world,
            String dimensionId,
            int chunkX,
            int chunkZ,
            Logger logger
    ) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null) {
            return "Candidate decay chunk info failed: storage unavailable";
        }

        return CandidateDecayEngine.chunkInfo(
                state.data(),
                WorldRemembersLivingLegends.config(),
                dimensionId,
                chunkX,
                chunkZ,
                gameTime(world)
        );
    }

    public static String debugCandidateDecayNearest(
            Object world,
            String dimensionId,
            int x,
            int y,
            int z,
            Logger logger
    ) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null) {
            return "Candidate decay nearest info failed: storage unavailable";
        }

        WorldPos position = new WorldPos(dimensionId, x, y, z);
        NamedPlace nearest = state.data().namedPlaces().stream()
                .filter(place -> place != null && dimensionId.equals(place.dimensionId()))
                .min(java.util.Comparator.comparingLong(place -> place.center().squaredDistanceTo(position)))
                .orElse(null);
        if (nearest == null) {
            return "Candidate decay nearest: no NamedPlace in this dimension";
        }

        return "Candidate decay nearest: NamedPlace is protected from candidate decay"
                + " id=" + nearest.placeIdString()
                + " type=" + nearest.placeType().name()
                + " score=" + nearest.score()
                + " sourceChunks=" + nearest.sourceChunks();
    }

    public static String touchCandidateDecayChunk(
            Object world,
            String dimensionId,
            int chunkX,
            int chunkZ,
            Logger logger
    ) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null) {
            return "Candidate decay touch failed: storage unavailable";
        }

        boolean changed = state.data().touchCandidateDecay(dimensionId, chunkX, chunkZ, gameTime(world));
        if (changed) {
            markDirty(state, "candidate_decay_touch_chunk", logger);
        }
        return "Candidate decay touch chunk " + WorldMemoryStorageData.chunkStatsKey(dimensionId, chunkX, chunkZ)
                + " changed=" + changed;
    }

    public static String setCandidateDecayScore(
            Object world,
            String dimensionId,
            int chunkX,
            int chunkZ,
            PlaceType placeType,
            double score,
            Logger logger
    ) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null) {
            return "Candidate decay setscore failed: storage unavailable";
        }

        boolean changed = state.data().setCandidateDecayScore(dimensionId, chunkX, chunkZ, placeType, score, gameTime(world));
        if (changed) {
            markDirty(state, "candidate_decay_setscore_chunk", logger);
        }
        return "Candidate decay setscore chunk " + WorldMemoryStorageData.chunkStatsKey(dimensionId, chunkX, chunkZ)
                + " type=" + placeType.name()
                + " score=" + score
                + " changed=" + changed;
    }

    public static boolean recordJournalDiscovery(Object world, String playerId, String placeId, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null) {
            return false;
        }
        if (state.data().namedPlace(placeId) == null) {
            if (logger != null && WorldRemembersLivingLegends.config().debug.enabled) {
                logger.debug("Skipping journal discovery for missing place id {}", placeId);
            }
            return false;
        }

        boolean changed = state.data().recordDiscoveredPlace(playerId, placeId);
        if (changed) {
            markDirty(state, "journal_discovery " + placeId, logger);
            NeoForgeMapIntegrationManager.syncPlayerById(world, playerId, logger);
        }
        return changed;
    }

    public static long gameTimeFor(Object world) {
        return gameTime(world);
    }

    public static String uniquePlaceId(Object world, String baseId, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null) {
            return baseId;
        }

        String id = baseId;
        int suffix = 2;
        while (state.data().namedPlace(id) != null) {
            id = baseId + "#" + suffix;
            suffix++;
        }
        return id;
    }

    public static String regeneratePlace(Object world, String placeId, boolean force, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null) {
            return "World Remembers regenerate failed: storage unavailable";
        }
        NamedPlace place = state.data().namedPlace(placeId);
        if (place == null) {
            return "World Remembers regenerate skipped id=" + placeId + " reason=not_found";
        }
        RegenerateResult result = regenerateOne(state.data(), place, force, gameTime(world));
        if (result.changed()) {
            WorldRemembersLivingLegendsNeoForgePlaceTitles.invalidateIndex();
            NeoForgeMapIntegrationManager.syncAllFromWorld(world, logger);
            markDirty(state, "regenerate_place " + placeId, logger);
        }
        return result.message();
    }

    public static String regenerateAll(Object world, boolean force, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null) {
            return "World Remembers regenerate failed: storage unavailable";
        }
        int changed = 0;
        int skipped = 0;
        StringBuilder message = new StringBuilder("World Remembers regenerate all");
        for (NamedPlace place : state.data().namedPlaces()) {
            RegenerateResult result = regenerateOne(state.data(), place, force, gameTime(world));
            if (result.changed()) {
                changed++;
            } else {
                skipped++;
            }
            message.append('\n').append(result.message());
        }
        if (changed > 0) {
            WorldRemembersLivingLegendsNeoForgePlaceTitles.invalidateIndex();
            NeoForgeMapIntegrationManager.syncAllFromWorld(world, logger);
            markDirty(state, "regenerate_all", logger);
        }
        message.insert("World Remembers regenerate all".length(), " changed=" + changed + " skipped=" + skipped);
        return message.toString();
    }

    public static String exportPlaces(Object world, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null) {
            return "World Remembers export failed: storage unavailable";
        }
        Path exportPath = LivingLegendsConfigManager.exportsPath(resolveGameDir()).resolve("places_export.json");
        try {
            Files.createDirectories(exportPath.getParent());
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("exportVersion", WorldRemembersDataVersions.CURRENT_EXPORT_VERSION);
            root.put("schemaVersion", WorldRemembersDataVersions.CURRENT_WORLD_STATE_VERSION);
            root.put("worldStateVersion", WorldRemembersDataVersions.CURRENT_WORLD_STATE_VERSION);
            root.put("namedPlaceVersion", WorldRemembersDataVersions.CURRENT_NAMED_PLACE_VERSION);
            root.put("nameRecipeVersion", WorldRemembersDataVersions.CURRENT_NAME_RECIPE_VERSION);
            List<Object> places = new ArrayList<>();
            for (NamedPlace place : state.data().namedPlaces()) {
                places.add(placeToMap(place));
            }
            root.put("namedPlaces", places);
            try (BufferedWriter writer = Files.newBufferedWriter(exportPath)) {
                writer.write(SimpleJson.stringify(root));
                writer.newLine();
            }
            return "World Remembers exported " + places.size() + " places to " + exportPath;
        } catch (IOException | RuntimeException exception) {
            if (logger != null) {
                logger.warn("World Remembers export failed: " + exception.getMessage());
            }
            return "World Remembers export failed: " + exception.getMessage();
        }
    }

    public static String importPlaces(Object world, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null) {
            return "World Remembers import failed: storage unavailable";
        }
        Path exportPath = LivingLegendsConfigManager.exportsPath(resolveGameDir()).resolve("places_export.json");
        int imported = 0;
        int skippedDuplicateId = 0;
        int skippedSpatialDuplicate = 0;
        int malformed = 0;
        int restoredDeleted = 0;
        try (BufferedReader reader = Files.newBufferedReader(exportPath)) {
            Map<String, Object> root = SimpleJson.parseObject(reader);
            int exportVersion = intValue(root, "exportVersion", 0);
            int schemaVersion = intValue(root, "schemaVersion", intValue(root, "worldStateVersion", 0));
            if (exportVersion > WorldRemembersDataVersions.CURRENT_EXPORT_VERSION
                    || schemaVersion > WorldRemembersDataVersions.CURRENT_WORLD_STATE_VERSION) {
                return "World Remembers import failed: unsupported version"
                        + " exportVersion=" + exportVersion
                        + " schemaVersion=" + schemaVersion
                        + " supportedExport=" + WorldRemembersDataVersions.CURRENT_EXPORT_VERSION
                        + " supportedSchema=" + WorldRemembersDataVersions.CURRENT_WORLD_STATE_VERSION;
            }
            Object placesValue = root.get("namedPlaces");
            if (!(placesValue instanceof List<?> placeMaps)) {
                return "World Remembers import failed: invalid schema, namedPlaces array missing";
            }
            for (Object value : placeMaps) {
                if (!(value instanceof Map<?, ?> rawMap)) {
                    malformed++;
                    continue;
                }
                try {
                    NamedPlace place = placeFromMap(castMap(rawMap));
                    if (state.data().namedPlace(place.placeIdString()) != null) {
                        skippedDuplicateId++;
                        continue;
                    }
                    String duplicateReason = importDuplicateReason(state.data(), place);
                    if (!duplicateReason.isBlank()) {
                        skippedSpatialDuplicate++;
                        if (logger != null && WorldRemembersLivingLegends.config().debug.enabled) {
                            logger.info("Skipped imported place " + place.placeIdString() + " reason=" + duplicateReason);
                        }
                        continue;
                    }
                    if (state.data().upsertNamedPlace(place)) {
                        imported++;
                        restoredDeleted += state.data().removeMatchingDeletedPlaceMarkers(place, WorldRemembersLivingLegends.config());
                        if (!WorldRemembersLivingLegends.config().placeTypes.autoGenerationEnabled(place.placeType())
                                && logger != null
                                && WorldRemembersLivingLegends.config().debug.enabled) {
                            logger.info("Imported stored place with disabled auto-generation type="
                                    + place.placeType().name()
                                    + " id=" + place.placeIdString());
                        }
                    }
                } catch (RuntimeException exception) {
                    malformed++;
                    if (logger != null) {
                        logger.warn("Skipped invalid imported place: " + exception.getMessage());
                    }
                }
            }
            if (imported > 0) {
                WorldRemembersLivingLegendsNeoForgePlaceTitles.invalidateIndex();
                NeoForgeMapIntegrationManager.syncAllFromWorld(world, logger);
                markDirty(state, "import_places", logger);
            }
            return "World Remembers imported " + imported
                    + " skippedDuplicateId=" + skippedDuplicateId
                    + " skippedSpatialDuplicate=" + skippedSpatialDuplicate
                    + " skippedMalformed=" + malformed
                    + " skippedInvalidSchema=0"
                    + " restoredDeleted=" + restoredDeleted
                    + " from " + exportPath;
        } catch (IOException | RuntimeException exception) {
            if (logger != null) {
                logger.warn("World Remembers import failed: " + exception.getMessage());
            }
            return "World Remembers import failed: " + exception.getMessage();
        }
    }

    public static String debugClusters(Object world, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null) {
            return "World Remembers cluster storage is unavailable";
        }
        return state.scoreQueue().debugClusters();
    }

    public static String debugValidate(Object world, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null) {
            return "World Remembers validate failed: storage unavailable";
        }

        WorldMemoryDataValidator.ValidationReport report = WorldMemoryDataValidator.validate(state.data());
        String formatted = report.formatDetailed(10)
                + "\nConfig validation " + WorldRemembersLivingLegends.config().validationSummary().compact()
                + "\nCompat registries " + WorldRemembersCompatRegistries.summaryLine();
        if (logger != null) {
            logger.info(report.formatDetailed(64));
        }
        return formatted;
    }

    public static String debugSelfTest(Object world, Logger logger) {
        DiagnosticSummary summary = new DiagnosticSummary();
        LivingLegendsConfig config = WorldRemembersLivingLegends.config();
        checkSelfTestConfig(summary, config);
        checkSelfTestSystems(summary);
        checkSelfTestCommands(summary);
        checkSelfTestCompat(summary);
        checkSelfTestNetworking(summary);
        checkSelfTestNaming(summary);

        WorldRemembersNeoForgeSavedData state = state(world, logger);
        checkSelfTestData(summary, state == null ? null : state.data());

        String message = summary.format("World Remembers self-test", 12);
        if (logger != null) {
            logger.info(summary.format("World Remembers self-test", 64));
        }
        return message;
    }

    public static String debugRepairDryRun(Object world, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null) {
            return "World Remembers repair dryrun failed: storage unavailable";
        }

        WorldMemoryDataValidator.ValidationReport report = WorldMemoryDataValidator.validate(state.data());
        int wouldRemoveOrphanJournalRefs = report.staleVisitedRefs();
        int wouldFillSourceChunks = report.invalidSourceChunks();
        int wouldFixInvalidRadii = report.invalidRadii();
        int wouldReviewNames = report.missingNames();
        String message = "World Remembers repair dryrun"
                + " result=" + ("FAIL".equals(report.result()) ? "WARN" : report.result())
                + " noDataChanged=true"
                + " wouldRemoveOrphanJournalRefs=" + wouldRemoveOrphanJournalRefs
                + " wouldFillMissingSourceChunks=" + wouldFillSourceChunks
                + " wouldFixInvalidRadii=" + wouldFixInvalidRadii
                + " wouldReviewMissingNames=" + wouldReviewNames
                + " errorsRequireManualReview=" + report.errorCount();
        if (logger != null) {
            logger.info(message + "\n" + report.formatDetailed(32));
        }
        return message;
    }

    public static String debugMigration(Object world, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null) {
            return "World Remembers migration debug failed: storage unavailable";
        }
        return "World Remembers data versions "
                + WorldRemembersDataVersions.debugSummary()
                + "\nloadedDataVersion=" + state.data().schemaVersion()
                + "\n" + WorldRemembersLivingLegendsNeoForgeNbt.lastMigrationSummary();
    }

    public static String debugCause(Object world, String dimensionId, int chunkX, int chunkZ, int x, int y, int z, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null) {
            return "World Remembers cause storage is unavailable";
        }

        WorldPos position = new WorldPos(dimensionId, x, y, z);
        NamedPlace nearest = null;
        long nearestDistance = Long.MAX_VALUE;
        for (NamedPlace place : state.data().namedPlaces()) {
            if (place == null || !place.dimensionId().equals(dimensionId)) {
                continue;
            }
            long distance = place.center().squaredDistanceTo(position);
            if (place.contains(position)) {
                nearest = place;
                nearestDistance = 0L;
                break;
            }
            if (distance < nearestDistance) {
                nearest = place;
                nearestDistance = distance;
            }
        }

        String key = WorldMemoryStorageData.chunkStatsKey(dimensionId, chunkX, chunkZ);
        ChunkMemoryStats stats = state.data().chunkStats(dimensionId, chunkX, chunkZ);
        PlaceCause chunkCause = CauseContextResolver.fromStats(
                PlaceType.GENERAL_LANDMARK,
                DeathSiteEnvironment.UNKNOWN,
                stats == null
                        ? PlaceStats.empty()
                        : new PlaceStats(
                        stats.eventCount(),
                        stats.visitCount(),
                        stats.deathCount(),
                        stats.combatEventCount(),
                        stats.buildEventCount(),
                        stats.totalImportance(),
                        0L,
                        stats.lastEventGameTime(),
                        EventType.CUSTOM,
                        stats.eventTypeCounts(),
                        stats.deathSiteEnvironmentCounts(),
                        stats.metadataCounts()
                ),
                "",
                ""
        );
        return "World Remembers cause"
                + " chunk=" + key
                + " chunkCause=" + chunkCause.debugString()
                + (nearest == null
                ? " nearestPlace=none"
                : " nearestPlace=" + nearest.placeIdString()
                + " distanceSquared=" + nearestDistance
                + " placeCause=" + nearest.cause().debugString());
    }

    public static String debugNearby(Object world, String dimensionId, int x, int y, int z, int radius, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null) {
            return "World Remembers nearby storage is unavailable";
        }

        WorldPos position = new WorldPos(dimensionId, x, y, z);
        int searchRadius = Math.max(0, radius);
        long maxDistanceSquared = searchRadius == 0 ? Long.MAX_VALUE : (long) searchRadius * searchRadius;
        List<NamedPlace> nearby = state.data().namedPlaces().stream()
                .filter(place -> place != null && dimensionId.equals(place.dimensionId()))
                .filter(place -> searchRadius == 0 || place.center().squaredDistanceTo(position) <= maxDistanceSquared || place.contains(position))
                .sorted(java.util.Comparator.comparingLong(place -> place.center().squaredDistanceTo(position)))
                .limit(12)
                .toList();

        StringBuilder message = new StringBuilder("World Remembers nearby")
                .append(" player=").append(dimensionId).append("@").append(x).append(",").append(y).append(",").append(z)
                .append(" radius=").append(searchRadius == 0 ? "all" : searchRadius)
                .append(" count=").append(nearby.size());
        for (NamedPlace place : nearby) {
            double distance = Math.sqrt(Math.max(0L, place.center().squaredDistanceTo(position)));
            boolean inside = place.contains(position);
            message.append('\n')
                    .append(place.placeIdString())
                    .append(" type=").append(place.placeType().name())
                    .append(" priority=").append(place.placeType().priorityRank())
                    .append(" distance=").append(formatDistance(distance))
                    .append(" inside=").append(inside)
                    .append(" radius=").append(place.radius())
                    .append(" biomeId=").append(place.biomeId())
                    .append(" biomeGroup=").append(place.biomeGroup())
                    .append(" biomeSource=").append(place.biomeSource())
                    .append(" bounds=").append(place.bounds().boundsIdString())
                    .append(" overlapStatus=").append(inside ? "inside_bounds" : (distance <= place.radius() ? "within_radius" : "outside"))
                    .append(" suppressesGeneralLandmark=").append(place.placeType() != PlaceType.GENERAL_LANDMARK);
        }
        return message.toString();
    }

    public static String debugSpacingHere(Object world, String dimensionId, int x, int y, int z, PlaceType placeType, Logger logger) {
        WorldRemembersNeoForgeSavedData state = state(world, logger);
        if (state == null) {
            return "World Remembers spacing storage is unavailable";
        }

        PlaceType resolvedType = placeType == null ? PlaceType.UNKNOWN : placeType;
        if (resolvedType != PlaceType.GENERAL_LANDMARK) {
            return "World Remembers spacing"
                    + " candidateCenter=" + new WorldPos(dimensionId, x, y, z).blockIdString()
                    + " type=" + resolvedType.name()
                    + " result=allowed"
                    + " reason=spacing_not_configured_for_type"
                    + " note=GENERAL_LANDMARK spacing does not suppress more specific place types.";
        }

        PlaceSpacingRules.GeneralLandmarkSpacingResult result = PlaceSpacingRules.analyzeGeneralLandmark(
                dimensionId,
                x,
                y,
                z,
                state.data(),
                WorldRemembersLivingLegends.config()
        );
        return "World Remembers spacing type=" + resolvedType.name()
                + " " + result.debugDetails();
    }

    private static void checkSelfTestConfig(DiagnosticSummary summary, LivingLegendsConfig config) {
        if (config == null) {
            summary.section("Config", "FAIL", "config is not loaded");
            summary.error("Config object is null.");
            return;
        }
        LivingLegendsConfig.ValidationSummary validation = config.validationSummary();
        if (validation.warnings() > 0) {
            summary.section("Config", "WARN", validation.compact());
            for (String warning : validation.firstWarnings(4)) {
                summary.warn("Config: " + warning);
            }
        } else {
            summary.section("Config", "PASS", validation.compact());
        }
        if (!REQUIRED_STYLE_IDS.contains(BuiltInNameData.builtInStyleId(config.naming.defaultStyle))) {
            summary.error("Default naming style is not one of the accepted built-in styles: " + config.naming.defaultStyle);
        }
        if (config.titleOverlay == null) {
            summary.error("titleOverlay config section is missing.");
        }
        if (config.journal == null) {
            summary.error("journal config section is missing.");
        }
        if (config.candidateDecay == null) {
            summary.error("candidateDecay config section is missing.");
        }
        if (config.generation == null || config.generation.spacing == null) {
            summary.error("generation.spacing config section is missing.");
        }
    }

    private static void checkSelfTestSystems(DiagnosticSummary summary) {
        boolean ok = true;
        for (PlaceType type : PlaceType.values()) {
            PlaceType resolved = PlaceType.fromId(type.idString());
            if (resolved != type) {
                ok = false;
                summary.error("PlaceType id does not round-trip: " + type.name() + " -> " + resolved.name());
            }
        }

        Map<String, NameDataPack> packs = new LinkedHashMap<>();
        for (NameDataPack pack : BuiltInNameData.allPacks()) {
            packs.put(pack.styleId(), pack);
        }
        for (String styleId : REQUIRED_STYLE_IDS) {
            NameDataPack pack = packs.get(styleId);
            if (pack == null) {
                ok = false;
                summary.error("Missing built-in naming style pack: " + styleId);
                continue;
            }
            if (pack.patterns().isEmpty() || pack.tokens().isEmpty()) {
                ok = false;
                summary.error("Built-in naming style pack is empty: " + styleId);
            }
            List<String> nameDataWarnings = NameDataDiagnostics.validate(pack, Set.of());
            if (!nameDataWarnings.isEmpty()) {
                summary.warn("Name data diagnostics for " + styleId + ": " + nameDataWarnings.getFirst());
            }
        }

        if (ScoreEngine.alphaCandidateTypes().isEmpty()) {
            ok = false;
            summary.error("ScoreEngine candidate type list is empty.");
        }
        summary.section("Systems", ok ? "PASS" : "FAIL", "placeTypes=" + PlaceType.values().length
                + " builtInStyles=" + packs.size()
                + " scoreCandidateTypes=" + ScoreEngine.alphaCandidateTypes().size());
    }

    private static void checkSelfTestCommands(DiagnosticSummary summary) {
        summary.section("Commands", "PASS",
                "/places debug selftest, /places debug validate, /places debug repair dryrun handlers reachable");
    }

    private static void checkSelfTestNetworking(DiagnosticSummary summary) {
        boolean journal = WorldJournalService.networkingRegistered();
        boolean titles = WorldRemembersLivingLegendsNeoForgePlaceTitles.networkingRegistered();
        if (!journal) {
            summary.error("World Journal networking is not registered.");
        }
        if (!titles) {
            summary.error("Place title overlay networking is not registered.");
        }
        summary.section("Networking", journal && titles ? "PASS" : "FAIL",
                "journal=" + journal + " titleOverlay=" + titles);
    }

    private static void checkSelfTestCompat(DiagnosticSummary summary) {
        WorldRemembersCompatRegistries.Summary compat = WorldRemembersCompatRegistries.summary();
        boolean examples = WorldRemembersCompatRegistries.selfTestPassesVanillaExamples();
        if (!examples) {
            summary.error("Compat registries did not resolve required vanilla examples.");
        }
        if (compat.warnings() > 0) {
            summary.section("Compat", examples ? "WARN" : "FAIL", compat.toString());
            for (String warning : WorldRemembersCompatRegistries.warnings().stream().limit(4).toList()) {
                summary.warn("Compat: " + warning);
            }
        } else {
            summary.section("Compat", examples ? "PASS" : "FAIL", compat.toString());
        }
    }

    private static void checkSelfTestNaming(DiagnosticSummary summary) {
        boolean ok = true;
        PlaceCluster cluster = new PlaceCluster(
                "selftest_death_site_surface",
                PlaceType.DEATH_SITE,
                "minecraft:overworld",
                DeathSiteEnvironment.SURFACE,
                0,
                64,
                0,
                64,
                64,
                32,
                0.0,
                0.0,
                0L,
                0L,
                false,
                PlaceType.DEATH_SITE.priorityRank(),
                "minecraft:overworld@chunk:0,0",
                PlaceStats.empty(),
                List.of("minecraft:overworld@chunk:0,0")
        );

        for (int index = 0; index < REQUIRED_STYLE_IDS.size(); index++) {
            String styleId = REQUIRED_STYLE_IDS.get(index);
            NameDataPack pack = BuiltInNameData.packForStyle(styleId);
            NameGenerationDiagnostics diagnostics = new NameGenerationDiagnostics();
            NameRecipe recipe = NameGenerator.generate(
                    cluster,
                    PlaceType.DEATH_SITE,
                    DeathSiteEnvironment.SURFACE,
                    0x5E1F7E57L + index,
                    pack,
                    List.of(),
                    diagnostics
            );
            String resolved = WorldRemembersLivingLegendsNeoForgeNameResolver.resolveToString(recipe);
            if (!styleId.equals(recipe.styleId())) {
                ok = false;
                summary.error("Naming style " + styleId + " generated recipe with style " + recipe.styleId() + ".");
            }
            if (recipe.patternKey().isBlank()) {
                ok = false;
                summary.error("Naming style " + styleId + " generated a blank pattern key.");
            }
            if (resolved == null || resolved.isBlank()) {
                ok = false;
                summary.error("Naming style " + styleId + " resolved to a blank name.");
            } else if (looksTechnical(resolved)) {
                ok = false;
                summary.error("Naming style " + styleId + " resolved to a technical-looking name: " + resolved);
            }
            if (diagnostics.selectedPatternSource() == NamePatternSource.SAFE_FALLBACK) {
                ok = false;
                summary.warn("Naming style " + styleId + " used fallback in self-test: " + diagnostics.summary());
            }
        }
        summary.section("Naming", ok ? "PASS" : "WARN", "sample=DEATH_SITE/SURFACE styles=" + REQUIRED_STYLE_IDS.size());
    }

    private static void checkSelfTestData(DiagnosticSummary summary, WorldMemoryStorageData data) {
        if (data == null) {
            summary.section("Data", "FAIL", "storage unavailable");
            summary.error("World memory storage is unavailable.");
            return;
        }
        String detail = "worldStateVersion=" + data.schemaVersion()
                + " namedPlaces=" + data.namedPlaceCount()
                + " chunkStats=" + data.chunkStatsCount()
                + " deletedMarkers=" + data.deletedPlaceMarkerCount()
                + " journalVisitedPlayers=" + data.discoveredPlaceIdsByPlayer().size();
        if (data.schemaVersion() < WorldRemembersDataVersions.CURRENT_WORLD_STATE_VERSION) {
            summary.section("Data", "WARN", detail);
            summary.warn("World data schema is older than current version: " + data.schemaVersion());
            return;
        }
        if (data.namedPlaceCount() <= 1000 && data.chunkStatsCount() <= 5000) {
            WorldMemoryDataValidator.ValidationReport report = WorldMemoryDataValidator.validate(data);
            if ("FAIL".equals(report.result())) {
                summary.section("Data", "FAIL", detail + " " + report.format());
                summary.error("World data validation failed: errors=" + report.errorCount());
            } else if ("WARN".equals(report.result())) {
                summary.section("Data", "WARN", detail + " " + report.format());
                summary.warn("World data validation has warnings=" + report.warningCount());
            } else {
                summary.section("Data", "PASS", detail);
            }
        } else {
            summary.section("Data", "WARN", detail + " deepScan=skipped_large_world");
            summary.warn("Self-test skipped deep data validation because the world data is large; run /places debug validate for a full scan.");
        }
    }

    private static boolean looksTechnical(String resolved) {
        if (NameTextSafety.looksBrokenOrTechnical(resolved)) {
            return true;
        }
        String value = resolved == null ? "" : resolved.trim().toLowerCase(Locale.ROOT);
        return value.isBlank()
                || value.contains("living_legends.")
                || value.contains("minecraft:")
                || value.contains("name.pattern")
                || value.contains("name.token");
    }

    private static RegenerateResult regenerateOne(
            WorldMemoryStorageData data,
            NamedPlace place,
            boolean force,
            long gameTime
    ) {
        if (place.manuallyRenamed() && !force) {
            return new RegenerateResult(
                    false,
                    "World Remembers regenerate skipped id=" + place.placeIdString()
                            + " reason=manual_name use_force=true"
            );
        }

        String requestedStyle = styleForRegeneration(place);
        NameDataPack nameData = BuiltInNameData.packForStyle(
                requestedStyle,
                WorldRemembersLivingLegends.config().naming.enabledStyles,
                WorldRemembersLivingLegends.config().naming.allowMixedStyleTokens
        );
        NameContext context = NameContext.from(place.placeType(), place.environment(), place.cause(), nameData.styleId());
        NameGenerationDiagnostics diagnostics = new NameGenerationDiagnostics();
        long seed = regenerationSeed(place, gameTime, force);
        List<NameRecipe> nearbyRecipes = nearbyRecipes(data, place);
        NameRecipe newRecipe = NameGenerator.generate(null, context, seed, nameData, nearbyRecipes, diagnostics);
        NameRecipe oldRecipe = place.nameRecipe();
        NamedPlace updated = place.withGeneratedNameRecipe(newRecipe, gameTime, force);
        boolean changed = data.upsertNamedPlace(updated);
        return new RegenerateResult(
                changed,
                "World Remembers regenerate id=" + place.placeIdString()
                        + " changed=" + changed
                        + " force=" + force
                        + " oldPatternKey=" + oldRecipe.patternKey()
                        + " oldSignature=" + oldRecipe.recipeSignature()
                        + " newPatternKey=" + newRecipe.patternKey()
                        + " newSignature=" + newRecipe.recipeSignature()
                        + " selectedPatternSource=" + diagnostics.selectedPatternSource().name()
                        + " style=" + newRecipe.styleId()
        );
    }

    private static List<NameRecipe> nearbyRecipes(WorldMemoryStorageData data, NamedPlace place) {
        int radius = Math.max(0, WorldRemembersLivingLegends.config().naming.duplicateNameAvoidanceRadiusBlocks);
        long radiusSquared = (long) radius * radius;
        return data.namedPlaces().stream()
                .filter(candidate -> candidate != null && !candidate.placeIdString().equals(place.placeIdString()))
                .filter(candidate -> candidate.dimensionId().equals(place.dimensionId()))
                .filter(candidate -> radius == 0 || candidate.center().squaredDistanceTo(place.center()) <= radiusSquared)
                .map(NamedPlace::nameRecipe)
                .toList();
    }

    private static String styleForRegeneration(NamedPlace place) {
        if (WorldRemembersLivingLegends.config().naming.existingPlacesKeepOriginalStyle
                && place.nameRecipe() != null
                && !place.nameRecipe().styleId().isBlank()) {
            return place.nameRecipe().styleId();
        }
        String configured = WorldRemembersLivingLegends.config().naming.defaultStyle;
        return configured == null || configured.isBlank() ? BuiltInNameData.DEFAULT_STYLE_ID : configured;
    }

    private static long regenerationSeed(NamedPlace place, long gameTime, boolean force) {
        long existingSeed = place.nameRecipe() == null ? 0L : place.nameRecipe().seed();
        long salt = force ? 0x5F3759DFL : 0x1F123BB5L;
        return existingSeed + gameTime + salt + place.placeIdString().hashCode();
    }

    private static Map<String, Object> placeToMap(NamedPlace place) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("namedPlaceVersion", WorldRemembersDataVersions.CURRENT_NAMED_PLACE_VERSION);
        values.put("id", place.placeIdString());
        values.put("type", place.placeType().idString());
        values.put("environment", place.environment().idString());
        values.put("dimension", place.dimensionId());
        values.put("center", worldPosToMap(place.center()));
        values.put("radius", place.radius());
        values.put("score", place.score());
        values.put("createdGameTime", place.createdAtGameTime());
        values.put("lastUpdatedGameTime", place.lastUpdatedGameTime());
        values.put("sourceChunks", place.sourceChunks());
        values.put("nameRecipe", nameRecipeToMap(place.nameRecipe()));
        values.put("rarity", place.rarity().idString());
        values.put("bounds", boundsToMap(place.bounds()));
        values.put("stats", statsToMap(place.stats()));
        values.put("structureId", place.structureId());
        values.put("firstDiscoveryKey", place.firstDiscoveryKey());
        values.put("cause", causeToMap(place.cause()));
        values.put("biomeId", place.biomeId());
        values.put("dominantBiomeId", place.dominantBiomeId());
        values.put("biomeGroup", place.biomeGroup());
        values.put("biomeTheme", place.biomeTheme());
        values.put("biomeSource", place.biomeSource());
        values.put("manualName", place.manualName());
        values.put("manuallyRenamed", place.manuallyRenamed());
        return values;
    }

    private static NamedPlace placeFromMap(Map<String, Object> values) {
        PlaceType type = PlaceType.fromId(stringValue(values, "type", stringValue(values, "placeType", "custom")));
        String dimension = stringValue(values, "dimension", stringValue(values, "dimensionId", "minecraft:overworld"));
        WorldPos center = worldPosFromMap(objectValue(values, "center"), dimension);
        String id = stringValue(values, "id", stringValue(values, "placeId", ""));
        if (id.isBlank()) {
            id = "legacy_import_" + type.idString()
                    + "_" + dimension.replace(':', '_').replace('/', '_')
                    + "_" + center.x() + "_" + center.y() + "_" + center.z();
        }
        DeathSiteEnvironment environment = DeathSiteEnvironment.fromId(stringValue(values, "environment", "unknown"));
        int radius = intValue(values, "radius", 24);
        if (radius <= 0) {
            radius = 24;
        }
        double score = doubleValue(values, "score", 0.0);
        long created = longValue(values, "createdGameTime", 0L);
        long updated = longValue(values, "lastUpdatedGameTime", created);
        NameRecipe recipe = nameRecipeFromMap(
                objectValue(values, "nameRecipe"),
                stringValue(values, "displayName", stringValue(values, "legacyDisplayName", "")),
                stringValue(values, "nameStyle", BuiltInNameData.DEFAULT_STYLE_ID)
        );
        PlaceRarity rarity = PlaceRarity.fromId(stringValue(values, "rarity", "common"));
        PlaceBounds bounds = boundsFromMap(objectValue(values, "bounds"), dimension, center, radius);
        PlaceStats stats = statsFromMap(objectValue(values, "stats"));
        PlaceCause cause = causeFromMap(objectValue(values, "cause"));
        return new NamedPlace(
                id,
                type,
                environment,
                dimension,
                center,
                radius,
                score,
                created,
                updated,
                sourceChunksFromMap(values.get("sourceChunks"), center),
                recipe,
                rarity,
                bounds,
                stats,
                stringValue(values, "structureId", ""),
                stringValue(values, "firstDiscoveryKey", ""),
                cause,
                stringValue(values, "biomeId", ""),
                stringValue(values, "dominantBiomeId", ""),
                stringValue(values, "biomeGroup", ""),
                stringValue(values, "biomeTheme", ""),
                stringValue(values, "biomeSource", ""),
                stringValue(values, "manualName", ""),
                booleanValue(values, "manuallyRenamed", false)
        );
    }

    private static String importDuplicateReason(WorldMemoryStorageData data, NamedPlace imported) {
        if (imported == null) {
            return "malformed_place";
        }
        if (imported.placeType() == PlaceType.FIRST_DISCOVERY && !imported.firstDiscoveryKey().isBlank()) {
            for (NamedPlace existing : data.namedPlaces()) {
                if (existing != null
                        && existing.placeType() == PlaceType.FIRST_DISCOVERY
                        && imported.firstDiscoveryKey().equals(existing.firstDiscoveryKey())) {
                    return "semantic_duplicate firstDiscoveryKey=" + imported.firstDiscoveryKey()
                            + " existingId=" + existing.placeIdString();
                }
            }
        }

        for (NamedPlace existing : data.namedPlaces()) {
            if (existing == null || !existing.dimensionId().equals(imported.dimensionId())) {
                continue;
            }
            if (sameSemanticPlace(existing, imported) && placesOverlap(existing, imported)) {
                return "spatial_duplicate existingId=" + existing.placeIdString()
                        + " existingType=" + existing.placeType().name()
                        + " importedType=" + imported.placeType().name();
            }
        }
        return "";
    }

    private static boolean sameSemanticPlace(NamedPlace existing, NamedPlace imported) {
        if (existing.placeType() != imported.placeType()) {
            return false;
        }
        if (existing.placeType() == PlaceType.DEATH_SITE && existing.environment() != imported.environment()) {
            return false;
        }
        if (!existing.firstDiscoveryKey().isBlank() || !imported.firstDiscoveryKey().isBlank()) {
            return existing.firstDiscoveryKey().equals(imported.firstDiscoveryKey());
        }
        if (!existing.structureId().isBlank() || !imported.structureId().isBlank()) {
            return existing.structureId().equals(imported.structureId());
        }
        PlaceCauseType existingCause = existing.cause() == null ? PlaceCauseType.UNKNOWN : existing.cause().causeType();
        PlaceCauseType importedCause = imported.cause() == null ? PlaceCauseType.UNKNOWN : imported.cause().causeType();
        return existingCause == PlaceCauseType.UNKNOWN
                || importedCause == PlaceCauseType.UNKNOWN
                || existingCause == importedCause;
    }

    private static boolean placesOverlap(NamedPlace first, NamedPlace second) {
        int mergeDistance = Math.max(
                PlaceGenerationLimits.maxMergeDistanceBlocks(first.placeType(), WorldRemembersLivingLegends.config()),
                PlaceGenerationLimits.maxMergeDistanceBlocks(second.placeType(), WorldRemembersLivingLegends.config())
        );
        int distance = Math.max(mergeDistance, Math.max(first.radius(), second.radius()));
        long distanceSquared = first.center().squaredDistanceTo(second.center());
        return distanceSquared <= (long) distance * distance
                || first.bounds().expanded(distance, Math.max(16, distance)).contains(second.center())
                || second.bounds().expanded(distance, Math.max(16, distance)).contains(first.center());
    }

    private static Map<String, Object> worldPosToMap(WorldPos pos) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("dimension", pos.dimensionId());
        values.put("x", pos.x());
        values.put("y", pos.y());
        values.put("z", pos.z());
        return values;
    }

    private static WorldPos worldPosFromMap(Map<String, Object> values, String fallbackDimension) {
        String dimension = stringValue(values, "dimension", stringValue(values, "dimensionId", fallbackDimension));
        return new WorldPos(
                nonBlank(dimension, fallbackDimension),
                intValue(values, "x", 0),
                intValue(values, "y", 64),
                intValue(values, "z", 0)
        );
    }

    private static List<String> sourceChunksFromMap(Object value, WorldPos center) {
        List<String> chunks = stringList(value);
        if (!chunks.isEmpty()) {
            return chunks;
        }
        return center == null ? List.of() : List.of(center.chunkIdString());
    }

    private static Map<String, Object> nameRecipeToMap(NameRecipe recipe) {
        NameRecipe resolved = recipe == null ? NameRecipe.empty() : recipe;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("nameRecipeVersion", WorldRemembersDataVersions.CURRENT_NAME_RECIPE_VERSION);
        values.put("style", resolved.styleId());
        values.put("patternKey", resolved.patternKey());
        values.put("tokenIds", resolved.selectedTokenIds());
        List<String> forms = new ArrayList<>();
        for (NameTokenForm form : resolved.requestedTokenForms()) {
            forms.add(form.idString());
        }
        values.put("forms", forms);
        values.put("seed", resolved.seed());
        values.put("signature", resolved.recipeSignature());
        values.put("fallbackResolvedName", resolved.fallbackResolvedName());
        return values;
    }

    private static NameRecipe nameRecipeFromMap(Map<String, Object> values, String legacyDisplayName, String fallbackStyle) {
        if (values.isEmpty()) {
            if (!normalize(legacyDisplayName).isBlank()) {
                return new NameRecipe(
                        normalize(fallbackStyle).isBlank() ? BuiltInNameData.DEFAULT_STYLE_ID : fallbackStyle,
                        "living_legends.name.pattern.legacy",
                        List.of(),
                        List.of(),
                        0L,
                        legacyDisplayName
                );
            }
            return NameRecipe.empty();
        }
        String patternKey = stringValue(values, "patternKey", "");
        if (patternKey.isBlank() && !normalize(legacyDisplayName).isBlank()) {
            return new NameRecipe(
                    normalize(fallbackStyle).isBlank() ? BuiltInNameData.DEFAULT_STYLE_ID : fallbackStyle,
                    "living_legends.name.pattern.legacy",
                    List.of(),
                    List.of(),
                    longValue(values, "seed", 0L),
                    legacyDisplayName
            );
        }
        List<NameTokenForm> forms = new ArrayList<>();
        for (String formId : stringList(values.get("forms"))) {
            forms.add(NameTokenForm.fromId(formId));
        }
        return new NameRecipe(
                stringValue(values, "style", stringValue(values, "styleId", BuiltInNameData.DEFAULT_STYLE_ID)),
                patternKey.isBlank() ? "living_legends.name.pattern.unknown" : patternKey,
                stringList(values.get("tokenIds")),
                forms,
                longValue(values, "seed", 0L),
                stringValue(values, "fallbackResolvedName", "")
        );
    }

    private static Map<String, Object> boundsToMap(PlaceBounds bounds) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("shape", bounds.shape().idString());
        values.put("dimension", bounds.dimensionId());
        values.put("centerX", bounds.centerX());
        values.put("centerY", bounds.centerY());
        values.put("centerZ", bounds.centerZ());
        values.put("radius", bounds.radius());
        values.put("minX", bounds.minX());
        values.put("minY", bounds.minY());
        values.put("minZ", bounds.minZ());
        values.put("maxX", bounds.maxX());
        values.put("maxY", bounds.maxY());
        values.put("maxZ", bounds.maxZ());
        return values;
    }

    private static PlaceBounds boundsFromMap(
            Map<String, Object> values,
            String fallbackDimension,
            WorldPos fallbackCenter,
            int fallbackRadius
    ) {
        if (values.isEmpty()) {
            return PlaceBounds.around(fallbackCenter, fallbackRadius, Math.max(8, fallbackRadius));
        }
        return new PlaceBounds(
                nonBlank(stringValue(values, "dimension", fallbackDimension), fallbackDimension),
                intValue(values, "minX", fallbackCenter.x() - fallbackRadius),
                intValue(values, "minY", fallbackCenter.y() - Math.max(8, fallbackRadius)),
                intValue(values, "minZ", fallbackCenter.z() - fallbackRadius),
                intValue(values, "maxX", fallbackCenter.x() + fallbackRadius),
                intValue(values, "maxY", fallbackCenter.y() + Math.max(8, fallbackRadius)),
                intValue(values, "maxZ", fallbackCenter.z() + fallbackRadius),
                PlaceBounds.Shape.fromId(stringValue(values, "shape", "cylinder")),
                intValue(values, "centerX", fallbackCenter.x()),
                intValue(values, "centerY", fallbackCenter.y()),
                intValue(values, "centerZ", fallbackCenter.z()),
                intValue(values, "radius", fallbackRadius)
        );
    }

    private static Map<String, Object> statsToMap(PlaceStats stats) {
        PlaceStats resolved = stats == null ? PlaceStats.empty() : stats;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("eventCount", resolved.eventCount());
        values.put("visitCount", resolved.visitCount());
        values.put("deathCount", resolved.deathCount());
        values.put("combatEventCount", resolved.combatEventCount());
        values.put("buildEventCount", resolved.buildEventCount());
        values.put("totalImportance", resolved.totalImportance());
        values.put("firstEventGameTime", resolved.firstEventGameTime());
        values.put("lastEventGameTime", resolved.lastEventGameTime());
        values.put("lastEventType", resolved.lastEventType().idString());
        values.put("eventTypeCounts", longMapToMap(resolved.eventTypeCounts()));
        values.put("deathSiteEnvironmentCounts", longMapToMap(resolved.deathSiteEnvironmentCounts()));
        values.put("metadataCounts", longMapToMap(resolved.metadataCounts()));
        return values;
    }

    private static PlaceStats statsFromMap(Map<String, Object> values) {
        if (values.isEmpty()) {
            return PlaceStats.empty();
        }
        return new PlaceStats(
                longValue(values, "eventCount", 0L),
                longValue(values, "visitCount", 0L),
                longValue(values, "deathCount", 0L),
                longValue(values, "combatEventCount", 0L),
                longValue(values, "buildEventCount", 0L),
                doubleValue(values, "totalImportance", 0.0),
                longValue(values, "firstEventGameTime", 0L),
                longValue(values, "lastEventGameTime", 0L),
                EventType.fromId(stringValue(values, "lastEventType", "custom")),
                longMapFromObject(values.get("eventTypeCounts")),
                longMapFromObject(values.get("deathSiteEnvironmentCounts")),
                longMapFromObject(values.get("metadataCounts"))
        );
    }

    private static Map<String, Object> causeToMap(PlaceCause cause) {
        PlaceCause resolved = cause == null ? PlaceCause.unknown() : cause;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("causeType", resolved.causeType().idString());
        values.put("primaryEventType", resolved.primaryEventType().idString());
        values.put("firstDiscoveryKey", resolved.firstDiscoveryKey());
        values.put("discoveryKind", resolved.discoveryKind());
        values.put("structureId", resolved.structureId());
        values.put("blockId", resolved.blockId());
        values.put("entityId", resolved.entityId());
        values.put("bossId", resolved.bossId());
        values.put("dominantMobType", resolved.dominantMobType());
        values.put("dominantPassiveMobType", resolved.dominantPassiveMobType());
        values.put("dominantHostileMobType", resolved.dominantHostileMobType());
        values.put("dominantNeutralMobType", resolved.dominantNeutralMobType());
        values.put("dominantValuableBlock", resolved.dominantValuableBlock());
        values.put("portalType", resolved.portalType());
        values.put("fromDimension", resolved.fromDimension());
        values.put("toDimension", resolved.toDimension());
        values.put("biomeId", resolved.biomeId());
        values.put("dominantBiomeId", resolved.dominantBiomeId());
        values.put("biomeGroup", resolved.biomeGroup());
        values.put("biomeTheme", resolved.biomeTheme());
        values.put("biomeSource", resolved.biomeSource());
        values.put("deathCause", resolved.deathCause());
        values.put("petName", resolved.petName());
        values.put("petType", resolved.petType());
        values.put("namedMobName", resolved.namedMobName());
        values.put("namedMobType", resolved.namedMobType());
        values.put("evidenceCounts", longMapToMap(resolved.evidenceCounts()));
        return values;
    }

    private static PlaceCause causeFromMap(Map<String, Object> values) {
        if (values.isEmpty()) {
            return PlaceCause.unknown();
        }
        return new PlaceCause(
                PlaceCauseType.fromId(stringValue(values, "causeType", "unknown")),
                EventType.fromId(stringValue(values, "primaryEventType", "custom")),
                stringValue(values, "firstDiscoveryKey", ""),
                stringValue(values, "discoveryKind", ""),
                stringValue(values, "structureId", ""),
                stringValue(values, "blockId", ""),
                stringValue(values, "entityId", ""),
                stringValue(values, "bossId", ""),
                stringValue(values, "dominantMobType", ""),
                stringValue(values, "dominantPassiveMobType", ""),
                stringValue(values, "dominantHostileMobType", ""),
                stringValue(values, "dominantNeutralMobType", ""),
                stringValue(values, "dominantValuableBlock", ""),
                stringValue(values, "portalType", ""),
                stringValue(values, "fromDimension", ""),
                stringValue(values, "toDimension", ""),
                stringValue(values, "biomeId", ""),
                stringValue(values, "dominantBiomeId", ""),
                stringValue(values, "biomeGroup", ""),
                stringValue(values, "biomeTheme", ""),
                stringValue(values, "biomeSource", ""),
                stringValue(values, "deathCause", ""),
                stringValue(values, "petName", ""),
                stringValue(values, "petType", ""),
                stringValue(values, "namedMobName", ""),
                stringValue(values, "namedMobType", ""),
                longMapFromObject(values.get("evidenceCounts"))
        );
    }

    private static Map<String, Object> longMapToMap(Map<String, Long> counts) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (counts == null) {
            return values;
        }
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                values.put(entry.getKey(), Math.max(0L, entry.getValue()));
            }
        }
        return values;
    }

    private static Map<String, Long> longMapFromObject(Object value) {
        Map<String, Long> values = new LinkedHashMap<>();
        if (!(value instanceof Map<?, ?> rawMap)) {
            return values;
        }
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            String key = normalize(entry.getKey());
            long count = numberValue(entry.getValue(), 0L).longValue();
            if (!key.isBlank() && count > 0L) {
                values.put(key, values.getOrDefault(key, 0L) + count);
            }
        }
        return values;
    }

    private static Map<String, Object> objectValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Map<?, ?> rawMap ? castMap(rawMap) : Map.of();
    }

    private static Map<String, Object> castMap(Map<?, ?> rawMap) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() != null) {
                values.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return values;
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof Iterable<?> values)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object entry : values) {
            String normalized = normalize(entry);
            if (!normalized.isBlank()) {
                result.add(normalized);
            }
        }
        return result;
    }

    private static String stringValue(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }

    private static int intValue(Map<String, Object> map, String key, int fallback) {
        return numberValue(map.get(key), fallback).intValue();
    }

    private static long longValue(Map<String, Object> map, String key, long fallback) {
        return numberValue(map.get(key), fallback).longValue();
    }

    private static double doubleValue(Map<String, Object> map, String key, double fallback) {
        return numberValue(map.get(key), fallback).doubleValue();
    }

    private static boolean booleanValue(Map<String, Object> map, String key, boolean fallback) {
        Object value = map.get(key);
        return value instanceof Boolean bool ? bool : fallback;
    }

    private static Number numberValue(Object value, Number fallback) {
        if (value instanceof Number number) {
            return number;
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static String normalize(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String nonBlank(String value, String fallback) {
        String normalized = normalize(value);
        return normalized.isBlank() ? fallback : normalized;
    }

    private static Path resolveGameDir() {
        try {
            Path gameDir = FMLPaths.GAMEDIR.get();
            if (gameDir != null) {
                return gameDir;
            }
        } catch (RuntimeException | LinkageError ignored) {
            // Current working directory is a safe fallback for command export/import.
        }
        return Path.of(".");
    }

    private static String formatDistance(double value) {
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 0.000_001) {
            return Long.toString((long) rounded);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private record RegenerateResult(boolean changed, String message) {
    }

    private static WorldRemembersNeoForgeSavedData state(Object world, Logger logger) {
        ServerLevel storageWorld = storageWorld(world);
        if (storageWorld == null) {
            warnOnce(logger, "Could not access World Remembers state: no server world is available");
            return null;
        }

        WorldRemembersNeoForgeSavedData state = storageWorld
                .getDataStorage()
                .computeIfAbsent(WorldRemembersNeoForgeSavedData.FACTORY, STATE_ID);
        if (state != null) {
            logLoadedOnce(state, logger);
        }
        return state;
    }

    private static ServerLevel storageWorld(Object world) {
        if (world instanceof ServerLevel serverLevel) {
            MinecraftServer server = serverLevel.getServer();
            ServerLevel overworld = server == null ? null : server.overworld();
            return overworld == null ? serverLevel : overworld;
        }
        return overworld(world);
    }

    private static ServerLevel overworld(Object server) {
        return server instanceof MinecraftServer minecraftServer ? minecraftServer.overworld() : null;
    }

    private static void logLoadedOnce(WorldRemembersNeoForgeSavedData state, Logger logger) {
        if (state == null || logger == null || !LOGGED_LOADED_STATES.add(System.identityHashCode(state))) {
            return;
        }

        WorldMemoryStorageData data = state.data();
        logger.info("Loaded " + data.chunkStatsCount()
                + " chunk memory stats, "
                + data.namedPlaceCount()
                + " named places and "
                + data.firstEventCount()
                + " first events");
        logger.info(WorldRemembersLivingLegendsNeoForgeNbt.lastMigrationSummary());
    }

    private static void markDirty(WorldRemembersNeoForgeSavedData state, String reason, Logger logger) {
        if (state != null) {
            state.markChanged();
        }
        LivingLegendsConfig config = WorldRemembersLivingLegends.config();
        if (logger != null && config != null && config.debug != null && config.debug.enabled) {
            logger.info("World Remembers state marked dirty after " + reason);
        }
    }

    private static void markDirty(WorldRemembersNeoForgeSavedData state, WorldMemoryEvent event, Logger logger) {
        if (state != null) {
            state.markChanged();
        }
        LivingLegendsConfig config = WorldRemembersLivingLegends.config();
        if (logger != null && config != null && config.debug != null && config.debug.enabled && event != null) {
            logger.info("World Remembers state marked dirty after event "
                    + event.eventType().idString()
                    + " at "
                    + event.position().chunkIdString());
        }
    }

    private static void logGeneratedPlace(NamedPlace place, Logger logger) {
        LivingLegendsConfig config = WorldRemembersLivingLegends.config();
        if (place != null && logger != null && config != null && config.debug != null && config.debug.enabled) {
            logger.info("World Remembers NeoForge generated place id="
                    + place.placeIdString()
                    + " type="
                    + place.placeType().name());
        }
    }

    private static void onGeneratedPlace(MinecraftServer server, NamedPlace place, Logger logger) {
        logGeneratedPlace(place, logger);
        WorldRemembersLivingLegendsNeoForgeNotifications.notifyPlaceCreated(server, place, logger);
        WorldRemembersLivingLegendsNeoForgePlaceTitles.onPlaceCreated(server, place, logger);
    }

    private static long gameTime(Object world) {
        if (world instanceof Level minecraftWorld) {
            return minecraftWorld.getGameTime();
        }
        ServerLevel overworld = overworld(world);
        return overworld == null ? 0L : overworld.getGameTime();
    }

    private static void warnOnce(Logger logger, String message) {
        if (!warningLogged && logger != null) {
            warningLogged = true;
            logger.warn(message);
        }
    }

    private static final class DiagnosticSummary {
        private final Map<String, Section> sections = new LinkedHashMap<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

        void section(String name, String status, String detail) {
            sections.put(name, new Section(status, detail == null ? "" : detail));
        }

        void warn(String warning) {
            warnings.add(warning == null || warning.isBlank() ? "Unknown warning." : warning);
        }

        void error(String error) {
            errors.add(error == null || error.isBlank() ? "Unknown error." : error);
        }

        String result() {
            if (!errors.isEmpty() || sections.values().stream().anyMatch(section -> "FAIL".equals(section.status()))) {
                return "FAIL";
            }
            if (!warnings.isEmpty() || sections.values().stream().anyMatch(section -> "WARN".equals(section.status()))) {
                return "WARN";
            }
            return "PASS";
        }

        String format(String title, int maxIssues) {
            StringBuilder builder = new StringBuilder(title)
                    .append(": ")
                    .append(result());
            if (!warnings.isEmpty() || !errors.isEmpty()) {
                builder.append(", ")
                        .append(warnings.size())
                        .append(" warnings, ")
                        .append(errors.size())
                        .append(" errors");
            }
            for (Map.Entry<String, Section> entry : sections.entrySet()) {
                builder.append('\n')
                        .append(entry.getKey())
                        .append(": ")
                        .append(entry.getValue().status());
                if (!entry.getValue().detail().isBlank()) {
                    builder.append(" - ").append(entry.getValue().detail());
                }
            }

            int limit = Math.max(0, maxIssues);
            int emitted = 0;
            for (String error : errors) {
                if (emitted >= limit) {
                    break;
                }
                builder.append('\n').append("ERROR: ").append(error);
                emitted++;
            }
            for (String warning : warnings) {
                if (emitted >= limit) {
                    break;
                }
                builder.append('\n').append("WARN: ").append(warning);
                emitted++;
            }
            int hidden = errors.size() + warnings.size() - emitted;
            if (hidden > 0) {
                builder.append('\n').append("... ").append(hidden).append(" more diagnostics in latest.log.");
            }
            return builder.toString();
        }

        private record Section(String status, String detail) {
        }
    }

    private static final class PersistentEventStore implements EventCollector.EventStore {
        private final WorldMemoryStorageData data;
        private final DirtyChunkScoreQueue scoreQueue;
        private final WorldRemembersNeoForgeSavedData state;
        private final Logger logger;

        private PersistentEventStore(
                WorldMemoryStorageData data,
                DirtyChunkScoreQueue scoreQueue,
                WorldRemembersNeoForgeSavedData state,
                Logger logger
        ) {
            this.data = data;
            this.scoreQueue = scoreQueue;
            this.state = state;
            this.logger = logger;
        }

        @Override
        public WorldMemoryStorageData data() {
            return data;
        }

        @Override
        public void markDirty(WorldMemoryEvent event, ChunkMemoryStats changedChunkStats) {
            if (changedChunkStats != null) {
                scoreQueue.enqueue(changedChunkStats);
            }
            WorldRemembersLivingLegendsNeoForgeStorage.markDirty(state, event, logger);
        }
    }
}
