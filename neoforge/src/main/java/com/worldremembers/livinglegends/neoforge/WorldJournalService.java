package com.worldremembers.livinglegends.neoforge;

import com.worldremembers.livinglegends.DeathSiteEnvironment;
import com.worldremembers.livinglegends.EventType;
import com.worldremembers.livinglegends.NameRecipe;
import com.worldremembers.livinglegends.NamedPlace;
import com.worldremembers.livinglegends.PlaceBounds;
import com.worldremembers.livinglegends.PlaceCause;
import com.worldremembers.livinglegends.PlaceCauseType;
import com.worldremembers.livinglegends.PlaceRarity;
import com.worldremembers.livinglegends.PlaceStats;
import com.worldremembers.livinglegends.PlaceType;
import com.worldremembers.livinglegends.WorldPos;
import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import com.worldremembers.livinglegends.config.LivingLegendsConfig;
import com.worldremembers.livinglegends.neoforge.network.WorldJournalC2SPayload;
import com.worldremembers.livinglegends.neoforge.network.WorldJournalS2CPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class WorldJournalService {
    private static final int BOOK_PAGE_SIZE = 4;
    private static Logger logger;

    private WorldJournalService() {
    }

    static void configure(Logger registerLogger) {
        logger = registerLogger;
    }

    public static boolean networkingRegistered() {
        return WorldRemembersLivingLegendsNeoForgeNetworking.networkingRegistered();
    }

    public static void openJournal(ServerPlayer player) {
        if (player == null) {
            return;
        }
        NeoForgeMapIntegrationManager.syncPlayer(player, logger, "journal_open");
        LivingLegendsConfig.Journal config = journalConfig();
        if (!config.enabled) {
            sendError(player, "living_legends.journal.error.disabled", "World Journal is disabled.");
            return;
        }
        int pageSize = journalBookPageSize(config);
        sendPayload(player, WorldJournalS2CPayload.open(
                pageSize,
                canManage(player),
                canTeleport(player),
                config.showExactCoordinates
        ));
        sendPage(player, WorldJournalC2SPayload.page(
                0,
                pageSize,
                "ALL_PLACES",
                "",
                "",
                "",
                "NAME",
                "ASC",
                ""
        ));
    }

    public static void handle(ServerPlayer player, WorldJournalC2SPayload payload) {
        if (player == null || payload == null) {
            return;
        }
        LivingLegendsConfig.Journal config = journalConfig();
        if (!config.enabled) {
            sendError(player, "living_legends.journal.error.disabled", "World Journal is disabled.");
            return;
        }

        switch (payload.action()) {
            case OPEN -> openJournal(player);
            case PAGE, REFRESH -> sendPage(player, payload);
            case RENAME -> rename(player, payload);
            case DELETE -> delete(player, payload);
            case RESTORE_GENERATED -> restoreGenerated(player, payload);
            case TELEPORT -> teleport(player, payload.placeId());
            case CREATE_CUSTOM -> createCustom(player, payload);
        }
    }

    private static void createCustom(ServerPlayer player, WorldJournalC2SPayload payload) {
        if (!canManage(player)) {
            sendError(player, "living_legends.journal.error.no_permission", "No permission.");
            return;
        }
        LivingLegendsConfig.Journal config = journalConfig();
        String name = cleanManualName(payload.text());
        if (name.isBlank()) {
            sendError(player, "living_legends.journal.error.rename_empty", "Name cannot be empty.");
            return;
        }
        if (name.length() > config.maxManualNameLength) {
            sendError(player, "living_legends.journal.error.rename_too_long", "Name is too long.");
            return;
        }
        PlaceType type = PlaceType.CUSTOM;
        if (!WorldRemembersLivingLegends.config().placeTypes.autoGenerationEnabled(type)
                && !WorldRemembersLivingLegends.config().placeTypes.allowManualCreateWhenDisabled) {
            sendError(player, "living_legends.journal.error.no_permission", "Manual custom place creation is disabled.");
            return;
        }

        ServerLevel world = player.serverLevel();
        BlockPos pos = player.blockPosition();
        String dimensionId = world.dimension().location().toString();
        WorldPos center = new WorldPos(dimensionId, pos.getX(), pos.getY(), pos.getZ());
        int radius = 32;
        String baseId = "living_legends:journal/custom_"
                + dimensionId.replace(':', '_').replace('/', '_') + "_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
        String id = WorldRemembersLivingLegendsNeoForgeStorage.uniquePlaceId(world, baseId, logger);
        long gameTime = WorldRemembersLivingLegendsNeoForgeStorage.gameTimeFor(world);
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
                Map.of("journal_create", 1L)
        );
        NamedPlace place = new NamedPlace(
                id,
                type,
                DeathSiteEnvironment.UNKNOWN,
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
        if (WorldRemembersLivingLegendsNeoForgeStorage.upsertPlace(world, place, "journal_create " + id, logger)) {
            WorldRemembersLivingLegendsNeoForgeStorage.recordJournalDiscovery(world, player.getUUID().toString(), id, logger);
            WorldRemembersLivingLegendsNeoForgePlaceTitles.onPlaceCreated(player.getServer(), place, logger);
        }
        sendPage(player, WorldJournalC2SPayload.page(
                0,
                journalBookPageSize(config),
                "ALL_PLACES",
                "",
                "",
                "",
                "NAME",
                "ASC",
                id
        ));
    }

    private static void rename(ServerPlayer player, WorldJournalC2SPayload payload) {
        if (!canManage(player)) {
            sendError(player, "living_legends.journal.error.no_permission", "No permission.");
            return;
        }
        String placeId = payload.placeId();
        String name = cleanManualName(payload.text());
        LivingLegendsConfig.Journal config = journalConfig();
        if (name.isBlank()) {
            sendError(player, "living_legends.journal.error.rename_empty", "Name cannot be empty.");
            return;
        }
        if (name.length() > config.maxManualNameLength) {
            sendError(player, "living_legends.journal.error.rename_too_long", "Name is too long.");
            return;
        }
        ServerLevel world = player.serverLevel();
        NamedPlace place = WorldRemembersLivingLegendsNeoForgeStorage.place(world, placeId, logger);
        if (place == null) {
            sendError(player, "living_legends.journal.error.place_not_found", "Place not found.");
            return;
        }
        NamedPlace updated = place.withManualName(name, WorldRemembersLivingLegendsNeoForgeStorage.gameTimeFor(world));
        if (WorldRemembersLivingLegendsNeoForgeStorage.upsertPlace(world, updated, "journal_rename " + placeId, logger)) {
            NeoForgeMapIntegrationManager.refreshDestinationFromWorld(world, updated, logger);
            sendPage(player, pageRequestLike(payload, updated.placeIdString(), config));
        }
    }

    private static void delete(ServerPlayer player, WorldJournalC2SPayload payload) {
        if (!canManage(player)) {
            sendError(player, "living_legends.journal.error.no_permission", "No permission.");
            return;
        }
        String placeId = payload.placeId();
        boolean deleted = WorldRemembersLivingLegendsNeoForgeStorage.deletePlace(player.serverLevel(), placeId, logger);
        if (!deleted) {
            sendError(player, "living_legends.journal.error.place_not_found", "Place not found.");
            return;
        }
        sendPage(
                player,
                pageRequestLike(payload, "", journalConfig()),
                WorldJournalS2CPayload.Action.DELETED,
                "living_legends.journal.place_deleted",
                "Place deleted"
        );
    }

    private static void restoreGenerated(ServerPlayer player, WorldJournalC2SPayload payload) {
        if (!canManage(player)) {
            sendError(player, "living_legends.journal.error.no_permission", "No permission.");
            return;
        }
        String placeId = payload.placeId();
        ServerLevel world = player.serverLevel();
        NamedPlace place = WorldRemembersLivingLegendsNeoForgeStorage.place(world, placeId, logger);
        if (place == null) {
            sendError(player, "living_legends.journal.error.place_not_found", "Place not found.");
            return;
        }
        if (!hasGeneratedRecipe(place.nameRecipe())) {
            sendError(player, "living_legends.journal.error.no_name_recipe", "This place has no generated name recipe.");
            return;
        }
        NamedPlace updated = place.withGeneratedNameRecipe(
                place.nameRecipe(),
                WorldRemembersLivingLegendsNeoForgeStorage.gameTimeFor(world),
                true
        );
        if (WorldRemembersLivingLegendsNeoForgeStorage.upsertPlace(world, updated, "journal_restore_name " + placeId, logger)) {
            NeoForgeMapIntegrationManager.refreshDestinationFromWorld(world, updated, logger);
            sendPage(player, pageRequestLike(payload, updated.placeIdString(), journalConfig()));
        }
    }

    private static WorldJournalC2SPayload pageRequestLike(
            WorldJournalC2SPayload payload,
            String selectedPlaceId,
            LivingLegendsConfig.Journal config
    ) {
        return WorldJournalC2SPayload.page(
                payload.page(),
                payload.pageSize() <= 0 ? journalBookPageSize(config) : payload.pageSize(),
                payload.tab().isBlank() ? "ALL_PLACES" : payload.tab(),
                payload.searchQuery(),
                payload.placeTypeFilter(),
                payload.dimensionFilter(),
                payload.sortMode().isBlank() ? "NAME" : payload.sortMode(),
                payload.sortDirection().isBlank() ? "ASC" : payload.sortDirection(),
                selectedPlaceId
        );
    }

    private static boolean hasGeneratedRecipe(NameRecipe recipe) {
        if (recipe == null || recipe.patternKey().isBlank()) {
            return false;
        }
        return !"living_legends.name.pattern.unknown".equals(recipe.patternKey());
    }

    private static void teleport(ServerPlayer player, String placeId) {
        if (!canTeleport(player)) {
            sendError(player, "living_legends.journal.error.teleport_not_allowed", "Teleport is not allowed.");
            return;
        }
        NamedPlace place = WorldRemembersLivingLegendsNeoForgeStorage.place(player.serverLevel(), placeId, logger);
        if (place == null) {
            sendError(player, "living_legends.journal.error.place_not_found", "Place not found.");
            return;
        }
        MinecraftServer server = player.getServer();
        ResourceLocation dimensionId = ResourceLocation.tryParse(place.dimensionId());
        if (server == null || dimensionId == null) {
            sendError(player, "living_legends.journal.error.place_not_found", "Invalid place dimension.");
            return;
        }
        ServerLevel targetWorld = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
        if (targetWorld == null) {
            sendError(player, "living_legends.journal.error.place_not_found", "Place dimension is not loaded.");
            return;
        }
        player.teleportTo(
                targetWorld,
                place.center().x() + 0.5,
                place.center().y(),
                place.center().z() + 0.5,
                player.getYRot(),
                player.getXRot()
        );
    }

    private static void sendPage(ServerPlayer player, WorldJournalC2SPayload request) {
        sendPage(player, request, WorldJournalS2CPayload.Action.PAGE, "", "");
    }

    private static void sendPage(
            ServerPlayer player,
            WorldJournalC2SPayload request,
            WorldJournalS2CPayload.Action action,
            String messageKey,
            String messageText
    ) {
        LivingLegendsConfig.Journal config = journalConfig();
        ServerLevel world = player.serverLevel();
        BlockPos playerPos = player.blockPosition();
        String playerDimension = world.dimension().location().toString();
        Set<String> discovered = config.visibilityMode() == LivingLegendsConfig.Journal.VisibilityMode.VISITED_BY_PLAYER
                ? WorldRemembersLivingLegendsNeoForgeStorage.discoveredPlaces(world, player.getUUID().toString(), logger)
                : Set.of();
        String selectedId = request.placeId();

        List<NamedPlace> filtered = new ArrayList<>();
        for (NamedPlace place : WorldRemembersLivingLegendsNeoForgeStorage.places(world, logger)) {
            if (place == null || place.placeType() == PlaceType.UNKNOWN || place.placeType() == PlaceType.CAMP) {
                continue;
            }
            if (!WorldRemembersLivingLegends.config().placeTypes.shouldDisplayExisting(place.placeType())) {
                continue;
            }
            if (config.visibilityMode() == LivingLegendsConfig.Journal.VisibilityMode.VISITED_BY_PLAYER
                    && !discovered.contains(place.placeIdString())) {
                continue;
            }
            if (!matchesType(place, request.placeTypeFilter())) {
                continue;
            }
            if (!matchesDimension(place, request.dimensionFilter())) {
                continue;
            }
            if (!matchesSearch(place, request.searchQuery())) {
                continue;
            }
            filtered.add(place);
        }

        filtered.sort(comparator(request, playerDimension, playerPos));
        int pageSize = Math.max(1, Math.min(BOOK_PAGE_SIZE, request.pageSize() <= 0 ? journalBookPageSize(config) : request.pageSize()));
        int total = filtered.size();
        int totalPages = totalPages(total, pageSize);
        int maxPage = totalPages - 1;
        int page = Math.max(0, Math.min(maxPage, request.page()));
        if (!selectedId.isBlank()) {
            for (int index = 0; index < filtered.size(); index++) {
                if (selectedId.equals(filtered.get(index).placeIdString())) {
                    page = Math.max(0, Math.min(maxPage, index / pageSize));
                    break;
                }
            }
        }
        int from = Math.min(total, page * pageSize);
        int to = Math.min(total, from + pageSize);
        List<WorldJournalS2CPayload.Entry> entries = new ArrayList<>();
        for (NamedPlace place : filtered.subList(from, to)) {
            entries.add(toEntry(place, playerDimension, playerPos));
        }
        if (selectedId.isBlank() && !entries.isEmpty()) {
            selectedId = entries.get(0).placeId();
        }

        sendPayload(player, new WorldJournalS2CPayload(
                action,
                total,
                page,
                pageSize,
                entries,
                selectedId,
                canManage(player),
                canTeleport(player),
                config.showExactCoordinates,
                messageKey,
                messageText
        ));
    }

    private static int totalPages(int totalCount, int pageSize) {
        int safePageSize = Math.max(1, pageSize);
        return Math.max(1, (Math.max(0, totalCount) + safePageSize - 1) / safePageSize);
    }

    private static WorldJournalS2CPayload.Entry toEntry(NamedPlace place, String playerDimension, BlockPos playerPos) {
        boolean sameDimension = place.dimensionId().equals(playerDimension);
        double distance = sameDimension
                ? Math.sqrt(place.center().squaredDistanceTo(new WorldPos(playerDimension, playerPos.getX(), playerPos.getY(), playerPos.getZ())))
                : -1.0;
        String fallback = displayName(place);
        return new WorldJournalS2CPayload.Entry(
                place.placeIdString(),
                place.placeType().idString(),
                place.dimensionId(),
                place.center().x(),
                place.center().y(),
                place.center().z(),
                place.radius(),
                place.createdAtGameTime(),
                place.lastUpdatedGameTime(),
                distance,
                place.nameRecipe().styleId(),
                place.manuallyRenamed(),
                place.manualName(),
                place.nameRecipe(),
                fallback
        );
    }

    private static Comparator<NamedPlace> comparator(WorldJournalC2SPayload request, String playerDimension, BlockPos playerPos) {
        SortMode sort = SortMode.fromId(request.sortMode(), request.tab());
        Comparator<NamedPlace> comparator = switch (sort) {
            case DISTANCE -> Comparator.comparingDouble(place -> distanceForSort(place, playerDimension, playerPos));
            case TYPE -> Comparator
                    .comparing((NamedPlace place) -> place.placeType().name())
                    .thenComparing(WorldJournalService::displayName, String.CASE_INSENSITIVE_ORDER);
            case CREATED_TIME -> Comparator.comparingLong(NamedPlace::createdAtGameTime);
            case UPDATED_TIME -> Comparator.comparingLong(NamedPlace::lastUpdatedGameTime);
            case NAME -> Comparator.comparing(WorldJournalService::displayName, String.CASE_INSENSITIVE_ORDER);
        };
        if (SortDirection.fromId(request.sortDirection()) == SortDirection.DESC) {
            comparator = comparator.reversed();
        }
        return comparator.thenComparing(NamedPlace::placeIdString);
    }

    private static double distanceForSort(NamedPlace place, String playerDimension, BlockPos playerPos) {
        if (!place.dimensionId().equals(playerDimension)) {
            return Double.MAX_VALUE;
        }
        return Math.sqrt(place.center().squaredDistanceTo(new WorldPos(playerDimension, playerPos.getX(), playerPos.getY(), playerPos.getZ())));
    }

    private static boolean matchesType(NamedPlace place, String filter) {
        return filter == null || filter.isBlank() || "ALL".equalsIgnoreCase(filter) || place.placeType().idString().equalsIgnoreCase(filter);
    }

    private static boolean matchesDimension(NamedPlace place, String filter) {
        return filter == null || filter.isBlank() || "ALL".equalsIgnoreCase(filter) || place.dimensionId().equalsIgnoreCase(filter);
    }

    private static boolean matchesSearch(NamedPlace place, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        return displayName(place).toLowerCase(Locale.ROOT).contains(normalized)
                || place.placeIdString().toLowerCase(Locale.ROOT).contains(normalized);
    }

    private static String displayName(NamedPlace place) {
        if (place == null) {
            return "";
        }
        if (place.manuallyRenamed()) {
            return place.manualName();
        }
        String resolved = WorldRemembersLivingLegendsNeoForgeNameResolver.resolveToString(place.nameRecipe());
        if (!resolved.isBlank()) {
            return resolved;
        }
        return place.displayName();
    }

    private static boolean canManage(ServerPlayer player) {
        LivingLegendsConfig.Journal config = journalConfig();
        MinecraftServer server = player.getServer();
        return switch (config.managementMode()) {
            case ALL_PLAYERS -> true;
            case OP_ONLY -> player.hasPermissions(2);
            case DISABLED -> false;
            case SINGLEPLAYER_OWNER_AND_OP -> player.hasPermissions(2)
                    || (server != null && server.isSingleplayer() && server.isSingleplayerOwner(player.getGameProfile()));
        };
    }

    private static boolean canTeleport(ServerPlayer player) {
        return journalConfig().allowTeleportForOps && player.hasPermissions(2);
    }

    private static LivingLegendsConfig.Journal journalConfig() {
        LivingLegendsConfig config = WorldRemembersLivingLegends.config();
        if (config.journal == null) {
            config.journal = new LivingLegendsConfig.Journal();
        }
        return config.journal;
    }

    private static int journalBookPageSize(LivingLegendsConfig.Journal config) {
        return Math.max(1, Math.min(BOOK_PAGE_SIZE, config == null ? BOOK_PAGE_SIZE : config.pageSize));
    }

    private static String cleanManualName(String value) {
        return value == null ? "" : value.trim().replace('\n', ' ').replace('\r', ' ');
    }

    private static void sendError(ServerPlayer player, String key, String fallback) {
        if (sendPayload(player, WorldJournalS2CPayload.error(key, fallback))) {
            return;
        }
        player.sendSystemMessage(Component.translatable(key).append(Component.literal(" " + fallback)));
    }

    private static boolean sendPayload(ServerPlayer player, WorldJournalS2CPayload payload) {
        try {
            PacketDistributor.sendToPlayer(player, payload);
            return true;
        } catch (RuntimeException exception) {
            if (logger != null && debugEnabled()) {
                logger.warn("Could not send journal payload: " + exception.getMessage());
            }
            return false;
        }
    }

    private static boolean debugEnabled() {
        LivingLegendsConfig config = WorldRemembersLivingLegends.config();
        return config != null && config.debug != null && config.debug.enabled;
    }

    private enum SortMode {
        DISTANCE,
        TYPE,
        NAME,
        CREATED_TIME,
        UPDATED_TIME;

        private static SortMode fromId(String id, String tab) {
            if ((id == null || id.isBlank()) && "NEARBY".equalsIgnoreCase(tab)) {
                return DISTANCE;
            }
            if (id != null) {
                for (SortMode mode : values()) {
                    if (mode.name().equalsIgnoreCase(id.trim())) {
                        return mode;
                    }
                }
            }
            return NAME;
        }
    }

    private enum SortDirection {
        ASC,
        DESC;

        private static SortDirection fromId(String id) {
            if (id != null) {
                for (SortDirection direction : values()) {
                    if (direction.name().equalsIgnoreCase(id.trim())) {
                        return direction;
                    }
                }
            }
            return ASC;
        }
    }
}
