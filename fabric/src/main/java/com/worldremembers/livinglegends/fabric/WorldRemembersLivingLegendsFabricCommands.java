package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.BuiltInNameData;
import com.worldremembers.livinglegends.DeathSiteEnvironment;
import com.worldremembers.livinglegends.DeletedPlaceMarker;
import com.worldremembers.livinglegends.EventCollector;
import com.worldremembers.livinglegends.EventType;
import com.worldremembers.livinglegends.NameDataPack;
import com.worldremembers.livinglegends.NameGenerationDiagnostics;
import com.worldremembers.livinglegends.NameGenerator;
import com.worldremembers.livinglegends.NamePattern;
import com.worldremembers.livinglegends.NamePatternSource;
import com.worldremembers.livinglegends.NameRecipe;
import com.worldremembers.livinglegends.NameContext;
import com.worldremembers.livinglegends.NameStyle;
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
import com.worldremembers.livinglegends.ScoreEngine;
import com.worldremembers.livinglegends.VanillaMobThemeRegistry;
import com.worldremembers.livinglegends.WorldPos;
import com.worldremembers.livinglegends.WorldRemembersCompatRegistries;
import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import com.worldremembers.livinglegends.config.LivingLegendsConfig;
import com.worldremembers.livinglegends.config.LivingLegendsConfigManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.slf4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class WorldRemembersLivingLegendsFabricCommands {
    private static final Pattern NAMESPACED_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_/.-]+");
    private static final int CHUNK_SIZE = 16;
    private static final Map<String, List<String>> LAST_LIST_INDEXES = new LinkedHashMap<>();
    private static final int MAX_INDEX_SESSIONS = 64;
    private static final List<NamePatternSource> NAME_SOURCE_ORDER = List.of(
            NamePatternSource.EXACT_CAUSE,
            NamePatternSource.DOMINANT_TARGET,
            NamePatternSource.CAUSE_TYPE,
            NamePatternSource.PLACE_TYPE_ENVIRONMENT,
            NamePatternSource.PLACE_TYPE_GENERIC,
            NamePatternSource.SAFE_FALLBACK
    );

    private WorldRemembersLivingLegendsFabricCommands() {
    }

    static void register(Logger logger) {
        try {
            Class<?> callbackClass = Class.forName("net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback");
            Object event = callbackClass.getField("EVENT").get(null);
            Object listener = Proxy.newProxyInstance(
                    callbackClass.getClassLoader(),
                    new Class<?>[]{callbackClass},
                    commandRegistrationHandler(logger)
            );
            Method register = event.getClass().getMethod("register", Object.class);
            register.setAccessible(true);
            register.invoke(event, listener);
        } catch (ReflectiveOperationException | LinkageError exception) {
            logger.warn("Failed to register /places debug command: "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static InvocationHandler commandRegistrationHandler(Logger logger) {
        return (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return handleObjectMethod(proxy, method, args);
            }

            if ("register".equals(method.getName()) && args != null && args.length > 0) {
                try {
                    registerPlacesCommand(args[0], logger);
                } catch (RuntimeException exception) {
                    logger.warn("Failed to build /places debug command: "
                            + exception.getClass().getSimpleName() + ": " + exception.getMessage());
                }
            }

            return null;
        };
    }

    private static void registerPlacesCommand(Object dispatcher, Logger logger) {
        Object places = literal("places");
        Object list = literal("list");
        Object nearest = literal("nearest");
        Object info = literal("info");
        Object rename = literal("rename");
        Object delete = literal("delete");
        Object create = literal("create");
        Object regenerate = literal("regenerate");
        Object deleted = literal("deleted");
        Object deletedList = literal("list");
        Object deletedClear = literal("clear");
        Object deletedClearAll = literal("all");
        Object export = literal("export");
        Object importCommand = literal("import");
        Object reload = literal("reload");
        Object debug = literal("debug");
        Object chunk = literal("chunk");
        Object score = literal("score");
        Object clusters = literal("clusters");
        Object cause = literal("cause");
        Object nearby = literal("nearby");
        Object farm = literal("farm");
        Object configDebug = literal("config");
        Object configValidateDebug = literal("validate");
        Object validateDebug = literal("validate");
        Object selfTestDebug = literal("selftest");
        Object repairDebug = literal("repair");
        Object repairDryRunDebug = literal("dryrun");
        Object compatDebug = literal("compat");
        Object compatSummaryDebug = literal("summary");
        Object compatLookupDebug = literal("lookup");
        Object migrationDebug = literal("migration");
        Object decayDebug = literal("decay");
        Object spacingDebug = literal("spacing");
        Object titleDebug = literal("title");
        Object name = literal("name");
        Object nameBatch = literal("namebatch");
        Object nameCause = literal("namecause");
        Object nameAudit = literal("nameaudit");
        Object mobTheme = literal("mobtheme");
        Object mobThemes = literal("mobthemes");
        Object missingMobThemes = literal("missing");
        Object listExecutes = command(context -> runList(context, logger, null, 1));
        Object nearestExecutes = command(context -> runNearest(context, logger, null));
        Object deletedListExecutes = command(context -> runDeletedList(context, logger));
        Object deletedClearAllExecutes = command(context -> runDeletedClearAll(context, logger));
        Object exportExecutes = command(context -> runExport(context, logger));
        Object importExecutes = command(context -> runImport(context, logger));
        Object reloadExecutes = command(context -> runReload(context, logger));
        Object chunkExecutes = command(context -> runDebugChunk(context, logger));
        Object scoreExecutes = command(context -> runDebugScore(context, logger, null, null));
        Object clustersExecutes = command(context -> runDebugClusters(context, logger));
        Object causeExecutes = command(context -> runDebugCause(context, logger));
        Object nearbyExecutes = command(context -> runDebugNearby(context, logger, 0));
        Object farmExecutes = command(context -> runDebugFarm(context, logger));
        Object configDebugExecutes = command(context -> runDebugConfig(context, logger));
        Object configValidateDebugExecutes = command(context -> runDebugConfigValidate(context, logger));
        Object validateDebugExecutes = command(context -> runDebugValidate(context, logger));
        Object selfTestDebugExecutes = command(context -> runDebugSelfTest(context, logger));
        Object repairDryRunDebugExecutes = command(context -> runDebugRepairDryRun(context, logger));
        Object compatSummaryDebugExecutes = command(context -> runDebugCompatSummary(context, logger));
        Object migrationDebugExecutes = command(context -> runDebugMigration(context, logger));
        Object nameAuditExecutes = command(context -> runDebugNameAudit(context, logger, null));
        Object missingMobThemesExecutes = command(context -> runDebugMobThemesMissing(context, logger));

        invoke(list, "executes", listExecutes);
        invoke(nearest, "executes", nearestExecutes);
        invoke(export, "executes", exportExecutes);
        invoke(importCommand, "executes", importExecutes);
        invoke(reload, "executes", reloadExecutes);
        attachListArguments(list, logger);
        attachNearestArgument(nearest, logger);
        attachInfoArgument(info, logger);
        attachRenameArguments(rename, logger);
        attachDeleteArgument(delete, logger);
        attachCreateArguments(create, logger);
        attachRegenerateArguments(regenerate, logger);
        invoke(deletedList, "executes", deletedListExecutes);
        invoke(deletedClearAll, "executes", deletedClearAllExecutes);
        attachDeletedClearArgument(deletedClear, logger);
        invoke(deletedClear, "then", deletedClearAll);
        invoke(deleted, "then", deletedList);
        invoke(deleted, "then", deletedClear);
        invoke(chunk, "executes", chunkExecutes);
        invoke(score, "executes", scoreExecutes);
        invoke(clusters, "executes", clustersExecutes);
        invoke(cause, "executes", causeExecutes);
        invoke(nearby, "executes", nearbyExecutes);
        invoke(farm, "executes", farmExecutes);
        invoke(configDebug, "executes", configDebugExecutes);
        invoke(configValidateDebug, "executes", configValidateDebugExecutes);
        invoke(configDebug, "then", configValidateDebug);
        invoke(validateDebug, "executes", validateDebugExecutes);
        invoke(selfTestDebug, "executes", selfTestDebugExecutes);
        invoke(repairDryRunDebug, "executes", repairDryRunDebugExecutes);
        invoke(repairDebug, "then", repairDryRunDebug);
        invoke(compatSummaryDebug, "executes", compatSummaryDebugExecutes);
        invoke(compatDebug, "executes", compatSummaryDebugExecutes);
        invoke(compatDebug, "then", compatSummaryDebug);
        attachCompatLookupArguments(compatLookupDebug, logger);
        invoke(compatDebug, "then", compatLookupDebug);
        invoke(migrationDebug, "executes", migrationDebugExecutes);
        attachScoreArguments(score, logger);
        attachDecayDebugArguments(decayDebug, logger);
        attachSpacingDebugArguments(spacingDebug, logger);
        attachTitleDebugArguments(titleDebug, logger);
        attachNameArguments(name, logger);
        attachNameBatchArguments(nameBatch, logger);
        attachNameCauseArguments(nameCause, logger);
        invoke(nameAudit, "executes", nameAuditExecutes);
        attachNameAuditArguments(nameAudit, logger);
        attachMobThemeArguments(mobTheme, logger);
        invoke(missingMobThemes, "executes", missingMobThemesExecutes);
        invoke(mobThemes, "then", missingMobThemes);
        invoke(places, "then", list);
        invoke(places, "then", nearest);
        invoke(places, "then", info);
        invoke(places, "then", rename);
        invoke(places, "then", delete);
        invoke(places, "then", create);
        invoke(places, "then", regenerate);
        invoke(places, "then", deleted);
        invoke(places, "then", export);
        invoke(places, "then", importCommand);
        invoke(places, "then", reload);
        invoke(debug, "then", chunk);
        invoke(debug, "then", score);
        invoke(debug, "then", clusters);
        invoke(debug, "then", cause);
        invoke(debug, "then", nearby);
        invoke(debug, "then", farm);
        invoke(debug, "then", configDebug);
        invoke(debug, "then", validateDebug);
        invoke(debug, "then", selfTestDebug);
        invoke(debug, "then", repairDebug);
        invoke(debug, "then", compatDebug);
        invoke(debug, "then", migrationDebug);
        invoke(debug, "then", decayDebug);
        invoke(debug, "then", spacingDebug);
        invoke(debug, "then", titleDebug);
        invoke(debug, "then", name);
        invoke(debug, "then", nameBatch);
        invoke(debug, "then", nameCause);
        invoke(debug, "then", nameAudit);
        invoke(debug, "then", mobTheme);
        invoke(debug, "then", mobThemes);
        invoke(places, "then", debug);
        invoke(dispatcher, "register", places);
    }

    private static Object literal(String name) {
        try {
            Class<?> commandManager = Class.forName("net.minecraft.class_2170");
            Method literal = commandManager.getMethod("method_9247", String.class);
            return literal.invoke(null, name);
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new IllegalStateException("Could not create command literal " + name, exception);
        }
    }

    private static Object argument(String name, Object argumentType) {
        try {
            Class<?> commandManager = Class.forName("net.minecraft.class_2170");
            Class<?> argumentTypeClass = Class.forName("com.mojang.brigadier.arguments.ArgumentType");
            Method argument = commandManager.getMethod("method_9244", String.class, argumentTypeClass);
            return argument.invoke(null, name, argumentType);
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new IllegalStateException("Could not create command argument " + name, exception);
        }
    }

    private static Object integerArgument() {
        try {
            Class<?> integerArgumentType = Class.forName("com.mojang.brigadier.arguments.IntegerArgumentType");
            return integerArgumentType.getMethod("integer").invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new IllegalStateException("Could not create integer argument", exception);
        }
    }

    private static Object longArgument() {
        try {
            Class<?> longArgumentType = Class.forName("com.mojang.brigadier.arguments.LongArgumentType");
            return longArgumentType.getMethod("longArg").invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new IllegalStateException("Could not create long argument", exception);
        }
    }

    private static Object stringArgument() {
        try {
            Class<?> stringArgumentType = Class.forName("com.mojang.brigadier.arguments.StringArgumentType");
            return stringArgumentType.getMethod("word").invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new IllegalStateException("Could not create string argument", exception);
        }
    }

    private static Object greedyStringArgument() {
        try {
            Class<?> stringArgumentType = Class.forName("com.mojang.brigadier.arguments.StringArgumentType");
            return stringArgumentType.getMethod("greedyString").invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new IllegalStateException("Could not create greedy string argument", exception);
        }
    }

    private static void attachListArguments(Object list, Logger logger) {
        try {
            Object page = argument("page", integerArgument());
            invoke(page, "executes", command(context -> runList(
                    context,
                    logger,
                    null,
                    intArgumentValue(context, "page")
            )));
            invoke(list, "then", page);

            Object type = argument("type", stringArgument());
            Object typePage = argument("page", integerArgument());
            invoke(type, "executes", command(context -> runList(
                    context,
                    logger,
                    stringArgumentValue(context, "type"),
                    1
            )));
            invoke(typePage, "executes", command(context -> runList(
                    context,
                    logger,
                    stringArgumentValue(context, "type"),
                    intArgumentValue(context, "page")
            )));
            invoke(type, "then", typePage);
            invoke(list, "then", type);
        } catch (RuntimeException exception) {
            logger.warn("Failed to add /places list arguments: "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static void attachNearestArgument(Object nearest, Logger logger) {
        try {
            Object radius = argument("radius", integerArgument());
            invoke(radius, "executes", command(context -> runNearest(
                    context,
                    logger,
                    intArgumentValue(context, "radius")
            )));
            invoke(nearest, "then", radius);
        } catch (RuntimeException exception) {
            logger.warn("Failed to add /places nearest <radius>: "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static void attachInfoArgument(Object info, Logger logger) {
        try {
            Object id = argument("id", greedyStringArgument());
            invoke(id, "executes", command(context -> runInfo(
                    context,
                    logger,
                    stringArgumentValue(context, "id")
            )));
            invoke(info, "then", id);
        } catch (RuntimeException exception) {
            logger.warn("Failed to add /places info <id>: "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static void attachRenameArguments(Object rename, Logger logger) {
        try {
            Object targetAndName = argument("targetAndName", greedyStringArgument());
            invoke(targetAndName, "executes", command(context -> runRename(
                    context,
                    logger,
                    renameTailId(stringArgumentValue(context, "targetAndName")),
                    renameTailName(stringArgumentValue(context, "targetAndName"))
            )));
            invoke(rename, "then", targetAndName);
        } catch (RuntimeException exception) {
            logger.warn("Failed to add /places rename <id> <new_name>: "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static void attachCreateArguments(Object create, Logger logger) {
        try {
            Object type = argument("type", stringArgument());
            Object radius = argument("radius", integerArgument());
            Object name = argument("name", greedyStringArgument());
            invoke(name, "executes", command(context -> runCreate(
                    context,
                    logger,
                    stringArgumentValue(context, "type"),
                    intArgumentValue(context, "radius"),
                    stringArgumentValue(context, "name")
            )));
            invoke(radius, "then", name);
            invoke(type, "then", radius);
            invoke(create, "then", type);
        } catch (RuntimeException exception) {
            logger.warn("Failed to add /places create <type> <radius> <name>: "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static void attachRegenerateArguments(Object regenerate, Logger logger) {
        try {
            Object targetAndForce = argument("targetAndForce", greedyStringArgument());
            invoke(targetAndForce, "executes", command(context -> runRegenerate(
                    context,
                    logger,
                    regenerateTailTarget(stringArgumentValue(context, "targetAndForce")),
                    regenerateTailForce(stringArgumentValue(context, "targetAndForce"))
            )));
            invoke(regenerate, "then", targetAndForce);
        } catch (RuntimeException exception) {
            logger.warn("Failed to add /places regenerate <id|all> [force]: "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static void attachDeletedClearArgument(Object deletedClear, Logger logger) {
        try {
            Object id = argument("id", greedyStringArgument());
            invoke(id, "executes", command(context -> runDeletedClear(
                    context,
                    logger,
                    stringArgumentValue(context, "id")
            )));
            invoke(deletedClear, "then", id);
        } catch (RuntimeException exception) {
            logger.warn("Failed to add /places deleted clear <id>: "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static void attachScoreArguments(Object score, Logger logger) {
        try {
            Object chunkX = argument("chunkX", integerArgument());
            Object chunkZ = argument("chunkZ", integerArgument());
            invoke(chunkZ, "executes", command(context -> runDebugScore(
                    context,
                    logger,
                    intArgumentValue(context, "chunkX"),
                    intArgumentValue(context, "chunkZ")
            )));
            invoke(chunkX, "then", chunkZ);
            invoke(score, "then", chunkX);
        } catch (RuntimeException exception) {
            logger.warn("Failed to add /places debug score <chunkX> <chunkZ>: "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static void attachDecayDebugArguments(Object decayDebug, Logger logger) {
        try {
            Object status = literal("status");
            invoke(status, "executes", command(context -> runDebugDecayStatus(context, logger)));

            Object run = literal("run");
            invoke(run, "executes", command(context -> runDebugDecayRun(context, logger, 0L)));
            Object ticks = argument("ticks", longArgument());
            invoke(ticks, "executes", command(context -> runDebugDecayRun(
                    context,
                    logger,
                    longArgumentValue(context, "ticks") == null ? 0L : longArgumentValue(context, "ticks")
            )));
            invoke(run, "then", ticks);

            Object info = literal("info");
            Object infoChunk = literal("chunk");
            Object infoNearest = literal("nearest");
            invoke(infoChunk, "executes", command(context -> runDebugDecayInfoChunk(context, logger)));
            invoke(infoNearest, "executes", command(context -> runDebugDecayInfoNearest(context, logger)));
            invoke(info, "then", infoChunk);
            invoke(info, "then", infoNearest);

            Object touch = literal("touch");
            Object touchChunk = literal("chunk");
            invoke(touchChunk, "executes", command(context -> runDebugDecayTouchChunk(context, logger)));
            invoke(touch, "then", touchChunk);

            Object setScore = literal("setscore");
            Object setScoreChunk = literal("chunk");
            Object type = argument("type", stringArgument());
            Object scoreValue = argument("score", stringArgument());
            invoke(scoreValue, "executes", command(context -> runDebugDecaySetScoreChunk(
                    context,
                    logger,
                    stringArgumentValue(context, "type"),
                    stringArgumentValue(context, "score")
            )));
            invoke(type, "then", scoreValue);
            invoke(setScoreChunk, "then", type);
            invoke(setScore, "then", setScoreChunk);

            invoke(decayDebug, "then", status);
            invoke(decayDebug, "then", run);
            invoke(decayDebug, "then", info);
            invoke(decayDebug, "then", touch);
            invoke(decayDebug, "then", setScore);
        } catch (RuntimeException exception) {
            logger.warn("Failed to add /places debug decay commands: "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static void attachSpacingDebugArguments(Object spacingDebug, Logger logger) {
        try {
            Object here = literal("here");
            Object type = argument("type", stringArgument());
            invoke(type, "executes", command(context -> runDebugSpacingHere(
                    context,
                    logger,
                    stringArgumentValue(context, "type")
            )));
            invoke(here, "then", type);
            invoke(spacingDebug, "then", here);
        } catch (RuntimeException exception) {
            logger.warn("Failed to add /places debug spacing here <type>: "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static void attachTitleDebugArguments(Object titleDebug, Logger logger) {
        try {
            Object test = literal("test");
            Object style = argument("style", stringArgument());
            Object placeType = argument("placeType", stringArgument());
            Object name = argument("name", greedyStringArgument());
            invoke(name, "executes", command(context -> runDebugTitleTest(
                    context,
                    logger,
                    stringArgumentValue(context, "style"),
                    stringArgumentValue(context, "placeType"),
                    stringArgumentValue(context, "name")
            )));
            invoke(placeType, "then", name);
            invoke(style, "then", placeType);
            invoke(test, "then", style);

            Object nearest = literal("nearest");
            invoke(nearest, "executes", command(context -> runDebugTitleNearest(context, logger)));

            Object here = literal("here");
            invoke(here, "executes", command(context -> runDebugTitleHere(context, logger)));

            Object clear = literal("clear");
            invoke(clear, "executes", command(context -> runDebugTitleClear(context, logger)));

            Object state = literal("state");
            invoke(state, "executes", command(context -> runDebugTitleState(context, logger)));

            invoke(titleDebug, "then", test);
            invoke(titleDebug, "then", nearest);
            invoke(titleDebug, "then", here);
            invoke(titleDebug, "then", clear);
            invoke(titleDebug, "then", state);
        } catch (RuntimeException exception) {
            logger.warn("Failed to add /places debug title arguments: "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static void attachNameArguments(Object name, Logger logger) {
        try {
            Object type = argument("type", stringArgument());
            Object environment = argument("environment", stringArgument());
            Object seed = argument("seed", longArgument());
            invoke(environment, "executes", command(context -> runDebugName(
                    context,
                    logger,
                    stringArgumentValue(context, "type"),
                    stringArgumentValue(context, "environment"),
                    null,
                    null
            )));
            invoke(seed, "executes", command(context -> runDebugName(
                    context,
                    logger,
                    stringArgumentValue(context, "type"),
                    stringArgumentValue(context, "environment"),
                    null,
                    longArgumentValue(context, "seed")
            )));
            invoke(environment, "then", seed);

            Object style = argument("style", stringArgument());
            Object styleSeed = argument("seed", longArgument());
            invoke(style, "executes", command(context -> runDebugName(
                    context,
                    logger,
                    stringArgumentValue(context, "type"),
                    stringArgumentValue(context, "environment"),
                    stringArgumentValue(context, "style"),
                    null
            )));
            invoke(styleSeed, "executes", command(context -> runDebugName(
                    context,
                    logger,
                    stringArgumentValue(context, "type"),
                    stringArgumentValue(context, "environment"),
                    stringArgumentValue(context, "style"),
                    longArgumentValue(context, "seed")
            )));
            invoke(style, "then", styleSeed);
            invoke(environment, "then", style);
            invoke(type, "then", environment);
            invoke(name, "then", type);
        } catch (RuntimeException exception) {
            logger.warn("Failed to add /places debug name <type> <environment> [style] [seed]: "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static void attachNameBatchArguments(Object nameBatch, Logger logger) {
        try {
            Object type = argument("type", stringArgument());
            Object typeCount = argument("count", integerArgument());
            invoke(typeCount, "executes", command(context -> runDebugNameBatch(
                    context,
                    logger,
                    stringArgumentValue(context, "type"),
                    DeathSiteEnvironment.UNKNOWN.idString(),
                    null,
                    intArgumentValue(context, "count")
            )));
            invoke(type, "then", typeCount);

            Object environment = argument("environment", stringArgument());
            Object environmentCount = argument("count", integerArgument());
            invoke(environmentCount, "executes", command(context -> runDebugNameBatch(
                    context,
                    logger,
                    stringArgumentValue(context, "type"),
                    stringArgumentValue(context, "environment"),
                    null,
                    intArgumentValue(context, "count")
            )));
            invoke(environment, "then", environmentCount);

            Object style = argument("style", stringArgument());
            Object styleCount = argument("count", integerArgument());
            invoke(styleCount, "executes", command(context -> runDebugNameBatch(
                    context,
                    logger,
                    stringArgumentValue(context, "type"),
                    stringArgumentValue(context, "environment"),
                    stringArgumentValue(context, "style"),
                    intArgumentValue(context, "count")
            )));
            invoke(style, "then", styleCount);
            invoke(environment, "then", style);
            invoke(type, "then", environment);
            invoke(nameBatch, "then", type);
        } catch (RuntimeException exception) {
            logger.warn("Failed to add /places debug namebatch arguments: "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static void attachNameCauseArguments(Object nameCause, Logger logger) {
        try {
            Object placeType = argument("placeType", stringArgument());
            Object causeType = argument("causeType", stringArgument());
            Object targetStyleCount = argument("targetStyleCount", greedyStringArgument());
            invoke(targetStyleCount, "executes", command(context -> runDebugNameCause(
                    context,
                    logger,
                    stringArgumentValue(context, "placeType"),
                    stringArgumentValue(context, "causeType"),
                    stringArgumentValue(context, "targetStyleCount")
            )));
            invoke(causeType, "then", targetStyleCount);
            invoke(placeType, "then", causeType);
            invoke(nameCause, "then", placeType);
        } catch (RuntimeException exception) {
            logger.warn("Failed to add /places debug namecause <placeType> <causeType> <targetId> <style> <count>: "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static void attachNameAuditArguments(Object nameAudit, Logger logger) {
        try {
            Object contextLiteral = literal("context");
            Object contextType = argument("placeType", stringArgument());
            Object contextEnvironment = argument("environment", stringArgument());
            Object contextStyle = argument("style", stringArgument());
            invoke(contextStyle, "executes", command(context -> runDebugNameAuditContext(
                    context,
                    logger,
                    stringArgumentValue(context, "placeType"),
                    stringArgumentValue(context, "environment"),
                    stringArgumentValue(context, "style")
            )));
            invoke(contextEnvironment, "then", contextStyle);
            invoke(contextType, "then", contextEnvironment);
            invoke(contextLiteral, "then", contextType);
            invoke(nameAudit, "then", contextLiteral);

            Object causeLiteral = literal("cause");
            Object causePlaceType = argument("placeType", stringArgument());
            Object causeType = argument("causeType", stringArgument());
            Object causeTargetStyle = argument("targetStyle", greedyStringArgument());
            invoke(causeTargetStyle, "executes", command(context -> runDebugNameAuditCause(
                    context,
                    logger,
                    stringArgumentValue(context, "placeType"),
                    stringArgumentValue(context, "causeType"),
                    stringArgumentValue(context, "targetStyle")
            )));
            invoke(causeType, "then", causeTargetStyle);
            invoke(causePlaceType, "then", causeType);
            invoke(causeLiteral, "then", causePlaceType);
            invoke(nameAudit, "then", causeLiteral);

            Object style = argument("style", stringArgument());
            invoke(style, "executes", command(context -> runDebugNameAudit(
                    context,
                    logger,
                    stringArgumentValue(context, "style")
            )));
            invoke(nameAudit, "then", style);
        } catch (RuntimeException exception) {
            logger.warn("Failed to add /places debug nameaudit [style|context|cause]: "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static void attachMobThemeArguments(Object mobTheme, Logger logger) {
        try {
            Object entityId = argument("entityId", greedyStringArgument());
            invoke(entityId, "executes", command(context -> runDebugMobTheme(
                    context,
                    logger,
                    stringArgumentValue(context, "entityId")
            )));
            invoke(mobTheme, "then", entityId);
        } catch (RuntimeException exception) {
            logger.warn("Failed to add /places debug mobtheme <entityId>: "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static void attachCompatLookupArguments(Object compatLookup, Logger logger) {
        try {
            for (String kind : List.of("boss", "structure", "biome", "mob", "block", "dimension")) {
                Object kindLiteral = literal(kind);
                Object id = argument(kind + "Id", greedyStringArgument());
                invoke(id, "executes", command(context -> runDebugCompatLookup(
                        context,
                        logger,
                        kind,
                        stringArgumentValue(context, kind + "Id")
                )));
                invoke(kindLiteral, "then", id);
                invoke(compatLookup, "then", kindLiteral);
            }
        } catch (RuntimeException exception) {
            logger.warn("Failed to add /places debug compat lookup arguments: "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static void attachDeleteArgument(Object delete, Logger logger) {
        try {
            Object id = argument("id", greedyStringArgument());
            invoke(id, "executes", command(context -> runDelete(
                    context,
                    logger,
                    stringArgumentValue(context, "id")
            )));
            invoke(delete, "then", id);
        } catch (RuntimeException exception) {
            logger.warn("Failed to add /places delete <id>: "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static Object command(CommandRunner runner) {
        try {
            Class<?> commandClass = Class.forName("com.mojang.brigadier.Command");
            return Proxy.newProxyInstance(
                    commandClass.getClassLoader(),
                    new Class<?>[]{commandClass},
                    (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            return handleObjectMethod(proxy, method, args);
                        }

                        if ("run".equals(method.getName())) {
                            return runner.run(args == null ? null : args[0]);
                        }

                        return 0;
                    }
            );
        } catch (ClassNotFoundException | LinkageError exception) {
            throw new IllegalStateException("Could not create Brigadier command", exception);
        }
    }

    private static int runDebugChunk(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
        Object world = invokeNoArg(source, "method_9225");
        Object player = invokeNoArg(source, "method_44023");
        Object pos = invokeNoArg(player, "method_24515");

        String dimensionId = dimensionId(world);
        int chunkX = Math.floorDiv(coordinate(pos, "method_10263"), CHUNK_SIZE);
        int chunkZ = Math.floorDiv(coordinate(pos, "method_10260"), CHUNK_SIZE);
        String message = WorldRemembersLivingLegendsFabricStorage.debugChunk(world, dimensionId, chunkX, chunkZ, logger)
                + "\n" + WorldRemembersLivingLegendsFabricStorage.debugScore(world, dimensionId, chunkX, chunkZ, logger)
                + "\n" + WorldRemembersLivingLegendsFabricStorage.debugClusters(world, logger);
        sendFeedback(source, message);
        return 1;
    }

    private static int runDebugScore(Object context, Logger logger, Integer requestedChunkX, Integer requestedChunkZ) {
        Object source = invokeNoArg(context, "getSource");
        Object world = invokeNoArg(source, "method_9225");
        Object player = invokeNoArg(source, "method_44023");
        Object pos = invokeNoArg(player, "method_24515");

        String dimensionId = dimensionId(world);
        int chunkX = requestedChunkX == null ? Math.floorDiv(coordinate(pos, "method_10263"), CHUNK_SIZE) : requestedChunkX;
        int chunkZ = requestedChunkZ == null ? Math.floorDiv(coordinate(pos, "method_10260"), CHUNK_SIZE) : requestedChunkZ;
        String message = WorldRemembersLivingLegendsFabricStorage.debugScore(world, dimensionId, chunkX, chunkZ, logger);
        sendFeedback(source, message);
        return 1;
    }

    private static int runList(Object context, Logger logger, String requestedType, Integer requestedPage) {
        Object source = invokeNoArg(context, "getSource");
        Object world = invokeNoArg(source, "method_9225");
        PlaceType filterType = requestedType == null || requestedType.isBlank() ? null : PlaceType.fromId(requestedType);
        List<NamedPlace> places = visiblePlaces(world, logger).stream()
                .filter(place -> filterType == null || place.placeType() == filterType)
                .sorted(Comparator.comparing(NamedPlace::placeIdString))
                .toList();
        if (places.isEmpty()) {
            sendFeedback(source, filterType == null
                    ? "World Remembers places: none"
                    : "World Remembers places: none for type=" + filterType.name());
            return 1;
        }

        int pageSize = Math.max(1, WorldRemembersLivingLegends.config().display.maxPlacesInList);
        int pageCount = Math.max(1, (int) Math.ceil(places.size() / (double) pageSize));
        int page = Math.max(1, Math.min(pageCount, requestedPage == null ? 1 : requestedPage));
        int start = (page - 1) * pageSize;
        int end = Math.min(places.size(), start + pageSize);
        sendFeedback(source, "World Remembers places: total=" + places.size()
                + (filterType == null ? "" : " type=" + filterType.name())
                + " page=" + page + "/" + pageCount);
        List<String> pageIds = new ArrayList<>();
        for (int index = start; index < end; index++) {
            int displayIndex = index - start + 1;
            NamedPlace place = places.get(index);
            pageIds.add(place.placeIdString());
            sendFeedbackText(source, placeLine(place, displayIndex));
        }
        rememberListIndexes(source, pageIds);
        sendFeedback(source, "World Remembers tip: click a line, or use /places info #1, /places info nearest, /places info here.");
        return 1;
    }

    private static int runNearest(Object context, Logger logger, Integer requestedRadius) {
        Object source = invokeNoArg(context, "getSource");
        Object world = invokeNoArg(source, "method_9225");
        Object player = invokeNoArg(source, "method_44023");
        Object pos = invokeNoArg(player, "method_24515");
        String dimensionId = dimensionId(world);
        WorldPos position = new WorldPos(
                dimensionId,
                coordinate(pos, "method_10263"),
                coordinate(pos, "method_10264"),
                coordinate(pos, "method_10260")
        );
        int radius = Math.max(0, requestedRadius == null ? 0 : requestedRadius);
        long maxDistanceSquared = radius == 0 ? Long.MAX_VALUE : (long) radius * radius;
        NamedPlace nearest = visiblePlaces(world, logger).stream()
                .filter(place -> place != null && dimensionId.equals(place.dimensionId()))
                .filter(place -> radius == 0 || place.center().squaredDistanceTo(position) <= maxDistanceSquared || place.contains(position))
                .min(Comparator.comparingLong(place -> place.center().squaredDistanceTo(position)))
                .orElse(null);
        if (nearest == null) {
            sendFeedback(source, radius == 0
                    ? "World Remembers nearest: no places in this dimension"
                    : "World Remembers nearest: no places within radius=" + radius);
            return 1;
        }

        double distance = Math.sqrt(Math.max(0L, nearest.center().squaredDistanceTo(position)));
        sendFeedbackText(source, Text.literal("World Remembers nearest id=" + nearest.placeIdString() + " name=")
                .append(displayNameText(nearest))
                .append(Text.literal(" type=" + nearest.placeType().name()
                        + " distance=" + formatDistance(distance)
                        + " center=" + nearest.centerString()
                        + " radius=" + nearest.radius()
                        + " inside=" + nearest.contains(position))));
        return 1;
    }

    private static int runInfo(Object context, Logger logger, String placeId) {
        Object source = invokeNoArg(context, "getSource");
        Object world = invokeNoArg(source, "method_9225");
        NamedPlace place = resolvePlaceReference(source, world, placeId, logger, true);
        if (place == null) {
            return 0;
        }

        sendFeedbackText(source, Text.literal("World Remembers place info id=" + place.placeIdString() + " name=")
                .append(displayNameText(place)));
        sendFeedback(source, "type=" + place.placeType().name()
                + " environment=" + place.environment().name()
                + " dimension=" + place.dimensionId()
                + " center=" + place.centerString()
                + " radius=" + place.radius()
                + " score=" + formatScore(place.score()));
        sendFeedback(source, "bounds=" + place.bounds().boundsIdString());
        sendFeedback(source, "biomeId=" + place.biomeId()
                + " dominantBiomeId=" + place.dominantBiomeId()
                + " biomeGroup=" + place.biomeGroup()
                + " biomeTheme=" + place.biomeTheme()
                + " biomeSource=" + place.biomeSource());
        sendFeedback(source, "createdGameTime=" + place.createdAtGameTime()
                + " lastUpdatedGameTime=" + place.lastUpdatedGameTime()
                + " sourceChunks=" + place.sourceChunks());
        sendFeedback(source, "manualName=" + place.manualName()
                + " manuallyRenamed=" + place.manuallyRenamed());
        sendFeedback(source, "cause " + place.cause().debugString());
        sendFeedback(source, "NameRecipe style=" + place.nameRecipe().styleId()
                + " patternKey=" + place.nameRecipe().patternKey()
                + " tokenIds=" + place.nameRecipe().selectedTokenIds()
                + " forms=" + recipeForms(place.nameRecipe())
                + " seed=" + place.nameRecipe().seed()
                + " signature=" + place.nameRecipe().recipeSignature()
                + " selectedPatternSource=not_stored");
        return 1;
    }

    private static int runDelete(Object context, Logger logger, String placeId) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }

        Object world = invokeNoArg(source, "method_9225");
        NamedPlace place = resolvePlaceReference(source, world, placeId, logger, true);
        if (place == null) {
            return 0;
        }
        boolean deleted = WorldRemembersLivingLegendsFabricStorage.deletePlace(world, place.placeIdString(), logger);
        sendFeedback(source, deleted
                ? "World Remembers deleted place " + place.placeIdString()
                : "World Remembers place not found: " + place.placeIdString());
        return deleted ? 1 : 0;
    }

    private static int runRename(Object context, Logger logger, String placeId, String newName) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }

        Object world = invokeNoArg(source, "method_9225");
        if (newName == null || newName.isBlank()) {
            sendFeedback(source, "World Remembers rename usage: /places rename #1 New Name");
            return 0;
        }
        NamedPlace place = resolvePlaceReference(source, world, placeId, logger, true);
        if (place == null) {
            return 0;
        }

        String oldName = displayNameString(place);
        NamedPlace updated = place.withManualName(newName, WorldRemembersLivingLegendsFabricStorage.gameTimeFor(world));
        boolean changed = WorldRemembersLivingLegendsFabricStorage.upsertPlace(world, updated, "rename_place " + placeId, logger);
        if (changed && logger != null) {
            logger.info("World Remembers place renamed id=" + placeId + " oldName=" + oldName + " newManualName=" + updated.manualName());
        }
        if (changed) {
            FabricMapIntegrationManager.refreshDestinationFromWorld(world, updated, logger);
        }
        sendFeedback(source, changed
                ? "World Remembers renamed place " + placeId + " to \"" + updated.manualName() + "\""
                : "World Remembers rename made no changes for " + placeId);
        return changed ? 1 : 0;
    }

    private static int runCreate(Object context, Logger logger, String requestedType, Integer requestedRadius, String name) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }

        Object world = invokeNoArg(source, "method_9225");
        Object player = invokeNoArg(source, "method_44023");
        Object pos = invokeNoArg(player, "method_24515");
        String dimensionId = dimensionId(world);
        int x = coordinate(pos, "method_10263");
        int y = coordinate(pos, "method_10264");
        int z = coordinate(pos, "method_10260");
        int radius = Math.max(1, requestedRadius == null ? 16 : requestedRadius);
        PlaceType type = PlaceType.fromId(requestedType);
        if (!WorldRemembersLivingLegends.config().placeTypes.autoGenerationEnabled(type)
                && !WorldRemembersLivingLegends.config().placeTypes.allowManualCreateWhenDisabled) {
            sendFeedback(source, "World Remembers create rejected: place type " + type.name()
                    + " is disabled and allowManualCreateWhenDisabled=false.");
            return 0;
        }
        DeathSiteEnvironment environment = type == PlaceType.DEATH_SITE
                ? inferEnvironment(dimensionId, y)
                : DeathSiteEnvironment.UNKNOWN;
        WorldPos center = new WorldPos(dimensionId, x, y, z);
        String baseId = "living_legends:manual/" + type.idString() + "_"
                + dimensionId.replace(':', '_').replace('/', '_') + "_" + x + "_" + y + "_" + z;
        String id = WorldRemembersLivingLegendsFabricStorage.uniquePlaceId(world, baseId, logger);
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
        long gameTime = WorldRemembersLivingLegendsFabricStorage.gameTimeFor(world);
        NamedPlace place = new NamedPlace(
                id,
                type,
                environment,
                dimensionId,
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
        boolean changed = WorldRemembersLivingLegendsFabricStorage.upsertPlace(world, place, "create_place " + id, logger);
        if (changed) {
            WorldRemembersLivingLegendsFabricPlaceTitles.onPlaceCreated(invokeNoArg(world, "method_8503"), place, logger);
        }
        sendFeedbackText(source, Text.literal(changed ? "World Remembers created place id=" + id + " name=" : "World Remembers create made no changes id=" + id + " name=")
                .append(displayNameText(place)));
        return changed ? 1 : 0;
    }

    private static int runRegenerate(Object context, Logger logger, String target, boolean force) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }

        Object world = invokeNoArg(source, "method_9225");
        String message;
        if ("all".equalsIgnoreCase(target)) {
            message = WorldRemembersLivingLegendsFabricStorage.regenerateAll(world, force, logger);
        } else {
            NamedPlace place = resolvePlaceReference(source, world, target, logger, true);
            if (place == null) {
                return 0;
            }
            message = WorldRemembersLivingLegendsFabricStorage.regeneratePlace(world, place.placeIdString(), force, logger);
        }
        sendFeedback(source, message);
        return 1;
    }

    private static int runDeletedList(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }
        Object world = invokeNoArg(source, "method_9225");
        List<DeletedPlaceMarker> markers = WorldRemembersLivingLegendsFabricStorage.deletedPlaceMarkers(world, logger);
        sendFeedback(source, "World Remembers deleted places: count=" + markers.size()
                + " suppressionEnabled=" + WorldRemembersLivingLegends.config().generation.deletedPlaceSuppressionEnabled
                + " suppressionDays=" + WorldRemembersLivingLegends.config().generation.deletedPlaceSuppressionDays);
        int index = 1;
        for (DeletedPlaceMarker marker : markers) {
            sendFeedback(source, "[" + index + "] " + marker.debugString());
            index++;
            if (index > 16) {
                sendFeedback(source, "World Remembers deleted places: list truncated at 15 markers");
                break;
            }
        }
        return 1;
    }

    private static int runDeletedClear(Object context, Logger logger, String markerId) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }
        Object world = invokeNoArg(source, "method_9225");
        sendFeedback(source, WorldRemembersLivingLegendsFabricStorage.clearDeletedPlaceMarker(world, markerId, logger));
        return 1;
    }

    private static int runDeletedClearAll(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }
        Object world = invokeNoArg(source, "method_9225");
        sendFeedback(source, WorldRemembersLivingLegendsFabricStorage.clearAllDeletedPlaceMarkers(world, logger));
        return 1;
    }

    private static int runExport(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }
        Object world = invokeNoArg(source, "method_9225");
        sendFeedback(source, WorldRemembersLivingLegendsFabricStorage.exportPlaces(world, logger));
        return 1;
    }

    private static int runImport(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }
        Object world = invokeNoArg(source, "method_9225");
        sendFeedback(source, WorldRemembersLivingLegendsFabricStorage.importPlaces(world, logger));
        return 1;
    }

    private static int runReload(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
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
        sendFeedback(source, "World Remembers reload complete"
                + " usedDefaultConfig=" + result.usedDefaultConfig()
                + " malformedConfig=" + result.malformedConfig()
                + " configFile=" + result.configPath()
                + " configValidation=" + result.config().validationSummary().compact()
                + " styles=" + BuiltInNameData.allPacks().size()
                + " patterns=" + patterns
                + " tokens=" + tokens
                + " existingPlaceNames=unchanged");
        for (String warning : result.config().validationSummary().firstWarnings(8)) {
            sendFeedback(source, "World Remembers config warning: " + warning);
        }
        if (result.config().validationWarnings().size() > 8) {
            sendFeedback(source, "World Remembers config warning: "
                    + (result.config().validationWarnings().size() - 8)
                    + " more warnings in latest.log");
        }
        Object server = invokeNoArg(source, "method_9211");
        FabricMapIntegrationManager.clearSyncFingerprints();
        FabricMapIntegrationManager.syncAllFromWorld(server == null ? invokeNoArg(source, "method_9225") : server, logger);
        return 1;
    }

    private static int runDebugClusters(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
        Object world = invokeNoArg(source, "method_9225");
        sendFeedback(source, WorldRemembersLivingLegendsFabricStorage.debugClusters(world, logger));
        return 1;
    }

    private static int runDebugCause(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
        Object world = invokeNoArg(source, "method_9225");
        Object player = invokeNoArg(source, "method_44023");
        Object pos = invokeNoArg(player, "method_24515");

        String dimensionId = dimensionId(world);
        int x = coordinate(pos, "method_10263");
        int y = coordinate(pos, "method_10264");
        int z = coordinate(pos, "method_10260");
        int chunkX = Math.floorDiv(x, CHUNK_SIZE);
        int chunkZ = Math.floorDiv(z, CHUNK_SIZE);
        sendFeedback(source, WorldRemembersLivingLegendsFabricStorage.debugCause(
                world,
                dimensionId,
                chunkX,
                chunkZ,
                x,
                y,
                z,
                logger
        ));
        return 1;
    }

    private static int runDebugNearby(Object context, Logger logger, int radius) {
        Object source = invokeNoArg(context, "getSource");
        Object world = invokeNoArg(source, "method_9225");
        Object player = invokeNoArg(source, "method_44023");
        Object pos = invokeNoArg(player, "method_24515");

        String dimensionId = dimensionId(world);
        int x = coordinate(pos, "method_10263");
        int y = coordinate(pos, "method_10264");
        int z = coordinate(pos, "method_10260");
        sendFeedback(source, WorldRemembersLivingLegendsFabricStorage.debugNearby(world, dimensionId, x, y, z, radius, logger));
        return 1;
    }

    private static int runDebugFarm(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
        sendFeedback(source, EventCollector.debugAntiFarmState());
        return 1;
    }

    private static int runDebugConfig(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
        var config = WorldRemembersLivingLegends.config();
        List<String> disabled = new ArrayList<>();
        int enabled = 0;
        for (PlaceType placeType : ScoreEngine.alphaCandidateTypes()) {
            if (config.placeTypes.autoGenerationEnabled(placeType)) {
                enabled++;
            } else {
                disabled.add(placeType.name());
            }
        }
        if (config.placeTypes.autoGenerationEnabled(PlaceType.CUSTOM)) {
            enabled++;
        } else {
            disabled.add(PlaceType.CUSTOM.name());
        }
        sendFeedback(source, "World Remembers config"
                + " configFile=" + LivingLegendsConfigManager.currentConfigPath()
                + " configRoot=" + WorldRemembersLivingLegends.CONFIG_ROOT
                + " validation=" + config.validationSummary().compact()
                + " debugEnabled=" + config.debug.enabled
                + " namingVerbose=" + config.debug.namingVerbose
                + " styleSelectionMode=" + config.naming.styleSelectionMode
                + " enabledPlaceTypes=" + enabled
                + " disabledPlaceTypes=" + disabled
                + " displayExistingWhenDisabled=" + config.placeTypes.displayExistingWhenDisabled
                + " allowManualCreateWhenDisabled=" + config.placeTypes.allowManualCreateWhenDisabled
                + " mapIntegration=" + config.mapIntegration.enabled
                + " journeyMap=" + config.mapIntegration.journeyMap.enabled
                + " xaero=" + config.mapIntegration.xaero.enabled
                + " ftbChunks=" + config.mapIntegration.ftbChunks.enabled
                + " mapLabels=" + config.mapIntegration.placeLabels.enabled
                + " destinations=" + config.mapIntegration.destinations.enabled);
        for (String warning : config.validationSummary().firstWarnings(8)) {
            sendFeedback(source, "World Remembers config warning: " + warning);
        }
        if (config.validationWarnings().size() > 8) {
            sendFeedback(source, "World Remembers config warning: "
                    + (config.validationWarnings().size() - 8)
                    + " more warnings in latest.log");
        }
        return 1;
    }

    private static int runDebugConfigValidate(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }
        LivingLegendsConfig config = WorldRemembersLivingLegends.config();
        LivingLegendsConfig.ValidationSummary summary = config.validationSummary();
        sendFeedback(source, "World Remembers config validate"
                + " " + summary.compact()
                + " configFile=" + LivingLegendsConfigManager.currentConfigPath()
                + " sections=general,generation,thresholds,requiredCounts,scoreThresholds,display,performance,eventCollection,antiFarm,naming,commands,placeTypes,biomeThemes,notifications,titleOverlay,mapIntegration,decay,candidateDecay,journal,permissions,debug");
        for (String warning : summary.firstWarnings(10)) {
            sendFeedback(source, "World Remembers config warning: " + warning);
        }
        if (summary.warnings() > 10) {
            sendFeedback(source, "World Remembers config warning: " + (summary.warnings() - 10) + " more warnings in latest.log");
        }
        return 1;
    }

    private static int runDebugValidate(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }
        Object world = invokeNoArg(source, "method_9225");
        sendFeedback(source, WorldRemembersLivingLegendsFabricStorage.debugValidate(world, logger));
        return 1;
    }

    private static int runDebugSelfTest(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }
        Object world = invokeNoArg(source, "method_9225");
        sendFeedback(source, WorldRemembersLivingLegendsFabricStorage.debugSelfTest(world, logger));
        return 1;
    }

    private static int runDebugRepairDryRun(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }
        Object world = invokeNoArg(source, "method_9225");
        sendFeedback(source, WorldRemembersLivingLegendsFabricStorage.debugRepairDryRun(world, logger));
        return 1;
    }

    private static int runDebugCompatSummary(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }
        sendFeedback(source, "World Remembers compat registries " + WorldRemembersCompatRegistries.summaryLine());
        for (String warning : WorldRemembersCompatRegistries.warnings().stream().limit(8).toList()) {
            sendFeedback(source, "World Remembers compat warning: " + warning);
        }
        int hidden = Math.max(0, WorldRemembersCompatRegistries.warnings().size() - 8);
        if (hidden > 0) {
            sendFeedback(source, "World Remembers compat warning: " + hidden + " more warnings in latest.log");
        }
        return 1;
    }

    private static int runDebugCompatLookup(Object context, Logger logger, String kind, String id) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }
        sendFeedback(source, WorldRemembersCompatRegistries.lookupDebug(kind, id));
        return 1;
    }

    private static int runDebugMigration(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
        Object world = invokeNoArg(source, "method_9225");
        sendFeedback(source, WorldRemembersLivingLegendsFabricStorage.debugMigration(world, logger));
        return 1;
    }

    private static int runDebugSpacingHere(Object context, Logger logger, String requestedType) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }
        Object world = invokeNoArg(source, "method_9225");
        Object player = invokeNoArg(source, "method_44023");
        Object pos = invokeNoArg(player, "method_24515");

        PlaceType placeType = PlaceType.fromId(requestedType);
        if (placeType == PlaceType.UNKNOWN) {
            sendFeedback(source, "World Remembers spacing failed: unknown place type " + requestedType);
            return 0;
        }

        String dimensionId = dimensionId(world);
        sendFeedback(source, WorldRemembersLivingLegendsFabricStorage.debugSpacingHere(
                world,
                dimensionId,
                coordinate(pos, "method_10263"),
                coordinate(pos, "method_10264"),
                coordinate(pos, "method_10260"),
                placeType,
                logger
        ));
        return 1;
    }

    private static int runDebugTitleTest(Object context, Logger logger, String styleId, String requestedType, String name) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }
        ServerPlayerEntity player = serverPlayer(source);
        if (player == null) {
            sendFeedback(source, "World Remembers title test failed: command must be run by a player");
            return 0;
        }
        sendFeedback(source, WorldRemembersLivingLegendsFabricPlaceTitles.sendLiteralTest(
                player,
                PlaceType.fromId(requestedType),
                styleId,
                name,
                logger
        ));
        return 1;
    }

    private static int runDebugTitleNearest(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }
        ServerPlayerEntity player = serverPlayer(source);
        if (player == null) {
            sendFeedback(source, "World Remembers title nearest failed: command must be run by a player");
            return 0;
        }
        sendFeedback(source, WorldRemembersLivingLegendsFabricPlaceTitles.sendNearestDebug(player, logger));
        return 1;
    }

    private static int runDebugTitleHere(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }
        ServerPlayerEntity player = serverPlayer(source);
        if (player == null) {
            sendFeedback(source, "World Remembers title here failed: command must be run by a player");
            return 0;
        }
        sendFeedback(source, WorldRemembersLivingLegendsFabricPlaceTitles.debugHere(player, logger));
        return 1;
    }

    private static int runDebugTitleClear(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }
        ServerPlayerEntity player = serverPlayer(source);
        if (player == null) {
            sendFeedback(source, "World Remembers title clear failed: command must be run by a player");
            return 0;
        }
        sendFeedback(source, WorldRemembersLivingLegendsFabricPlaceTitles.clear(player, logger));
        return 1;
    }

    private static int runDebugTitleState(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }
        ServerPlayerEntity player = serverPlayer(source);
        if (player == null) {
            sendFeedback(source, "World Remembers title state failed: command must be run by a player");
            return 0;
        }
        sendFeedback(source, WorldRemembersLivingLegendsFabricPlaceTitles.debugState(player));
        return 1;
    }

    private static int runDebugDecayStatus(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }
        sendFeedback(source, WorldRemembersLivingLegendsFabricStorage.candidateDecayStatus());
        return 1;
    }

    private static int runDebugDecayRun(Object context, Logger logger, long simulatedTicks) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }
        Object world = invokeNoArg(source, "method_9225");
        sendFeedback(source, WorldRemembersLivingLegendsFabricStorage.runCandidateDecayNow(world, Math.max(0L, simulatedTicks), logger));
        return 1;
    }

    private static int runDebugDecayInfoChunk(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }
        Object world = invokeNoArg(source, "method_9225");
        Object player = invokeNoArg(source, "method_44023");
        Object pos = invokeNoArg(player, "method_24515");

        String dimensionId = dimensionId(world);
        int chunkX = Math.floorDiv(coordinate(pos, "method_10263"), CHUNK_SIZE);
        int chunkZ = Math.floorDiv(coordinate(pos, "method_10260"), CHUNK_SIZE);
        sendFeedback(source, WorldRemembersLivingLegendsFabricStorage.debugCandidateDecayChunk(
                world,
                dimensionId,
                chunkX,
                chunkZ,
                logger
        ));
        return 1;
    }

    private static int runDebugDecayInfoNearest(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }
        Object world = invokeNoArg(source, "method_9225");
        Object player = invokeNoArg(source, "method_44023");
        Object pos = invokeNoArg(player, "method_24515");

        String dimensionId = dimensionId(world);
        sendFeedback(source, WorldRemembersLivingLegendsFabricStorage.debugCandidateDecayNearest(
                world,
                dimensionId,
                coordinate(pos, "method_10263"),
                coordinate(pos, "method_10264"),
                coordinate(pos, "method_10260"),
                logger
        ));
        return 1;
    }

    private static int runDebugDecayTouchChunk(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }
        Object world = invokeNoArg(source, "method_9225");
        Object player = invokeNoArg(source, "method_44023");
        Object pos = invokeNoArg(player, "method_24515");

        String dimensionId = dimensionId(world);
        int chunkX = Math.floorDiv(coordinate(pos, "method_10263"), CHUNK_SIZE);
        int chunkZ = Math.floorDiv(coordinate(pos, "method_10260"), CHUNK_SIZE);
        sendFeedback(source, WorldRemembersLivingLegendsFabricStorage.touchCandidateDecayChunk(
                world,
                dimensionId,
                chunkX,
                chunkZ,
                logger
        ));
        return 1;
    }

    private static int runDebugDecaySetScoreChunk(Object context, Logger logger, String requestedType, String requestedScore) {
        Object source = invokeNoArg(context, "getSource");
        if (!requireOp(source)) {
            return 0;
        }
        Object world = invokeNoArg(source, "method_9225");
        Object player = invokeNoArg(source, "method_44023");
        Object pos = invokeNoArg(player, "method_24515");

        PlaceType placeType = PlaceType.fromId(requestedType);
        if (placeType == PlaceType.UNKNOWN) {
            sendFeedback(source, "Candidate decay setscore failed: unknown place type " + requestedType);
            return 0;
        }

        double score;
        try {
            score = Math.max(0.0, Double.parseDouble(requestedScore));
        } catch (NumberFormatException exception) {
            sendFeedback(source, "Candidate decay setscore failed: score must be a number");
            return 0;
        }

        String dimensionId = dimensionId(world);
        int chunkX = Math.floorDiv(coordinate(pos, "method_10263"), CHUNK_SIZE);
        int chunkZ = Math.floorDiv(coordinate(pos, "method_10260"), CHUNK_SIZE);
        sendFeedback(source, WorldRemembersLivingLegendsFabricStorage.setCandidateDecayScore(
                world,
                dimensionId,
                chunkX,
                chunkZ,
                placeType,
                score,
                logger
        ));
        return 1;
    }

    private static int runDebugName(
            Object context,
            Logger logger,
            String requestedType,
            String requestedEnvironment,
            String requestedStyle,
            Long requestedSeed
    ) {
        Object source = invokeNoArg(context, "getSource");
        Object world = invokeNoArg(source, "method_9225");
        Object player = invokeNoArg(source, "method_44023");
        Object pos = invokeNoArg(player, "method_24515");

        String dimensionId = dimensionId(world);
        int x = coordinate(pos, "method_10263");
        int y = coordinate(pos, "method_10264");
        int z = coordinate(pos, "method_10260");
        int chunkX = Math.floorDiv(x, CHUNK_SIZE);
        int chunkZ = Math.floorDiv(z, CHUNK_SIZE);
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

        PlaceCluster cluster = new PlaceCluster(
                "debug_name_" + type.idString() + "_" + environment.idString(),
                type,
                dimensionId,
                environment,
                x,
                y,
                z,
                y,
                y,
                32,
                0.0,
                0.0,
                0L,
                0L,
                false,
                type.priorityRank(),
                dimensionId + "@chunk:" + chunkX + "," + chunkZ,
                PlaceStats.empty(),
                List.of(dimensionId + "@chunk:" + chunkX + "," + chunkZ)
        );

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
                    .append(WorldRemembersLivingLegendsFabricNameResolver.resolveToString(recipe))
                    .append(" | ")
                    .append(NameGenerator.debugRecipe(recipe))
                    .append(" fallback=")
                    .append(fallbackBehavior(requestedStyleId, recipe.styleId()))
                    .append(" ")
                    .append(diagnostics.summary());
            logNameRejectionDetails(logger, diagnostics);
        }
        sendFeedback(source, message.toString());
        return 1;
    }

    private static int runDebugNameBatch(
            Object context,
            Logger logger,
            String requestedType,
            String requestedEnvironment,
            String requestedStyle,
            Integer requestedCount
    ) {
        Object source = invokeNoArg(context, "getSource");
        Object world = invokeNoArg(source, "method_9225");
        Object player = invokeNoArg(source, "method_44023");
        Object pos = invokeNoArg(player, "method_24515");

        String dimensionId = dimensionId(world);
        int x = coordinate(pos, "method_10263");
        int y = coordinate(pos, "method_10264");
        int z = coordinate(pos, "method_10260");
        int chunkX = Math.floorDiv(x, CHUNK_SIZE);
        int chunkZ = Math.floorDiv(z, CHUNK_SIZE);
        PlaceType type = PlaceType.fromId(requestedType);
        DeathSiteEnvironment environment = DeathSiteEnvironment.fromId(requestedEnvironment);
        String requestedStyleId = normalizedStyleId(requestedStyle);
        NameDataPack nameData = BuiltInNameData.packForStyle(
                requestedStyleId,
                WorldRemembersLivingLegends.config().naming.enabledStyles,
                WorldRemembersLivingLegends.config().naming.allowMixedStyleTokens
        );
        String activeStyleId = nameData.styleId();
        int count = Math.max(1, Math.min(50, requestedCount == null ? 10 : requestedCount));
        long baseSeed = 31L * type.ordinal() + 17L * environment.ordinal() + 341873128712L * chunkX + 132897987541L * chunkZ;

        PlaceCluster cluster = new PlaceCluster(
                "debug_namebatch_" + type.idString() + "_" + environment.idString(),
                type,
                dimensionId,
                environment,
                x,
                y,
                z,
                y,
                y,
                32,
                0.0,
                0.0,
                0L,
                0L,
                false,
                type.priorityRank(),
                dimensionId + "@chunk:" + chunkX + "," + chunkZ,
                PlaceStats.empty(),
                List.of(dimensionId + "@chunk:" + chunkX + "," + chunkZ)
        );

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
                .append(activeStyleId)
                .append(" fallback=")
                .append(fallbackBehavior(requestedStyleId, activeStyleId))
                .append(" count=")
                .append(count)
                .append(" baseSeed=")
                .append(baseSeed);
        if (!requestedStyleId.equals(activeStyleId)) {
            message.append('\n')
                    .append("Requested style data unavailable; using ")
                    .append(activeStyleId)
                    .append(".");
        }
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
                resolvedName = WorldRemembersLivingLegendsFabricNameResolver.resolveToString(recipe);
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
        sendFeedback(source, message.toString());
        return 1;
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

    private static int runDebugNameCause(
            Object context,
            Logger logger,
            String requestedPlaceType,
            String requestedCauseType,
            String targetStyleCount
    ) {
        Object source = invokeNoArg(context, "getSource");
        PlaceType placeType = PlaceType.fromId(requestedPlaceType);
        PlaceCauseType causeType = PlaceCauseType.fromId(requestedCauseType);
        String[] parts = splitNameCauseTail(targetStyleCount);
        if (parts.length < 3) {
            sendFeedback(source, "Usage: /places debug namecause <placeType> <causeType> <targetId> <style> <count>");
            return 0;
        }
        String requestedTargetId = parts[0];
        String requestedStyle = parts[1];
        Integer requestedCount = parsePositiveInt(parts[2], 10);
        String runtimeName = runtimeNameOption(parts, 3);
        String targetId = requestedTargetId == null ? "" : requestedTargetId.trim().toLowerCase(Locale.ROOT);
        String requestedStyleId = normalizedStyleId(requestedStyle);
        NameDataPack nameData = BuiltInNameData.packForStyle(
                requestedStyleId,
                WorldRemembersLivingLegends.config().naming.enabledStyles,
                WorldRemembersLivingLegends.config().naming.allowMixedStyleTokens
        );
        int count = Math.max(1, Math.min(50, requestedCount == null ? 10 : requestedCount));
        PlaceCause cause = debugCauseFor(placeType, causeType, targetId, runtimeName);
        DeathSiteEnvironment environment = debugEnvironmentForCause(placeType, causeType, targetId);
        NameContext nameContext = NameContext.from(placeType, environment, cause, nameData.styleId());
        long baseSeed = 9137L * placeType.ordinal() + 1777L * causeType.ordinal() + targetId.hashCode() + 31L * runtimeName.hashCode();

        sendFeedback(source, "World Remembers name cause"
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
                resolvedName = WorldRemembersLivingLegendsFabricNameResolver.resolveToString(recipe);
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
            MutableText line = Text.literal((index + 1) + ". ")
                    .append(NameResolver.resolve(recipe, nameData))
                    .append(Text.literal(" | patternKey=" + recipe.patternKey()
                            + " selectedPatternSource=" + diagnostics.selectedPatternSource().name()
                            + " fallbackUsed=" + (!fallbackReason.isBlank())
                            + (fallbackReason.isBlank() ? "" : " fallbackReason=" + fallbackReason)
                            + " tokens=" + recipe.selectedTokenIds()
                            + " " + diagnostics.summary()));
            sendFeedbackText(source, line);
            logNameRejectionDetails(logger, diagnostics);
        }
        return 1;
    }

    private static int runDebugNameAuditContext(
            Object context,
            Logger logger,
            String requestedPlaceType,
            String requestedEnvironment,
            String requestedStyle
    ) {
        Object source = invokeNoArg(context, "getSource");
        PlaceType placeType = PlaceType.fromId(requestedPlaceType);
        DeathSiteEnvironment environment = DeathSiteEnvironment.fromId(requestedEnvironment);
        String requestedStyleId = normalizedStyleId(requestedStyle);
        NameDataPack nameData = BuiltInNameData.packForStyle(
                requestedStyleId,
                WorldRemembersLivingLegends.config().naming.enabledStyles,
                WorldRemembersLivingLegends.config().naming.allowMixedStyleTokens
        );
        PlaceCluster cluster = debugAuditCluster(placeType, environment);
        NameContext nameContext = NameContext.from(cluster, placeType, environment, nameData.styleId());
        NameReachabilityAudit audit = auditReachability(nameData, placeType, environment, nameContext, cluster);
        sendFeedback(source, audit.message("context", requestedStyleId, "", ""));
        if (logger != null) {
            logger.info(audit.message("context", requestedStyleId, "", ""));
        }
        return 1;
    }

    private static int runDebugNameAuditCause(
            Object context,
            Logger logger,
            String requestedPlaceType,
            String requestedCauseType,
            String targetStyle
    ) {
        Object source = invokeNoArg(context, "getSource");
        PlaceType placeType = PlaceType.fromId(requestedPlaceType);
        PlaceCauseType causeType = PlaceCauseType.fromId(requestedCauseType);
        String[] parts = splitNameCauseTail(targetStyle);
        if (parts.length < 2) {
            sendFeedback(source, "Usage: /places debug nameaudit cause <placeType> <causeType> <targetId> <style>");
            return 0;
        }
        String targetId = parts[0] == null ? "" : parts[0].trim().toLowerCase(Locale.ROOT);
        String runtimeName = runtimeNameOption(parts, 2);
        String requestedStyleId = normalizedStyleId(parts[1]);
        NameDataPack nameData = BuiltInNameData.packForStyle(
                requestedStyleId,
                WorldRemembersLivingLegends.config().naming.enabledStyles,
                WorldRemembersLivingLegends.config().naming.allowMixedStyleTokens
        );
        PlaceCause cause = debugCauseFor(placeType, causeType, targetId, runtimeName);
        DeathSiteEnvironment environment = debugEnvironmentForCause(placeType, causeType, targetId);
        NameContext nameContext = NameContext.from(placeType, environment, cause, nameData.styleId());
        NameReachabilityAudit audit = auditReachability(nameData, placeType, environment, nameContext, null);
        sendFeedback(source, audit.message("cause", requestedStyleId, causeType.name(), targetId));
        if (logger != null) {
            logger.info(audit.message("cause", requestedStyleId, causeType.name(), targetId));
        }
        return 1;
    }

    private static PlaceCluster debugAuditCluster(PlaceType placeType, DeathSiteEnvironment environment) {
        return new PlaceCluster(
                "debug_nameaudit_" + placeType.idString() + "_" + environment.idString(),
                placeType,
                "minecraft:overworld",
                environment,
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
                placeType.priorityRank(),
                "minecraft:overworld@chunk:0,0",
                PlaceStats.empty(),
                List.of("minecraft:overworld@chunk:0,0")
        );
    }

    private static NameReachabilityAudit auditReachability(
            NameDataPack nameData,
            PlaceType placeType,
            DeathSiteEnvironment environment,
            NameContext context,
            PlaceCluster cluster
    ) {
        List<NamePattern> all = nameData.patterns();
        List<NamePattern> placeMatches = all.stream()
                .filter(pattern -> supportsPlaceType(pattern, placeType))
                .toList();
        List<NamePattern> environmentMatches = placeMatches.stream()
                .filter(pattern -> supportsEnvironment(pattern, environment))
                .toList();
        List<NamePattern> causeMatches = environmentMatches.stream()
                .filter(pattern -> pattern.causeConstraints().matchesPattern(context))
                .toList();

        Map<String, Integer> bySource = new LinkedHashMap<>();
        for (NamePattern pattern : causeMatches) {
            increment(bySource, pattern.sourceFor(environment).name());
        }
        NamePatternSource selectedSource = NamePatternSource.SAFE_FALLBACK;
        List<NamePattern> selectedSourcePatterns = List.of();
        for (NamePatternSource source : NAME_SOURCE_ORDER) {
            List<NamePattern> matches = causeMatches.stream()
                    .filter(pattern -> pattern.sourceFor(environment) == source)
                    .toList();
            if (!matches.isEmpty()) {
                selectedSource = source;
                selectedSourcePatterns = applyDominantTargetPriority(matches, context, source);
                break;
            }
        }

        NameGenerationDiagnostics diagnostics = new NameGenerationDiagnostics();
        List<NameRecipe> generatedRecipes = new ArrayList<>();
        Set<String> resolvedNames = new LinkedHashSet<>();
        Set<String> patternKeys = new LinkedHashSet<>();
        long baseSeed = 77_777L + placeType.ordinal() * 1_013L + environment.ordinal() * 9_173L + context.hashCode();
        for (int index = 0; index < 512; index++) {
            NameRecipe recipe = cluster == null
                    ? NameGenerator.generate(context, baseSeed + index * 10_007L, nameData, generatedRecipes, diagnostics)
                    : NameGenerator.generate(cluster, placeType, environment, baseSeed + index * 10_007L, nameData, generatedRecipes, diagnostics);
            generatedRecipes.add(recipe);
            patternKeys.add(recipe.patternKey());
            String resolved = WorldRemembersLivingLegendsFabricNameResolver.resolveToString(recipe);
            if (!resolved.isBlank()) {
                resolvedNames.add(resolved);
            }
        }
        boolean fixedFrequentScenario = highFrequencyNameScenario(placeType, context.causeType(), context.deathCause())
                && patternKeys.size() <= 1
                && resolvedNames.size() <= 1;

        return new NameReachabilityAudit(
                nameData.styleId(),
                placeType,
                environment,
                all.size(),
                all.size(),
                placeMatches.size(),
                environmentMatches.size(),
                causeMatches.size(),
                selectedSource,
                selectedSourcePatterns.size(),
                patternKeys.size(),
                resolvedNames,
                fixedFrequentScenario,
                bySource,
                diagnostics.summary()
        );
    }

    private static List<NamePattern> applyDominantTargetPriority(
            List<NamePattern> patterns,
            NameContext context,
            NamePatternSource source
    ) {
        if (source != NamePatternSource.DOMINANT_TARGET) {
            return patterns;
        }
        int highestPriority = 0;
        for (NamePattern pattern : patterns) {
            highestPriority = Math.max(highestPriority, pattern.causeConstraints().dominantTargetPriority(context));
        }
        if (highestPriority <= 0) {
            return patterns;
        }
        int selectedPriority = highestPriority;
        return patterns.stream()
                .filter(pattern -> pattern.causeConstraints().dominantTargetPriority(context) == selectedPriority)
                .toList();
    }

    private static boolean supportsPlaceType(NamePattern pattern, PlaceType placeType) {
        return pattern.supportedPlaceTypes().isEmpty() || pattern.supportedPlaceTypes().contains(placeType);
    }

    private static boolean supportsEnvironment(NamePattern pattern, DeathSiteEnvironment environment) {
        return pattern.supportedEnvironments().isEmpty() || pattern.supportedEnvironments().contains(environment);
    }

    private static int runDebugNameAudit(Object context, Logger logger, String requestedStyle) {
        Object source = invokeNoArg(context, "getSource");
        String styleFilter = requestedStyle == null || requestedStyle.isBlank()
                ? ""
                : BuiltInNameData.builtInStyleId(requestedStyle);
        List<NameDataPack> packs = BuiltInNameData.allPacks().stream()
                .filter(pack -> styleFilter.isBlank() || pack.styleId().equals(styleFilter))
                .toList();
        if (packs.isEmpty()) {
            sendFeedback(source, "World Remembers name audit: no style data for " + requestedStyle);
            return 0;
        }

        Map<String, Set<String>> crossStyleDisplays = new LinkedHashMap<>();
        Map<String, String> crossStyleExamples = new LinkedHashMap<>();
        StringBuilder message = new StringBuilder("World Remembers name audit");
        if (!styleFilter.isBlank()) {
            message.append(" style=").append(styleFilter);
        }
        message.append(" styles=").append(packs.size())
                .append(" duplicateLangKeys=not_runtime_visible");

        for (NameDataPack pack : packs) {
            NameAuditSummary summary = summarizeNamePack(pack, crossStyleDisplays, crossStyleExamples);
            message.append('\n')
                    .append(summary.headerLine())
                    .append('\n')
                    .append("  byType=").append(compactCounts(summary.byType, 10))
                    .append('\n')
                    .append("  bySource=").append(compactCounts(summary.bySource, 8))
                    .append('\n')
                    .append("  topRoots=").append(compactCounts(summary.semanticRoots, 8))
                    .append('\n')
                    .append("  watchedWords=").append(compactCounts(summary.watchedWords, 8));
            if (!summary.warnings.isEmpty()) {
                message.append('\n').append("  warnings=").append(String.join("; ", summary.warnings.subList(0, Math.min(6, summary.warnings.size()))));
            }
        }

        List<String> duplicateDisplays = crossStyleDisplays.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> entry.getKey() + " -> " + entry.getValue())
                .limit(10)
                .toList();
        message.append('\n')
                .append("crossStyleDuplicateDisplays=")
                .append(duplicateDisplays.size());
        if (!duplicateDisplays.isEmpty()) {
            message.append(" examples=").append(duplicateDisplays);
        }
        if (logger != null) {
            logger.info(message.toString());
        }
        sendFeedback(source, message.toString());
        return 1;
    }

    private static NameAuditSummary summarizeNamePack(
            NameDataPack pack,
            Map<String, Set<String>> crossStyleDisplays,
            Map<String, String> crossStyleExamples
    ) {
        NameAuditSummary summary = new NameAuditSummary(pack.styleId(), pack.patterns().size(), pack.tokens().size());
        Set<String> seenDisplays = new HashSet<>();
        for (NamePattern pattern : pack.patterns()) {
            for (PlaceType placeType : pattern.supportedPlaceTypes()) {
                increment(summary.byType, placeType.name());
            }
            NamePatternSource source = pattern.sourceFor(DeathSiteEnvironment.UNKNOWN);
            increment(summary.bySource, source.name());
            increment(summary.semanticRoots, pattern.semanticRoot());

            String display = WorldRemembersLivingLegendsFabricNameResolver.resolveToString(
                    new NameRecipe(pack.styleId(), pattern.translationKey(), List.of(), List.of(), 0L, "")
            );
            String normalizedDisplay = normalizedResolvedName(display);
            if (display.isBlank() || display.equals(pattern.translationKey()) || display.contains("living_legends.name.pattern")) {
                summary.missingLangKeys++;
            }
            if (!normalizedDisplay.isBlank() && !seenDisplays.add(normalizedDisplay)) {
                summary.duplicateDisplays++;
            }
            if (!normalizedDisplay.isBlank()) {
                crossStyleDisplays.computeIfAbsent(normalizedDisplay, key -> new LinkedHashSet<>()).add(pack.styleId());
                crossStyleExamples.putIfAbsent(normalizedDisplay, display);
            }
            if (wordCount(display) > 5) {
                summary.longNames++;
            }
            if (display.contains("%s-а") || display.contains("%s-a") || display.contains("%s'а")) {
                summary.unsafeRuntimePatterns++;
            }
            countWatchedWords(summary, display);
            checkStyleBleed(summary, display);
        }
        if (summary.missingLangKeys > 0) {
            summary.warnings.add("missingLangKeys=" + summary.missingLangKeys);
        }
        if (summary.duplicateDisplays > 0) {
            summary.warnings.add("duplicateDisplays=" + summary.duplicateDisplays);
        }
        if (summary.longNames > 0) {
            summary.warnings.add("longNamesOver5Words=" + summary.longNames);
        }
        if (summary.unsafeRuntimePatterns > 0) {
            summary.warnings.add("unsafeRuntimeNamePatterns=" + summary.unsafeRuntimePatterns);
        }
        if (summary.styleBleedWarnings > 0) {
            summary.warnings.add("styleBleedWarnings=" + summary.styleBleedWarnings);
        }
        return summary;
    }

    private static void countWatchedWords(NameAuditSummary summary, String display) {
        String value = display == null ? "" : display.toLowerCase(Locale.ROOT);
        for (String word : List.of("след", "место", "память", "порог", "врата", "поле", "тропа", "падение", "драконопад",
                "trace", "site", "memory", "threshold", "gate", "field", "trail", "fall", "dragonfall")) {
            if (value.contains(word)) {
                increment(summary.watchedWords, word);
            }
        }
    }

    private static void checkStyleBleed(NameAuditSummary summary, String display) {
        String value = display == null ? "" : display.toLowerCase(Locale.ROOT);
        String style = summary.styleId;
        if (!NameStyle.FUNNY_COMMUNITY.idString().equals(style)
                && containsAny(value, "кринж", "скилл-ишью", " ой", "gg", "мем", "флекс", "respawn", "cringe", "skill issue")) {
            summary.styleBleedWarnings++;
        }
        if ((NameStyle.COZY_SURVIVAL.idString().equals(style) || NameStyle.NEUTRAL_SERVER.idString().equals(style))
                && containsAny(value, "кровь", "череп", "прокля", "резня", "бойня", "могила", "skull", "curse", "slaughter")) {
            summary.styleBleedWarnings++;
        }
        if (NameStyle.DARK_FANTASY.idString().equals(style)
                && containsAny(value, "домик", "уют", "кринж", "скилл", "cottage", "cozy", "cringe")) {
            summary.styleBleedWarnings++;
        }
        if (NameStyle.EPIC_MYTHOLOGY.idString().equals(style)
                && containsAny(value, "кринж", "скилл", " ой", "gg", "cringe", "skill issue")) {
            summary.styleBleedWarnings++;
        }
        if (NameStyle.NEUTRAL_SERVER.idString().equals(style)
                && containsAny(value, "сага", "венец", "клятва", "трон", "saga", "crown", "oath", "throne")) {
            summary.styleBleedWarnings++;
        }
    }

    private static boolean containsAny(String value, String... needles) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static int wordCount(String display) {
        String normalized = display == null ? "" : display
                .replace('“', ' ')
                .replace('”', ' ')
                .replace('"', ' ')
                .trim()
                .replaceAll("\\s+", " ");
        return normalized.isBlank() ? 0 : normalized.split(" ").length;
    }

    private static void increment(Map<String, Integer> counts, String key) {
        String normalized = key == null || key.isBlank() ? "unknown" : key;
        counts.put(normalized, counts.getOrDefault(normalized, 0) + 1);
    }

    private static String compactCounts(Map<String, Integer> counts, int limit) {
        if (counts.isEmpty()) {
            return "{}";
        }
        return counts.entrySet().stream()
                .sorted((left, right) -> Integer.compare(right.getValue(), left.getValue()))
                .limit(limit)
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .toList()
                .toString();
    }

    private static final class NameReachabilityAudit {
        private final String styleId;
        private final PlaceType placeType;
        private final DeathSiteEnvironment environment;
        private final int activeBeforeFiltering;
        private final int afterStyleFiltering;
        private final int afterPlaceTypeFiltering;
        private final int afterEnvironmentFiltering;
        private final int afterCauseTargetFiltering;
        private final NamePatternSource selectedSource;
        private final int selectedSourceCandidates;
        private final int sampledPatternKeysAfterRootConstraints;
        private final Set<String> resolvedNames;
        private final boolean fixedFrequentScenario;
        private final Map<String, Integer> bySource;
        private final String diagnosticsSummary;

        private NameReachabilityAudit(
                String styleId,
                PlaceType placeType,
                DeathSiteEnvironment environment,
                int activeBeforeFiltering,
                int afterStyleFiltering,
                int afterPlaceTypeFiltering,
                int afterEnvironmentFiltering,
                int afterCauseTargetFiltering,
                NamePatternSource selectedSource,
                int selectedSourceCandidates,
                int sampledPatternKeysAfterRootConstraints,
                Set<String> resolvedNames,
                boolean fixedFrequentScenario,
                Map<String, Integer> bySource,
                String diagnosticsSummary
        ) {
            this.styleId = styleId;
            this.placeType = placeType;
            this.environment = environment;
            this.activeBeforeFiltering = activeBeforeFiltering;
            this.afterStyleFiltering = afterStyleFiltering;
            this.afterPlaceTypeFiltering = afterPlaceTypeFiltering;
            this.afterEnvironmentFiltering = afterEnvironmentFiltering;
            this.afterCauseTargetFiltering = afterCauseTargetFiltering;
            this.selectedSource = selectedSource;
            this.selectedSourceCandidates = selectedSourceCandidates;
            this.sampledPatternKeysAfterRootConstraints = sampledPatternKeysAfterRootConstraints;
            this.resolvedNames = resolvedNames;
            this.fixedFrequentScenario = fixedFrequentScenario;
            this.bySource = bySource;
            this.diagnosticsSummary = diagnosticsSummary;
        }

        private String message(String mode, String requestedStyleId, String causeType, String targetId) {
            List<String> firstNames = resolvedNames.stream().limit(20).toList();
            StringBuilder message = new StringBuilder("World Remembers name audit ")
                    .append(mode)
                    .append(" requestedStyle=")
                    .append(requestedStyleId)
                    .append(" selectedStyle=")
                    .append(styleId)
                    .append(" placeType=")
                    .append(placeType.name())
                    .append(" environment=")
                    .append(environment.name());
            if (causeType != null && !causeType.isBlank()) {
                message.append(" causeType=").append(causeType);
            }
            if (targetId != null && !targetId.isBlank()) {
                message.append(" targetId=").append(targetId);
            }
            return message
                    .append('\n')
                    .append("  activeBeforeFiltering=").append(activeBeforeFiltering)
                    .append(" afterStyleFiltering=").append(afterStyleFiltering)
                    .append(" afterPlaceTypeFiltering=").append(afterPlaceTypeFiltering)
                    .append(" afterEnvironmentFiltering=").append(afterEnvironmentFiltering)
                    .append(" afterCauseTargetFiltering=").append(afterCauseTargetFiltering)
                    .append('\n')
                    .append("  bySource=").append(compactCounts(bySource, 8))
                    .append(" selectedSource=").append(selectedSource.name())
                    .append(" selectedSourceCandidates=").append(selectedSourceCandidates)
                    .append(fixedFrequentScenario ? " warning=fixed_pattern_pool" : "")
                    .append('\n')
                    .append("  sampledPatternKeysAfterRootConstraints=").append(sampledPatternKeysAfterRootConstraints)
                    .append(" finalReachableUniqueResolvedNames=").append(resolvedNames.size())
                    .append('\n')
                    .append("  topRejectionReasons=").append(diagnosticsSummary)
                    .append('\n')
                    .append("  first20ReachableResolvedNames=").append(firstNames)
                    .toString();
        }
    }

    private static final class NameAuditSummary {
        private final String styleId;
        private final int patterns;
        private final int tokens;
        private final Map<String, Integer> byType = new LinkedHashMap<>();
        private final Map<String, Integer> bySource = new LinkedHashMap<>();
        private final Map<String, Integer> semanticRoots = new LinkedHashMap<>();
        private final Map<String, Integer> watchedWords = new LinkedHashMap<>();
        private final List<String> warnings = new ArrayList<>();
        private int missingLangKeys;
        private int duplicateDisplays;
        private int longNames;
        private int unsafeRuntimePatterns;
        private int styleBleedWarnings;

        private NameAuditSummary(String styleId, int patterns, int tokens) {
            this.styleId = styleId;
            this.patterns = patterns;
            this.tokens = tokens;
        }

        private String headerLine() {
            return "style=" + styleId
                    + " activePatterns=" + patterns
                    + " activeTokens=" + tokens
                    + " missingLangKeys=" + missingLangKeys
                    + " duplicateDisplays=" + duplicateDisplays
                    + " longNames=" + longNames
                    + " unsafeRuntimePatterns=" + unsafeRuntimePatterns
                    + " styleBleedWarnings=" + styleBleedWarnings;
        }
    }

    private static int runDebugMobTheme(Object context, Logger logger, String requestedEntityId) {
        Object source = invokeNoArg(context, "getSource");
        String entityId = requestedEntityId == null ? "" : requestedEntityId.trim().toLowerCase(Locale.ROOT);
        VanillaMobThemeRegistry.MobTheme theme = VanillaMobThemeRegistry.lookup(entityId);
        if (!theme.hasMapping()) {
            sendFeedback(source, "World Remembers mob theme missing exact mapping"
                    + " entityId=" + entityId
                    + " fallbackGroup=group:unknown"
                    + " safeFallback=true");
            return 1;
        }

        String fallbackGroup = theme.primaryGroup().isBlank() ? "group:unknown" : "group:" + theme.primaryGroup();
        sendFeedback(source, "World Remembers mob theme "
                + theme.debugString()
                + " fallbackGroup=" + fallbackGroup
                + " exactMapping=true");
        return 1;
    }

    private static int runDebugMobThemesMissing(Object context, Logger logger) {
        Object source = invokeNoArg(context, "getSource");
        List<String> missing = VanillaMobThemeRegistry.missingRelevantMappings();
        if (missing.isEmpty()) {
            sendFeedback(source, "World Remembers mob themes missing: none");
            return 1;
        }

        sendFeedback(source, "World Remembers mob themes missing: " + missing.size());
        for (String entityId : missing) {
            sendFeedback(source, entityId + " safeFallbackGroup=group:unknown");
        }
        return 1;
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

    private static Integer parsePositiveInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Math.max(1, Integer.parseInt(value.trim()));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static PlaceCause debugCauseFor(PlaceType placeType, PlaceCauseType causeType, String targetId) {
        return debugCauseFor(placeType, causeType, targetId, "");
    }

    private static PlaceCause debugCauseFor(PlaceType placeType, PlaceCauseType causeType, String targetId, String runtimeName) {
        String target = targetId == null ? "" : targetId.trim().toLowerCase(Locale.ROOT);
        String name = RuntimeNameFormatter.sanitize(runtimeName);
        return switch (causeType) {
            case PLAYER_DEATHS -> new PlaceCause(causeType, EventType.PLAYER_DEATH, "", "", "", "", "", "", "", target, "", "", java.util.Map.of("debug_target=" + target, 1L));
            case FIRST_STRUCTURE_DISCOVERY -> new PlaceCause(causeType, EventType.STRUCTURE_DISCOVERED, firstDiscoveryKeyForTarget(causeType, target), "structure", target, "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", java.util.Map.of("debug_target=" + target, 1L));
            case MINING, FIRST_BLOCK_DISCOVERY -> new PlaceCause(causeType, EventType.VALUABLE_BLOCK_MINED, firstDiscoveryKeyForTarget(causeType, target), causeType == PlaceCauseType.FIRST_BLOCK_DISCOVERY ? "block" : "", "", target, "", "", "", "", "", "", target, "", "", "", "", "", "", "", "", java.util.Map.of("debug_target=" + target, 1L));
            case MOB_BATTLE -> new PlaceCause(causeType, EventType.PLAYER_KILLED_HOSTILE_MOB, "", "", "", "", target, "", target, "", target, "", "", "", "", "", "", "", "", "", "", java.util.Map.of("debug_target=" + target, 1L));
            case PASSIVE_SLAUGHTER -> new PlaceCause(causeType, EventType.PLAYER_KILLED_PASSIVE_MOB, "", "", "", "", "", "", target, target, "", "", "", "", "", "", "", "", "", "", "", java.util.Map.of("debug_target=" + target, 1L));
            case PORTAL_USAGE, DIMENSION_THRESHOLD, FIRST_DIMENSION_DISCOVERY -> new PlaceCause(causeType, EventType.PLAYER_ENTERED_DIMENSION, firstDiscoveryKeyForTarget(causeType, target), causeType == PlaceCauseType.FIRST_DIMENSION_DISCOVERY ? "dimension" : "", "", "", "", "", "", "", "", "", "", portalTypeForTarget(target), "", target, "", "", "", "", "", java.util.Map.of("debug_target=" + target, 1L));
            case BOSS_KILL, FIRST_BOSS_KILL -> new PlaceCause(causeType, EventType.BOSS_KILLED, firstDiscoveryKeyForTarget(causeType, target), causeType == PlaceCauseType.FIRST_BOSS_KILL ? "boss_kill" : "", "", "", target, target, target, "", "", "", "", "", "", "", "", "", "", "", "", java.util.Map.of("debug_target=" + target, 1L));
            case PET_DEATH -> new PlaceCause(
                    causeType, EventType.PET_DIED,
                    "", "", "", "", "", "", target, "", "", "", "", "", "", "",
                    "", name, target, "", "",
                    java.util.Map.of("debug_target=" + target, 1L)
            );
            case NAMED_MOB_DEATH -> new PlaceCause(
                    causeType, EventType.NAMED_MOB_DIED,
                    "", "", "", "", "", "", target, "", "", "", "", "", "", "",
                    "", "", "", name, target,
                    java.util.Map.of("debug_target=" + target, 1L)
            );
            default -> new PlaceCause(causeType, EventType.CUSTOM, "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", java.util.Map.of("debug_target=" + target, 1L));
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
            Object source,
            Object world,
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
                sendFeedback(source, "World Remembers place index not found: " + reference
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
                sendFeedback(source, "World Remembers found no nearby place in this dimension.");
            }
            return null;
        }

        NamedPlace exact = WorldRemembersLivingLegendsFabricStorage.place(world, reference, logger);
        if (exact != null) {
            return exact;
        }

        NamedPlace byName = exactManualOrDisplayName(world, reference, logger);
        if (byName != null) {
            return byName;
        }

        NamedPlace byCoordinate = placeByCopiedCoordinate(world, reference, logger);
        if (byCoordinate != null) {
            return byCoordinate;
        }

        if (sendNotFound) {
            sendPlaceNotFound(source, world, reference, logger);
        }
        return null;
    }

    private static List<NamedPlace> visiblePlaces(Object world, Logger logger) {
        return WorldRemembersLivingLegendsFabricStorage.places(world, logger).stream()
                .filter(place -> WorldRemembersLivingLegends.config().placeTypes.shouldDisplayExisting(place.placeType()))
                .toList();
    }

    private static NamedPlace resolveIndexedPlace(Object source, Object world, String reference, Logger logger) {
        Integer index = parseIndexReference(reference);
        if (index == null) {
            return null;
        }
        List<String> ids = LAST_LIST_INDEXES.get(sourceKey(source));
        if (ids == null || index < 1 || index > ids.size()) {
            return null;
        }
        return WorldRemembersLivingLegendsFabricStorage.place(world, ids.get(index - 1), logger);
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

    private static NamedPlace nearestPlace(Object source, Object world, boolean requireInside, Logger logger) {
        Object player = invokeNoArg(source, "method_44023");
        Object pos = invokeNoArg(player, "method_24515");
        if (pos == null) {
            return null;
        }
        String dimensionId = dimensionId(world);
        WorldPos position = new WorldPos(
                dimensionId,
                coordinate(pos, "method_10263"),
                coordinate(pos, "method_10264"),
                coordinate(pos, "method_10260")
        );
        List<NamedPlace> places = visiblePlaces(world, logger).stream()
                .filter(place -> place != null && dimensionId.equals(place.dimensionId()))
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

    private static NamedPlace exactManualOrDisplayName(Object world, String reference, Logger logger) {
        String normalized = reference.trim();
        for (NamedPlace place : visiblePlaces(world, logger)) {
            if (place.manualName().equalsIgnoreCase(normalized)
                    || displayNameString(place).equalsIgnoreCase(normalized)) {
                return place;
            }
        }
        return null;
    }

    private static NamedPlace placeByCopiedCoordinate(Object world, String reference, Logger logger) {
        Matcher coordinateMatcher = Pattern.compile("(-?\\d+),(-?\\d+),(-?\\d+)").matcher(reference);
        if (!coordinateMatcher.find()) {
            return null;
        }
        int x = Integer.parseInt(coordinateMatcher.group(1));
        int y = Integer.parseInt(coordinateMatcher.group(2));
        int z = Integer.parseInt(coordinateMatcher.group(3));
        String upper = reference.toUpperCase(Locale.ROOT);
        for (NamedPlace place : visiblePlaces(world, logger)) {
            if (place.center().x() == x
                    && place.center().y() == y
                    && place.center().z() == z
                    && (upper.indexOf('[') < 0 || upper.contains(place.placeType().name()))) {
                return place;
            }
        }
        return null;
    }

    private static void sendPlaceNotFound(Object source, Object world, String reference, Logger logger) {
        sendFeedback(source, "World Remembers place not found: " + reference);
        sendFeedback(source, "Use /places list and click/copy the full id, or use /places info nearest.");
        List<String> suggestions = matchingPlaceSuggestions(world, reference, logger);
        if (!suggestions.isEmpty()) {
            sendFeedback(source, "Possible matches: " + String.join(", ", suggestions));
        }
    }

    private static List<String> matchingPlaceSuggestions(Object world, String reference, Logger logger) {
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

    private static void rememberListIndexes(Object source, List<String> placeIds) {
        synchronized (LAST_LIST_INDEXES) {
            LAST_LIST_INDEXES.put(sourceKey(source), List.copyOf(placeIds));
            while (LAST_LIST_INDEXES.size() > MAX_INDEX_SESSIONS) {
                String firstKey = LAST_LIST_INDEXES.keySet().iterator().next();
                LAST_LIST_INDEXES.remove(firstKey);
            }
        }
    }

    private static String sourceKey(Object source) {
        Object player = invokeNoArg(source, "method_44023");
        Object uuid = invokeNoArg(player, "method_5667");
        if (uuid != null) {
            return String.valueOf(uuid);
        }
        Object name = invokeNoArg(source, "method_9214");
        return name == null ? "source@" + System.identityHashCode(source) : String.valueOf(name);
    }

    private static String renameTailId(String value) {
        String[] parts = splitRenameTail(value);
        return parts[0];
    }

    private static String renameTailName(String value) {
        String[] parts = splitRenameTail(value);
        return parts[1];
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

    private static boolean requireOp(Object source) {
        int level = Math.max(0, Math.min(4, WorldRemembersLivingLegends.config().commands.requiredOpLevel));
        if (sourceHasPermission(source, level)) {
            return true;
        }
        sendFeedback(source, "World Remembers: permission denied (requires OP level " + level + ")");
        return false;
    }

    private static ServerPlayerEntity serverPlayer(Object source) {
        Object player = invokeNoArg(source, "method_44023");
        return player instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null;
    }

    private static MutableText displayNameText(NamedPlace place) {
        if (place.manuallyRenamed()) {
            return Text.literal(place.manualName());
        }
        return NameResolver.resolve(place.nameRecipe());
    }

    private static String displayNameString(NamedPlace place) {
        if (place.manuallyRenamed()) {
            return place.manualName();
        }
        return WorldRemembersLivingLegendsFabricNameResolver.resolveToString(place.nameRecipe());
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

    private static Path resolveGameDir() {
        try {
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = loaderClass.getMethod("getInstance").invoke(null);
            Object gameDir = loaderClass.getMethod("getGameDir").invoke(loader);
            if (gameDir instanceof Path path) {
                return path;
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Current working directory is an acceptable fallback for config reload.
        }
        return Path.of(".");
    }

    private static void sendFeedback(Object source, String message) {
        if (message != null && message.contains("\n")) {
            for (String line : message.split("\\R")) {
                sendFeedback(source, line);
            }
            return;
        }

        Object text = text(message);
        if (!invokeVoid(source, "method_9213", text)) {
            invokeVoid(source, "method_45068", text);
        }
    }

    private static void sendFeedbackText(Object source, Object text) {
        if (text == null) {
            return;
        }
        if (!invokeVoid(source, "method_9213", text)) {
            invokeVoid(source, "method_45068", text);
        }
    }

    private static MutableText placeLine(NamedPlace place, int displayIndex) {
        String infoCommand = "/places info " + place.placeIdString();
        return Text.literal("[" + displayIndex + "] ")
                .append(Text.literal(place.placeIdString())
                        .styled(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, infoCommand))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to suggest " + infoCommand)))))
                .append(Text.literal(" [" + place.placeType().name() + "] "))
                .append(displayNameText(place))
                .append(Text.literal(" env=" + place.environment().name()))
                .append(Text.literal(" dim=" + place.dimensionId()))
                .append(Text.literal(" center=" + place.centerString()))
                .append(Text.literal(" radius=" + place.radius()))
                .append(Text.literal(" score=" + formatScore(place.score())));
    }

    private static Object text(String message) {
        try {
            Class<?> textClass = Class.forName("net.minecraft.class_2561");
            Method literal = textClass.getMethod("method_30163", String.class);
            return literal.invoke(null, message);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private static String dimensionId(Object world) {
        Object registryKey = invokeNoArg(world, "method_27983");
        Object identifier = invokeNoArg(registryKey, "method_29177");
        String value = identifier == null ? String.valueOf(registryKey) : String.valueOf(identifier);
        Matcher matcher = NAMESPACED_ID.matcher(value);
        String lastMatch = "";
        while (matcher.find()) {
            lastMatch = matcher.group();
        }
        return lastMatch.isBlank() ? "minecraft:overworld" : lastMatch;
    }

    private static boolean sourceHasPermission(Object source, int level) {
        Object result = invoke(source, "method_9259", Math.max(0, level));
        return !(result instanceof Boolean permitted) || permitted;
    }

    private static int coordinate(Object pos, String methodName) {
        Object value = invokeNoArg(pos, methodName);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static Integer intArgumentValue(Object context, String name) {
        Object value = invoke(context, "getArgument", name, Integer.class);
        return value instanceof Integer integer ? integer : null;
    }

    private static Long longArgumentValue(Object context, String name) {
        Object value = invoke(context, "getArgument", name, Long.class);
        return value instanceof Long number ? number : null;
    }

    private static String stringArgumentValue(Object context, String name) {
        Object value = invoke(context, "getArgument", name, String.class);
        return value instanceof String text ? text : "";
    }

    private static String formatScore(double value) {
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 0.000_001) {
            return Long.toString((long) rounded);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static String formatDistance(double value) {
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 0.000_001) {
            return Long.toString((long) rounded);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static Object invokeNoArg(Object target, String methodName) {
        return invoke(target, methodName);
    }

    private static boolean invokeVoid(Object target, String methodName, Object... args) {
        if (target == null) {
            return false;
        }

        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
                continue;
            }

            if (!parametersCompatible(method.getParameterTypes(), args)) {
                continue;
            }

            try {
                method.setAccessible(true);
                method.invoke(target, args);
                return true;
            } catch (ReflectiveOperationException | LinkageError exception) {
                return false;
            }
        }

        return false;
    }

    private static Object invoke(Object target, String methodName, Object... args) {
        if (target == null) {
            return null;
        }

        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
                continue;
            }

            if (!parametersCompatible(method.getParameterTypes(), args)) {
                continue;
            }

            try {
                method.setAccessible(true);
                return method.invoke(target, args);
            } catch (ReflectiveOperationException | LinkageError exception) {
                return null;
            }
        }

        return null;
    }

    private static boolean parametersCompatible(Class<?>[] parameterTypes, Object[] args) {
        for (int index = 0; index < args.length; index++) {
            Object arg = args[index];
            if (arg == null) {
                continue;
            }

            Class<?> parameterType = parameterTypes[index];
            boolean compatible = parameterType.isPrimitive()
                    ? isBoxedPrimitive(parameterType, arg)
                    : parameterType.isInstance(arg);
            if (!compatible) {
                return false;
            }
        }

        return true;
    }

    private static boolean isBoxedPrimitive(Class<?> primitiveType, Object value) {
        return (primitiveType == Integer.TYPE && value instanceof Integer)
                || (primitiveType == Long.TYPE && value instanceof Long)
                || (primitiveType == Double.TYPE && value instanceof Double)
                || (primitiveType == Float.TYPE && value instanceof Float)
                || (primitiveType == Boolean.TYPE && value instanceof Boolean)
                || (primitiveType == Byte.TYPE && value instanceof Byte)
                || (primitiveType == Short.TYPE && value instanceof Short)
                || (primitiveType == Character.TYPE && value instanceof Character);
    }

    private static Object handleObjectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> WorldRemembersLivingLegends.MOD_ID + " /places debug command";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args == null ? null : args[0]);
            default -> null;
        };
    }

    @FunctionalInterface
    private interface CommandRunner {
        int run(Object context);
    }
}
