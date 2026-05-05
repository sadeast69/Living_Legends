package com.worldremembers.livinglegends.neoforge;

import com.worldremembers.livinglegends.EventCollector;
import com.worldremembers.livinglegends.EventType;
import com.worldremembers.livinglegends.FirstDiscoveryDefinition;
import com.worldremembers.livinglegends.FirstDiscoveryDefinitions;
import com.worldremembers.livinglegends.PlaceBounds;
import com.worldremembers.livinglegends.RuntimeNameFormatter;
import com.worldremembers.livinglegends.WorldMemoryEvent;
import com.worldremembers.livinglegends.WorldPos;
import com.worldremembers.livinglegends.WorldRemembersCompatApi;
import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class WorldRemembersLivingLegendsNeoForgeEvents {
    private static final int DEFAULT_VISIT_SAMPLE_INTERVAL_TICKS = 20 * 20;
    private static final int DEFAULT_STRUCTURE_DISCOVERY_CHECK_INTERVAL_TICKS = 20 * 10;
    private static final long SOURCE_ANCHOR_MAX_AGE_TICKS = 200L;
    private static final TagKey<Block> VALUABLE_BLOCKS_TAG = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(WorldRemembersLivingLegends.MOD_ID, "valuable_blocks")
    );

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
    private static final Map<String, SourceAnchor> PLAYER_SOURCE_ANCHORS = new LinkedHashMap<>();

    private WorldRemembersLivingLegendsNeoForgeEvents() {
    }

    static void register(Logger logger) {
        NeoForge.EVENT_BUS.addListener((ServerStartingEvent event) -> onServerStarting(event, logger));
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> onServerStarted(event, logger));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> onServerTick(event, logger));
        NeoForge.EVENT_BUS.addListener((LivingDeathEvent event) -> onLivingDeath(event, logger));
        NeoForge.EVENT_BUS.addListener((BlockEvent.BreakEvent event) -> onBlockBreak(event, logger));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> onPlayerChangedDimension(event, logger));
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock event) -> onRightClickBlock(event, logger));
        NeoForge.EVENT_BUS.addListener((PlayerSetSpawnEvent event) -> onPlayerSetSpawn(event, logger));
    }

    private static void onServerStarting(ServerStartingEvent event, Logger logger) {
        WorldRemembersLivingLegends.loadConfigOnServerStart(
                FMLPaths.GAMEDIR.get(),
                logger == null ? null : logger::info,
                logger == null ? null : logger::warn
        );
    }

    private static void onServerStarted(ServerStartedEvent event, Logger logger) {
        WorldRemembersLivingLegendsNeoForgeStorage.initializeServer(event.getServer(), logger);
    }

    private static void onServerTick(ServerTickEvent.Post event, Logger logger) {
        MinecraftServer server = event.getServer();
        visitSampleTickCounter++;
        structureDiscoveryTickCounter++;
        cachePlayerSourceAnchors(server);

        EventCollector.expireTemporaryWindows();
        WorldRemembersLivingLegendsNeoForgeStorage.processDirtyScoreQueue(server, logger);
        WorldRemembersLivingLegendsNeoForgeStorage.processCandidateDecay(server, logger);
        WorldRemembersLivingLegendsNeoForgePlaceTitles.tick(server, logger);

        int visitSampleIntervalTicks = visitSampleIntervalTicks();
        if (visitSampleTickCounter % visitSampleIntervalTicks == 0L) {
            samplePlayerVisits(server, logger, visitSampleIntervalTicks);
        }

        int structureDiscoveryIntervalTicks = structureDiscoveryCheckIntervalTicks();
        if (structureDiscoveryTickCounter % structureDiscoveryIntervalTicks == 0L) {
            sampleStructureDiscoveries(server, logger, structureDiscoveryIntervalTicks);
        }
    }

    private static void onBlockBreak(BlockEvent.BreakEvent event, Logger logger) {
        if (event.isCanceled()
                || !(event.getPlayer() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel)) {
            return;
        }

        BlockState state = event.getState();
        if (!isValuableBlock(state)) {
            return;
        }

        String blockId = blockId(state.getBlock());
        collect(
                player,
                EventType.VALUABLE_BLOCK_MINED,
                event.getPos(),
                playerId(player),
                blockId,
                3.0,
                "valuable_block_mined player_name=" + entityName(player) + " block=" + blockId,
                logger
        );
    }

    private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event, Logger logger) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        String originDimension = event.getFrom().location().toString();
        String destinationDimension = event.getTo().location().toString();
        SourceAnchor sourceAnchor = resolveSourceAnchor(player, originDimension);
        boolean fallback = sourceAnchor == null;
        ServerLevel sourceWorld = player.getServer() == null ? null : player.getServer().getLevel(event.getFrom());
        ServerLevel fallbackWorld = player.level() instanceof ServerLevel serverLevel ? serverLevel : sourceWorld;
        BlockPos destinationPos = player.blockPosition();
        debug(logger, "portal_travel_source_anchor"
                + " player=" + entityName(player)
                + " from=" + originDimension
                + " to=" + destinationDimension
                + " sourcePos=" + sourcePositionLog(sourceAnchor, destinationDimension, destinationPos)
                + " destinationPos=" + positionLog(destinationDimension, destinationPos)
                + " fallback=" + fallback);
        if (!destinationDimension.isBlank() && !destinationDimension.equals(originDimension)) {
            String note = "player_entered_dimension player_name=" + entityName(player)
                    + " from=" + originDimension
                    + " to=" + destinationDimension;
            if (fallback) {
                collect(
                        player,
                        EventType.PLAYER_ENTERED_DIMENSION,
                        destinationPos,
                        playerId(player),
                        destinationDimension,
                        1.0,
                        note,
                        logger
                );
            } else {
                collectAtSource(
                        sourceWorld,
                        fallbackWorld,
                        player,
                        EventType.PLAYER_ENTERED_DIMENSION,
                        sourceAnchor,
                        playerId(player),
                        destinationDimension,
                        1.0,
                        note,
                        logger
                );
            }
        }

        EventType portalEvent = portalEventType(originDimension, destinationDimension);
        if (portalEvent != null) {
            String note = "portal_travel from=" + originDimension + " to=" + destinationDimension;
            if (fallback) {
                collect(
                        player,
                        portalEvent,
                        destinationPos,
                        playerId(player),
                        destinationDimension,
                        portalEvent == EventType.END_PORTAL_USED ? 8.0 : 3.0,
                        note,
                        logger
                );
            } else {
                collectAtSource(
                        sourceWorld,
                        fallbackWorld,
                        player,
                        portalEvent,
                        sourceAnchor,
                        playerId(player),
                        destinationDimension,
                        portalEvent == EventType.END_PORTAL_USED ? 8.0 : 3.0,
                        note,
                        logger
                );
            }
        }
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event, Logger logger) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel)) {
            return;
        }

        captureBlockPlacementIntent(player, event.getHand(), event.getPos(), event.getFace(), logger);
    }

    private static void onPlayerSetSpawn(PlayerSetSpawnEvent event, Logger logger) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getNewSpawn() == null || event.isForced()) {
            return;
        }

        collect(
                player,
                EventType.RESPAWN_POINT_SET,
                event.getNewSpawn(),
                playerId(player),
                "minecraft:bed",
                2.0,
                "respawn_point_set source=bed_sleep",
                logger
        );
    }

    private static void onLivingDeath(LivingDeathEvent event, Logger logger) {
        LivingEntity deadEntity = event.getEntity();
        DamageSource source = event.getSource();
        if (!(deadEntity.level() instanceof ServerLevel)) {
            return;
        }

        boolean ownedPet = isPlayerOwnedPet(deadEntity);
        if (deadEntity instanceof ServerPlayer deadPlayer) {
            collect(
                    deadPlayer,
                    EventType.PLAYER_DEATH,
                    deadPlayer.blockPosition(),
                    playerId(deadPlayer),
                    deathSubjectId(source, deadPlayer),
                    4.0,
                    "player_death player_name=" + entityName(deadPlayer)
                            + " cause=" + damageSourceName(source)
                            + " underwater=" + isUnderwater(deadPlayer),
                    logger
            );
        } else if (ownedPet) {
            String petName = customEntityName(deadEntity);
            String ownerUuid = ownerId(deadEntity);
            debug(logger, "Pet death classified: entity=" + entityTypeId(deadEntity)
                    + " name=" + (petName.isBlank() ? "<unnamed>" : petName)
                    + " ownerUuid=" + ownerUuid
                    + " event=pet_died");
            collect(
                    deadEntity,
                    EventType.PET_DIED,
                    deadEntity.blockPosition(),
                    ownerUuid,
                    entityTypeId(deadEntity),
                    5.0,
                    "pet_died pet_type=" + entityTypeId(deadEntity)
                            + notePart("pet_name", petName)
                            + notePart("owner_uuid", ownerUuid)
                            + notePart("owner_name", ownerName(deadEntity))
                            + " customName=" + hasCustomName(deadEntity)
                            + " cause=" + damageSourceName(source),
                    logger
            );
            return;
        } else if (hasCustomName(deadEntity)) {
            String mobName = customEntityName(deadEntity);
            debug(logger, "Named mob death classified: entity=" + entityTypeId(deadEntity)
                    + " name=" + (mobName.isBlank() ? "<unnamed>" : mobName)
                    + " event=named_mob_died reason=not_tamed_or_no_owner");
            collect(
                    deadEntity,
                    EventType.NAMED_MOB_DIED,
                    deadEntity.blockPosition(),
                    deathActorId(source, deadEntity),
                    entityTypeId(deadEntity),
                    3.0,
                    "named_mob_died mob_type=" + entityTypeId(deadEntity)
                            + notePart("mob_name", mobName)
                            + " cause=" + damageSourceName(source),
                    logger
            );
        }

        Entity attacker = attackerFromDamageSource(source);
        if (attacker instanceof ServerPlayer player && deadEntity != player) {
            EventType eventType = classifyKill(deadEntity);
            String killedType = entityTypeId(deadEntity);
            collect(
                    player,
                    eventType,
                    deadEntity.blockPosition(),
                    playerId(player),
                    entityId(deadEntity),
                    killImportance(eventType),
                    "player_kill target_type=" + killedType + " " + positionNote("player_pos", player.blockPosition()),
                    logger
            );

            if (isBoss(deadEntity)) {
                collect(
                        player,
                        EventType.BOSS_KILLED,
                        deadEntity.blockPosition(),
                        playerId(player),
                        killedType,
                        10.0,
                        "boss_killed player_name=" + entityName(player)
                                + " boss_type=" + killedType
                                + " boss_name=" + entityName(deadEntity),
                        logger
                );
            }
        }
    }

    private static void cachePlayerSourceAnchors(MinecraftServer server) {
        if (server == null) {
            PLAYER_SOURCE_ANCHORS.clear();
            return;
        }
        Set<String> onlinePlayerIds = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player == null || !(player.level() instanceof ServerLevel world)) {
                continue;
            }
            String playerId = playerId(player);
            if (playerId.isBlank()) {
                continue;
            }
            BlockPos pos = player.blockPosition();
            onlinePlayerIds.add(playerId);
            PLAYER_SOURCE_ANCHORS.put(playerId, new SourceAnchor(
                    dimensionId(world),
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    gameTime(world),
                    visitSampleTickCounter
            ));
        }
        PLAYER_SOURCE_ANCHORS.keySet().removeIf(playerId -> !onlinePlayerIds.contains(playerId));
    }

    private static SourceAnchor resolveSourceAnchor(ServerPlayer player, String originDimension) {
        SourceAnchor anchor = PLAYER_SOURCE_ANCHORS.get(playerId(player));
        if (anchor == null || !anchor.dimensionId().equals(originDimension)) {
            return null;
        }
        long ageTicks = Math.max(0L, visitSampleTickCounter - anchor.capturedAtTick());
        return ageTicks <= SOURCE_ANCHOR_MAX_AGE_TICKS ? anchor : null;
    }

    private static EventType classifyKill(Entity killedEntity) {
        if (killedEntity instanceof ServerPlayer) {
            return EventType.PLAYER_KILLED_PLAYER;
        }

        String typeId = entityTypeId(killedEntity);
        var compat = WorldRemembersCompatApi.getMobTheme(typeId);
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

    private static boolean isValuableBlock(BlockState state) {
        if (state == null) {
            return false;
        }

        String id = blockId(state.getBlock());
        return WorldRemembersCompatApi.isValuableBlock(id)
                || ALPHA_VALUABLE_BLOCK_IDS.contains(id)
                || isInValuableBlockTag(state);
    }

    private static boolean isInValuableBlockTag(BlockState state) {
        try {
            return state.is(VALUABLE_BLOCKS_TAG);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static void captureBlockPlacementIntent(
            ServerPlayer player,
            InteractionHand hand,
            BlockPos clickedPos,
            Direction face,
            Logger logger
    ) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        String blockId = blockId(blockItem.getBlock());
        if (blockId.isBlank()) {
            return;
        }

        BlockPos placedPos = face == null ? clickedPos : clickedPos.relative(face);
        collect(
                player,
                EventType.PLAYER_BLOCK_PLACED,
                placedPos,
                playerId(player),
                blockId,
                0.0,
                "block_place_intent block=" + blockId,
                logger
        );
    }

    private static void samplePlayerVisits(MinecraftServer server, Logger logger, int intervalTicks) {
        if (server == null) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            collect(
                    player,
                    EventType.PLAYER_VISIT,
                    player.blockPosition(),
                    playerId(player),
                    dimensionId(player.level()),
                    0.0,
                    "visit_sample interval_ticks=" + intervalTicks,
                    logger
            );
        }
    }

    private static void sampleStructureDiscoveries(MinecraftServer server, Logger logger, int intervalTicks) {
        if (server == null) {
            return;
        }

        List<FirstDiscoveryDefinition> definitions = FirstDiscoveryDefinitions.structureDiscoveryDefinitions();
        if (definitions.isEmpty()) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player.level() instanceof ServerLevel world)) {
                continue;
            }

            BlockPos pos = player.blockPosition();
            for (FirstDiscoveryDefinition definition : definitions) {
                if (WorldRemembersLivingLegendsNeoForgeStorage.hasWorldFirstDiscovery(
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
            ServerLevel world,
            BlockPos pos,
            FirstDiscoveryDefinition definition,
            int intervalTicks
    ) {
        String key = dimensionId(world)
                + "@"
                + Math.floorDiv(pos.getX(), 16)
                + ","
                + Math.floorDiv(pos.getZ(), 16)
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

    private static StructureDetection detectStructureAt(ServerLevel world, BlockPos pos, String structureId) {
        Structure structure = structureById(world, structureId);
        if (structure == null) {
            return StructureDetection.missing();
        }

        try {
            StructureStart structureStart = world.structureManager().getStructureAt(pos, structure);
            if (!validStructureStart(structureStart)) {
                structureStart = world.structureManager().getStructureWithPieceAt(pos, structure);
            }
            if (!validStructureStart(structureStart)) {
                return StructureDetection.missing();
            }
            return new StructureDetection(true, boundsFromStructureStart(world, structureStart));
        } catch (RuntimeException exception) {
            return StructureDetection.missing();
        }
    }

    private static Structure structureById(ServerLevel world, String structureId) {
        ResourceLocation id = ResourceLocation.tryParse(structureId);
        if (id == null) {
            return null;
        }

        try {
            return world.registryAccess().registryOrThrow(Registries.STRUCTURE).get(id);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static boolean validStructureStart(StructureStart structureStart) {
        return structureStart != null && structureStart.isValid();
    }

    private static PlaceBounds boundsFromStructureStart(ServerLevel world, StructureStart structureStart) {
        if (structureStart == null) {
            return null;
        }

        BoundingBox box = structureStart.getBoundingBox();
        if (box == null) {
            return null;
        }

        String dimensionId = dimensionId(world);
        if (dimensionId.isBlank()) {
            dimensionId = "minecraft:overworld";
        }
        return PlaceBounds.structureBounds(
                dimensionId,
                box.minX(),
                box.minY(),
                box.minZ(),
                box.maxX(),
                box.maxY(),
                box.maxZ()
        );
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

    private static boolean isBoss(Entity entity) {
        String entityId = entityTypeId(entity);
        return BOSS_MOB_IDS.contains(entityId) || WorldRemembersCompatApi.isBoss(entityId);
    }

    private static boolean isPlayerOwnedPet(Entity entity) {
        if (entity instanceof TamableAnimal tamableAnimal) {
            return tamableAnimal.getOwnerUUID() != null || tamableAnimal.isTame();
        }
        if (entity instanceof AbstractHorse horse) {
            return horse.getOwnerUUID() != null || horse.isTamed();
        }
        return false;
    }

    private static boolean hasCustomName(Entity entity) {
        return entity != null && entity.hasCustomName();
    }

    private static boolean isUnderwater(Entity entity) {
        return entity != null && (entity.isUnderWater() || entity.isInWaterOrBubble());
    }

    private static void collect(
            Entity anchorEntity,
            EventType eventType,
            BlockPos pos,
            String actorId,
            String subjectId,
            double importance,
            String note,
            Logger logger
    ) {
        Level level = anchorEntity.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos resolvedPos = pos == null ? anchorEntity.blockPosition() : pos;
        WorldMemoryEvent event = new WorldMemoryEvent(
                eventId(eventType, anchorEntity, serverLevel, resolvedPos),
                eventType,
                worldPos(serverLevel, resolvedPos),
                actorId,
                subjectId,
                gameTime(serverLevel),
                System.currentTimeMillis(),
                importance,
                note
        );
        WorldRemembersLivingLegendsNeoForgeStorage.recordEvent(serverLevel, event, logger);
    }

    private static void collect(
            Entity anchorEntity,
            EventType eventType,
            BlockPos pos,
            String actorId,
            String subjectId,
            double importance,
            String note,
            String firstDiscoveryKey,
            String structureId,
            PlaceBounds structureBounds,
            Logger logger
    ) {
        Level level = anchorEntity.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos resolvedPos = pos == null ? anchorEntity.blockPosition() : pos;
        WorldMemoryEvent event = new WorldMemoryEvent(
                eventId(eventType, anchorEntity, serverLevel, resolvedPos),
                eventType,
                worldPos(serverLevel, resolvedPos),
                actorId,
                subjectId,
                gameTime(serverLevel),
                System.currentTimeMillis(),
                importance,
                note,
                firstDiscoveryKey,
                structureId,
                structureBounds
        );
        WorldRemembersLivingLegendsNeoForgeStorage.recordEvent(serverLevel, event, logger);
    }

    private static void collectAtSource(
            ServerLevel sourceWorld,
            ServerLevel fallbackWorld,
            Entity anchorEntity,
            EventType eventType,
            SourceAnchor sourceAnchor,
            String actorId,
            String subjectId,
            double importance,
            String note,
            Logger logger
    ) {
        if (sourceAnchor == null) {
            collect(anchorEntity, eventType, anchorEntity.blockPosition(), actorId, subjectId, importance, note, logger);
            return;
        }
        ServerLevel storageWorld = sourceWorld == null ? fallbackWorld : sourceWorld;
        if (storageWorld == null) {
            return;
        }
        long time = sourceAnchor.gameTime() > 0L ? sourceAnchor.gameTime() : gameTime(storageWorld);
        WorldMemoryEvent event = new WorldMemoryEvent(
                eventId(eventType, anchorEntity, sourceAnchor.dimensionId(), sourceAnchor.x(), sourceAnchor.y(), sourceAnchor.z(), time),
                eventType,
                new WorldPos(sourceAnchor.dimensionId(), sourceAnchor.x(), sourceAnchor.y(), sourceAnchor.z()),
                actorId,
                subjectId,
                time,
                System.currentTimeMillis(),
                importance,
                note
        );
        WorldRemembersLivingLegendsNeoForgeStorage.recordEvent(storageWorld, event, logger);
    }

    private static String eventId(EventType eventType, Entity entity, Level world, BlockPos pos) {
        return eventId(
                eventType,
                entity,
                dimensionId(world),
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                gameTime(world)
        );
    }

    private static String eventId(
            EventType eventType,
            Entity entity,
            String dimensionId,
            int x,
            int y,
            int z,
            long gameTime
    ) {
        return eventType.idString()
                + ":" + entityId(entity)
                + ":" + (dimensionId == null || dimensionId.isBlank() ? "minecraft:overworld" : dimensionId)
                + ":" + x
                + "," + y
                + "," + z
                + ":" + gameTime;
    }

    private static WorldPos worldPos(Level world, BlockPos pos) {
        String dimensionId = dimensionId(world);
        if (dimensionId.isBlank()) {
            dimensionId = "minecraft:overworld";
        }
        return new WorldPos(dimensionId, pos.getX(), pos.getY(), pos.getZ());
    }

    private static String positionNote(String key, BlockPos pos) {
        return key + "=" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static String sourcePositionLog(SourceAnchor sourceAnchor, String fallbackEventDimension, BlockPos fallbackPos) {
        if (sourceAnchor != null) {
            return positionLog(sourceAnchor.dimensionId(), sourceAnchor.x(), sourceAnchor.y(), sourceAnchor.z());
        }
        return positionLog(fallbackEventDimension, fallbackPos);
    }

    private static String positionLog(String dimensionId, BlockPos pos) {
        if (pos == null) {
            return positionLog(dimensionId, 0, 0, 0);
        }
        return positionLog(dimensionId, pos.getX(), pos.getY(), pos.getZ());
    }

    private static String positionLog(String dimensionId, int x, int y, int z) {
        String resolvedDimension = dimensionId == null || dimensionId.isBlank() ? "minecraft:overworld" : dimensionId;
        return resolvedDimension + "@" + x + "," + y + "," + z;
    }

    private static long gameTime(Level world) {
        return world == null ? 0L : world.getGameTime();
    }

    private static String dimensionId(Level world) {
        return world == null ? "" : world.dimension().location().toString();
    }

    private static String playerId(ServerPlayer player) {
        return player == null ? "" : player.getUUID().toString();
    }

    private static String entityId(Entity entity) {
        return entity == null ? "" : entity.getStringUUID();
    }

    private static String entityTypeId(Entity entity) {
        if (entity == null) {
            return "";
        }
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }

    private static String blockId(Block block) {
        return block == null ? "" : BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private static String entityName(Entity entity) {
        if (entity == null) {
            return "";
        }
        String name = entity.getName().getString();
        if (name != null && !name.isBlank()) {
            return RuntimeNameFormatter.encodeNoteValue(name);
        }
        String id = entityId(entity);
        return id.isBlank() ? entityTypeId(entity) : id;
    }

    private static String customEntityName(Entity entity) {
        if (!hasCustomName(entity) || entity.getCustomName() == null) {
            return "";
        }
        return RuntimeNameFormatter.sanitize(entity.getCustomName().getString());
    }

    private static String ownerId(Entity entity) {
        if (entity instanceof TamableAnimal tamableAnimal && tamableAnimal.getOwnerUUID() != null) {
            return tamableAnimal.getOwnerUUID().toString();
        }
        if (entity instanceof AbstractHorse horse && horse.getOwnerUUID() != null) {
            return horse.getOwnerUUID().toString();
        }
        return "";
    }

    private static String ownerName(Entity entity) {
        LivingEntity owner = null;
        if (entity instanceof TamableAnimal tamableAnimal) {
            owner = tamableAnimal.getOwner();
        }
        if (owner == null && entity instanceof AbstractHorse horse) {
            owner = horse.getOwner();
        }
        return owner == null ? "" : entityName(owner);
    }

    private static String deathActorId(DamageSource damageSource, Entity deadEntity) {
        Entity attacker = attackerFromDamageSource(damageSource);
        String attackerId = entityId(attacker);
        return attackerId.isBlank() ? entityId(deadEntity) : attackerId;
    }

    private static String deathSubjectId(DamageSource damageSource, Entity deadEntity) {
        Entity attacker = attackerFromDamageSource(damageSource);
        String attackerId = entityId(attacker);
        if (!attackerId.isBlank() && !attackerId.equals(entityId(deadEntity))) {
            return attackerId;
        }

        String cause = damageSourceName(damageSource);
        return cause.isBlank() ? entityId(deadEntity) : "cause:" + cause;
    }

    private static Entity attackerFromDamageSource(DamageSource damageSource) {
        if (damageSource == null) {
            return null;
        }
        Entity attacker = damageSource.getEntity();
        return attacker == null ? damageSource.getDirectEntity() : attacker;
    }

    private static String damageSourceName(DamageSource damageSource) {
        return damageSource == null ? "unknown" : damageSource.getMsgId();
    }

    private static void debug(Logger logger, String message) {
        if (logger != null && debugEnabled()) {
            logger.info(message);
        }
    }

    private static boolean debugEnabled() {
        return WorldRemembersLivingLegends.config().debug.enabled;
    }

    private static String notePart(String key, String value) {
        String encoded = RuntimeNameFormatter.encodeNoteValue(value);
        return encoded.isBlank() ? "" : " " + key + "=" + encoded;
    }

    private record StructureDetection(boolean found, PlaceBounds bounds) {
        private static StructureDetection missing() {
            return new StructureDetection(false, null);
        }
    }

    private record SourceAnchor(String dimensionId, int x, int y, int z, long gameTime, long capturedAtTick) {
        private SourceAnchor {
            dimensionId = dimensionId == null || dimensionId.isBlank() ? "minecraft:overworld" : dimensionId;
        }
    }
}
