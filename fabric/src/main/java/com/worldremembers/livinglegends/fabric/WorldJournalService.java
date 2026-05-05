package com.worldremembers.livinglegends.fabric;

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
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class WorldJournalService {
    private static final int BOOK_PAGE_SIZE = 4;
    private static Logger logger;
    private static volatile boolean networkingRegistered;

    private WorldJournalService() {
    }

    static void registerNetworking(Logger registerLogger) {
        logger = registerLogger;
        WorldJournalC2SPayload.registerType();
        WorldJournalS2CPayload.registerType();
        ServerPlayNetworking.registerGlobalReceiver(WorldJournalC2SPayload.ID, (payload, context) ->
                handle(context.player(), payload)
        );
        networkingRegistered = true;
    }

    static boolean networkingRegistered() {
        return networkingRegistered;
    }

    static void openJournal(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
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
        FabricMapIntegrationManager.syncPlayer(player, logger, "journal_open");
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

    private static void handle(ServerPlayerEntity player, WorldJournalC2SPayload payload) {
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

    private static void createCustom(ServerPlayerEntity player, WorldJournalC2SPayload payload) {
        LivingLegendsConfig.Journal config = journalConfig();
        String name = payload.text() == null ? "" : payload.text().trim().replace('\n', ' ').replace('\r', ' ');
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

        ServerWorld world = player.getServerWorld();
        BlockPos pos = player.getBlockPos();
        String dimensionId = world.getRegistryKey().getValue().toString();
        WorldPos center = new WorldPos(dimensionId, pos.getX(), pos.getY(), pos.getZ());
        int radius = 32;
        String baseId = "living_legends:journal/custom_"
                + dimensionId.replace(':', '_').replace('/', '_') + "_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
        String id = WorldRemembersLivingLegendsFabricStorage.uniquePlaceId(world, baseId, logger);
        long gameTime = WorldRemembersLivingLegendsFabricStorage.gameTimeFor(world);
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
        if (WorldRemembersLivingLegendsFabricStorage.upsertPlace(world, place, "journal_create " + id, logger)) {
            WorldRemembersLivingLegendsFabricStorage.recordJournalDiscovery(world, player.getUuidAsString(), id, logger);
            WorldRemembersLivingLegendsFabricPlaceTitles.onPlaceCreated(player.getServer(), place, logger);
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

    private static void rename(ServerPlayerEntity player, WorldJournalC2SPayload payload) {
        if (!canManage(player)) {
            sendError(player, "living_legends.journal.error.no_permission", "No permission.");
            return;
        }
        String placeId = payload.placeId();
        String newName = payload.text();
        String name = newName == null ? "" : newName.trim().replace('\n', ' ').replace('\r', ' ');
        LivingLegendsConfig.Journal config = journalConfig();
        if (name.isBlank()) {
            sendError(player, "living_legends.journal.error.rename_empty", "Name cannot be empty.");
            return;
        }
        if (name.length() > config.maxManualNameLength) {
            sendError(player, "living_legends.journal.error.rename_too_long", "Name is too long.");
            return;
        }
        ServerWorld world = player.getServerWorld();
        NamedPlace place = WorldRemembersLivingLegendsFabricStorage.place(world, placeId, logger);
        if (place == null) {
            sendError(player, "living_legends.journal.error.place_not_found", "Place not found.");
            return;
        }
        NamedPlace updated = place.withManualName(name, WorldRemembersLivingLegendsFabricStorage.gameTimeFor(world));
        if (WorldRemembersLivingLegendsFabricStorage.upsertPlace(world, updated, "journal_rename " + placeId, logger)) {
            FabricMapIntegrationManager.refreshDestinationFromWorld(world, updated, logger);
            sendPage(player, pageRequestLike(payload, updated.placeIdString(), config));
        }
    }

    private static void delete(ServerPlayerEntity player, WorldJournalC2SPayload payload) {
        if (!canManage(player)) {
            sendError(player, "living_legends.journal.error.no_permission", "No permission.");
            return;
        }
        String placeId = payload.placeId();
        boolean deleted = WorldRemembersLivingLegendsFabricStorage.deletePlace(player.getServerWorld(), placeId, logger);
        if (!deleted) {
            sendError(player, "living_legends.journal.error.place_not_found", "Place not found.");
            return;
        }
        sendPage(player, pageRequestLike(payload, "", journalConfig()));
    }

    private static void restoreGenerated(ServerPlayerEntity player, WorldJournalC2SPayload payload) {
        if (!canManage(player)) {
            sendError(player, "living_legends.journal.error.no_permission", "No permission.");
            return;
        }
        String placeId = payload.placeId();
        ServerWorld world = player.getServerWorld();
        NamedPlace place = WorldRemembersLivingLegendsFabricStorage.place(world, placeId, logger);
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
                WorldRemembersLivingLegendsFabricStorage.gameTimeFor(world),
                true
        );
        if (WorldRemembersLivingLegendsFabricStorage.upsertPlace(world, updated, "journal_restore_name " + placeId, logger)) {
            FabricMapIntegrationManager.refreshDestinationFromWorld(world, updated, logger);
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

    private static void teleport(ServerPlayerEntity player, String placeId) {
        if (!canTeleport(player)) {
            sendError(player, "living_legends.journal.error.teleport_not_allowed", "Teleport is not allowed.");
            return;
        }
        NamedPlace place = WorldRemembersLivingLegendsFabricStorage.place(player.getServerWorld(), placeId, logger);
        if (place == null) {
            sendError(player, "living_legends.journal.error.place_not_found", "Place not found.");
            return;
        }
        MinecraftServer server = player.getServer();
        Identifier dimensionId;
        try {
            dimensionId = Identifier.of(place.dimensionId());
        } catch (RuntimeException exception) {
            sendError(player, "living_legends.journal.error.place_not_found", "Invalid place dimension.");
            return;
        }
        ServerWorld targetWorld = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, dimensionId));
        if (targetWorld == null) {
            sendError(player, "living_legends.journal.error.place_not_found", "Place dimension is not loaded.");
            return;
        }
        player.teleport(
                targetWorld,
                place.center().x() + 0.5,
                place.center().y(),
                place.center().z() + 0.5,
                player.getYaw(),
                player.getPitch()
        );
    }

    private static void sendPage(ServerPlayerEntity player, WorldJournalC2SPayload request) {
        LivingLegendsConfig.Journal config = journalConfig();
        ServerWorld world = player.getServerWorld();
        BlockPos playerPos = player.getBlockPos();
        String playerDimension = world.getRegistryKey().getValue().toString();
        Set<String> discovered = config.visibilityMode() == LivingLegendsConfig.Journal.VisibilityMode.VISITED_BY_PLAYER
                ? WorldRemembersLivingLegendsFabricStorage.discoveredPlaces(world, player.getUuidAsString(), logger)
                : Set.of();
        String selectedId = request.placeId();

        List<NamedPlace> filtered = new ArrayList<>();
        for (NamedPlace place : WorldRemembersLivingLegendsFabricStorage.places(world, logger)) {
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
                WorldJournalS2CPayload.Action.PAGE,
                total,
                page,
                pageSize,
                entries,
                selectedId,
                canManage(player),
                canTeleport(player),
                config.showExactCoordinates,
                "",
                ""
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
        String fallback = place.manuallyRenamed()
                ? place.manualName()
                : WorldRemembersLivingLegendsFabricNameResolver.resolveToString(place.nameRecipe());
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
        String resolved = WorldRemembersLivingLegendsFabricNameResolver.resolveToString(place.nameRecipe());
        return resolved.isBlank() ? place.displayName() : resolved;
    }

    private static boolean canManage(ServerPlayerEntity player) {
        LivingLegendsConfig.Journal config = journalConfig();
        return switch (config.managementMode()) {
            case ALL_PLAYERS -> true;
            case OP_ONLY -> player.hasPermissionLevel(2);
            case DISABLED -> false;
            case SINGLEPLAYER_OWNER_AND_OP -> player.hasPermissionLevel(2)
                    || (player.getServer().isSingleplayer() && player.getServer().isHost(player.getGameProfile()));
        };
    }

    private static boolean canTeleport(ServerPlayerEntity player) {
        return journalConfig().allowTeleportForOps && player.hasPermissionLevel(2);
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

    private static void sendError(ServerPlayerEntity player, String key, String fallback) {
        if (sendPayload(player, WorldJournalS2CPayload.error(key, fallback))) {
            return;
        }
        player.sendMessage(Text.translatable(key).append(Text.literal(" " + fallback)), false);
    }

    private static boolean sendPayload(ServerPlayerEntity player, WorldJournalS2CPayload payload) {
        try {
            if (!ServerPlayNetworking.canSend(player, WorldJournalS2CPayload.ID)) {
                return false;
            }
            ServerPlayNetworking.send(player, payload);
            return true;
        } catch (RuntimeException exception) {
            if (logger != null && WorldRemembersLivingLegends.config().debug.enabled) {
                logger.warn("Could not send journal payload: " + exception.getMessage());
            }
            return false;
        }
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
