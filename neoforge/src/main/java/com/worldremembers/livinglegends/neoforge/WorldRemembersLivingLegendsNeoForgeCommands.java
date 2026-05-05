package com.worldremembers.livinglegends.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.worldremembers.livinglegends.BuiltInNameData;
import com.worldremembers.livinglegends.DeathSiteEnvironment;
import com.worldremembers.livinglegends.DeletedPlaceMarker;
import com.worldremembers.livinglegends.EventCollector;
import com.worldremembers.livinglegends.EventType;
import com.worldremembers.livinglegends.NameContext;
import com.worldremembers.livinglegends.NameDataPack;
import com.worldremembers.livinglegends.NameGenerationDiagnostics;
import com.worldremembers.livinglegends.NameGenerator;
import com.worldremembers.livinglegends.NameRecipe;
import com.worldremembers.livinglegends.NameToken;
import com.worldremembers.livinglegends.NameTokenForm;
import com.worldremembers.livinglegends.NamedPlace;
import com.worldremembers.livinglegends.PlaceBounds;
import com.worldremembers.livinglegends.PlaceCause;
import com.worldremembers.livinglegends.PlaceCauseType;
import com.worldremembers.livinglegends.PlaceCluster;
import com.worldremembers.livinglegends.PlaceRarity;
import com.worldremembers.livinglegends.PlaceStats;
import com.worldremembers.livinglegends.PlaceType;
import com.worldremembers.livinglegends.RuntimeNameFormatter;
import com.worldremembers.livinglegends.VanillaMobThemeRegistry;
import com.worldremembers.livinglegends.WorldRemembersCompatRegistries;
import com.worldremembers.livinglegends.WorldPos;
import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import com.worldremembers.livinglegends.config.LivingLegendsConfig;
import com.worldremembers.livinglegends.config.LivingLegendsConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class WorldRemembersLivingLegendsNeoForgeCommands {
    private static final int CHUNK_SIZE = 16;
    private static final int MAX_INDEX_SESSIONS = 64;
    private static final Map<String, List<String>> LAST_LIST_INDEXES = new LinkedHashMap<>();

    private WorldRemembersLivingLegendsNeoForgeCommands() {
    }

    public static void register(Logger logger) {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> registerPlacesCommand(event.getDispatcher(), logger));
    }

    private static void registerPlacesCommand(CommandDispatcher<CommandSourceStack> dispatcher, Logger logger) {
        dispatcher.register(Commands.literal("places")
                .then(Commands.literal("list")
                        .executes(context -> runList(context.getSource(), logger, null, 1))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(context -> runList(
                                        context.getSource(),
                                        logger,
                                        null,
                                        IntegerArgumentType.getInteger(context, "page")
                                )))
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(placeTypeIds(), builder))
                                .executes(context -> runList(
                                        context.getSource(),
                                        logger,
                                        StringArgumentType.getString(context, "type"),
                                        1
                                ))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(context -> runList(
                                                context.getSource(),
                                                logger,
                                                StringArgumentType.getString(context, "type"),
                                                IntegerArgumentType.getInteger(context, "page")
                                        )))))
                .then(Commands.literal("nearest")
                        .executes(context -> runNearest(context.getSource(), logger, null))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                                .executes(context -> runNearest(
                                        context.getSource(),
                                        logger,
                                        IntegerArgumentType.getInteger(context, "radius")
                                ))))
                .then(Commands.literal("info")
                        .then(Commands.argument("target", StringArgumentType.greedyString())
                                .executes(context -> runInfo(
                                        context.getSource(),
                                        logger,
                                        StringArgumentType.getString(context, "target")
                                ))))
                .then(Commands.literal("create")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(placeTypeIds(), builder))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                .executes(context -> runCreate(
                                                        context.getSource(),
                                                        logger,
                                                        StringArgumentType.getString(context, "type"),
                                                        IntegerArgumentType.getInteger(context, "radius"),
                                                        StringArgumentType.getString(context, "name")
                                                ))))))
                .then(Commands.literal("rename")
                        .then(Commands.argument("target_and_name", StringArgumentType.greedyString())
                                .executes(context -> runRenameTail(
                                        context.getSource(),
                                        logger,
                                        StringArgumentType.getString(context, "target_and_name")
                                ))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("target", StringArgumentType.greedyString())
                                .executes(context -> runDelete(
                                        context.getSource(),
                                        logger,
                                        StringArgumentType.getString(context, "target")
                                ))))
                .then(Commands.literal("regenerate")
                        .then(Commands.literal("all")
                                .executes(context -> runRegenerate(context.getSource(), logger, "all", false))
                                .then(Commands.literal("force")
                                        .executes(context -> runRegenerate(context.getSource(), logger, "all", true))))
                        .then(Commands.argument("targetAndForce", StringArgumentType.greedyString())
                                .executes(context -> runRegenerate(
                                        context.getSource(),
                                        logger,
                                        regenerateTailTarget(StringArgumentType.getString(context, "targetAndForce")),
                                        regenerateTailForce(StringArgumentType.getString(context, "targetAndForce"))
                                ))))
                .then(Commands.literal("deleted")
                        .then(Commands.literal("list")
                                .executes(context -> runDeletedList(context.getSource(), logger)))
                        .then(Commands.literal("clear")
                                .then(Commands.literal("all")
                                        .executes(context -> runDeletedClearAll(context.getSource(), logger)))
                                .then(Commands.argument("id", StringArgumentType.greedyString())
                                        .executes(context -> runDeletedClear(
                                                context.getSource(),
                                                logger,
                                                StringArgumentType.getString(context, "id")
                                        )))))
                .then(Commands.literal("export")
                        .executes(context -> runExport(context.getSource(), logger)))
                .then(Commands.literal("import")
                        .executes(context -> runImport(context.getSource(), logger)))
                .then(Commands.literal("reload")
                        .executes(context -> runReload(context.getSource(), logger)))
                .then(Commands.literal("debug")
                        .then(debugChunkCommand(logger))
                        .then(debugScoreCommand(logger))
                        .then(Commands.literal("clusters")
                                .executes(context -> runDebugClusters(context.getSource(), logger)))
                        .then(Commands.literal("cause")
                                .executes(context -> runDebugCause(context.getSource(), logger)))
                        .then(debugNearbyCommand(logger))
                        .then(Commands.literal("farm")
                                .executes(context -> runDebugFarm(context.getSource(), logger)))
                        .then(Commands.literal("validate")
                                .executes(context -> runDebugValidate(context.getSource(), logger)))
                        .then(Commands.literal("config")
                                .then(Commands.literal("validate")
                                        .executes(context -> runDebugConfigValidate(context.getSource(), logger))))
                        .then(Commands.literal("selftest")
                                .executes(context -> runDebugSelfTest(context.getSource(), logger)))
                        .then(Commands.literal("repair")
                                .then(Commands.literal("dryrun")
                                        .executes(context -> runDebugRepairDryRun(context.getSource(), logger))))
                        .then(compatCommand(logger))
                        .then(Commands.literal("migration")
                                .executes(context -> runDebugMigration(context.getSource(), logger)))
                        .then(debugDecayCommand(logger))
                        .then(debugSpacingCommand(logger))
                        .then(debugNameCommand(logger))
                        .then(debugNameBatchCommand(logger))
                        .then(debugNameCauseCommand(logger))
                        .then(debugNameAuditCommand(logger))
                        .then(Commands.literal("mobtheme")
                                .then(Commands.argument("entityId", StringArgumentType.greedyString())
                                        .executes(context -> runDebugMobTheme(
                                                context.getSource(),
                                                logger,
                                                StringArgumentType.getString(context, "entityId")
                                        ))))
                        .then(Commands.literal("mobthemes")
                                .then(Commands.literal("missing")
                                        .executes(context -> runDebugMobThemesMissing(context.getSource(), logger))))
                        .then(Commands.literal("title")
                                .then(Commands.literal("test")
                                        .then(Commands.argument("style", StringArgumentType.word())
                                                .then(Commands.argument("placeType", StringArgumentType.word())
                                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(placeTypeIds(), builder))
                                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                                .executes(context -> runDebugTitleTest(
                                                                        context.getSource(),
                                                                        logger,
                                                                        StringArgumentType.getString(context, "style"),
                                                                        StringArgumentType.getString(context, "placeType"),
                                                                        StringArgumentType.getString(context, "name")
                                                                ))))))
                                .then(Commands.literal("nearest")
                                        .executes(context -> runDebugTitleNearest(context.getSource(), logger)))
                                .then(Commands.literal("here")
                                        .executes(context -> runDebugTitleHere(context.getSource(), logger)))
                                .then(Commands.literal("clear")
                                        .executes(context -> runDebugTitleClear(context.getSource(), logger)))
                                .then(Commands.literal("state")
                                        .executes(context -> runDebugTitleState(context.getSource(), logger))))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> compatCommand(Logger logger) {
        var compat = Commands.literal("compat")
                .executes(context -> runDebugCompatSummary(context.getSource(), logger))
                .then(Commands.literal("summary")
                        .executes(context -> runDebugCompatSummary(context.getSource(), logger)));
        var lookup = Commands.literal("lookup");
        for (String kind : List.of("boss", "structure", "biome", "mob", "block", "dimension")) {
            lookup.then(Commands.literal(kind)
                    .then(Commands.argument(kind + "Id", StringArgumentType.greedyString())
                            .executes(context -> runDebugCompatLookup(
                                    context.getSource(),
                                    logger,
                                    kind,
                                    StringArgumentType.getString(context, kind + "Id")
                            ))));
        }
        return compat.then(lookup);
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> debugChunkCommand(Logger logger) {
        return Commands.literal("chunk")
                .executes(context -> runDebugChunk(context.getSource(), logger));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> debugScoreCommand(Logger logger) {
        return Commands.literal("score")
                .executes(context -> runDebugScore(context.getSource(), logger, null, null))
                .then(Commands.argument("chunkX", IntegerArgumentType.integer())
                        .then(Commands.argument("chunkZ", IntegerArgumentType.integer())
                                .executes(context -> runDebugScore(
                                        context.getSource(),
                                        logger,
                                        IntegerArgumentType.getInteger(context, "chunkX"),
                                        IntegerArgumentType.getInteger(context, "chunkZ")
                                ))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> debugNearbyCommand(Logger logger) {
        return Commands.literal("nearby")
                .executes(context -> runDebugNearby(context.getSource(), logger, 0))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                        .executes(context -> runDebugNearby(
                                context.getSource(),
                                logger,
                                IntegerArgumentType.getInteger(context, "radius")
                        )));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> debugDecayCommand(Logger logger) {
        return Commands.literal("decay")
                .executes(context -> runDebugDecayStatus(context.getSource(), logger))
                .then(Commands.literal("status")
                        .executes(context -> runDebugDecayStatus(context.getSource(), logger)))
                .then(Commands.literal("run")
                        .executes(context -> runDebugDecayRun(context.getSource(), logger, 0L))
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(0))
                                .executes(context -> runDebugDecayRun(
                                        context.getSource(),
                                        logger,
                                        IntegerArgumentType.getInteger(context, "ticks")
                                ))))
                .then(Commands.literal("info")
                        .then(Commands.literal("chunk")
                                .executes(context -> runDebugDecayInfoChunk(context.getSource(), logger)))
                        .then(Commands.literal("nearest")
                                .executes(context -> runDebugDecayInfoNearest(context.getSource(), logger))))
                .then(Commands.literal("touch")
                        .then(Commands.literal("chunk")
                                .executes(context -> runDebugDecayTouchChunk(context.getSource(), logger))))
                .then(Commands.literal("setscore")
                        .then(Commands.literal("chunk")
                                .then(Commands.argument("placeType", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(placeTypeIds(), builder))
                                        .then(Commands.argument("score", StringArgumentType.word())
                                                .executes(context -> runDebugDecaySetScoreChunk(
                                                        context.getSource(),
                                                        logger,
                                                        StringArgumentType.getString(context, "placeType"),
                                                        StringArgumentType.getString(context, "score")
                                                ))))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> debugSpacingCommand(Logger logger) {
        return Commands.literal("spacing")
                .executes(context -> runDebugSpacingHere(context.getSource(), logger, PlaceType.GENERAL_LANDMARK.idString()))
                .then(Commands.literal("here")
                        .then(Commands.argument("placeType", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(placeTypeIds(), builder))
                                .executes(context -> runDebugSpacingHere(
                                        context.getSource(),
                                        logger,
                                        StringArgumentType.getString(context, "placeType")
                                ))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> debugNameCommand(Logger logger) {
        return Commands.literal("name")
                .executes(context -> runDebugName(
                        context.getSource(),
                        logger,
                        PlaceType.DEATH_SITE.idString(),
                        DeathSiteEnvironment.SURFACE.idString(),
                        "",
                        null
                ))
                .then(Commands.argument("placeType", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(placeTypeIds(), builder))
                        .then(Commands.argument("environment", StringArgumentType.word())
                                .then(Commands.argument("style", StringArgumentType.word())
                                        .then(Commands.argument("seed", StringArgumentType.word())
                                                .executes(context -> runDebugName(
                                                        context.getSource(),
                                                        logger,
                                                        StringArgumentType.getString(context, "placeType"),
                                                        StringArgumentType.getString(context, "environment"),
                                                        StringArgumentType.getString(context, "style"),
                                                        parseLongOrNull(StringArgumentType.getString(context, "seed"))
                                                )))
                                        .executes(context -> runDebugName(
                                                context.getSource(),
                                                logger,
                                                StringArgumentType.getString(context, "placeType"),
                                                StringArgumentType.getString(context, "environment"),
                                                StringArgumentType.getString(context, "style"),
                                                null
                                        )))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> debugNameBatchCommand(Logger logger) {
        return Commands.literal("namebatch")
                .executes(context -> runDebugNameBatch(
                        context.getSource(),
                        logger,
                        PlaceType.DEATH_SITE.idString(),
                        DeathSiteEnvironment.SURFACE.idString(),
                        "",
                        10
                ))
                .then(Commands.argument("placeType", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(placeTypeIds(), builder))
                        .then(Commands.argument("environment", StringArgumentType.word())
                                .then(Commands.argument("style", StringArgumentType.word())
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 50))
                                                .executes(context -> runDebugNameBatch(
                                                        context.getSource(),
                                                        logger,
                                                        StringArgumentType.getString(context, "placeType"),
                                                        StringArgumentType.getString(context, "environment"),
                                                        StringArgumentType.getString(context, "style"),
                                                        IntegerArgumentType.getInteger(context, "count")
                                                )))
                                        .executes(context -> runDebugNameBatch(
                                                context.getSource(),
                                                logger,
                                                StringArgumentType.getString(context, "placeType"),
                                                StringArgumentType.getString(context, "environment"),
                                                StringArgumentType.getString(context, "style"),
                                                10
                                        )))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> debugNameCauseCommand(Logger logger) {
        return Commands.literal("namecause")
                .executes(context -> {
                    send(context.getSource(), "Usage: /places debug namecause <placeType> <causeType> <targetId> <style> <count> [name=<runtimeName>]");
                    return 1;
                })
                .then(Commands.argument("placeType", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(placeTypeIds(), builder))
                        .then(Commands.argument("causeType", StringArgumentType.word())
                                .then(Commands.argument("targetStyleCount", StringArgumentType.greedyString())
                                        .executes(context -> runDebugNameCause(
                                                context.getSource(),
                                                logger,
                                                StringArgumentType.getString(context, "placeType"),
                                                StringArgumentType.getString(context, "causeType"),
                                                StringArgumentType.getString(context, "targetStyleCount")
                                        )))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> debugNameAuditCommand(Logger logger) {
        return Commands.literal("nameaudit")
                .executes(context -> runDebugNameAudit(context.getSource(), logger, null))
                .then(Commands.literal("context")
                        .then(Commands.argument("placeType", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(placeTypeIds(), builder))
                                .then(Commands.argument("environment", StringArgumentType.word())
                                        .then(Commands.argument("style", StringArgumentType.word())
                                                .executes(context -> runDebugNameAuditContext(
                                                        context.getSource(),
                                                        logger,
                                                        StringArgumentType.getString(context, "placeType"),
                                                        StringArgumentType.getString(context, "environment"),
                                                        StringArgumentType.getString(context, "style")
                                                ))))))
                .then(Commands.literal("cause")
                        .then(Commands.argument("placeType", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(placeTypeIds(), builder))
                                .then(Commands.argument("causeType", StringArgumentType.word())
                                        .then(Commands.argument("targetStyle", StringArgumentType.greedyString())
                                                .executes(context -> runDebugNameAuditCause(
                                                        context.getSource(),
                                                        logger,
                                                        StringArgumentType.getString(context, "placeType"),
                                                        StringArgumentType.getString(context, "causeType"),
                                                        StringArgumentType.getString(context, "targetStyle")
                                                ))))))
                .then(Commands.argument("style", StringArgumentType.word())
                        .executes(context -> runDebugNameAudit(
                                context.getSource(),
                                logger,
                                StringArgumentType.getString(context, "style")
                        )));
    }

    private static int runList(CommandSourceStack source, Logger logger, String requestedType, int requestedPage) {
        ServerLevel world = source.getLevel();
        PlaceType filterType = requestedType == null || requestedType.isBlank() ? null : PlaceType.fromId(requestedType);
        List<NamedPlace> places = visiblePlaces(world, logger).stream()
                .filter(place -> filterType == null || place.placeType() == filterType)
                .sorted(Comparator.comparing(NamedPlace::placeIdString))
                .toList();
        if (places.isEmpty()) {
            send(source, filterType == null
                    ? "World Remembers places: none"
                    : "World Remembers places: none for type=" + filterType.name());
            return 1;
        }

        int pageSize = Math.max(1, WorldRemembersLivingLegends.config().display.maxPlacesInList);
        int pageCount = Math.max(1, (int) Math.ceil(places.size() / (double) pageSize));
        int page = Math.max(1, Math.min(pageCount, requestedPage));
        int start = (page - 1) * pageSize;
        int end = Math.min(places.size(), start + pageSize);
        send(source, "World Remembers places: total=" + places.size()
                + (filterType == null ? "" : " type=" + filterType.name())
                + " page=" + page + "/" + pageCount);

        List<String> pageIds = new ArrayList<>();
        for (int index = start; index < end; index++) {
            int displayIndex = index - start + 1;
            NamedPlace place = places.get(index);
            pageIds.add(place.placeIdString());
            send(source, placeLine(place, displayIndex));
        }
        rememberListIndexes(source, pageIds);
        send(source, "World Remembers tip: use /places info #1, /places info nearest, or /places info here.");
        return 1;
    }

    private static int runNearest(CommandSourceStack source, Logger logger, Integer requestedRadius) {
        ServerLevel world = source.getLevel();
        WorldPos position = sourcePosition(source);
        int radius = Math.max(0, requestedRadius == null ? 0 : requestedRadius);
        long maxDistanceSquared = radius == 0 ? Long.MAX_VALUE : (long) radius * radius;
        NamedPlace nearest = visiblePlaces(world, logger).stream()
                .filter(place -> place != null && position.dimensionId().equals(place.dimensionId()))
                .filter(place -> radius == 0 || place.center().squaredDistanceTo(position) <= maxDistanceSquared || place.contains(position))
                .min(Comparator.comparingLong(place -> place.center().squaredDistanceTo(position)))
                .orElse(null);
        if (nearest == null) {
            send(source, radius == 0
                    ? "World Remembers nearest: no places in this dimension"
                    : "World Remembers nearest: no places within radius=" + radius);
            return 1;
        }

        double distance = Math.sqrt(Math.max(0L, nearest.center().squaredDistanceTo(position)));
        send(source, Component.literal("World Remembers nearest id=" + nearest.placeIdString() + " name=")
                .append(displayName(nearest))
                .append(Component.literal(" type=" + nearest.placeType().name()
                        + " distance=" + formatDistance(distance)
                        + " center=" + nearest.centerString()
                        + " radius=" + nearest.radius()
                        + " inside=" + nearest.contains(position))));
        return 1;
    }

    private static int runInfo(CommandSourceStack source, Logger logger, String target) {
        NamedPlace place = resolvePlaceReference(source, source.getLevel(), target, logger, true);
        if (place == null) {
            return 0;
        }

        send(source, Component.literal("World Remembers place info id=" + place.placeIdString() + " name=")
                .append(displayName(place)));
        send(source, "type=" + place.placeType().name()
                + " environment=" + place.environment().name()
                + " dimension=" + place.dimensionId()
                + " center=" + place.centerString()
                + " radius=" + place.radius()
                + " score=" + formatScore(place.score()));
        send(source, "bounds=" + place.bounds().boundsIdString());
        send(source, "biomeId=" + place.biomeId()
                + " dominantBiomeId=" + place.dominantBiomeId()
                + " biomeGroup=" + place.biomeGroup()
                + " biomeTheme=" + place.biomeTheme()
                + " biomeSource=" + place.biomeSource());
        send(source, "createdGameTime=" + place.createdAtGameTime()
                + " lastUpdatedGameTime=" + place.lastUpdatedGameTime()
                + " sourceChunks=" + place.sourceChunks());
        send(source, "manualName=" + place.manualName()
                + " manuallyRenamed=" + place.manuallyRenamed());
        send(source, "cause " + place.cause().debugString());
        send(source, "NameRecipe style=" + place.nameRecipe().styleId()
                + " patternKey=" + place.nameRecipe().patternKey()
                + " tokenIds=" + place.nameRecipe().selectedTokenIds()
                + " forms=" + recipeForms(place.nameRecipe())
                + " seed=" + place.nameRecipe().seed()
                + " signature=" + place.nameRecipe().recipeSignature()
                + " selectedPatternSource=not_stored");
        return 1;
    }

    private static int runCreate(
            CommandSourceStack source,
            Logger logger,
            String requestedType,
            int requestedRadius,
            String name
    ) {
        if (!requireOp(source)) {
            return 0;
        }
        if (name == null || name.isBlank()) {
            send(source, "World Remembers create usage: /places create custom 32 My Place");
            return 0;
        }

        ServerLevel world = source.getLevel();
        WorldPos center = sourcePosition(source);
        int radius = Math.max(1, requestedRadius);
        PlaceType type = PlaceType.fromId(requestedType);
        if (!WorldRemembersLivingLegends.config().placeTypes.autoGenerationEnabled(type)
                && !WorldRemembersLivingLegends.config().placeTypes.allowManualCreateWhenDisabled) {
            send(source, "World Remembers create rejected: place type " + type.name()
                    + " is disabled and allowManualCreateWhenDisabled=false.");
            return 0;
        }

        DeathSiteEnvironment environment = type == PlaceType.DEATH_SITE
                ? inferEnvironment(center.dimensionId(), center.y())
                : DeathSiteEnvironment.UNKNOWN;
        String baseId = "living_legends:manual/" + type.idString() + "_"
                + center.dimensionId().replace(':', '_').replace('/', '_')
                + "_" + center.x() + "_" + center.y() + "_" + center.z();
        String id = WorldRemembersLivingLegendsNeoForgeStorage.uniquePlaceId(world, baseId, logger);
        PlaceCause cause = new PlaceCause(
                PlaceCauseType.CUSTOM,
                EventType.CUSTOM,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                Map.of("manual_create", 1L)
        );
        long gameTime = WorldRemembersLivingLegendsNeoForgeStorage.gameTimeFor(world);
        NamedPlace place = new NamedPlace(
                id,
                type,
                environment,
                center.dimensionId(),
                center,
                radius,
                0.0,
                gameTime,
                gameTime,
                List.of(center.chunkIdString()),
                NameRecipe.empty(),
                PlaceRarity.COMMON,
                PlaceBounds.around(center, radius, Math.max(8, radius)),
                PlaceStats.empty(),
                "",
                "",
                cause,
                name,
                true
        );
        boolean changed = WorldRemembersLivingLegendsNeoForgeStorage.upsertPlace(world, place, "create_place " + id, logger);
        send(source, Component.literal(changed
                        ? "World Remembers created place id=" + id + " name="
                        : "World Remembers create made no changes id=" + id + " name=")
                .append(displayName(place)));
        return changed ? 1 : 0;
    }

    private static int runRenameTail(CommandSourceStack source, Logger logger, String targetAndName) {
        String[] parts = splitRenameTail(targetAndName);
        return runRename(source, logger, parts[0], parts[1]);
    }

    private static int runRename(CommandSourceStack source, Logger logger, String target, String newName) {
        if (!requireOp(source)) {
            return 0;
        }
        if (newName == null || newName.isBlank()) {
            send(source, "World Remembers rename usage: /places rename nearest New Name");
            return 0;
        }

        ServerLevel world = source.getLevel();
        NamedPlace place = resolvePlaceReference(source, world, target, logger, true);
        if (place == null) {
            return 0;
        }

        String oldName = displayNameString(place);
        NamedPlace updated = place.withManualName(newName, WorldRemembersLivingLegendsNeoForgeStorage.gameTimeFor(world));
        boolean changed = WorldRemembersLivingLegendsNeoForgeStorage.upsertPlace(
                world,
                updated,
                "rename_place " + place.placeIdString(),
                logger
        );
        if (changed && logger != null) {
            logger.info("World Remembers place renamed id=" + place.placeIdString()
                    + " oldName=" + oldName
                    + " newManualName=" + updated.manualName());
        }
        if (changed) {
            NeoForgeMapIntegrationManager.refreshDestinationFromWorld(world, updated, logger);
        }
        send(source, changed
                ? "World Remembers renamed place " + place.placeIdString() + " to \"" + updated.manualName() + "\""
                : "World Remembers rename made no changes for " + place.placeIdString());
        return changed ? 1 : 0;
    }

    private static int runDelete(CommandSourceStack source, Logger logger, String target) {
        if (!requireOp(source)) {
            return 0;
        }

        ServerLevel world = source.getLevel();
        NamedPlace place = resolvePlaceReference(source, world, target, logger, true);
        if (place == null) {
            return 0;
        }

        boolean deleted = WorldRemembersLivingLegendsNeoForgeStorage.deletePlace(world, place.placeIdString(), logger);
        send(source, deleted
                ? "World Remembers deleted place " + place.placeIdString()
                : "World Remembers place not found: " + place.placeIdString());
        return deleted ? 1 : 0;
    }

    private static int runRegenerate(CommandSourceStack source, Logger logger, String target, boolean force) {
        if (!requireOp(source)) {
            return 0;
        }

        ServerLevel world = source.getLevel();
        String message;
        if ("all".equalsIgnoreCase(target)) {
            message = WorldRemembersLivingLegendsNeoForgeStorage.regenerateAll(world, force, logger);
        } else {
            NamedPlace place = resolvePlaceReference(source, world, target, logger, true);
            if (place == null) {
                return 0;
            }
            message = WorldRemembersLivingLegendsNeoForgeStorage.regeneratePlace(world, place.placeIdString(), force, logger);
        }
        send(source, message);
        return 1;
    }

    private static int runDeletedList(CommandSourceStack source, Logger logger) {
        if (!requireOp(source)) {
            return 0;
        }
        ServerLevel world = source.getLevel();
        List<DeletedPlaceMarker> markers = WorldRemembersLivingLegendsNeoForgeStorage.deletedPlaceMarkers(world, logger);
        send(source, "World Remembers deleted places: count=" + markers.size()
                + " suppressionEnabled=" + WorldRemembersLivingLegends.config().generation.deletedPlaceSuppressionEnabled
                + " suppressionDays=" + WorldRemembersLivingLegends.config().generation.deletedPlaceSuppressionDays);
        int index = 1;
        for (DeletedPlaceMarker marker : markers) {
            send(source, "[" + index + "] " + marker.debugString());
            index++;
            if (index > 16) {
                send(source, "World Remembers deleted places: list truncated at 15 markers");
                break;
            }
        }
        return 1;
    }

    private static int runDeletedClear(CommandSourceStack source, Logger logger, String markerId) {
        if (!requireOp(source)) {
            return 0;
        }
        send(source, WorldRemembersLivingLegendsNeoForgeStorage.clearDeletedPlaceMarker(source.getLevel(), markerId, logger));
        return 1;
    }

    private static int runDeletedClearAll(CommandSourceStack source, Logger logger) {
        if (!requireOp(source)) {
            return 0;
        }
        send(source, WorldRemembersLivingLegendsNeoForgeStorage.clearAllDeletedPlaceMarkers(source.getLevel(), logger));
        return 1;
    }

    private static int runExport(CommandSourceStack source, Logger logger) {
        if (!requireOp(source)) {
            return 0;
        }
        send(source, WorldRemembersLivingLegendsNeoForgeStorage.exportPlaces(source.getLevel(), logger));
        return 1;
    }

    private static int runImport(CommandSourceStack source, Logger logger) {
        if (!requireOp(source)) {
            return 0;
        }
        send(source, WorldRemembersLivingLegendsNeoForgeStorage.importPlaces(source.getLevel(), logger));
        return 1;
    }

    private static int runReload(CommandSourceStack source, Logger logger) {
        if (!requireOp(source)) {
            return 0;
        }
        var result = WorldRemembersLivingLegends.reloadConfig(
                resolveGameDir(),
                logger == null ? null : logger::info,
                logger == null ? null : logger::warn
        );
        int patterns = 0;
        int tokens = 0;
        for (NameDataPack pack : BuiltInNameData.allPacks()) {
            patterns += pack.patterns().size();
            tokens += pack.tokens().size();
        }
        send(source, "World Remembers reload complete"
                + " usedDefaultConfig=" + result.usedDefaultConfig()
                + " malformedConfig=" + result.malformedConfig()
                + " configFile=" + result.configPath()
                + " configValidation=" + result.config().validationSummary().compact()
                + " styles=" + BuiltInNameData.allPacks().size()
                + " patterns=" + patterns
                + " tokens=" + tokens
                + " existingPlaceNames=unchanged");
        for (String warning : result.config().validationSummary().firstWarnings(8)) {
            send(source, "World Remembers config warning: " + warning);
        }
        if (result.config().validationWarnings().size() > 8) {
            send(source, "World Remembers config warning: "
                    + (result.config().validationWarnings().size() - 8)
                    + " more warnings in latest.log");
        }
        NeoForgeMapIntegrationManager.clearSyncFingerprints();
        NeoForgeMapIntegrationManager.syncAllFromWorld(source.getServer(), logger);
        return 1;
    }

    private static String[] splitRenameTail(String value) {
        if (value == null || value.isBlank()) {
            return new String[]{"", ""};
        }
        String trimmed = value.trim();
        int split = trimmed.indexOf(' ');
        if (split < 0) {
            return new String[]{trimmed, ""};
        }
        return new String[]{trimmed.substring(0, split).trim(), trimmed.substring(split + 1).trim()};
    }

    private static String regenerateTailTarget(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.toLowerCase(Locale.ROOT).endsWith(" force")) {
            return trimmed.substring(0, trimmed.length() - " force".length()).trim();
        }
        return trimmed;
    }

    private static boolean regenerateTailForce(String value) {
        String trimmed = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return trimmed.endsWith(" force");
    }

    private static int runDebugValidate(CommandSourceStack source, Logger logger) {
        if (!requireOp(source)) {
            return 0;
        }
        send(source, WorldRemembersLivingLegendsNeoForgeStorage.debugValidate(source.getLevel(), logger));
        return 1;
    }

    private static int runDebugConfigValidate(CommandSourceStack source, Logger logger) {
        if (!requireOp(source)) {
            return 0;
        }
        LivingLegendsConfig config = WorldRemembersLivingLegends.config();
        LivingLegendsConfig.ValidationSummary summary = config.validationSummary();
        send(source, "World Remembers config validate"
                + " " + summary.compact()
                + " configFile=" + LivingLegendsConfigManager.currentConfigPath()
                + " sections=general,generation,thresholds,requiredCounts,scoreThresholds,display,performance,eventCollection,antiFarm,naming,commands,placeTypes,biomeThemes,notifications,titleOverlay,decay,candidateDecay,journal,permissions,debug");
        for (String warning : summary.firstWarnings(10)) {
            send(source, "World Remembers config warning: " + warning);
        }
        if (summary.warnings() > 10) {
            send(source, "World Remembers config warning: " + (summary.warnings() - 10) + " more warnings in latest.log");
        }
        return 1;
    }

    private static int runDebugSelfTest(CommandSourceStack source, Logger logger) {
        if (!requireOp(source)) {
            return 0;
        }
        send(source, WorldRemembersLivingLegendsNeoForgeStorage.debugSelfTest(source.getLevel(), logger));
        return 1;
    }

    private static int runDebugRepairDryRun(CommandSourceStack source, Logger logger) {
        if (!requireOp(source)) {
            return 0;
        }
        send(source, WorldRemembersLivingLegendsNeoForgeStorage.debugRepairDryRun(source.getLevel(), logger));
        return 1;
    }

    private static int runDebugCompatSummary(CommandSourceStack source, Logger logger) {
        if (!requireOp(source)) {
            return 0;
        }
        send(source, "World Remembers compat registries " + WorldRemembersCompatRegistries.summaryLine());
        for (String warning : WorldRemembersCompatRegistries.warnings().stream().limit(8).toList()) {
            send(source, "World Remembers compat warning: " + warning);
        }
        int hidden = Math.max(0, WorldRemembersCompatRegistries.warnings().size() - 8);
        if (hidden > 0) {
            send(source, "World Remembers compat warning: " + hidden + " more warnings in latest.log");
        }
        return 1;
    }

    private static int runDebugCompatLookup(CommandSourceStack source, Logger logger, String kind, String id) {
        if (!requireOp(source)) {
            return 0;
        }
        send(source, WorldRemembersCompatRegistries.lookupDebug(kind, id));
        return 1;
    }

    private static int runDebugMigration(CommandSourceStack source, Logger logger) {
        send(source, WorldRemembersLivingLegendsNeoForgeStorage.debugMigration(source.getLevel(), logger));
        return 1;
    }

    private static int runDebugChunk(CommandSourceStack source, Logger logger) {
        WorldPos position = sourcePosition(source);
        int chunkX = Math.floorDiv(position.x(), CHUNK_SIZE);
        int chunkZ = Math.floorDiv(position.z(), CHUNK_SIZE);
        String message = WorldRemembersLivingLegendsNeoForgeStorage.debugChunk(source.getLevel(), position.dimensionId(), chunkX, chunkZ, logger)
                + "\n" + WorldRemembersLivingLegendsNeoForgeStorage.debugScore(source.getLevel(), position.dimensionId(), chunkX, chunkZ, logger)
                + "\n" + WorldRemembersLivingLegendsNeoForgeStorage.debugClusters(source.getLevel(), logger);
        send(source, message);
        return 1;
    }

    private static int runDebugScore(CommandSourceStack source, Logger logger, Integer requestedChunkX, Integer requestedChunkZ) {
        WorldPos position = sourcePosition(source);
        int chunkX = requestedChunkX == null ? Math.floorDiv(position.x(), CHUNK_SIZE) : requestedChunkX;
        int chunkZ = requestedChunkZ == null ? Math.floorDiv(position.z(), CHUNK_SIZE) : requestedChunkZ;
        send(source, WorldRemembersLivingLegendsNeoForgeStorage.debugScore(source.getLevel(), position.dimensionId(), chunkX, chunkZ, logger));
        return 1;
    }

    private static int runDebugClusters(CommandSourceStack source, Logger logger) {
        send(source, WorldRemembersLivingLegendsNeoForgeStorage.debugClusters(source.getLevel(), logger));
        return 1;
    }

    private static int runDebugCause(CommandSourceStack source, Logger logger) {
        WorldPos position = sourcePosition(source);
        int chunkX = Math.floorDiv(position.x(), CHUNK_SIZE);
        int chunkZ = Math.floorDiv(position.z(), CHUNK_SIZE);
        send(source, WorldRemembersLivingLegendsNeoForgeStorage.debugCause(
                source.getLevel(),
                position.dimensionId(),
                chunkX,
                chunkZ,
                position.x(),
                position.y(),
                position.z(),
                logger
        ));
        return 1;
    }

    private static int runDebugNearby(CommandSourceStack source, Logger logger, int radius) {
        WorldPos position = sourcePosition(source);
        send(source, WorldRemembersLivingLegendsNeoForgeStorage.debugNearby(
                source.getLevel(),
                position.dimensionId(),
                position.x(),
                position.y(),
                position.z(),
                radius,
                logger
        ));
        return 1;
    }

    private static int runDebugFarm(CommandSourceStack source, Logger logger) {
        send(source, EventCollector.debugAntiFarmState());
        return 1;
    }

    private static int runDebugDecayStatus(CommandSourceStack source, Logger logger) {
        if (!requireOp(source)) {
            return 0;
        }
        send(source, WorldRemembersLivingLegendsNeoForgeStorage.candidateDecayStatus());
        return 1;
    }

    private static int runDebugDecayRun(CommandSourceStack source, Logger logger, long simulatedTicks) {
        if (!requireOp(source)) {
            return 0;
        }
        send(source, WorldRemembersLivingLegendsNeoForgeStorage.runCandidateDecayNow(source.getLevel(), Math.max(0L, simulatedTicks), logger));
        return 1;
    }

    private static int runDebugDecayInfoChunk(CommandSourceStack source, Logger logger) {
        if (!requireOp(source)) {
            return 0;
        }
        WorldPos position = sourcePosition(source);
        send(source, WorldRemembersLivingLegendsNeoForgeStorage.debugCandidateDecayChunk(
                source.getLevel(),
                position.dimensionId(),
                Math.floorDiv(position.x(), CHUNK_SIZE),
                Math.floorDiv(position.z(), CHUNK_SIZE),
                logger
        ));
        return 1;
    }

    private static int runDebugDecayInfoNearest(CommandSourceStack source, Logger logger) {
        if (!requireOp(source)) {
            return 0;
        }
        WorldPos position = sourcePosition(source);
        send(source, WorldRemembersLivingLegendsNeoForgeStorage.debugCandidateDecayNearest(
                source.getLevel(),
                position.dimensionId(),
                position.x(),
                position.y(),
                position.z(),
                logger
        ));
        return 1;
    }

    private static int runDebugDecayTouchChunk(CommandSourceStack source, Logger logger) {
        if (!requireOp(source)) {
            return 0;
        }
        WorldPos position = sourcePosition(source);
        send(source, WorldRemembersLivingLegendsNeoForgeStorage.touchCandidateDecayChunk(
                source.getLevel(),
                position.dimensionId(),
                Math.floorDiv(position.x(), CHUNK_SIZE),
                Math.floorDiv(position.z(), CHUNK_SIZE),
                logger
        ));
        return 1;
    }

    private static int runDebugDecaySetScoreChunk(CommandSourceStack source, Logger logger, String requestedType, String requestedScore) {
        if (!requireOp(source)) {
            return 0;
        }
        PlaceType placeType = PlaceType.fromId(requestedType);
        if (placeType == PlaceType.UNKNOWN) {
            send(source, "Candidate decay setscore failed: unknown place type " + requestedType);
            return 0;
        }
        double score;
        try {
            score = Math.max(0.0, Double.parseDouble(requestedScore));
        } catch (NumberFormatException exception) {
            send(source, "Candidate decay setscore failed: score must be a number");
            return 0;
        }
        WorldPos position = sourcePosition(source);
        send(source, WorldRemembersLivingLegendsNeoForgeStorage.setCandidateDecayScore(
                source.getLevel(),
                position.dimensionId(),
                Math.floorDiv(position.x(), CHUNK_SIZE),
                Math.floorDiv(position.z(), CHUNK_SIZE),
                placeType,
                score,
                logger
        ));
        return 1;
    }

    private static int runDebugSpacingHere(CommandSourceStack source, Logger logger, String requestedType) {
        if (!requireOp(source)) {
            return 0;
        }
        PlaceType placeType = PlaceType.fromId(requestedType);
        if (placeType == PlaceType.UNKNOWN) {
            send(source, "World Remembers spacing failed: unknown place type " + requestedType);
            return 0;
        }
        WorldPos position = sourcePosition(source);
        send(source, WorldRemembersLivingLegendsNeoForgeStorage.debugSpacingHere(
                source.getLevel(),
                position.dimensionId(),
                position.x(),
                position.y(),
                position.z(),
                placeType,
                logger
        ));
        return 1;
    }

    private static int runDebugName(
            CommandSourceStack source,
            Logger logger,
            String requestedType,
            String requestedEnvironment,
            String requestedStyle,
            Long requestedSeed
    ) {
        WorldPos position = sourcePosition(source);
        int chunkX = Math.floorDiv(position.x(), CHUNK_SIZE);
        int chunkZ = Math.floorDiv(position.z(), CHUNK_SIZE);
        PlaceType type = PlaceType.fromId(requestedType);
        DeathSiteEnvironment environment = DeathSiteEnvironment.fromId(requestedEnvironment);
        String requestedStyleId = normalizedStyleId(requestedStyle);
        NameDataPack nameData = BuiltInNameData.packForStyle(
                requestedStyleId,
                WorldRemembersLivingLegends.config().naming.enabledStyles,
                WorldRemembersLivingLegends.config().naming.allowMixedStyleTokens
        );
        long baseSeed = requestedSeed == null
                ? 31L * type.ordinal() + 17L * environment.ordinal() + 341873128712L * chunkX + 132897987541L * chunkZ
                : requestedSeed;
        PlaceCluster cluster = debugCluster(type, environment, position, "debug_name");

        StringBuilder message = new StringBuilder();
        message.append("World Remembers name debug type=")
                .append(type.name())
                .append(" environment=")
                .append(environment.name())
                .append(" requestedStyle=")
                .append(requestedStyleId)
                .append(" selectedStyle=")
                .append(nameData.styleId())
                .append(" fallback=")
                .append(fallbackBehavior(requestedStyleId, nameData.styleId()))
                .append(" seed=")
                .append(baseSeed);
        for (int index = 0; index < 5; index++) {
            NameGenerationDiagnostics diagnostics = new NameGenerationDiagnostics();
            NameRecipe recipe = NameGenerator.generate(
                    cluster,
                    type,
                    environment,
                    baseSeed + index,
                    nameData,
                    List.of(),
                    diagnostics
            );
            message.append('\n')
                    .append(index + 1)
                    .append(". ")
                    .append(WorldRemembersLivingLegendsNeoForgeNameResolver.resolveToString(recipe))
                    .append(" | ")
                    .append(NameGenerator.debugRecipe(recipe))
                    .append(" fallback=")
                    .append(fallbackBehavior(requestedStyleId, recipe.styleId()))
                    .append(" ")
                    .append(diagnostics.summary());
            logNameRejectionDetails(logger, diagnostics);
        }
        send(source, message.toString());
        return 1;
    }

    private static int runDebugNameBatch(
            CommandSourceStack source,
            Logger logger,
            String requestedType,
            String requestedEnvironment,
            String requestedStyle,
            Integer requestedCount
    ) {
        WorldPos position = sourcePosition(source);
        int chunkX = Math.floorDiv(position.x(), CHUNK_SIZE);
        int chunkZ = Math.floorDiv(position.z(), CHUNK_SIZE);
        PlaceType type = PlaceType.fromId(requestedType);
        DeathSiteEnvironment environment = DeathSiteEnvironment.fromId(requestedEnvironment);
        String requestedStyleId = normalizedStyleId(requestedStyle);
        NameDataPack nameData = BuiltInNameData.packForStyle(
                requestedStyleId,
                WorldRemembersLivingLegends.config().naming.enabledStyles,
                WorldRemembersLivingLegends.config().naming.allowMixedStyleTokens
        );
        int count = Math.max(1, Math.min(50, requestedCount == null ? 10 : requestedCount));
        long baseSeed = 31L * type.ordinal() + 17L * environment.ordinal() + 341873128712L * chunkX + 132897987541L * chunkZ;
        PlaceCluster cluster = debugCluster(type, environment, position, "debug_namebatch");

        List<NameRecipe> generatedRecipes = new ArrayList<>();
        Set<String> resolvedNames = new HashSet<>();
        Set<String> signatures = new HashSet<>();
        NameGenerationDiagnostics diagnostics = new NameGenerationDiagnostics();
        StringBuilder message = new StringBuilder();
        message.append("World Remembers name batch type=")
                .append(type.name())
                .append(" environment=")
                .append(environment.name())
                .append(" requestedStyle=")
                .append(requestedStyleId)
                .append(" selectedStyle=")
                .append(nameData.styleId())
                .append(" fallback=")
                .append(fallbackBehavior(requestedStyleId, nameData.styleId()))
                .append(" count=")
                .append(count)
                .append(" baseSeed=")
                .append(baseSeed);
        for (int index = 0; index < count; index++) {
            NameRecipe recipe = NameRecipe.empty();
            String resolvedName = "";
            boolean duplicateAllowed = false;
            for (int attempt = 0; attempt < 96; attempt++) {
                recipe = NameGenerator.generate(
                        cluster,
                        type,
                        environment,
                        baseSeed + index + attempt * 10_007L,
                        nameData,
                        generatedRecipes,
                        diagnostics
                );
                resolvedName = WorldRemembersLivingLegendsNeoForgeNameResolver.resolveToString(recipe);
                boolean duplicateSignature = signatures.contains(recipe.recipeSignature());
                boolean duplicateResolvedName = resolvedNames.contains(normalizedResolvedName(resolvedName));
                if (!duplicateSignature && !duplicateResolvedName) {
                    duplicateAllowed = false;
                    break;
                }
                diagnostics.duplicateAvoided();
                duplicateAllowed = true;
            }
            generatedRecipes.add(recipe);
            signatures.add(recipe.recipeSignature());
            resolvedNames.add(normalizedResolvedName(resolvedName));
            message.append('\n')
                    .append(index + 1)
                    .append(". ")
                    .append(resolvedName)
                    .append(" | ")
                    .append(NameGenerator.debugRecipe(recipe))
                    .append(" fallback=")
                    .append(fallbackBehavior(requestedStyleId, recipe.styleId()));
            if (duplicateAllowed) {
                message.append(" duplicate_allowed_small_pool");
            }
        }
        message.append('\n').append(diagnostics.summary());
        logNameRejectionDetails(logger, diagnostics);
        send(source, message.toString());
        return 1;
    }

    private static int runDebugNameCause(
            CommandSourceStack source,
            Logger logger,
            String requestedPlaceType,
            String requestedCauseType,
            String targetStyleCount
    ) {
        PlaceType placeType = PlaceType.fromId(requestedPlaceType);
        PlaceCauseType causeType = PlaceCauseType.fromId(requestedCauseType);
        String[] parts = splitNameCauseTail(targetStyleCount);
        if (parts.length < 3) {
            send(source, "Usage: /places debug namecause <placeType> <causeType> <targetId> <style> <count> [name=<runtimeName>]");
            return 0;
        }
        String targetId = parts[0] == null ? "" : parts[0].trim().toLowerCase(Locale.ROOT);
        String requestedStyleId = normalizedStyleId(parts[1]);
        int count = Math.max(1, Math.min(50, parsePositiveInt(parts[2], 10)));
        String runtimeName = runtimeNameOption(parts, 3);
        NameDataPack nameData = BuiltInNameData.packForStyle(
                requestedStyleId,
                WorldRemembersLivingLegends.config().naming.enabledStyles,
                WorldRemembersLivingLegends.config().naming.allowMixedStyleTokens
        );
        PlaceCause cause = debugCauseFor(placeType, causeType, targetId, runtimeName);
        DeathSiteEnvironment environment = debugEnvironmentForCause(placeType, causeType, targetId);
        NameContext nameContext = NameContext.from(placeType, environment, cause, nameData.styleId());
        long baseSeed = 9137L * placeType.ordinal() + 1777L * causeType.ordinal() + targetId.hashCode() + 31L * runtimeName.hashCode();
        send(source, "World Remembers name cause"
                + " placeType=" + placeType.name()
                + " environment=" + environment.name()
                + " causeType=" + causeType.name()
                + " targetId=" + targetId
                + (runtimeName.isBlank() ? "" : " runtimeName=" + runtimeName)
                + " selectedStyle=" + nameData.styleId()
                + " " + cause.debugString());
        List<NameRecipe> generatedRecipes = new ArrayList<>();
        Set<String> resolvedNames = new HashSet<>();
        Set<String> signatures = new HashSet<>();
        for (int index = 0; index < count; index++) {
            NameGenerationDiagnostics diagnostics = new NameGenerationDiagnostics();
            NameRecipe recipe = NameRecipe.empty();
            String resolvedName = "";
            for (int attempt = 0; attempt < 48; attempt++) {
                recipe = NameGenerator.generate(
                        nameContext,
                        baseSeed + index * 31L + attempt * 10_007L,
                        nameData,
                        generatedRecipes,
                        diagnostics
                );
                resolvedName = WorldRemembersLivingLegendsNeoForgeNameResolver.resolveToString(recipe);
                if (!signatures.contains(recipe.recipeSignature())
                        && !resolvedNames.contains(normalizedResolvedName(resolvedName))) {
                    break;
                }
                diagnostics.duplicateAvoided();
            }
            generatedRecipes.add(recipe);
            signatures.add(recipe.recipeSignature());
            resolvedNames.add(normalizedResolvedName(resolvedName));
            String fallbackReason = fallbackReason(recipe, requestedStyleId, nameData.styleId(), diagnostics);
            Component line = Component.literal((index + 1) + ". ")
                    .append(WorldRemembersLivingLegendsNeoForgeNameResolver.resolve(recipe, nameData))
                    .append(Component.literal(" | patternKey=" + recipe.patternKey()
                            + " selectedPatternSource=" + diagnostics.selectedPatternSource().name()
                            + " fallbackUsed=" + (!fallbackReason.isBlank())
                            + (fallbackReason.isBlank() ? "" : " fallbackReason=" + fallbackReason)
                            + " tokens=" + recipe.selectedTokenIds()
                            + " " + diagnostics.summary()));
            send(source, line);
            logNameRejectionDetails(logger, diagnostics);
        }
        return 1;
    }

    private static int runDebugNameAuditContext(
            CommandSourceStack source,
            Logger logger,
            String requestedPlaceType,
            String requestedEnvironment,
            String requestedStyle
    ) {
        PlaceType placeType = PlaceType.fromId(requestedPlaceType);
        DeathSiteEnvironment environment = DeathSiteEnvironment.fromId(requestedEnvironment);
        String requestedStyleId = normalizedStyleId(requestedStyle);
        NameDataPack nameData = BuiltInNameData.packForStyle(
                requestedStyleId,
                WorldRemembersLivingLegends.config().naming.enabledStyles,
                WorldRemembersLivingLegends.config().naming.allowMixedStyleTokens
        );
        String message = "World Remembers name audit context"
                + " requestedStyle=" + requestedStyleId
                + " selectedStyle=" + nameData.styleId()
                + " placeType=" + placeType.name()
                + " environment=" + environment.name()
                + " activePatterns=" + nameData.patterns().size()
                + " activeTokens=" + nameData.tokens().size();
        send(source, message);
        if (logger != null) {
            logger.info(message);
        }
        return 1;
    }

    private static int runDebugNameAuditCause(
            CommandSourceStack source,
            Logger logger,
            String requestedPlaceType,
            String requestedCauseType,
            String targetStyle
    ) {
        String[] parts = splitNameCauseTail(targetStyle);
        if (parts.length < 2) {
            send(source, "Usage: /places debug nameaudit cause <placeType> <causeType> <targetId> <style>");
            return 0;
        }
        PlaceType placeType = PlaceType.fromId(requestedPlaceType);
        PlaceCauseType causeType = PlaceCauseType.fromId(requestedCauseType);
        String targetId = parts[0] == null ? "" : parts[0].trim().toLowerCase(Locale.ROOT);
        String requestedStyleId = normalizedStyleId(parts[1]);
        String runtimeName = runtimeNameOption(parts, 2);
        NameDataPack nameData = BuiltInNameData.packForStyle(
                requestedStyleId,
                WorldRemembersLivingLegends.config().naming.enabledStyles,
                WorldRemembersLivingLegends.config().naming.allowMixedStyleTokens
        );
        PlaceCause cause = debugCauseFor(placeType, causeType, targetId, runtimeName);
        DeathSiteEnvironment environment = debugEnvironmentForCause(placeType, causeType, targetId);
        NameContext nameContext = NameContext.from(placeType, environment, cause, nameData.styleId());
        List<NameRecipe> generatedRecipes = new ArrayList<>();
        Set<String> patternKeys = new HashSet<>();
        Set<String> resolvedNames = new HashSet<>();
        List<String> samples = new ArrayList<>();
        NameGenerationDiagnostics diagnostics = new NameGenerationDiagnostics(false);
        long baseSeed = 0x51A7E5L + 31L * placeType.ordinal() + 997L * causeType.ordinal() + targetId.hashCode();
        for (int index = 0; index < 64; index++) {
            NameRecipe recipe = NameGenerator.generate(
                    nameContext,
                    baseSeed + index * 10_007L,
                    nameData,
                    generatedRecipes,
                    diagnostics
            );
            generatedRecipes.add(recipe);
            patternKeys.add(recipe.patternKey());
            String resolvedName = WorldRemembersLivingLegendsNeoForgeNameResolver.resolveToString(recipe);
            if (!resolvedName.isBlank()) {
                resolvedNames.add(normalizedResolvedName(resolvedName));
                if (samples.size() < 8 && !samples.contains(resolvedName)) {
                    samples.add(resolvedName);
                }
            }
        }
        boolean fixedFrequentScenario = highFrequencyNameScenario(placeType, causeType, targetId)
                && patternKeys.size() <= 1
                && resolvedNames.size() <= 1;
        String message = "World Remembers name audit cause"
                + " requestedStyle=" + requestedStyleId
                + " selectedStyle=" + nameData.styleId()
                + " placeType=" + placeType.name()
                + " environment=" + environment.name()
                + " causeType=" + causeType.name()
                + " targetId=" + targetId
                + " samples=64"
                + " uniquePatternKeys=" + patternKeys.size()
                + " uniqueNames=" + resolvedNames.size()
                + (fixedFrequentScenario ? " warning=fixed_pattern_pool" : "")
                + " firstNames=" + samples
                + " " + diagnostics.summary();
        send(source, message);
        if (logger != null) {
            logger.info(message);
        }
        return 1;
    }

    private static int runDebugNameAudit(CommandSourceStack source, Logger logger, String requestedStyle) {
        String styleFilter = requestedStyle == null || requestedStyle.isBlank()
                ? ""
                : BuiltInNameData.builtInStyleId(requestedStyle);
        List<NameDataPack> packs = BuiltInNameData.allPacks().stream()
                .filter(pack -> styleFilter.isBlank() || pack.styleId().equals(styleFilter))
                .toList();
        if (packs.isEmpty()) {
            send(source, "World Remembers name audit: no style data for " + requestedStyle);
            return 0;
        }
        int manual = 0;
        int generated = 0;
        int missing = 0;
        for (NamedPlace place : WorldRemembersLivingLegendsNeoForgeStorage.places(source.getLevel(), logger)) {
            if (place.manuallyRenamed()) {
                manual++;
            } else if (place.nameRecipe() == null || place.nameRecipe().patternKey().isBlank()) {
                missing++;
            } else {
                generated++;
            }
        }
        StringBuilder message = new StringBuilder("World Remembers name audit");
        if (!styleFilter.isBlank()) {
            message.append(" style=").append(styleFilter);
        }
        message.append(" styles=").append(packs.size())
                .append(" existingPlaces manual=").append(manual)
                .append(" generated=").append(generated)
                .append(" missingGeneratedName=").append(missing);
        for (NameDataPack pack : packs) {
            message.append('\n')
                    .append("style=").append(pack.styleId())
                    .append(" activePatterns=").append(pack.patterns().size())
                    .append(" activeTokens=").append(pack.tokens().size());
        }
        if (logger != null) {
            logger.info(message.toString());
        }
        send(source, message.toString());
        return 1;
    }

    private static int runDebugMobTheme(CommandSourceStack source, Logger logger, String requestedEntityId) {
        String entityId = requestedEntityId == null ? "" : requestedEntityId.trim().toLowerCase(Locale.ROOT);
        VanillaMobThemeRegistry.MobTheme theme = VanillaMobThemeRegistry.lookup(entityId);
        if (!theme.hasMapping()) {
            send(source, "World Remembers mob theme missing exact mapping"
                    + " entityId=" + entityId
                    + " fallbackGroup=group:unknown"
                    + " safeFallback=true");
            return 1;
        }
        String fallbackGroup = theme.primaryGroup().isBlank() ? "group:unknown" : "group:" + theme.primaryGroup();
        send(source, "World Remembers mob theme "
                + theme.debugString()
                + " fallbackGroup=" + fallbackGroup
                + " exactMapping=true");
        return 1;
    }

    private static int runDebugMobThemesMissing(CommandSourceStack source, Logger logger) {
        List<String> missing = VanillaMobThemeRegistry.missingRelevantMappings();
        if (missing.isEmpty()) {
            send(source, "World Remembers mob themes missing: none");
            return 1;
        }
        send(source, "World Remembers mob themes missing: " + missing.size());
        for (String entityId : missing) {
            send(source, entityId + " safeFallbackGroup=group:unknown");
        }
        return 1;
    }

    private static int runDebugTitleTest(
            CommandSourceStack source,
            Logger logger,
            String styleId,
            String requestedType,
            String name
    ) {
        if (!requireOp(source)) {
            return 0;
        }
        ServerPlayer player = serverPlayer(source);
        if (player == null) {
            send(source, "World Remembers title test failed: command must be run by a player");
            return 0;
        }
        send(source, WorldRemembersLivingLegendsNeoForgePlaceTitles.sendLiteralTestMessage(
                player,
                PlaceType.fromId(requestedType),
                styleId,
                name,
                logger
        ));
        return 1;
    }

    private static int runDebugTitleNearest(CommandSourceStack source, Logger logger) {
        if (!requireOp(source)) {
            return 0;
        }
        ServerPlayer player = serverPlayer(source);
        if (player == null) {
            send(source, "World Remembers title nearest failed: command must be run by a player");
            return 0;
        }
        send(source, WorldRemembersLivingLegendsNeoForgePlaceTitles.sendNearestDebug(player, logger));
        return 1;
    }

    private static int runDebugTitleHere(CommandSourceStack source, Logger logger) {
        if (!requireOp(source)) {
            return 0;
        }
        ServerPlayer player = serverPlayer(source);
        if (player == null) {
            send(source, "World Remembers title here failed: command must be run by a player");
            return 0;
        }
        send(source, WorldRemembersLivingLegendsNeoForgePlaceTitles.debugHere(player, logger));
        return 1;
    }

    private static int runDebugTitleClear(CommandSourceStack source, Logger logger) {
        if (!requireOp(source)) {
            return 0;
        }
        ServerPlayer player = serverPlayer(source);
        if (player == null) {
            send(source, "World Remembers title clear failed: command must be run by a player");
            return 0;
        }
        send(source, WorldRemembersLivingLegendsNeoForgePlaceTitles.clearMessage(player, logger));
        return 1;
    }

    private static int runDebugTitleState(CommandSourceStack source, Logger logger) {
        if (!requireOp(source)) {
            return 0;
        }
        ServerPlayer player = serverPlayer(source);
        if (player == null) {
            send(source, "World Remembers title state failed: command must be run by a player");
            return 0;
        }
        send(source, WorldRemembersLivingLegendsNeoForgePlaceTitles.debugState(player));
        return 1;
    }

    private static PlaceCluster debugCluster(PlaceType type, DeathSiteEnvironment environment, WorldPos position, String prefix) {
        int chunkX = Math.floorDiv(position.x(), CHUNK_SIZE);
        int chunkZ = Math.floorDiv(position.z(), CHUNK_SIZE);
        return new PlaceCluster(
                prefix + "_" + type.idString() + "_" + environment.idString(),
                type,
                position.dimensionId(),
                environment,
                position.x(),
                position.y(),
                position.z(),
                position.y(),
                position.y(),
                32,
                0.0,
                0.0,
                0L,
                0L,
                false,
                type.priorityRank(),
                position.dimensionId() + "@chunk:" + chunkX + "," + chunkZ,
                PlaceStats.empty(),
                List.of(position.dimensionId() + "@chunk:" + chunkX + "," + chunkZ)
        );
    }

    private static void logNameRejectionDetails(Logger logger, NameGenerationDiagnostics diagnostics) {
        if (logger == null
                || diagnostics == null
                || !WorldRemembersLivingLegends.config().debug.namingVerbose) {
            return;
        }
        for (String detail : diagnostics.rejectionDetails()) {
            logger.info("NameGenerator " + detail);
        }
    }

    private static String normalizedResolvedName(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static String fallbackReason(
            NameRecipe recipe,
            String requestedStyleId,
            String activeStyleId,
            NameGenerationDiagnostics diagnostics
    ) {
        if (recipe == null || recipe.patternKey().isBlank()) {
            return "missing_pattern_key";
        }
        if ("living_legends.name.pattern.safe_fallback".equals(recipe.patternKey())
                || diagnostics.selectedPatternSource() == com.worldremembers.livinglegends.NamePatternSource.SAFE_FALLBACK) {
            return "safe_fallback";
        }
        String requested = requestedStyleId == null || requestedStyleId.isBlank()
                ? BuiltInNameData.DEFAULT_STYLE_ID
                : requestedStyleId;
        String active = activeStyleId == null || activeStyleId.isBlank()
                ? BuiltInNameData.DEFAULT_STYLE_ID
                : activeStyleId;
        if (!requested.equals(active)) {
            return "style_fallback_to_" + active;
        }
        return "";
    }

    private static String[] splitNameCauseTail(String value) {
        if (value == null || value.isBlank()) {
            return new String[0];
        }
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        char quote = 0;
        String trimmed = value.trim();
        for (int index = 0; index < trimmed.length(); index++) {
            char character = trimmed.charAt(index);
            if ((character == '"' || character == '\'') && (!quoted || quote == character)) {
                quoted = !quoted;
                quote = quoted ? character : 0;
                continue;
            }
            if (Character.isWhitespace(character) && !quoted) {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(character);
        }
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        return parts.toArray(String[]::new);
    }

    private static String runtimeNameOption(String[] parts, int startIndex) {
        if (parts == null) {
            return "";
        }
        for (int index = Math.max(0, startIndex); index < parts.length; index++) {
            String part = parts[index] == null ? "" : parts[index].trim();
            if (part.startsWith("name=")) {
                return RuntimeNameFormatter.sanitize(part.substring("name=".length()));
            }
        }
        return "";
    }

    private static int parsePositiveInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Math.max(1, Integer.parseInt(value.trim()));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static PlaceCause debugCauseFor(PlaceType placeType, PlaceCauseType causeType, String targetId, String runtimeName) {
        String target = targetId == null ? "" : targetId.trim().toLowerCase(Locale.ROOT);
        String name = RuntimeNameFormatter.sanitize(runtimeName);
        return switch (causeType) {
            case PLAYER_DEATHS -> new PlaceCause(causeType, EventType.PLAYER_DEATH, "", "", "", "", "", "", "", target, "", "", Map.of("debug_target=" + target, 1L));
            case FIRST_STRUCTURE_DISCOVERY -> new PlaceCause(causeType, EventType.STRUCTURE_DISCOVERED, firstDiscoveryKeyForTarget(causeType, target), "structure", target, "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", Map.of("debug_target=" + target, 1L));
            case MINING, FIRST_BLOCK_DISCOVERY -> new PlaceCause(causeType, EventType.VALUABLE_BLOCK_MINED, firstDiscoveryKeyForTarget(causeType, target), causeType == PlaceCauseType.FIRST_BLOCK_DISCOVERY ? "block" : "", "", target, "", "", "", "", "", "", target, "", "", "", "", "", "", "", "", Map.of("debug_target=" + target, 1L));
            case MOB_BATTLE -> new PlaceCause(causeType, EventType.PLAYER_KILLED_HOSTILE_MOB, "", "", "", "", target, "", target, "", target, "", "", "", "", "", "", "", "", "", "", Map.of("debug_target=" + target, 1L));
            case PASSIVE_SLAUGHTER -> new PlaceCause(causeType, EventType.PLAYER_KILLED_PASSIVE_MOB, "", "", "", "", "", "", target, target, "", "", "", "", "", "", "", "", "", "", "", Map.of("debug_target=" + target, 1L));
            case PORTAL_USAGE, DIMENSION_THRESHOLD, FIRST_DIMENSION_DISCOVERY -> new PlaceCause(causeType, EventType.PLAYER_ENTERED_DIMENSION, firstDiscoveryKeyForTarget(causeType, target), causeType == PlaceCauseType.FIRST_DIMENSION_DISCOVERY ? "dimension" : "", "", "", "", "", "", "", "", "", "", portalTypeForTarget(target), "", target, "", "", "", "", "", Map.of("debug_target=" + target, 1L));
            case BOSS_KILL, FIRST_BOSS_KILL -> new PlaceCause(causeType, EventType.BOSS_KILLED, firstDiscoveryKeyForTarget(causeType, target), causeType == PlaceCauseType.FIRST_BOSS_KILL ? "boss_kill" : "", "", "", target, target, target, "", "", "", "", "", "", "", "", "", "", "", "", Map.of("debug_target=" + target, 1L));
            case PET_DEATH -> new PlaceCause(
                    causeType, EventType.PET_DIED,
                    "", "", "", "", "", "", target, "", "", "", "", "", "", "",
                    "", name, target, "", "",
                    Map.of("debug_target=" + target, 1L)
            );
            case NAMED_MOB_DEATH -> new PlaceCause(
                    causeType, EventType.NAMED_MOB_DIED,
                    "", "", "", "", "", "", target, "", "", "", "", "", "", "",
                    "", "", "", name, target,
                    Map.of("debug_target=" + target, 1L)
            );
            default -> new PlaceCause(causeType, EventType.CUSTOM, "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", Map.of("debug_target=" + target, 1L));
        };
    }

    private static DeathSiteEnvironment debugEnvironmentForCause(PlaceType placeType, PlaceCauseType causeType, String targetId) {
        if (placeType != PlaceType.DEATH_SITE || causeType != PlaceCauseType.PLAYER_DEATHS) {
            return DeathSiteEnvironment.UNKNOWN;
        }
        String target = targetId == null ? "" : targetId.trim().toLowerCase(Locale.ROOT);
        return switch (target) {
            case "lava", "fire" -> DeathSiteEnvironment.NETHER;
            case "drowning", "drown", "water" -> DeathSiteEnvironment.WATER;
            case "void" -> DeathSiteEnvironment.END;
            case "fall", "" -> DeathSiteEnvironment.SURFACE;
            default -> DeathSiteEnvironment.SURFACE;
        };
    }

    private static boolean highFrequencyNameScenario(PlaceType placeType, PlaceCauseType causeType, String targetId) {
        if (placeType == PlaceType.DEATH_SITE && causeType == PlaceCauseType.PLAYER_DEATHS) {
            return true;
        }
        return switch (placeType) {
            case BATTLEFIELD, MINING_SITE, PORTAL_LANDMARK, DIMENSION_THRESHOLD, GENERAL_LANDMARK -> true;
            default -> false;
        };
    }

    private static String firstDiscoveryKeyForTarget(PlaceCauseType causeType, String targetId) {
        String target = targetId == null ? "" : targetId.trim().toLowerCase(Locale.ROOT);
        if (causeType == PlaceCauseType.FIRST_STRUCTURE_DISCOVERY && "minecraft:stronghold".equals(target)) {
            return "world:first_stronghold_found";
        }
        if (causeType == PlaceCauseType.FIRST_BLOCK_DISCOVERY) {
            if ("minecraft:diamond_ore".equals(target) || "minecraft:deepslate_diamond_ore".equals(target)) {
                return "world:first_diamond_ore_mined";
            }
            if ("minecraft:ancient_debris".equals(target)) {
                return "world:first_ancient_debris_mined";
            }
        }
        if (causeType == PlaceCauseType.FIRST_DIMENSION_DISCOVERY) {
            if ("minecraft:the_nether".equals(target) || "nether".equals(target)) {
                return "world:first_nether_entry";
            }
            if ("minecraft:the_end".equals(target) || "end".equals(target)) {
                return "world:first_end_entry";
            }
        }
        if (causeType == PlaceCauseType.FIRST_BOSS_KILL && "minecraft:ender_dragon".equals(target)) {
            return "world:first_ender_dragon_killed";
        }
        return "";
    }

    private static String portalTypeForTarget(String targetId) {
        if ("minecraft:the_nether".equals(targetId) || "nether".equals(targetId)) {
            return "nether";
        }
        if ("minecraft:the_end".equals(targetId) || "end".equals(targetId)) {
            return "end";
        }
        return "dimension";
    }

    private static String fallbackBehavior(String requestedStyleId, String activeStyleId) {
        String requested = requestedStyleId == null || requestedStyleId.isBlank()
                ? BuiltInNameData.DEFAULT_STYLE_ID
                : requestedStyleId;
        String active = activeStyleId == null || activeStyleId.isBlank()
                ? BuiltInNameData.DEFAULT_STYLE_ID
                : activeStyleId;
        return requested.equals(active) ? "none" : "style_to_" + active;
    }

    private static String normalizedStyleId(String requestedStyle) {
        if (requestedStyle == null || requestedStyle.isBlank()) {
            String configuredStyle = WorldRemembersLivingLegends.config().naming.defaultStyle;
            if (configuredStyle == null || configuredStyle.isBlank()) {
                return BuiltInNameData.DEFAULT_STYLE_ID;
            }
            return configuredStyle.trim().toLowerCase(Locale.ROOT);
        }
        return requestedStyle.trim().toLowerCase(Locale.ROOT);
    }

    private static NamedPlace resolvePlaceReference(
            CommandSourceStack source,
            ServerLevel world,
            String requestedReference,
            Logger logger,
            boolean sendNotFound
    ) {
        String reference = requestedReference == null ? "" : requestedReference.trim();
        if (reference.isBlank()) {
            if (sendNotFound) {
                sendPlaceNotFound(source, world, reference, logger);
            }
            return null;
        }

        if (reference.startsWith("#")) {
            NamedPlace indexed = resolveIndexedPlace(source, world, reference, logger);
            if (indexed != null) {
                return indexed;
            }
            if (sendNotFound) {
                send(source, "World Remembers place index not found: " + reference
                        + ". Run /places list again, then use /places info #1.");
            }
            return null;
        }

        if ("nearest".equalsIgnoreCase(reference) || "here".equalsIgnoreCase(reference)) {
            NamedPlace nearest = nearestPlace(source, world, "here".equalsIgnoreCase(reference), logger);
            if (nearest != null) {
                return nearest;
            }
            if (sendNotFound) {
                send(source, "World Remembers found no nearby place in this dimension.");
            }
            return null;
        }

        NamedPlace exact = WorldRemembersLivingLegendsNeoForgeStorage.place(world, reference, logger);
        if (exact != null) {
            return exact;
        }

        NamedPlace byName = exactManualOrDisplayName(world, reference, logger);
        if (byName != null) {
            return byName;
        }

        if (sendNotFound) {
            sendPlaceNotFound(source, world, reference, logger);
        }
        return null;
    }

    private static List<NamedPlace> visiblePlaces(ServerLevel world, Logger logger) {
        return WorldRemembersLivingLegendsNeoForgeStorage.places(world, logger).stream()
                .filter(place -> WorldRemembersLivingLegends.config().placeTypes.shouldDisplayExisting(place.placeType()))
                .toList();
    }

    private static NamedPlace resolveIndexedPlace(CommandSourceStack source, ServerLevel world, String reference, Logger logger) {
        Integer index = parseIndexReference(reference);
        if (index == null) {
            return null;
        }
        List<String> ids = LAST_LIST_INDEXES.get(sourceKey(source));
        if (ids == null || index < 1 || index > ids.size()) {
            return null;
        }
        return WorldRemembersLivingLegendsNeoForgeStorage.place(world, ids.get(index - 1), logger);
    }

    private static Integer parseIndexReference(String reference) {
        if (reference == null || !reference.startsWith("#")) {
            return null;
        }
        try {
            return Integer.parseInt(reference.substring(1).trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static NamedPlace nearestPlace(
            CommandSourceStack source,
            ServerLevel world,
            boolean requireInside,
            Logger logger
    ) {
        WorldPos position = sourcePosition(source);
        List<NamedPlace> places = visiblePlaces(world, logger).stream()
                .filter(place -> place != null && position.dimensionId().equals(place.dimensionId()))
                .sorted(Comparator.comparingLong(place -> place.center().squaredDistanceTo(position)))
                .toList();
        if (requireInside) {
            for (NamedPlace place : places) {
                if (place.contains(position)) {
                    return place;
                }
            }
        }
        return places.isEmpty() ? null : places.get(0);
    }

    private static NamedPlace exactManualOrDisplayName(ServerLevel world, String reference, Logger logger) {
        String normalized = reference.trim();
        for (NamedPlace place : visiblePlaces(world, logger)) {
            if (place.manualName().equalsIgnoreCase(normalized)
                    || displayNameString(place).equalsIgnoreCase(normalized)) {
                return place;
            }
        }
        return null;
    }

    private static void sendPlaceNotFound(CommandSourceStack source, ServerLevel world, String reference, Logger logger) {
        send(source, "World Remembers place not found: " + reference);
        send(source, "Use /places list and copy the full id, or use /places info nearest.");
        List<String> suggestions = matchingPlaceSuggestions(world, reference, logger);
        if (!suggestions.isEmpty()) {
            send(source, "Possible matches: " + String.join(", ", suggestions));
        }
    }

    private static List<String> matchingPlaceSuggestions(ServerLevel world, String reference, Logger logger) {
        String normalized = reference == null ? "" : reference.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return List.of();
        }
        List<String> suggestions = new ArrayList<>();
        for (NamedPlace place : visiblePlaces(world, logger)) {
            String manual = place.manualName().toLowerCase(Locale.ROOT);
            String display = displayNameString(place).toLowerCase(Locale.ROOT);
            if ((!manual.isBlank() && (manual.contains(normalized) || normalized.contains(manual)))
                    || (!display.isBlank() && (display.contains(normalized) || normalized.contains(display)))) {
                suggestions.add(place.placeIdString());
            }
            if (suggestions.size() >= 5) {
                break;
            }
        }
        return suggestions;
    }

    private static void rememberListIndexes(CommandSourceStack source, List<String> placeIds) {
        synchronized (LAST_LIST_INDEXES) {
            LAST_LIST_INDEXES.put(sourceKey(source), List.copyOf(placeIds));
            while (LAST_LIST_INDEXES.size() > MAX_INDEX_SESSIONS) {
                String firstKey = LAST_LIST_INDEXES.keySet().iterator().next();
                LAST_LIST_INDEXES.remove(firstKey);
            }
        }
    }

    private static String sourceKey(CommandSourceStack source) {
        if (source.getEntity() != null) {
            return source.getEntity().getUUID().toString();
        }
        return source.getTextName();
    }

    private static ServerPlayer serverPlayer(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player ? player : null;
    }

    private static boolean requireOp(CommandSourceStack source) {
        int level = Math.max(0, Math.min(4, WorldRemembersLivingLegends.config().commands.requiredOpLevel));
        if (source.hasPermission(level)) {
            return true;
        }
        send(source, "World Remembers: permission denied (requires OP level " + level + ")");
        return false;
    }

    private static WorldPos sourcePosition(CommandSourceStack source) {
        ServerLevel world = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        return new WorldPos(
                world.dimension().location().toString(),
                pos.getX(),
                pos.getY(),
                pos.getZ()
        );
    }

    private static Component displayName(NamedPlace place) {
        if (place.manuallyRenamed()) {
            return Component.literal(place.manualName());
        }
        return resolveName(place.nameRecipe());
    }

    private static Component resolveName(NameRecipe recipe) {
        NameRecipe resolvedRecipe = recipe == null ? NameRecipe.empty() : recipe;
        NameDataPack data = BuiltInNameData.packForStyle(resolvedRecipe.styleId());
        Map<String, NameToken> tokens = data.tokenMap();
        List<Component> tokenTexts = new ArrayList<>();
        for (int index = 0; index < resolvedRecipe.selectedTokenIds().size(); index++) {
            String tokenId = resolvedRecipe.selectedTokenIds().get(index);
            if (NameRecipe.isLiteralToken(tokenId)) {
                tokenTexts.add(Component.literal(NameRecipe.literalTokenValue(tokenId)));
                continue;
            }
            NameTokenForm form = index < resolvedRecipe.requestedTokenForms().size()
                    ? resolvedRecipe.requestedTokenForms().get(index)
                    : NameTokenForm.BASE;
            NameToken token = tokens.get(tokenId);
            tokenTexts.add(token == null
                    ? Component.literal(tokenId.replace('_', ' '))
                    : Component.translatable(token.translationKey(form)));
        }

        if (resolvedRecipe.patternKey().isBlank()) {
            return Component.literal(resolvedRecipe.fallbackResolvedName());
        }
        return Component.translatable(resolvedRecipe.patternKey(), tokenTexts.toArray());
    }

    private static String displayNameString(NamedPlace place) {
        if (place.manuallyRenamed()) {
            return place.manualName();
        }
        NameRecipe recipe = place.nameRecipe();
        if (recipe == null) {
            return "";
        }
        if (!recipe.fallbackResolvedName().isBlank()) {
            return recipe.fallbackResolvedName();
        }
        return recipe.patternKey();
    }

    private static List<String> recipeForms(NameRecipe recipe) {
        List<String> forms = new ArrayList<>();
        for (NameTokenForm form : recipe.requestedTokenForms()) {
            forms.add(form.idString());
        }
        return forms;
    }

    private static DeathSiteEnvironment inferEnvironment(String dimensionId, int y) {
        if ("minecraft:the_nether".equals(dimensionId)) {
            return DeathSiteEnvironment.NETHER;
        }
        if ("minecraft:the_end".equals(dimensionId)) {
            return DeathSiteEnvironment.END;
        }
        if (y < 50) {
            return DeathSiteEnvironment.CAVE;
        }
        if (y > 110) {
            return DeathSiteEnvironment.MOUNTAIN;
        }
        return DeathSiteEnvironment.SURFACE;
    }

    private static Component placeLine(NamedPlace place, int displayIndex) {
        return Component.literal("[" + displayIndex + "] ")
                .append(Component.literal(place.placeIdString()).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" [" + place.placeType().name() + "] "))
                .append(displayName(place))
                .append(Component.literal(" env=" + place.environment().name()))
                .append(Component.literal(" dim=" + place.dimensionId()))
                .append(Component.literal(" center=" + place.centerString()))
                .append(Component.literal(" radius=" + place.radius()))
                .append(Component.literal(" score=" + formatScore(place.score())));
    }

    private static List<String> placeTypeIds() {
        List<String> ids = new ArrayList<>();
        for (PlaceType type : PlaceType.values()) {
            ids.add(type.idString());
            ids.add(type.name().toLowerCase(Locale.ROOT));
        }
        return ids;
    }

    private static String formatScore(double value) {
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 0.000_001) {
            return Long.toString((long) rounded);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String formatDistance(double value) {
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 0.000_001) {
            return Long.toString((long) rounded);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static Path resolveGameDir() {
        try {
            Path gameDir = FMLPaths.GAMEDIR.get();
            if (gameDir != null) {
                return gameDir;
            }
        } catch (RuntimeException | LinkageError ignored) {
            // Current working directory is a safe fallback for config reload.
        }
        return Path.of(".");
    }

    private static void send(CommandSourceStack source, String message) {
        if (message == null) {
            return;
        }
        if (message.contains("\n")) {
            for (String line : message.split("\\R")) {
                send(source, line);
            }
            return;
        }
        send(source, Component.literal(message));
    }

    private static void send(CommandSourceStack source, Component message) {
        source.sendSuccess(() -> message, false);
    }
}
