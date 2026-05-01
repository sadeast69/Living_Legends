package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.EventCollector;
import com.worldremembers.livinglegends.EventType;
import com.worldremembers.livinglegends.FirstDiscoveryDefinition;
import com.worldremembers.livinglegends.FirstDiscoveryDefinitions;
import com.worldremembers.livinglegends.PlaceBounds;
import com.worldremembers.livinglegends.RuntimeNameFormatter;
import com.worldremembers.livinglegends.WorldRemembersCompatApi;
import com.worldremembers.livinglegends.WorldRemembersCompatRegistries;
import com.worldremembers.livinglegends.WorldMemoryEvent;
import com.worldremembers.livinglegends.WorldPos;
import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class WorldRemembersLivingLegendsFabricEvents {
    private static final int DEFAULT_VISIT_SAMPLE_INTERVAL_TICKS = 20 * 20;
    private static final int DEFAULT_STRUCTURE_DISCOVERY_CHECK_INTERVAL_TICKS = 20 * 10;

    private static final String SERVER_PLAYER_CLASS = "net.minecraft.class_3222";
    private static final String BLOCK_CLASS = "net.minecraft.class_2248";
    private static final String BLOCK_POS_CLASS = "net.minecraft.class_2338";
    private static final String DIRECTION_CLASS = "net.minecraft.class_2350";
    private static final String BLOCK_ITEM_CLASS = "net.minecraft.class_1747";
    private static final String ACTION_RESULT_CLASS = "net.minecraft.class_1269";
    private static final String TAMEABLE_ENTITY_CLASS = "net.minecraft.class_1321";
    private static final String TAMEABLE_INTERFACE_CLASS = "net.minecraft.class_6025";
    private static final String ABSTRACT_HORSE_ENTITY_CLASS = "net.minecraft.class_1496";

    private static final Pattern NAMESPACED_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_/.-]+");

    private static final Set<String> ALPHA_VALUABLE_BLOCK_IDS = Set.of(
            "minecraft:diamond_ore",
            "minecraft:deepslate_diamond_ore",
            "minecraft:emerald_ore",
            "minecraft:deepslate_emerald_ore",
            "minecraft:ancient_debris",
            "minecraft:spawner",
            "minecraft:trial_spawner",
            "minecraft:vault",
            "minecraft:suspicious_sand",
            "minecraft:suspicious_gravel"
    );

    private static final Set<String> PASSIVE_MOB_IDS = Set.of(
            "minecraft:allay",
            "minecraft:armadillo",
            "minecraft:axolotl",
            "minecraft:bat",
            "minecraft:camel",
            "minecraft:cat",
            "minecraft:chicken",
            "minecraft:cod",
            "minecraft:cow",
            "minecraft:donkey",
            "minecraft:frog",
            "minecraft:glow_squid",
            "minecraft:horse",
            "minecraft:mooshroom",
            "minecraft:mule",
            "minecraft:ocelot",
            "minecraft:parrot",
            "minecraft:pig",
            "minecraft:pufferfish",
            "minecraft:rabbit",
            "minecraft:salmon",
            "minecraft:sheep",
            "minecraft:skeleton_horse",
            "minecraft:sniffer",
            "minecraft:snow_golem",
            "minecraft:squid",
            "minecraft:strider",
            "minecraft:tadpole",
            "minecraft:tropical_fish",
            "minecraft:turtle",
            "minecraft:villager",
            "minecraft:wandering_trader"
    );

    private static final Set<String> NEUTRAL_MOB_IDS = Set.of(
            "minecraft:bee",
            "minecraft:cave_spider",
            "minecraft:dolphin",
            "minecraft:enderman",
            "minecraft:fox",
            "minecraft:goat",
            "minecraft:iron_golem",
            "minecraft:llama",
            "minecraft:panda",
            "minecraft:piglin",
            "minecraft:polar_bear",
            "minecraft:spider",
            "minecraft:trader_llama",
            "minecraft:wolf",
            "minecraft:zombified_piglin"
    );

    private static final Set<String> BOSS_MOB_IDS = Set.of(
            "minecraft:ender_dragon",
            "minecraft:wither",
            "minecraft:warden",
            "minecraft:elder_guardian"
    );

    private static long visitSampleTickCounter;
    private static long structureDiscoveryTickCounter;
    private static final Map<String, Long> STRUCTURE_DISCOVERY_CHECK_CACHE = new LinkedHashMap<>();
    private static boolean valuableBlockTagResolved;
    private static Object valuableBlockTag;

    private WorldRemembersLivingLegendsFabricEvents() {
    }

    static void register(Logger logger) {
        registerPlayerDeaths(logger);
        registerPlayerKills(logger);
        registerValuableBlockMining(logger);
        registerPortalTravel(logger);
        registerBlockPlacement(logger);
        registerRespawnPointSetting(logger);
        registerVisitSampling(logger);
        // TODO(stage 4 storage): add PET_TAMED when taming ownership can be captured without brittle hooks.
        // TODO(stage 4 storage): add RAID_WON when raid lifecycle integration is added.
    }

    private static void registerPlayerDeaths(Logger logger) {
        registerEvent(
                "player death",
                "net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents",
                "AFTER_DEATH",
                "net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents$AfterDeath",
                (proxy, method, args) -> {
                    if (isObjectMethod(method)) {
                        return handleObjectMethod(proxy, method, args, "player death hook");
                    }

                    Object entity = arg(args, 0);
                    Object damageSource = arg(args, 1);
                    if (isServerPlayer(entity)) {
                        collect(
                                entity,
                                EventType.PLAYER_DEATH,
                                blockPos(entity),
                                playerId(entity),
                                deathSubjectId(damageSource, entity),
                                4.0,
                                "player_death player_name=" + entityName(entity)
                                        + " cause=" + damageSourceName(damageSource)
                                        + " underwater=" + isUnderwater(entity),
                                logger
                        );
                        return defaultReturn(method);
                    }

                    boolean ownedPet = isPlayerOwnedPet(entity);
                    if (ownedPet) {
                        String petName = customEntityName(entity);
                        String ownerUuid = ownerId(entity);
                        debug(logger, "Pet death classified: entity=" + entityTypeId(entity)
                                + " name=" + (petName.isBlank() ? "<unnamed>" : petName)
                                + " owner=" + ownerUuid
                                + " event=pet_died");
                        collect(
                                entity,
                                EventType.PET_DIED,
                                blockPos(entity),
                                ownerUuid,
                                entityTypeId(entity),
                                5.0,
                                "pet_died pet_type=" + entityTypeId(entity)
                                        + notePart("pet_name", petName)
                                        + notePart("owner_uuid", ownerUuid)
                                        + notePart("owner_name", ownerName(entity))
                                        + " customName=" + hasCustomName(entity)
                                        + " cause=" + damageSourceName(damageSource),
                                logger
                        );
                        return defaultReturn(method);
                    }

                    if (hasCustomName(entity)) {
                        String mobName = customEntityName(entity);
                        debug(logger, "Named mob death classified: entity=" + entityTypeId(entity)
                                + " name=" + (mobName.isBlank() ? "<unnamed>" : mobName)
                                + " event=named_mob_died reason=not_tamed_or_no_owner");
                        collect(
                                entity,
                                EventType.NAMED_MOB_DIED,
                                blockPos(entity),
                                deathActorId(damageSource, entity),
                                entityTypeId(entity),
                                3.0,
                                "named_mob_died mob_type=" + entityTypeId(entity)
                                        + notePart("mob_name", mobName)
                                        + " cause=" + damageSourceName(damageSource),
                                logger
                        );
                    }
                    return defaultReturn(method);
                },
                logger
        );
    }

    private static void registerPlayerKills(Logger logger) {
        registerEvent(
                "player kill",
                "net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents",
                "AFTER_KILLED_OTHER_ENTITY",
                "net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents$AfterKilledOtherEntity",
                (proxy, method, args) -> {
                    if (isObjectMethod(method)) {
                        return handleObjectMethod(proxy, method, args, "player kill hook");
                    }

                    Object player = arg(args, 1);
                    Object killedEntity = arg(args, 2);
                    if (isServerPlayer(player) && killedEntity != null) {
                        if (isPlayerOwnedPet(killedEntity)) {
                            return defaultReturn(method);
                        }
                        EventType eventType = classifyKill(killedEntity);
                        String killedType = entityTypeId(killedEntity);
                        collect(
                                player,
                                eventType,
                                blockPos(killedEntity),
                                playerId(player),
                                entityId(killedEntity),
                                killImportance(eventType),
                                "player_kill target_type=" + killedType + " " + positionNote("player_pos", blockPos(player)),
                                logger
                        );

                        if (isBoss(killedEntity)) {
                            collect(
                                    player,
                                    EventType.BOSS_KILLED,
                                    blockPos(killedEntity),
                                    playerId(player),
                                    killedType,
                                    10.0,
                                    "boss_killed player_name=" + entityName(player)
                                            + " boss_type=" + killedType
                                            + " boss_name=" + entityName(killedEntity),
                                    logger
                            );
                        }
                    }
                    return defaultReturn(method);
                },
                logger
        );
    }

    private static void registerValuableBlockMining(Logger logger) {
        registerEvent(
                "valuable block mining",
                "net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents",
                "AFTER",
                "net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents$After",
                (proxy, method, args) -> {
                    if (isObjectMethod(method)) {
                        return handleObjectMethod(proxy, method, args, "valuable block mining hook");
                    }

                    Object player = arg(args, 1);
                    Object pos = arg(args, 2);
                    Object state = arg(args, 3);
                    if (isServerPlayer(player) && isValuableBlock(state)) {
                        String blockId = blockId(blockFromState(state));
                        collect(
                                player,
                                EventType.VALUABLE_BLOCK_MINED,
                                pos,
                                playerId(player),
                                blockId,
                                3.0,
                                "valuable_block_mined player_name=" + entityName(player) + " block=" + blockId,
                                logger
                        );
                    }
                    return defaultReturn(method);
                },
                logger
        );
    }

    private static void registerPortalTravel(Logger logger) {
        registerEvent(
                "portal travel",
                "net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents",
                "AFTER_PLAYER_CHANGE_WORLD",
                "net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents$AfterPlayerChange",
                (proxy, method, args) -> {
                    if (isObjectMethod(method)) {
                        return handleObjectMethod(proxy, method, args, "portal travel hook");
                    }

                    Object player = arg(args, 0);
                    Object origin = arg(args, 1);
                    Object destination = arg(args, 2);
                    String originDimension = dimensionId(origin);
                    String destinationDimension = dimensionId(destination);
                    if (isServerPlayer(player) && !destinationDimension.isBlank() && !destinationDimension.equals(originDimension)) {
                        collect(
                                player,
                                EventType.PLAYER_ENTERED_DIMENSION,
                                blockPos(player),
                                playerId(player),
                                destinationDimension,
                                1.0,
                                "player_entered_dimension player_name=" + entityName(player)
                                        + " from=" + originDimension
                                        + " to=" + destinationDimension,
                                logger
                        );

                    }

                    EventType portalEvent = portalEventType(originDimension, destinationDimension);
                    if (isServerPlayer(player) && portalEvent != null) {
                        collect(
                                player,
                                portalEvent,
                                blockPos(player),
                                playerId(player),
                                destinationDimension,
                                portalEvent == EventType.END_PORTAL_USED ? 8.0 : 3.0,
                                "portal_travel from=" + originDimension + " to=" + destinationDimension,
                                logger
                        );
                    }
                    return defaultReturn(method);
                },
                logger
        );
    }

    private static void registerBlockPlacement(Logger logger) {
        registerEvent(
                "block placement",
                "net.fabricmc.fabric.api.event.player.UseBlockCallback",
                "EVENT",
                "net.fabricmc.fabric.api.event.player.UseBlockCallback",
                (proxy, method, args) -> {
                    if (isObjectMethod(method)) {
                        return handleObjectMethod(proxy, method, args, "block placement hook");
                    }

                    Object player = arg(args, 0);
                    Object hand = arg(args, 2);
                    Object hitResult = arg(args, 3);
                    if (isServerPlayer(player)) {
                        captureBlockPlacementIntent(player, hand, hitResult, logger);
                    }
                    return defaultReturn(method);
                },
                logger
        );
    }

    private static void registerRespawnPointSetting(Logger logger) {
        registerEvent(
                "respawn point",
                "net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents",
                "ALLOW_SETTING_SPAWN",
                "net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents$AllowSettingSpawn",
                (proxy, method, args) -> {
                    if (isObjectMethod(method)) {
                        return handleObjectMethod(proxy, method, args, "respawn point hook");
                    }

                    Object player = arg(args, 0);
                    Object sleepingPos = arg(args, 1);
                    if (isServerPlayer(player)) {
                        collect(
                                player,
                                EventType.RESPAWN_POINT_SET,
                                sleepingPos,
                                playerId(player),
                                "minecraft:bed",
                                2.0,
                                "respawn_point_set source=bed_sleep",
                                logger
                        );
                    }
                    return Boolean.TRUE;
                },
                logger
        );
    }

    private static void registerVisitSampling(Logger logger) {
        registerEvent(
                "visit sampling",
                "net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents",
                "END_SERVER_TICK",
                "net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents$EndTick",
                (proxy, method, args) -> {
                    if (isObjectMethod(method)) {
                        return handleObjectMethod(proxy, method, args, "visit sampling hook");
                    }

                    visitSampleTickCounter++;
                    structureDiscoveryTickCounter++;
                    Object server = arg(args, 0);
                    EventCollector.expireTemporaryWindows();
                    WorldRemembersLivingLegendsFabricStorage.processDirtyScoreQueue(server, logger);
                    WorldRemembersLivingLegendsFabricStorage.processCandidateDecay(server, logger);
                    WorldRemembersLivingLegendsFabricPlaceTitles.tick(server, logger);

                    int visitSampleIntervalTicks = visitSampleIntervalTicks();
                    if (visitSampleTickCounter % visitSampleIntervalTicks == 0L) {
                        samplePlayerVisits(server, logger, visitSampleIntervalTicks);
                    }
                    int structureDiscoveryIntervalTicks = structureDiscoveryCheckIntervalTicks();
                    if (structureDiscoveryTickCounter % structureDiscoveryIntervalTicks == 0L) {
                        sampleStructureDiscoveries(server, logger, structureDiscoveryIntervalTicks);
                    }
                    return defaultReturn(method);
                },
                logger
        );
    }

    private static void registerEvent(
            String eventName,
            String ownerClassName,
            String fieldName,
            String listenerClassName,
            InvocationHandler handler,
            Logger logger
    ) {
        try {
            Class<?> ownerClass = Class.forName(ownerClassName);
            Object event = ownerClass.getField(fieldName).get(null);
            Class<?> listenerClass = Class.forName(listenerClassName);
            Object listener = Proxy.newProxyInstance(
                    listenerClass.getClassLoader(),
                    new Class<?>[]{listenerClass},
                    handler
            );
            Method register = event.getClass().getMethod("register", Object.class);
            register.setAccessible(true);
            register.invoke(event, listener);
        } catch (ReflectiveOperationException | LinkageError exception) {
            logger.warn("Failed to register Fabric " + eventName + " hook: "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static void captureBlockPlacementIntent(Object player, Object hand, Object hitResult, Logger logger) {
        Object stack = invoke(player, "method_5998", hand);
        Object item = invoke(stack, "method_7909");

        if (!isInstance(item, BLOCK_ITEM_CLASS)) {
            return;
        }

        Object block = invoke(item, "method_7711");
        String blockId = blockId(block);
        if (blockId.isBlank()) {
            return;
        }

        collect(
                player,
                EventType.PLAYER_BLOCK_PLACED,
                placedBlockPos(player, hitResult),
                playerId(player),
                blockId,
                0.0,
                "block_place_intent block=" + blockId,
                logger
        );
    }

    private static void samplePlayerVisits(Object server, Logger logger, int intervalTicks) {
        Object playerManager = invokeNoArg(server, "method_3760");
        Object players = invokeNoArg(playerManager, "method_14571");
        if (!(players instanceof Collection<?> playerCollection)) {
            return;
        }

        for (Object player : playerCollection) {
            if (!isServerPlayer(player)) {
                continue;
            }

            collect(
                    player,
                    EventType.PLAYER_VISIT,
                    blockPos(player),
                    playerId(player),
                    dimensionId(serverWorld(player)),
                    0.0,
                    "visit_sample interval_ticks=" + intervalTicks,
                    logger
            );
        }
    }

    private static void sampleStructureDiscoveries(Object server, Logger logger, int intervalTicks) {
        List<FirstDiscoveryDefinition> definitions = FirstDiscoveryDefinitions.structureDiscoveryDefinitions();
        if (definitions.isEmpty()) {
            return;
        }

        Object playerManager = invokeNoArg(server, "method_3760");
        Object players = invokeNoArg(playerManager, "method_14571");
        if (!(players instanceof Collection<?> playerCollection)) {
            return;
        }

        for (Object player : playerCollection) {
            if (!isServerPlayer(player)) {
                continue;
            }

            Object world = serverWorld(player);
            Object pos = blockPos(player);
            if (world == null || pos == null) {
                continue;
            }

            for (FirstDiscoveryDefinition definition : definitions) {
                if (WorldRemembersLivingLegendsFabricStorage.hasWorldFirstDiscovery(
                        world,
                        definition.discoveryIdString(),
                        logger
                )) {
                    continue;
                }

                if (structureDiscoveryCacheActive(world, pos, definition, intervalTicks)) {
                    continue;
                }

                StructureDetection detection = detectStructureAt(world, pos, definition.targetIdString());
                if (!detection.found()) {
                    continue;
                }

                if (detection.bounds() == null && debugEnabled() && logger != null) {
                    logger.info("structure_bounds_unavailable_fallback_radius"
                            + " structureId=" + definition.targetIdString()
                            + " firstDiscoveryKey=" + definition.discoveryIdString()
                            + " fallbackRadius=" + definition.fallbackRadius()
                            + " position=" + worldPos(world, pos).blockIdString());
                }

                collect(
                        player,
                        EventType.STRUCTURE_DISCOVERED,
                        pos,
                        playerId(player),
                        definition.targetIdString(),
                        definition.weight(),
                        "structure_discovered"
                                + " player_name=" + entityName(player)
                                + " structureId=" + definition.targetIdString()
                                + " firstDiscoveryKey=" + definition.discoveryIdString()
                                + " bounds_found=" + (detection.bounds() != null)
                                + " fallbackRadius=" + definition.fallbackRadius()
                                + " interval_ticks=" + intervalTicks,
                        definition.discoveryIdString(),
                        definition.targetIdString(),
                        detection.bounds(),
                        logger
                );
            }
        }

        trimStructureDiscoveryCache();
    }

    private static int visitSampleIntervalTicks() {
        return Math.max(
                20,
                WorldRemembersLivingLegends.config().performance.visitSampleIntervalTicks == 0
                        ? DEFAULT_VISIT_SAMPLE_INTERVAL_TICKS
                        : WorldRemembersLivingLegends.config().performance.visitSampleIntervalTicks
        );
    }

    private static int structureDiscoveryCheckIntervalTicks() {
        return Math.max(
                20,
                WorldRemembersLivingLegends.config().performance.structureDiscoveryCheckIntervalTicks == 0
                        ? DEFAULT_STRUCTURE_DISCOVERY_CHECK_INTERVAL_TICKS
                        : WorldRemembersLivingLegends.config().performance.structureDiscoveryCheckIntervalTicks
        );
    }

    private static boolean structureDiscoveryCacheActive(
            Object world,
            Object pos,
            FirstDiscoveryDefinition definition,
            int intervalTicks
    ) {
        String key = dimensionId(world)
                + "@"
                + Math.floorDiv(coordinate(pos, "method_10263"), 16)
                + ","
                + Math.floorDiv(coordinate(pos, "method_10260"), 16)
                + ":"
                + definition.targetIdString();
        long now = gameTime(world);
        long expiresAt = STRUCTURE_DISCOVERY_CHECK_CACHE.getOrDefault(key, Long.MIN_VALUE);
        if (expiresAt > now) {
            return true;
        }

        long cacheTicks = Math.max((long) intervalTicks * 4L, 400L);
        STRUCTURE_DISCOVERY_CHECK_CACHE.put(key, now + cacheTicks);
        return false;
    }

    private static void trimStructureDiscoveryCache() {
        if (STRUCTURE_DISCOVERY_CHECK_CACHE.size() <= 4096) {
            return;
        }

        var iterator = STRUCTURE_DISCOVERY_CHECK_CACHE.keySet().iterator();
        while (STRUCTURE_DISCOVERY_CHECK_CACHE.size() > 2048 && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    private static StructureDetection detectStructureAt(Object world, Object pos, String structureId) {
        Object structure = structureById(world, structureId);
        if (structure == null) {
            return StructureDetection.missing();
        }

        Object accessor = invokeNoArg(world, "method_27056");
        Object structureStart = invoke(accessor, "method_38854", pos, structure);
        if (!validStructureStart(structureStart)) {
            structureStart = invoke(accessor, "method_28388", pos, structure);
        }
        if (!validStructureStart(structureStart)) {
            return StructureDetection.missing();
        }

        return new StructureDetection(true, boundsFromStructureStart(world, structureStart));
    }

    private static Object structureById(Object world, String structureId) {
        try {
            Object accessor = invokeNoArg(world, "method_27056");
            Object registryManager = invokeNoArg(accessor, "method_41036");
            Object structureRegistryKey = staticField("net.minecraft.class_7924", "field_41246");
            Object structureRegistry = invoke(registryManager, "method_30530", structureRegistryKey);
            Object identifier = identifier(structureId);
            return invoke(structureRegistry, "method_10223", identifier);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private static boolean validStructureStart(Object structureStart) {
        if (structureStart == null) {
            return false;
        }

        Object hasChildren = invokeNoArg(structureStart, "method_16657");
        return hasChildren instanceof Boolean value && value;
    }

    private static PlaceBounds boundsFromStructureStart(Object world, Object structureStart) {
        Object box = invokeNoArg(structureStart, "method_14969");
        if (box == null) {
            return null;
        }

        String dimensionId = dimensionId(world);
        if (dimensionId.isBlank()) {
            dimensionId = "minecraft:overworld";
        }
        return PlaceBounds.structureBounds(
                dimensionId,
                boxCoordinate(box, "method_35415"),
                boxCoordinate(box, "method_35416"),
                boxCoordinate(box, "method_35417"),
                boxCoordinate(box, "method_35418"),
                boxCoordinate(box, "method_35419"),
                boxCoordinate(box, "method_35420")
        );
    }

    private static int boxCoordinate(Object box, String methodName) {
        Object value = invokeNoArg(box, methodName);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static Object identifier(String id) throws ReflectiveOperationException {
        String normalized = cleanId(id);
        String namespace = "minecraft";
        String path = normalized;
        int separator = normalized.indexOf(':');
        if (separator >= 0) {
            namespace = normalized.substring(0, separator);
            path = normalized.substring(separator + 1);
        }

        Class<?> identifierClass = Class.forName("net.minecraft.class_2960");
        Method identifierOf = identifierClass.getMethod("method_60655", String.class, String.class);
        return identifierOf.invoke(null, namespace, path);
    }

    private static boolean debugEnabled() {
        return WorldRemembersLivingLegends.config().debug.enabled;
    }

    private static EventType classifyKill(Object killedEntity) {
        if (isServerPlayer(killedEntity)) {
            return EventType.PLAYER_KILLED_PLAYER;
        }

        String typeId = entityTypeId(killedEntity);
        WorldRemembersCompatRegistries.CompatLookup<com.worldremembers.livinglegends.CompatThemeDefinitions.MobThemeDefinition> compat =
                WorldRemembersCompatApi.getMobTheme(typeId);
        if (compat.matched()) {
            return switch (compat.definition().combatRole()) {
                case "passive", "companion" -> EventType.PLAYER_KILLED_PASSIVE_MOB;
                case "neutral" -> EventType.PLAYER_KILLED_NEUTRAL_MOB;
                case "hostile", "boss" -> EventType.PLAYER_KILLED_HOSTILE_MOB;
                default -> EventType.PLAYER_KILLED_HOSTILE_MOB;
            };
        }
        if (NEUTRAL_MOB_IDS.contains(typeId)) {
            return EventType.PLAYER_KILLED_NEUTRAL_MOB;
        }
        if (PASSIVE_MOB_IDS.contains(typeId)) {
            return EventType.PLAYER_KILLED_PASSIVE_MOB;
        }
        if (!typeId.isBlank()) {
            return EventType.PLAYER_KILLED_HOSTILE_MOB;
        }

        return EventType.MOB_KILL;
    }

    private static double killImportance(EventType eventType) {
        return switch (eventType) {
            case PLAYER_KILLED_PLAYER, PLAYER_KILL_PLAYER -> 5.0;
            case PLAYER_KILLED_HOSTILE_MOB, PLAYER_KILL_HOSTILE_MOB -> 2.0;
            case PLAYER_KILLED_NEUTRAL_MOB, PLAYER_KILL_NEUTRAL_MOB -> 1.5;
            case PLAYER_KILLED_PASSIVE_MOB, PLAYER_KILL_PASSIVE_MOB -> 0.5;
            default -> 1.0;
        };
    }

    private static EventType portalEventType(String origin, String destination) {
        String destinationPortalTheme = WorldRemembersCompatApi.getDimensionTheme(destination).matched()
                ? WorldRemembersCompatApi.getDimensionTheme(destination).definition().portalTheme()
                : "";
        String originPortalTheme = WorldRemembersCompatApi.getDimensionTheme(origin).matched()
                ? WorldRemembersCompatApi.getDimensionTheme(origin).definition().portalTheme()
                : "";
        if ("end".equals(destinationPortalTheme) || "end".equals(originPortalTheme)) {
            return EventType.END_PORTAL_USED;
        }
        if ("nether".equals(destinationPortalTheme) || "nether".equals(originPortalTheme)) {
            return EventType.NETHER_PORTAL_USED;
        }
        if ("minecraft:the_end".equals(origin) || "minecraft:the_end".equals(destination)) {
            return EventType.END_PORTAL_USED;
        }
        if ("minecraft:the_nether".equals(origin) || "minecraft:the_nether".equals(destination)) {
            return EventType.NETHER_PORTAL_USED;
        }

        return null;
    }

    private static boolean isValuableBlock(Object state) {
        Object block = blockFromState(state);
        String blockId = blockId(block);
        return WorldRemembersCompatApi.isValuableBlock(blockId)
                || ALPHA_VALUABLE_BLOCK_IDS.contains(blockId)
                || isInValuableBlockTag(state);
    }

    private static boolean isBoss(Object entity) {
        String entityId = entityTypeId(entity);
        return WorldRemembersCompatApi.isBoss(entityId) || BOSS_MOB_IDS.contains(entityId);
    }

    private static boolean isPlayerOwnedPet(Object entity) {
        if (entity == null) {
            return false;
        }

        if (isInstance(entity, TAMEABLE_ENTITY_CLASS) || isInstance(entity, TAMEABLE_INTERFACE_CLASS)) {
            Object tamed = invokeNoArg(entity, "method_6181");
            String ownerUuid = ownerId(entity);
            if (tamed instanceof Boolean value) {
                return value && !ownerUuid.isBlank();
            }
            return !ownerUuid.isBlank();
        }

        if (isInstance(entity, ABSTRACT_HORSE_ENTITY_CLASS)) {
            Object tame = invokeNoArg(entity, "method_6727");
            return tame instanceof Boolean value && value && !ownerId(entity).isBlank();
        }

        return false;
    }

    private static boolean hasCustomName(Object entity) {
        Object hasCustomName = invokeNoArg(entity, "method_16914");
        return hasCustomName instanceof Boolean value && value;
    }

    private static boolean isUnderwater(Object entity) {
        Object submerged = invokeNoArg(entity, "method_5869");
        if (submerged instanceof Boolean value && value) {
            return true;
        }

        Object touchingWater = invokeNoArg(entity, "method_5799");
        return touchingWater instanceof Boolean value && value;
    }

    private static boolean isInValuableBlockTag(Object state) {
        Object tag = valuableBlockTag();
        if (state == null || tag == null) {
            return false;
        }

        Object result = invoke(state, "method_26164", tag);
        return result instanceof Boolean value && value;
    }

    private static Object valuableBlockTag() {
        if (valuableBlockTagResolved) {
            return valuableBlockTag;
        }

        valuableBlockTagResolved = true;
        try {
            Class<?> identifierClass = Class.forName("net.minecraft.class_2960");
            Method identifierOf = identifierClass.getMethod("method_60655", String.class, String.class);
            Object identifier = identifierOf.invoke(null, WorldRemembersLivingLegends.MOD_ID, "valuable_blocks");

            Object blockRegistryKey = staticField("net.minecraft.class_7924", "field_41254");
            Class<?> registryKeyClass = Class.forName("net.minecraft.class_5321");
            Class<?> tagKeyClass = Class.forName("net.minecraft.class_6862");
            Method tagKeyOf = tagKeyClass.getMethod("method_40092", registryKeyClass, identifierClass);
            valuableBlockTag = tagKeyOf.invoke(null, blockRegistryKey, identifier);
        } catch (ReflectiveOperationException | LinkageError exception) {
            valuableBlockTag = null;
        }

        return valuableBlockTag;
    }

    private static void collect(
            Object player,
            EventType eventType,
            Object pos,
            String actorId,
            String subjectId,
            double importance,
            String note,
            Logger logger
    ) {
        collect(player, eventType, pos, actorId, subjectId, importance, note, "", "", null, logger);
    }

    private static void collect(
            Object player,
            EventType eventType,
            Object pos,
            String actorId,
            String subjectId,
            double importance,
            String note,
            String firstDiscoveryKey,
            String structureId,
            PlaceBounds structureBounds,
            Logger logger
    ) {
        Object world = serverWorld(player);
        Object resolvedPos = pos == null ? blockPos(player) : pos;
        WorldMemoryEvent event = new WorldMemoryEvent(
                eventId(eventType, player, world, resolvedPos),
                eventType,
                worldPos(world, resolvedPos),
                actorId,
                subjectId,
                gameTime(world),
                System.currentTimeMillis(),
                importance,
                note,
                firstDiscoveryKey,
                structureId,
                structureBounds
        );
        WorldRemembersLivingLegendsFabricStorage.recordEvent(world, event, logger);
    }

    private static String eventId(EventType eventType, Object player, Object world, Object pos) {
        return eventType.idString()
                + ":" + playerId(player)
                + ":" + dimensionId(world)
                + ":" + coordinate(pos, "method_10263")
                + "," + coordinate(pos, "method_10264")
                + "," + coordinate(pos, "method_10260")
                + ":" + gameTime(world);
    }

    private static WorldPos worldPos(Object world, Object pos) {
        String dimensionId = dimensionId(world);
        if (dimensionId.isBlank()) {
            dimensionId = "minecraft:overworld";
        }

        return new WorldPos(
                dimensionId,
                coordinate(pos, "method_10263"),
                coordinate(pos, "method_10264"),
                coordinate(pos, "method_10260")
        );
    }

    private static Object placedBlockPos(Object player, Object hitResult) {
        Object basePos = invokeNoArg(hitResult, "method_17777");
        Object side = invokeNoArg(hitResult, "method_17780");
        Object placedPos = invoke(basePos, "method_10093", side);

        if (isInstance(placedPos, BLOCK_POS_CLASS)) {
            return placedPos;
        }

        return blockPos(player);
    }

    private static Object blockPos(Object entity) {
        Object pos = invokeNoArg(entity, "method_24515");
        if (isInstance(pos, BLOCK_POS_CLASS)) {
            return pos;
        }

        return null;
    }

    private static String positionNote(String key, Object pos) {
        return key
                + "=" + coordinate(pos, "method_10263")
                + "," + coordinate(pos, "method_10264")
                + "," + coordinate(pos, "method_10260");
    }

    private static Object serverWorld(Object player) {
        Object world = invokeNoArg(player, "method_51469");
        if (world != null) {
            return world;
        }

        world = invokeNoArg(player, "method_37908");
        if (world != null) {
            return world;
        }

        return invokeNoArg(player, "method_5770");
    }

    private static long gameTime(Object world) {
        Object time = invokeNoArg(world, "method_8510");
        if (time instanceof Number number) {
            return number.longValue();
        }

        time = invokeNoArg(world, "method_8532");
        if (time instanceof Number number) {
            return number.longValue();
        }

        return 0L;
    }

    private static int coordinate(Object pos, String methodName) {
        Object value = invokeNoArg(pos, methodName);
        if (value instanceof Number number) {
            return number.intValue();
        }

        return 0;
    }

    private static String dimensionId(Object world) {
        Object registryKey = invokeNoArg(world, "method_27983");
        Object identifier = invokeNoArg(registryKey, "method_29177");
        return cleanId(identifier == null ? String.valueOf(registryKey) : String.valueOf(identifier));
    }

    private static String playerId(Object player) {
        return entityId(player);
    }

    private static String entityId(Object entity) {
        if (entity == null) {
            return "";
        }

        Object id = invokeNoArg(entity, "method_5845");
        if (id instanceof String value && !value.isBlank()) {
            return value;
        }

        id = invokeNoArg(entity, "method_5667");
        return id == null ? "" : String.valueOf(id);
    }

    private static String entityTypeId(Object entity) {
        Object type = invokeNoArg(entity, "method_5864");
        return idFromRegistry("field_41177", type);
    }

    private static String entityName(Object entity) {
        Object text = invokeNoArg(entity, "method_5477");
        Object value = invokeNoArg(text, "getString");
        if (value instanceof String name && !name.isBlank()) {
            return compactNoteValue(name);
        }

        value = invokeNoArg(entity, "method_5845");
        if (value instanceof String id && !id.isBlank()) {
            return id;
        }

        return entityTypeId(entity);
    }

    private static String customEntityName(Object entity) {
        if (!hasCustomName(entity)) {
            return "";
        }

        Object text = invokeNoArg(entity, "method_5797");
        Object value = invokeNoArg(text, "getString");
        if (value instanceof String name && !name.isBlank()) {
            return RuntimeNameFormatter.sanitize(name);
        }

        Object fallbackName = invokeNoArg(entity, "method_5477");
        value = invokeNoArg(fallbackName, "getString");
        return value instanceof String name && !name.isBlank()
                ? RuntimeNameFormatter.sanitize(name)
                : "";
    }

    private static String ownerId(Object entity) {
        Object id = invokeNoArg(entity, "method_6139");
        if (id != null) {
            return String.valueOf(id);
        }

        id = instanceField(entity, "field_42462");
        return id == null ? "" : String.valueOf(id);
    }

    private static String ownerName(Object entity) {
        Object owner = invokeNoArg(entity, "method_24921");
        if (owner == null) {
            owner = invokeNoArg(entity, "method_35057");
        }
        return owner == null ? "" : entityName(owner);
    }

    private static String deathActorId(Object damageSource, Object deadEntity) {
        Object attacker = attackerFromDamageSource(damageSource);
        String attackerId = entityId(attacker);
        return attackerId.isBlank() ? entityId(deadEntity) : attackerId;
    }

    private static String deathSubjectId(Object damageSource, Object deadEntity) {
        Object attacker = attackerFromDamageSource(damageSource);
        String attackerId = entityId(attacker);
        if (!attackerId.isBlank() && !attackerId.equals(entityId(deadEntity))) {
            return attackerId;
        }

        String cause = damageSourceName(damageSource);
        return cause.isBlank() ? entityId(deadEntity) : "cause:" + cause;
    }

    private static String blockId(Object block) {
        return idFromRegistry("field_41175", block);
    }

    private static String idFromRegistry(String registryField, Object value) {
        if (value == null) {
            return "";
        }

        try {
            Object registry = staticField("net.minecraft.class_7923", registryField);
            Method getId = registry.getClass().getMethod("method_10221", Object.class);
            getId.setAccessible(true);
            Object id = getId.invoke(registry, value);
            return cleanId(String.valueOf(id));
        } catch (ReflectiveOperationException | LinkageError exception) {
            return "";
        }
    }

    private static Object blockFromState(Object state) {
        Object block = invokeNoArg(state, "method_26204");
        if (isInstance(block, BLOCK_CLASS)) {
            return block;
        }

        return null;
    }

    private static Object attackerFromDamageSource(Object damageSource) {
        Object attacker = invokeNoArg(damageSource, "method_5529");
        if (attacker != null) {
            return attacker;
        }

        return invokeNoArg(damageSource, "method_5526");
    }

    private static String damageSourceName(Object damageSource) {
        Object name = invokeNoArg(damageSource, "method_5525");
        return name == null ? "unknown" : String.valueOf(name);
    }

    private static boolean isServerPlayer(Object object) {
        return isInstance(object, SERVER_PLAYER_CLASS);
    }

    private static boolean isInstance(Object object, String className) {
        if (object == null) {
            return false;
        }

        try {
            return Class.forName(className).isInstance(object);
        } catch (ClassNotFoundException | LinkageError exception) {
            return false;
        }
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }

        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private static Object invoke(Object target, String methodName, Object... args) {
        if (target == null || args == null) {
            return null;
        }

        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean compatible = true;
            for (int index = 0; index < args.length; index++) {
                if (args[index] != null && !parameterTypes[index].isInstance(args[index])) {
                    compatible = false;
                    break;
                }
            }

            if (!compatible) {
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

    private static Object staticField(String className, String fieldName) throws ReflectiveOperationException {
        Class<?> type = Class.forName(className);
        Field field = type.getField(fieldName);
        field.setAccessible(true);
        return field.get(null);
    }

    private static Object instanceField(Object target, String fieldName) {
        if (target == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }

        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (ReflectiveOperationException exception) {
                type = type.getSuperclass();
            } catch (LinkageError exception) {
                return null;
            }
        }
        return null;
    }

    private static void debug(Logger logger, String message) {
        if (logger != null && debugEnabled()) {
            logger.info(message);
        }
    }

    private static String cleanId(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        Matcher matcher = NAMESPACED_ID.matcher(value);
        String lastMatch = "";
        while (matcher.find()) {
            lastMatch = matcher.group();
        }

        return lastMatch.isBlank() ? value.trim() : lastMatch;
    }

    private static String compactNoteValue(String value) {
        return RuntimeNameFormatter.encodeNoteValue(value);
    }

    private static String notePart(String key, String value) {
        String encoded = RuntimeNameFormatter.encodeNoteValue(value);
        return encoded.isBlank() ? "" : " " + key + "=" + encoded;
    }

    private static Object defaultReturn(Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType == Void.TYPE) {
            return null;
        }
        if (returnType == Boolean.TYPE) {
            return Boolean.FALSE;
        }
        if (returnType == Integer.TYPE) {
            return 0;
        }
        if (returnType == Long.TYPE) {
            return 0L;
        }
        if (returnType == Float.TYPE) {
            return 0.0F;
        }
        if (returnType == Double.TYPE) {
            return 0.0D;
        }
        if (returnType == Byte.TYPE) {
            return (byte) 0;
        }
        if (returnType == Short.TYPE) {
            return (short) 0;
        }
        if (returnType == Character.TYPE) {
            return (char) 0;
        }
        if (ACTION_RESULT_CLASS.equals(returnType.getName())) {
            return actionResultPass();
        }

        return null;
    }

    private static Object actionResultPass() {
        try {
            return staticField(ACTION_RESULT_CLASS, "field_5811");
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private static Object arg(Object[] args, int index) {
        if (args == null || index < 0 || index >= args.length) {
            return null;
        }

        return args[index];
    }

    private static boolean isObjectMethod(Method method) {
        return method.getDeclaringClass() == Object.class;
    }

    private static Object handleObjectMethod(Object proxy, Method method, Object[] args, String name) {
        return switch (method.getName()) {
            case "toString" -> WorldRemembersLivingLegends.MOD_ID + " " + name;
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == arg(args, 0);
            default -> null;
        };
    }

    private record StructureDetection(boolean found, PlaceBounds bounds) {
        private static StructureDetection missing() {
            return new StructureDetection(false, null);
        }
    }
}
