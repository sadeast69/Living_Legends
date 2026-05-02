package com.worldremembers.livinglegends.neoforge;

import com.worldremembers.livinglegends.ChunkMemoryStats;
import com.worldremembers.livinglegends.CandidateDecayState;
import com.worldremembers.livinglegends.DeathSiteEnvironment;
import com.worldremembers.livinglegends.DeletedPlaceMarker;
import com.worldremembers.livinglegends.DiscoveryTriggerType;
import com.worldremembers.livinglegends.EventType;
import com.worldremembers.livinglegends.NameStyle;
import com.worldremembers.livinglegends.NameRecipe;
import com.worldremembers.livinglegends.NameTokenForm;
import com.worldremembers.livinglegends.NamedPlace;
import com.worldremembers.livinglegends.PlaceCause;
import com.worldremembers.livinglegends.PlaceCauseType;
import com.worldremembers.livinglegends.PlaceBounds;
import com.worldremembers.livinglegends.PlaceRarity;
import com.worldremembers.livinglegends.PlaceStats;
import com.worldremembers.livinglegends.PlaceType;
import com.worldremembers.livinglegends.WorldMemoryEvent;
import com.worldremembers.livinglegends.WorldMemoryStorageData;
import com.worldremembers.livinglegends.WorldFirstDiscoveryRecord;
import com.worldremembers.livinglegends.WorldPos;
import com.worldremembers.livinglegends.WorldRemembersDataVersions;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class WorldRemembersLivingLegendsNeoForgeNbt {
    private static final byte TAG_COMPOUND = 10;
    private static volatile MigrationSummary lastMigrationSummary = MigrationSummary.fresh();

    private WorldRemembersLivingLegendsNeoForgeNbt() {
    }

    static WorldMemoryStorageData read(CompoundTag nbt) {
        if (nbt == null) {
            lastMigrationSummary = MigrationSummary.fresh();
            return new WorldMemoryStorageData();
        }

        MigrationStats migration = new MigrationStats(
                getInt(nbt, "worldStateVersion", getInt(nbt, "schemaVersion", 0))
        );
        WorldMemoryStorageData data = new WorldMemoryStorageData(
                migration.fromVersion(),
                readChunkStats(nbt, migration),
                readNamedPlaces(nbt, migration),
                readFirstDiscoveries(nbt, migration),
                readFirstEvents(nbt, "firstWorldEvents", migration),
                readFirstEvents(nbt, "firstPlayerWorldEvents", migration),
                readEventCounters(nbt),
                readDeletedPlaceMarkers(nbt, migration),
                readDiscoveredPlaces(nbt, migration)
        );
        lastMigrationSummary = migration.finish(data);
        return data;
    }

    static CompoundTag write(WorldMemoryStorageData data, CompoundTag nbt) {
        CompoundTag target = nbt == null ? newCompound() : nbt;
        putInt(target, "schemaVersion", WorldRemembersDataVersions.CURRENT_WORLD_STATE_VERSION);
        putInt(target, "worldStateVersion", WorldRemembersDataVersions.CURRENT_WORLD_STATE_VERSION);
        putInt(target, "namedPlaceVersion", WorldRemembersDataVersions.CURRENT_NAMED_PLACE_VERSION);
        putInt(target, "nameRecipeVersion", WorldRemembersDataVersions.CURRENT_NAME_RECIPE_VERSION);
        putInt(target, "chunkMemoryVersion", WorldRemembersDataVersions.CURRENT_CHUNK_MEMORY_VERSION);
        putInt(target, "journalVersion", WorldRemembersDataVersions.CURRENT_JOURNAL_VERSION);
        putInt(target, "candidateDecayVersion", WorldRemembersDataVersions.CURRENT_CANDIDATE_DECAY_VERSION);
        putList(target, "chunkStats", writeChunkStats(data.chunkStatsByKey()));
        putList(target, "namedPlaces", writeNamedPlaces(data.namedPlaces()));
        putList(target, "firstDiscoveries", writeFirstDiscoveries(data.firstDiscoveries()));
        putList(target, "firstWorldEvents", writeFirstEvents(data.firstWorldEvents()));
        putList(target, "firstPlayerWorldEvents", writeFirstEvents(data.firstPlayerWorldEvents()));
        putList(target, "eventCounters", writeEventCounters(data.eventCounters()));
        putList(target, "deletedPlaceMarkers", writeDeletedPlaceMarkers(data.deletedPlaceMarkers()));
        putList(target, "playerDiscoveredPlaces", writeDiscoveredPlaces(data.discoveredPlaceIdsByPlayer()));
        return target;
    }

    static String lastMigrationSummary() {
        return lastMigrationSummary.format();
    }

    private static Map<String, ChunkMemoryStats> readChunkStats(CompoundTag nbt, MigrationStats migration) {
        Map<String, ChunkMemoryStats> result = new LinkedHashMap<>();
        for (CompoundTag entry : compounds(nbt, "chunkStats")) {
            try {
                ChunkMemoryStats stats = new ChunkMemoryStats(
                        getString(entry, "dimensionId", "minecraft:overworld"),
                        getInt(entry, "chunkX", 0),
                        getInt(entry, "chunkZ", 0),
                        getLong(entry, "eventCount", 0L),
                        getLong(entry, "visitCount", 0L),
                        getLong(entry, "deathCount", 0L),
                        getLong(entry, "combatEventCount", 0L),
                        getLong(entry, "buildEventCount", 0L),
                        getDouble(entry, "totalImportance", 0.0),
                        getLong(entry, "lastEventGameTime", 0L),
                        getInt(entry, "minY", 0),
                        getInt(entry, "maxY", 0),
                        readCounterMap(entry, "eventTypeCounts"),
                        readCounterMap(entry, "deathSiteEnvironmentCounts"),
                        readCounterMap(entry, "metadataCounts"),
                        readCandidateDecayStates(entry, migration)
                );
                result.put(WorldMemoryStorageData.chunkStatsKey(stats.dimensionId(), stats.chunkX(), stats.chunkZ()), stats);
                migration.chunkStatsMigrated++;
            } catch (RuntimeException exception) {
                migration.skippedBrokenChunkStats++;
            }
        }
        return result;
    }

    private static ListTag writeChunkStats(Map<String, ChunkMemoryStats> statsByKey) {
        ListTag list = newList();
        for (ChunkMemoryStats stats : statsByKey.values()) {
            CompoundTag entry = newCompound();
            putInt(entry, "chunkMemoryVersion", WorldRemembersDataVersions.CURRENT_CHUNK_MEMORY_VERSION);
            putString(entry, "dimensionId", stats.dimensionId());
            putInt(entry, "chunkX", stats.chunkX());
            putInt(entry, "chunkZ", stats.chunkZ());
            putLong(entry, "eventCount", stats.eventCount());
            putLong(entry, "visitCount", stats.visitCount());
            putLong(entry, "deathCount", stats.deathCount());
            putLong(entry, "combatEventCount", stats.combatEventCount());
            putLong(entry, "buildEventCount", stats.buildEventCount());
            putDouble(entry, "totalImportance", stats.totalImportance());
            putLong(entry, "lastEventGameTime", stats.lastEventGameTime());
            putInt(entry, "minY", stats.minY());
            putInt(entry, "maxY", stats.maxY());
            putList(entry, "eventTypeCounts", writeCounterMap(stats.eventTypeCounts()));
            putList(entry, "deathSiteEnvironmentCounts", writeCounterMap(stats.deathSiteEnvironmentCounts()));
            putList(entry, "metadataCounts", writeCounterMap(stats.metadataCounts()));
            putList(entry, "candidateDecayStates", writeCandidateDecayStates(stats.candidateDecayStates()));
            addToList(list, entry);
        }
        return list;
    }

    private static Map<String, WorldFirstDiscoveryRecord> readFirstDiscoveries(CompoundTag nbt, MigrationStats migration) {
        Map<String, WorldFirstDiscoveryRecord> result = new LinkedHashMap<>();
        for (CompoundTag entry : compounds(nbt, "firstDiscoveries")) {
            String discoveryId = getString(entry, "discoveryId", "");
            String targetId = getString(entry, "targetId", "");
            if (discoveryId.isBlank() || targetId.isBlank()) {
                migration.skippedBrokenFirstEvents++;
                continue;
            }

            try {
                WorldFirstDiscoveryRecord record = new WorldFirstDiscoveryRecord(
                        discoveryId,
                        DiscoveryTriggerType.fromId(getString(entry, "triggerType", DiscoveryTriggerType.CUSTOM.idString())),
                        targetId,
                        PlaceType.fromId(getString(entry, "placeType", PlaceType.FIRST_DISCOVERY.idString())),
                        getDouble(entry, "weight", 0.0),
                        readStringMap(entry, "nameTokens"),
                        getString(entry, "sourceEventId", ""),
                        EventType.fromId(getString(entry, "sourceEventType", EventType.CUSTOM.idString())),
                        getString(entry, "actorId", ""),
                        getString(entry, "playerName", ""),
                        readPos(getCompound(entry, "position")),
                        getLong(entry, "gameTime", 0L),
                        getLong(entry, "createdAtEpochMillis", 0L),
                        getString(entry, "structureId", ""),
                        getInt(entry, "hasStructureBounds", 0) > 0 ? readBounds(getCompound(entry, "structureBounds")) : null,
                        getInt(entry, "useStructureBounds", 0) > 0,
                        getInt(entry, "fallbackRadius", 0)
                );
                result.put(record.discoveryIdString(), record);
                migration.firstEventsMigrated++;
            } catch (RuntimeException exception) {
                migration.skippedBrokenFirstEvents++;
            }
        }
        return result;
    }

    private static ListTag writeFirstDiscoveries(Map<String, WorldFirstDiscoveryRecord> discoveries) {
        ListTag list = newList();
        for (Map.Entry<String, WorldFirstDiscoveryRecord> storedDiscovery : discoveries.entrySet()) {
            WorldFirstDiscoveryRecord discovery = storedDiscovery.getValue();
            CompoundTag entry = newCompound();
            putInt(entry, "firstEventVersion", WorldRemembersDataVersions.CURRENT_FIRST_EVENT_VERSION);
            putString(entry, "key", storedDiscovery.getKey());
            putString(entry, "discoveryId", discovery.discoveryIdString());
            putString(entry, "triggerType", discovery.triggerTypeIdString());
            putString(entry, "targetId", discovery.targetIdString());
            putString(entry, "placeType", discovery.placeType().idString());
            putDouble(entry, "weight", discovery.weight());
            putList(entry, "nameTokens", writeStringMap(discovery.nameTokens()));
            putString(entry, "sourceEventId", discovery.sourceEventId());
            putString(entry, "sourceEventType", discovery.sourceEventType().idString());
            putString(entry, "actorId", discovery.actorId());
            putString(entry, "playerName", discovery.playerName());
            putCompound(entry, "position", writePos(discovery.position()));
            putLong(entry, "gameTime", discovery.gameTime());
            putLong(entry, "createdAtEpochMillis", discovery.createdAtEpochMillis());
            putString(entry, "structureId", discovery.structureIdString());
            putInt(entry, "useStructureBounds", discovery.useStructureBounds() ? 1 : 0);
            putInt(entry, "fallbackRadius", discovery.fallbackRadius());
            putInt(entry, "hasStructureBounds", discovery.structureBounds() == null ? 0 : 1);
            if (discovery.structureBounds() != null) {
                putCompound(entry, "structureBounds", writeBounds(discovery.structureBounds()));
            }
            addToList(list, entry);
        }
        return list;
    }

    private static List<NamedPlace> readNamedPlaces(CompoundTag nbt, MigrationStats migration) {
        List<NamedPlace> result = new ArrayList<>();
        int index = 0;
        for (CompoundTag entry : compounds(nbt, "namedPlaces")) {
            try {
                boolean hasCenter = hasKey(entry, "center");
                boolean hasBounds = hasKey(entry, "bounds");
                if (!hasCenter && !hasBounds) {
                    migration.skippedBrokenPlaces++;
                    index++;
                    continue;
                }
                CompoundTag bounds = getCompound(entry, "bounds");
                CompoundTag stats = getCompound(entry, "stats");
                PlaceBounds readBounds = readBounds(bounds);
                WorldPos center = readCenter(entry, readBounds);
                PlaceStats readStats = readPlaceStats(stats);
                PlaceType type = PlaceType.fromId(getString(entry, "placeType", getString(entry, "type", PlaceType.CUSTOM.idString())));
                DeathSiteEnvironment environment = DeathSiteEnvironment.fromId(getString(entry, "environment", DeathSiteEnvironment.UNKNOWN.idString()));
                String dimension = getString(entry, "dimensionId", getString(entry, "dimension", readBounds.dimensionId()));
                int radius = getInt(entry, "radius", radiusFromBounds(readBounds));
                if (radius <= 0) {
                    radius = Math.max(24, radiusFromBounds(readBounds));
                    migration.defaultedRadii++;
                }
                String placeId = getString(entry, "placeId", getString(entry, "id", ""));
                if (placeId.isBlank()) {
                    placeId = legacyPlaceId(type, dimension, center, index);
                    migration.defaultedIds++;
                }
                List<String> sourceChunks = readStringList(entry, "sourceChunks");
                if (sourceChunks.isEmpty()) {
                    sourceChunks = List.of(center.chunkIdString());
                    migration.defaultedSourceChunks++;
                }
                result.add(new NamedPlace(
                        placeId,
                        type,
                        environment,
                        dimension,
                        center,
                        radius,
                        getDouble(entry, "score", 0.0),
                        getLong(entry, "createdAtGameTime", 0L),
                        getLong(entry, "lastUpdatedGameTime", getLong(entry, "createdAtGameTime", 0L)),
                        sourceChunks,
                        readNameRecipe(entry, migration),
                        PlaceRarity.fromId(getString(entry, "rarity", PlaceRarity.COMMON.idString())),
                        readBounds,
                        readStats,
                        getString(entry, "structureId", ""),
                        getString(entry, "firstDiscoveryKey", ""),
                        readPlaceCause(entry),
                        getString(entry, "biomeId", ""),
                        getString(entry, "dominantBiomeId", ""),
                        getString(entry, "biomeGroup", ""),
                        getString(entry, "biomeTheme", ""),
                        getString(entry, "biomeSource", ""),
                        getString(entry, "manualName", ""),
                        getInt(entry, "manuallyRenamed", 0) > 0
                ));
                migration.namedPlacesMigrated++;
            } catch (RuntimeException exception) {
                migration.skippedBrokenPlaces++;
            }
            index++;
        }
        return result;
    }

    private static ListTag writeNamedPlaces(List<NamedPlace> namedPlaces) {
        ListTag list = newList();
        for (NamedPlace place : namedPlaces) {
            CompoundTag entry = newCompound();
            putInt(entry, "namedPlaceVersion", WorldRemembersDataVersions.CURRENT_NAMED_PLACE_VERSION);
            putString(entry, "placeId", place.placeId());
            putString(entry, "placeType", place.placeType().idString());
            putString(entry, "environment", place.environment().idString());
            putString(entry, "dimensionId", place.dimensionId());
            putCompound(entry, "center", writePos(place.center()));
            putInt(entry, "radius", place.radius());
            putDouble(entry, "score", place.score());
            putLong(entry, "createdAtGameTime", place.createdAtGameTime());
            putLong(entry, "lastUpdatedGameTime", place.lastUpdatedGameTime());
            putList(entry, "sourceChunks", writeStringList(place.sourceChunks()));
            putCompound(entry, "nameRecipe", writeNameRecipe(place.nameRecipe()));
            putString(entry, "rarity", place.rarity().idString());
            putCompound(entry, "bounds", writeBounds(place.bounds()));
            putCompound(entry, "stats", writePlaceStats(place.stats()));
            putString(entry, "structureId", place.structureId());
            putString(entry, "firstDiscoveryKey", place.firstDiscoveryKey());
            putCompound(entry, "cause", writePlaceCause(place.cause()));
            putString(entry, "biomeId", place.biomeId());
            putString(entry, "dominantBiomeId", place.dominantBiomeId());
            putString(entry, "biomeGroup", place.biomeGroup());
            putString(entry, "biomeTheme", place.biomeTheme());
            putString(entry, "biomeSource", place.biomeSource());
            putString(entry, "manualName", place.manualName());
            putInt(entry, "manuallyRenamed", place.manuallyRenamed() ? 1 : 0);
            addToList(list, entry);
        }
        return list;
    }

    private static List<DeletedPlaceMarker> readDeletedPlaceMarkers(CompoundTag nbt, MigrationStats migration) {
        List<DeletedPlaceMarker> result = new ArrayList<>();
        for (CompoundTag entry : compounds(nbt, "deletedPlaceMarkers")) {
            try {
                if (!hasKey(entry, "center") && !hasKey(entry, "bounds")) {
                    migration.skippedBrokenDeletedMarkers++;
                    continue;
                }
                PlaceBounds bounds = readBounds(getCompound(entry, "bounds"));
                WorldPos center = readCenter(entry, bounds);
                String originalPlaceId = getString(entry, "originalPlaceId", getString(entry, "placeId", ""));
                if (originalPlaceId.isBlank()) {
                    migration.skippedBrokenDeletedMarkers++;
                    continue;
                }
                int radius = getInt(entry, "radius", radiusFromBounds(bounds));
                if (radius <= 0) {
                    radius = Math.max(24, radiusFromBounds(bounds));
                }
                result.add(new DeletedPlaceMarker(
                        originalPlaceId,
                        PlaceType.fromId(getString(entry, "placeType", PlaceType.CUSTOM.idString())),
                        getString(entry, "dimensionId", bounds.dimensionId()),
                        center,
                        radius,
                        bounds,
                        DeathSiteEnvironment.fromId(getString(entry, "environment", DeathSiteEnvironment.UNKNOWN.idString())),
                        getString(entry, "firstDiscoveryKey", ""),
                        getString(entry, "structureId", ""),
                        PlaceCauseType.fromId(getString(entry, "causeType", PlaceCauseType.UNKNOWN.idString())),
                        getLong(entry, "deletedGameTime", 0L),
                        getInt(entry, "suppressAutoRecreate", 1) > 0
                ));
                migration.deletedMarkersMigrated++;
            } catch (RuntimeException exception) {
                migration.skippedBrokenDeletedMarkers++;
            }
        }
        return result;
    }

    private static ListTag writeDeletedPlaceMarkers(List<DeletedPlaceMarker> markers) {
        ListTag list = newList();
        for (DeletedPlaceMarker marker : markers) {
            CompoundTag entry = newCompound();
            putInt(entry, "deletedMarkerVersion", WorldRemembersDataVersions.CURRENT_DELETED_MARKER_VERSION);
            putString(entry, "originalPlaceId", marker.originalPlaceId());
            putString(entry, "placeType", marker.placeType().idString());
            putString(entry, "dimensionId", marker.dimensionId());
            putCompound(entry, "center", writePos(marker.center()));
            putInt(entry, "radius", marker.radius());
            putCompound(entry, "bounds", writeBounds(marker.bounds()));
            putString(entry, "environment", marker.environment().idString());
            putString(entry, "firstDiscoveryKey", marker.firstDiscoveryKey());
            putString(entry, "structureId", marker.structureId());
            putString(entry, "causeType", marker.causeType().idString());
            putLong(entry, "deletedGameTime", marker.deletedGameTime());
            putInt(entry, "suppressAutoRecreate", marker.suppressAutoRecreate() ? 1 : 0);
            addToList(list, entry);
        }
        return list;
    }

    private static String legacyPlaceId(PlaceType type, String dimensionId, WorldPos center, int index) {
        String dimension = dimensionId == null ? "minecraft:overworld" : dimensionId.replace(':', '_').replace('/', '_');
        PlaceType resolvedType = type == null ? PlaceType.CUSTOM : type;
        WorldPos resolvedCenter = center == null ? new WorldPos("minecraft:overworld", 0, 64, 0) : center;
        return "legacy_" + resolvedType.idString()
                + "_" + dimension
                + "_" + resolvedCenter.x()
                + "_" + resolvedCenter.y()
                + "_" + resolvedCenter.z()
                + "_" + Math.max(0, index);
    }

    private static NameRecipe readNameRecipe(CompoundTag placeEntry, MigrationStats migration) {
        CompoundTag recipe = getCompound(placeEntry, "nameRecipe");
        String patternKey = getString(recipe, "patternKey", "");
        if (!patternKey.isBlank()) {
            List<String> tokenIds = new ArrayList<>();
            List<NameTokenForm> forms = new ArrayList<>();
            for (CompoundTag tokenEntry : compounds(recipe, "selectedTokens")) {
                String tokenId = getString(tokenEntry, "tokenId", "");
                if (tokenId.isBlank()) {
                    continue;
                }
                tokenIds.add(tokenId);
                forms.add(NameTokenForm.fromId(getString(tokenEntry, "form", NameTokenForm.BASE.idString())));
            }
            if (tokenIds.isEmpty()) {
                for (String tokenId : readStringList(recipe, "tokenIds")) {
                    tokenIds.add(tokenId);
                    forms.add(NameTokenForm.BASE);
                }
            }
            migration.nameRecipesMigrated++;
            return new NameRecipe(
                    getString(recipe, "styleId", "vanilla_adventure"),
                    patternKey,
                    tokenIds,
                    forms,
                    getLong(recipe, "seed", 0L),
                    getString(recipe, "fallbackResolvedName", "")
            );
        }

        migration.nameRecipesMigrated++;
        return new NameRecipe(
                getString(placeEntry, "nameStyle", NameStyle.PLAIN.idString()),
                "living_legends.name.pattern.legacy",
                List.of(),
                List.of(),
                0L,
                getString(placeEntry, "displayName", "")
        );
    }

    private static CompoundTag writeNameRecipe(NameRecipe recipe) {
        NameRecipe storedRecipe = recipe == null ? NameRecipe.empty() : recipe.withoutFallback();
        CompoundTag nbt = newCompound();
        putInt(nbt, "nameRecipeVersion", WorldRemembersDataVersions.CURRENT_NAME_RECIPE_VERSION);
        putString(nbt, "styleId", storedRecipe.styleId());
        putString(nbt, "patternKey", storedRecipe.patternKey());
        putLong(nbt, "seed", storedRecipe.seed());
        putString(nbt, "signature", storedRecipe.recipeSignature());
        putString(nbt, "fallbackResolvedName", storedRecipe.fallbackResolvedName());

        ListTag selectedTokens = newList();
        for (int index = 0; index < storedRecipe.selectedTokenIds().size(); index++) {
            CompoundTag tokenEntry = newCompound();
            putString(tokenEntry, "tokenId", storedRecipe.selectedTokenIds().get(index));
            NameTokenForm form = index < storedRecipe.requestedTokenForms().size()
                    ? storedRecipe.requestedTokenForms().get(index)
                    : NameTokenForm.BASE;
            putString(tokenEntry, "form", form.idString());
            addToList(selectedTokens, tokenEntry);
        }
        putList(nbt, "selectedTokens", selectedTokens);
        return nbt;
    }

    private static PlaceCause readPlaceCause(CompoundTag entry) {
        CompoundTag nbt = getCompound(entry, "cause");
        return new PlaceCause(
                PlaceCauseType.fromId(getString(nbt, "causeType", PlaceCauseType.UNKNOWN.idString())),
                EventType.fromId(getString(nbt, "primaryEventType", EventType.CUSTOM.idString())),
                getString(nbt, "firstDiscoveryKey", ""),
                getString(nbt, "discoveryKind", ""),
                getString(nbt, "structureId", ""),
                getString(nbt, "blockId", ""),
                getString(nbt, "entityId", ""),
                getString(nbt, "bossId", ""),
                getString(nbt, "dominantMobType", getString(nbt, "mobType", "")),
                getString(nbt, "dominantPassiveMobType", ""),
                getString(nbt, "dominantHostileMobType", ""),
                getString(nbt, "dominantNeutralMobType", ""),
                getString(nbt, "dominantValuableBlock", ""),
                getString(nbt, "portalType", ""),
                getString(nbt, "fromDimension", ""),
                getString(nbt, "toDimension", ""),
                getString(nbt, "biomeId", ""),
                getString(nbt, "dominantBiomeId", ""),
                getString(nbt, "biomeGroup", ""),
                getString(nbt, "biomeTheme", ""),
                getString(nbt, "biomeSource", ""),
                getString(nbt, "deathCause", ""),
                getString(nbt, "petName", ""),
                getString(nbt, "petType", ""),
                getString(nbt, "namedMobName", ""),
                getString(nbt, "namedMobType", ""),
                readCounterMap(nbt, "evidenceCounts")
        );
    }

    private static CompoundTag writePlaceCause(PlaceCause cause) {
        PlaceCause storedCause = cause == null ? PlaceCause.unknown() : cause;
        CompoundTag nbt = newCompound();
        putString(nbt, "causeType", storedCause.causeType().idString());
        putString(nbt, "primaryEventType", storedCause.primaryEventType().idString());
        putString(nbt, "firstDiscoveryKey", storedCause.firstDiscoveryKey());
        putString(nbt, "discoveryKind", storedCause.discoveryKind());
        putString(nbt, "structureId", storedCause.structureId());
        putString(nbt, "blockId", storedCause.blockId());
        putString(nbt, "entityId", storedCause.entityId());
        putString(nbt, "bossId", storedCause.bossId());
        putString(nbt, "dominantMobType", storedCause.dominantMobType());
        putString(nbt, "dominantPassiveMobType", storedCause.dominantPassiveMobType());
        putString(nbt, "dominantHostileMobType", storedCause.dominantHostileMobType());
        putString(nbt, "dominantNeutralMobType", storedCause.dominantNeutralMobType());
        putString(nbt, "dominantValuableBlock", storedCause.dominantValuableBlock());
        putString(nbt, "portalType", storedCause.portalType());
        putString(nbt, "fromDimension", storedCause.fromDimension());
        putString(nbt, "toDimension", storedCause.toDimension());
        putString(nbt, "biomeId", storedCause.biomeId());
        putString(nbt, "dominantBiomeId", storedCause.dominantBiomeId());
        putString(nbt, "biomeGroup", storedCause.biomeGroup());
        putString(nbt, "biomeTheme", storedCause.biomeTheme());
        putString(nbt, "biomeSource", storedCause.biomeSource());
        putString(nbt, "deathCause", storedCause.deathCause());
        putString(nbt, "petName", storedCause.petName());
        putString(nbt, "petType", storedCause.petType());
        putString(nbt, "namedMobName", storedCause.namedMobName());
        putString(nbt, "namedMobType", storedCause.namedMobType());
        putList(nbt, "evidenceCounts", writeCounterMap(storedCause.evidenceCounts()));
        return nbt;
    }

    private static WorldPos readCenter(CompoundTag entry, PlaceBounds fallbackBounds) {
        CompoundTag center = getCompound(entry, "center");
        String dimensionId = getString(center, "dimensionId", "");
        if (dimensionId.isBlank()) {
            return fallbackBounds.center();
        }
        return readPos(center);
    }

    private static int radiusFromBounds(PlaceBounds bounds) {
        WorldPos center = bounds.center();
        int xRadius = Math.max(Math.abs(center.x() - bounds.minX()), Math.abs(bounds.maxX() - center.x()));
        int zRadius = Math.max(Math.abs(center.z() - bounds.minZ()), Math.abs(bounds.maxZ() - center.z()));
        return Math.max(xRadius, zRadius);
    }

    private static Map<String, WorldMemoryEvent> readFirstEvents(CompoundTag nbt, String key, MigrationStats migration) {
        Map<String, WorldMemoryEvent> result = new LinkedHashMap<>();
        for (CompoundTag entry : compounds(nbt, key)) {
            String eventKey = getString(entry, "key", "");
            WorldMemoryEvent event = readEvent(getCompound(entry, "event"));
            if (!eventKey.isBlank() && event != null) {
                result.put(eventKey, event);
                migration.firstEventsMigrated++;
            } else {
                migration.skippedBrokenFirstEvents++;
            }
        }
        return result;
    }

    private static ListTag writeFirstEvents(Map<String, WorldMemoryEvent> events) {
        ListTag list = newList();
        for (Map.Entry<String, WorldMemoryEvent> storedEvent : events.entrySet()) {
            CompoundTag entry = newCompound();
            putInt(entry, "firstEventVersion", WorldRemembersDataVersions.CURRENT_FIRST_EVENT_VERSION);
            putString(entry, "key", storedEvent.getKey());
            putCompound(entry, "event", writeEvent(storedEvent.getValue()));
            addToList(list, entry);
        }
        return list;
    }

    private static Map<String, Long> readEventCounters(CompoundTag nbt) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (CompoundTag entry : compounds(nbt, "eventCounters")) {
            String eventType = getString(entry, "eventType", "");
            if (!eventType.isBlank()) {
                result.put(eventType, getLong(entry, "count", 0L));
            }
        }
        return result;
    }

    private static ListTag writeEventCounters(Map<String, Long> eventCounters) {
        ListTag list = newList();
        for (Map.Entry<String, Long> counter : eventCounters.entrySet()) {
            CompoundTag entry = newCompound();
            putString(entry, "eventType", counter.getKey());
            putLong(entry, "count", counter.getValue());
            addToList(list, entry);
        }
        return list;
    }

    private static Map<String, List<String>> readDiscoveredPlaces(CompoundTag nbt, MigrationStats migration) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (CompoundTag entry : compounds(nbt, "playerDiscoveredPlaces")) {
            String playerId = getString(entry, "playerId", "");
            if (playerId.isBlank()) {
                continue;
            }
            List<String> placeIds = readStringList(entry, "placeIds");
            if (!placeIds.isEmpty()) {
                result.put(playerId, placeIds);
                migration.journalRecordsMigrated++;
            }
        }
        return result;
    }

    private static ListTag writeDiscoveredPlaces(Map<String, List<String>> discoveredPlaces) {
        ListTag list = newList();
        for (Map.Entry<String, List<String>> entry : discoveredPlaces.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            CompoundTag playerEntry = newCompound();
            putInt(playerEntry, "journalVersion", WorldRemembersDataVersions.CURRENT_JOURNAL_VERSION);
            putString(playerEntry, "playerId", entry.getKey());
            putList(playerEntry, "placeIds", writeStringList(entry.getValue()));
            addToList(list, playerEntry);
        }
        return list;
    }

    private static WorldMemoryEvent readEvent(CompoundTag nbt) {
        if (nbt == null) {
            return null;
        }

        return new WorldMemoryEvent(
                getString(nbt, "eventId", ""),
                EventType.fromId(getString(nbt, "eventType", EventType.CUSTOM.idString())),
                readPos(getCompound(nbt, "position")),
                getString(nbt, "actorId", ""),
                getString(nbt, "subjectId", ""),
                getLong(nbt, "gameTime", 0L),
                getLong(nbt, "createdAtEpochMillis", 0L),
                getDouble(nbt, "importance", 0.0),
                getString(nbt, "note", ""),
                getString(nbt, "firstDiscoveryKey", ""),
                getString(nbt, "structureId", ""),
                getInt(nbt, "hasStructureBounds", 0) > 0 ? readBounds(getCompound(nbt, "structureBounds")) : null
        );
    }

    private static CompoundTag writeEvent(WorldMemoryEvent event) {
        CompoundTag nbt = newCompound();
        putInt(nbt, "firstEventVersion", WorldRemembersDataVersions.CURRENT_FIRST_EVENT_VERSION);
        putString(nbt, "eventId", event.eventIdString());
        putString(nbt, "eventType", event.eventType().idString());
        putCompound(nbt, "position", writePos(event.position()));
        putString(nbt, "actorId", event.actorIdString());
        putString(nbt, "subjectId", event.subjectIdString());
        putLong(nbt, "gameTime", event.gameTime());
        putLong(nbt, "createdAtEpochMillis", event.createdAtEpochMillis());
        putDouble(nbt, "importance", event.importance());
        putString(nbt, "note", event.note());
        putString(nbt, "firstDiscoveryKey", event.firstDiscoveryKeyString());
        putString(nbt, "structureId", event.structureIdString());
        putInt(nbt, "hasStructureBounds", event.structureBounds() == null ? 0 : 1);
        if (event.structureBounds() != null) {
            putCompound(nbt, "structureBounds", writeBounds(event.structureBounds()));
        }
        return nbt;
    }

    private static WorldPos readPos(CompoundTag nbt) {
        return new WorldPos(
                getString(nbt, "dimensionId", "minecraft:overworld"),
                getInt(nbt, "x", 0),
                getInt(nbt, "y", 0),
                getInt(nbt, "z", 0)
        );
    }

    private static CompoundTag writePos(WorldPos pos) {
        CompoundTag nbt = newCompound();
        putString(nbt, "dimensionId", pos.dimensionId());
        putInt(nbt, "x", pos.x());
        putInt(nbt, "y", pos.y());
        putInt(nbt, "z", pos.z());
        return nbt;
    }

    private static PlaceBounds readBounds(CompoundTag nbt) {
        String dimensionId = getString(nbt, "dimensionId", "minecraft:overworld");
        int minX = getInt(nbt, "minX", 0);
        int minY = getInt(nbt, "minY", 0);
        int minZ = getInt(nbt, "minZ", 0);
        int maxX = getInt(nbt, "maxX", 0);
        int maxY = getInt(nbt, "maxY", 0);
        int maxZ = getInt(nbt, "maxZ", 0);
        int fallbackCenterX = Math.min(minX, maxX) + Math.floorDiv(Math.abs(maxX - minX), 2);
        int fallbackCenterY = Math.min(minY, maxY) + Math.floorDiv(Math.abs(maxY - minY), 2);
        int fallbackCenterZ = Math.min(minZ, maxZ) + Math.floorDiv(Math.abs(maxZ - minZ), 2);
        int fallbackRadius = Math.max(
                Math.max(Math.abs(fallbackCenterX - minX), Math.abs(maxX - fallbackCenterX)),
                Math.max(Math.abs(fallbackCenterZ - minZ), Math.abs(maxZ - fallbackCenterZ))
        );
        return new PlaceBounds(
                dimensionId,
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ,
                PlaceBounds.Shape.fromId(getString(nbt, "shape", PlaceBounds.Shape.BOX.idString())),
                hasKey(nbt, "centerX") ? getInt(nbt, "centerX", fallbackCenterX) : fallbackCenterX,
                hasKey(nbt, "centerY") ? getInt(nbt, "centerY", fallbackCenterY) : fallbackCenterY,
                hasKey(nbt, "centerZ") ? getInt(nbt, "centerZ", fallbackCenterZ) : fallbackCenterZ,
                hasKey(nbt, "radius") ? getInt(nbt, "radius", fallbackRadius) : fallbackRadius
        );
    }

    private static CompoundTag writeBounds(PlaceBounds bounds) {
        CompoundTag nbt = newCompound();
        putString(nbt, "dimensionId", bounds.dimensionId());
        putInt(nbt, "minX", bounds.minX());
        putInt(nbt, "minY", bounds.minY());
        putInt(nbt, "minZ", bounds.minZ());
        putInt(nbt, "maxX", bounds.maxX());
        putInt(nbt, "maxY", bounds.maxY());
        putInt(nbt, "maxZ", bounds.maxZ());
        putString(nbt, "shape", bounds.shape().idString());
        putInt(nbt, "centerX", bounds.centerX());
        putInt(nbt, "centerY", bounds.centerY());
        putInt(nbt, "centerZ", bounds.centerZ());
        putInt(nbt, "radius", bounds.radius());
        return nbt;
    }

    private static PlaceStats readPlaceStats(CompoundTag nbt) {
        return new PlaceStats(
                getLong(nbt, "eventCount", 0L),
                getLong(nbt, "visitCount", 0L),
                getLong(nbt, "deathCount", 0L),
                getLong(nbt, "combatEventCount", 0L),
                getLong(nbt, "buildEventCount", 0L),
                getDouble(nbt, "totalImportance", 0.0),
                getLong(nbt, "firstEventGameTime", 0L),
                getLong(nbt, "lastEventGameTime", 0L),
                EventType.fromId(getString(nbt, "lastEventType", EventType.CUSTOM.idString())),
                readCounterMap(nbt, "eventTypeCounts"),
                readCounterMap(nbt, "deathSiteEnvironmentCounts"),
                readCounterMap(nbt, "metadataCounts")
        );
    }

    private static CompoundTag writePlaceStats(PlaceStats stats) {
        CompoundTag nbt = newCompound();
        putLong(nbt, "eventCount", stats.eventCount());
        putLong(nbt, "visitCount", stats.visitCount());
        putLong(nbt, "deathCount", stats.deathCount());
        putLong(nbt, "combatEventCount", stats.combatEventCount());
        putLong(nbt, "buildEventCount", stats.buildEventCount());
        putDouble(nbt, "totalImportance", stats.totalImportance());
        putLong(nbt, "firstEventGameTime", stats.firstEventGameTime());
        putLong(nbt, "lastEventGameTime", stats.lastEventGameTime());
        putString(nbt, "lastEventType", stats.lastEventType().idString());
        putList(nbt, "eventTypeCounts", writeCounterMap(stats.eventTypeCounts()));
        putList(nbt, "deathSiteEnvironmentCounts", writeCounterMap(stats.deathSiteEnvironmentCounts()));
        putList(nbt, "metadataCounts", writeCounterMap(stats.metadataCounts()));
        return nbt;
    }

    private static Map<String, Long> readCounterMap(CompoundTag nbt, String key) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (CompoundTag entry : compounds(nbt, key)) {
            String eventType = getString(entry, "eventType", "");
            if (!eventType.isBlank()) {
                result.put(eventType, getLong(entry, "count", 0L));
            }
        }
        return result;
    }

    private static Map<String, String> readStringMap(CompoundTag nbt, String key) {
        Map<String, String> result = new LinkedHashMap<>();
        for (CompoundTag entry : compounds(nbt, key)) {
            String tokenKey = getString(entry, "key", "");
            String tokenValue = getString(entry, "value", "");
            if (!tokenKey.isBlank() && !tokenValue.isBlank()) {
                result.put(tokenKey, tokenValue);
            }
        }
        return result;
    }

    private static List<String> readStringList(CompoundTag nbt, String key) {
        List<String> result = new ArrayList<>();
        for (CompoundTag entry : compounds(nbt, key)) {
            String value = getString(entry, "value", "");
            if (!value.isBlank()) {
                result.add(value);
            }
        }
        return result;
    }

    private static ListTag writeCounterMap(Map<String, Long> counters) {
        ListTag list = newList();
        for (Map.Entry<String, Long> counter : counters.entrySet()) {
            CompoundTag entry = newCompound();
            putString(entry, "eventType", counter.getKey());
            putLong(entry, "count", counter.getValue());
            addToList(list, entry);
        }
        return list;
    }

    private static Map<String, CandidateDecayState> readCandidateDecayStates(CompoundTag nbt, MigrationStats migration) {
        Map<String, CandidateDecayState> result = new LinkedHashMap<>();
        for (CompoundTag entry : compounds(nbt, "candidateDecayStates")) {
            String placeType = getString(entry, "placeType", "");
            if (!placeType.isBlank()) {
                result.put(placeType, new CandidateDecayState(
                        getDouble(entry, "score", 0.0),
                        getLong(entry, "lastRelevantEventGameTime", 0L),
                        getLong(entry, "lastDecayGameTime", 0L)
                ));
                migration.candidateDecayMigrated++;
            }
        }
        return result;
    }

    private static ListTag writeCandidateDecayStates(Map<String, CandidateDecayState> states) {
        ListTag list = newList();
        for (Map.Entry<String, CandidateDecayState> storedState : states.entrySet()) {
            CandidateDecayState state = storedState.getValue();
            if (state == null) {
                continue;
            }
            CompoundTag entry = newCompound();
            putInt(entry, "candidateDecayVersion", WorldRemembersDataVersions.CURRENT_CANDIDATE_DECAY_VERSION);
            putString(entry, "placeType", storedState.getKey());
            putDouble(entry, "score", state.score());
            putLong(entry, "lastRelevantEventGameTime", state.lastRelevantEventGameTime());
            putLong(entry, "lastDecayGameTime", state.lastDecayGameTime());
            addToList(list, entry);
        }
        return list;
    }

    private static ListTag writeStringMap(Map<String, String> values) {
        ListTag list = newList();
        for (Map.Entry<String, String> value : values.entrySet()) {
            CompoundTag entry = newCompound();
            putString(entry, "key", value.getKey());
            putString(entry, "value", value.getValue());
            addToList(list, entry);
        }
        return list;
    }

    private static ListTag writeStringList(List<String> values) {
        ListTag list = newList();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            CompoundTag entry = newCompound();
            putString(entry, "value", value);
            addToList(list, entry);
        }
        return list;
    }

    private static List<CompoundTag> compounds(CompoundTag nbt, String key) {
        ListTag values = nbt.getList(key, TAG_COMPOUND);
        if (values.isEmpty()) {
            return List.of();
        }

        List<CompoundTag> result = new ArrayList<>();
        for (Tag value : values) {
            if (value instanceof CompoundTag compound) {
                result.add(compound);
            }
        }
        return result;
    }

    private static CompoundTag getCompound(CompoundTag nbt, String key) {
        return nbt == null || !nbt.contains(key, Tag.TAG_COMPOUND) ? newCompound() : nbt.getCompound(key);
    }

    private static int getInt(CompoundTag nbt, String key, int fallback) {
        return nbt != null && nbt.contains(key, Tag.TAG_ANY_NUMERIC) ? nbt.getInt(key) : fallback;
    }

    private static long getLong(CompoundTag nbt, String key, long fallback) {
        return nbt != null && nbt.contains(key, Tag.TAG_ANY_NUMERIC) ? nbt.getLong(key) : fallback;
    }

    private static double getDouble(CompoundTag nbt, String key, double fallback) {
        return nbt != null && nbt.contains(key, Tag.TAG_ANY_NUMERIC) ? nbt.getDouble(key) : fallback;
    }

    private static String getString(CompoundTag nbt, String key, String fallback) {
        if (nbt == null || !nbt.contains(key, Tag.TAG_STRING)) {
            return fallback;
        }
        String value = nbt.getString(key);
        return value.isBlank() ? fallback : value;
    }

    private static boolean hasKey(CompoundTag nbt, String key) {
        return nbt != null && nbt.contains(key);
    }

    private static void putInt(CompoundTag nbt, String key, int value) {
        nbt.putInt(key, value);
    }

    private static void putLong(CompoundTag nbt, String key, long value) {
        nbt.putLong(key, value);
    }

    private static void putDouble(CompoundTag nbt, String key, double value) {
        nbt.putDouble(key, value);
    }

    private static void putString(CompoundTag nbt, String key, String value) {
        nbt.putString(key, value == null ? "" : value);
    }

    private static void putCompound(CompoundTag nbt, String key, CompoundTag value) {
        nbt.put(key, value);
    }

    private static void putList(CompoundTag nbt, String key, ListTag value) {
        nbt.put(key, value);
    }

    private static void addToList(ListTag list, CompoundTag value) {
        list.add(value);
    }

    private static CompoundTag newCompound() {
        return new CompoundTag();
    }

    private static ListTag newList() {
        return new ListTag();
    }

    private static final class MigrationStats {
        private final int fromVersion;
        private int namedPlacesMigrated;
        private int nameRecipesMigrated;
        private int chunkStatsMigrated;
        private int firstEventsMigrated;
        private int journalRecordsMigrated;
        private int candidateDecayMigrated;
        private int deletedMarkersMigrated;
        private int defaultedIds;
        private int defaultedRadii;
        private int defaultedSourceChunks;
        private int skippedBrokenPlaces;
        private int skippedBrokenChunkStats;
        private int skippedBrokenDeletedMarkers;
        private int skippedBrokenFirstEvents;

        private MigrationStats(int fromVersion) {
            this.fromVersion = Math.max(0, fromVersion);
        }

        private int fromVersion() {
            return fromVersion;
        }

        private MigrationSummary finish(WorldMemoryStorageData data) {
            return new MigrationSummary(
                    fromVersion,
                    WorldRemembersDataVersions.CURRENT_WORLD_STATE_VERSION,
                    namedPlacesMigrated,
                    nameRecipesMigrated,
                    chunkStatsMigrated,
                    firstEventsMigrated,
                    journalRecordsMigrated,
                    candidateDecayMigrated,
                    deletedMarkersMigrated,
                    defaultedIds,
                    defaultedRadii,
                    defaultedSourceChunks,
                    skippedBrokenPlaces,
                    skippedBrokenChunkStats,
                    skippedBrokenDeletedMarkers,
                    skippedBrokenFirstEvents,
                    data == null ? 0 : data.namedPlaceCount(),
                    data == null ? 0 : data.chunkStatsCount()
            );
        }
    }

    private record MigrationSummary(
            int fromVersion,
            int toVersion,
            int namedPlacesMigrated,
            int nameRecipesMigrated,
            int chunkStatsMigrated,
            int firstEventsMigrated,
            int journalRecordsMigrated,
            int candidateDecayMigrated,
            int deletedMarkersMigrated,
            int defaultedIds,
            int defaultedRadii,
            int defaultedSourceChunks,
            int skippedBrokenPlaces,
            int skippedBrokenChunkStats,
            int skippedBrokenDeletedMarkers,
            int skippedBrokenFirstEvents,
            int loadedNamedPlaces,
            int loadedChunkStats
    ) {
        private static MigrationSummary fresh() {
            return new MigrationSummary(
                    WorldRemembersDataVersions.CURRENT_WORLD_STATE_VERSION,
                    WorldRemembersDataVersions.CURRENT_WORLD_STATE_VERSION,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            );
        }

        private String format() {
            return "World Remembers migration:"
                    + " fromVersion=" + fromVersion
                    + " toVersion=" + toVersion
                    + " namedPlaces migrated=" + namedPlacesMigrated
                    + " nameRecipes migrated=" + nameRecipesMigrated
                    + " chunkStats migrated=" + chunkStatsMigrated
                    + " firstEvents migrated=" + firstEventsMigrated
                    + " journal records migrated=" + journalRecordsMigrated
                    + " candidateDecay migrated=" + candidateDecayMigrated
                    + " deletedMarkers migrated=" + deletedMarkersMigrated
                    + " defaultedIds=" + defaultedIds
                    + " defaultedRadii=" + defaultedRadii
                    + " defaultedSourceChunks=" + defaultedSourceChunks
                    + " skippedBrokenPlaces=" + skippedBrokenPlaces
                    + " skippedBrokenChunkStats=" + skippedBrokenChunkStats
                    + " skippedBrokenDeletedMarkers=" + skippedBrokenDeletedMarkers
                    + " skippedBrokenFirstEvents=" + skippedBrokenFirstEvents
                    + " loadedNamedPlaces=" + loadedNamedPlaces
                    + " loadedChunkStats=" + loadedChunkStats;
        }
    }
}

