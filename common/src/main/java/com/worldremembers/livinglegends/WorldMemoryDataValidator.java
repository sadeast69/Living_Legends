package com.worldremembers.livinglegends;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class WorldMemoryDataValidator {
    private WorldMemoryDataValidator() {
    }

    public static ValidationReport validate(WorldMemoryStorageData data) {
        if (data == null) {
            return new ValidationReport(
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    List.of(),
                    List.of("World memory storage is unavailable.")
            );
        }

        Set<String> seenPlaceIds = new HashSet<>();
        Set<String> activePlaceIds = new HashSet<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int duplicateIds = 0;
        int invalidRadii = 0;
        int missingNames = 0;
        int invalidSourceChunks = 0;
        int invalidNamedPlaces = 0;
        for (NamedPlace place : data.namedPlaces()) {
            if (place == null) {
                invalidNamedPlaces++;
                errors.add("NamedPlace list contains a null entry.");
                continue;
            }
            if (!seenPlaceIds.add(place.placeIdString())) {
                duplicateIds++;
                errors.add("Duplicate active place id: " + place.placeIdString());
            }
            activePlaceIds.add(place.placeIdString());
            if (place.placeIdString().isBlank()) {
                invalidNamedPlaces++;
                errors.add("NamedPlace has blank id.");
            }
            if (place.placeType() == PlaceType.UNKNOWN) {
                invalidNamedPlaces++;
                errors.add("NamedPlace " + place.placeIdString() + " has UNKNOWN type.");
            }
            if (place.dimensionId().isBlank()) {
                invalidNamedPlaces++;
                errors.add("NamedPlace " + place.placeIdString() + " has blank dimension.");
            }
            if (place.center() == null) {
                invalidNamedPlaces++;
                errors.add("NamedPlace " + place.placeIdString() + " has missing center.");
            }
            if (place.radius() <= 0) {
                invalidRadii++;
                warnings.add("NamedPlace " + place.placeIdString() + " has non-positive radius " + place.radius() + ".");
            }
            if (!Double.isFinite(place.score())) {
                invalidNamedPlaces++;
                errors.add("NamedPlace " + place.placeIdString() + " has non-finite score.");
            }
            if (!hasDisplayableName(place)) {
                missingNames++;
                warnings.add("NamedPlace " + place.placeIdString() + " has no displayable name path.");
            }
            if (!hasResolvableRecipe(place)) {
                warnings.add("NamedPlace " + place.placeIdString() + " has a NameRecipe without a matching built-in pattern and no fallback.");
            }
            if (place.sourceChunks().isEmpty()) {
                invalidSourceChunks++;
                warnings.add("NamedPlace " + place.placeIdString() + " has no source chunks.");
            }
            for (String sourceChunk : place.sourceChunks()) {
                if (sourceChunk == null || sourceChunk.isBlank()) {
                    invalidSourceChunks++;
                    warnings.add("NamedPlace " + place.placeIdString() + " has a blank source chunk entry.");
                    break;
                }
            }
        }

        int invalidChunkKeys = 0;
        int invalidChunkCounters = 0;
        int candidateDecayRecords = 0;
        int invalidCandidateDecayStates = 0;
        for (Map.Entry<String, ChunkMemoryStats> entry : data.chunkStatsByKey().entrySet()) {
            ChunkMemoryStats stats = entry.getValue();
            if (stats == null || !entry.getKey().equals(stats.chunkIdString())) {
                invalidChunkKeys++;
                errors.add("Chunk memory key mismatch or null stats at key: " + entry.getKey());
                continue;
            }
            if (!Double.isFinite(stats.totalImportance())) {
                invalidChunkCounters++;
                errors.add("Chunk " + entry.getKey() + " has non-finite totalImportance.");
            }
            if (stats.eventCount() < 0 || stats.visitCount() < 0 || stats.deathCount() < 0
                    || stats.combatEventCount() < 0 || stats.buildEventCount() < 0) {
                invalidChunkCounters++;
                errors.add("Chunk " + entry.getKey() + " has a negative counter.");
            }
            for (Map.Entry<String, Long> count : stats.eventTypeCounts().entrySet()) {
                if (count.getValue() == null || count.getValue() < 0) {
                    invalidChunkCounters++;
                    errors.add("Chunk " + entry.getKey() + " has invalid event counter for " + count.getKey() + ".");
                    break;
                }
            }
            for (Map.Entry<String, CandidateDecayState> stateEntry : stats.candidateDecayStates().entrySet()) {
                candidateDecayRecords++;
                PlaceType type = PlaceType.fromId(stateEntry.getKey());
                if (type == PlaceType.UNKNOWN || stateEntry.getValue() == null) {
                    invalidCandidateDecayStates++;
                    warnings.add("Chunk " + entry.getKey() + " has invalid candidate decay entry " + stateEntry.getKey() + ".");
                    continue;
                }
                CandidateDecayState state = stateEntry.getValue();
                if (!Double.isFinite(state.score()) || state.score() < 0.0
                        || state.lastRelevantEventGameTime() < 0L
                        || state.lastDecayGameTime() < 0L) {
                    invalidCandidateDecayStates++;
                    warnings.add("Chunk " + entry.getKey() + " has invalid candidate decay state for " + stateEntry.getKey() + ".");
                }
            }
        }

        int invalidDeletedMarkers = 0;
        for (DeletedPlaceMarker marker : data.deletedPlaceMarkers()) {
            if (marker == null || marker.originalPlaceId().isBlank() || marker.radius() < 0 || marker.dimensionId().isBlank() || marker.center() == null) {
                invalidDeletedMarkers++;
                warnings.add("DeletedPlaceMarker is invalid or incomplete.");
            }
        }

        int staleVisitedRefs = 0;
        for (Map.Entry<String, java.util.List<String>> entry : data.discoveredPlaceIdsByPlayer().entrySet()) {
            if (!validUuid(entry.getKey())) {
                warnings.add("Journal visited data has a non-UUID player id: " + entry.getKey());
            }
            for (String placeId : entry.getValue()) {
                if (!activePlaceIds.contains(placeId)) {
                    staleVisitedRefs++;
                    warnings.add("Journal visited data references missing place id: " + placeId);
                }
            }
        }

        return new ValidationReport(
                data.schemaVersion(),
                data.namedPlaceCount(),
                data.chunkStatsCount(),
                data.deletedPlaceMarkerCount(),
                data.discoveredPlaceIdsByPlayer().size(),
                candidateDecayRecords,
                duplicateIds,
                invalidRadii,
                missingNames,
                invalidSourceChunks,
                invalidChunkKeys + invalidChunkCounters,
                invalidDeletedMarkers,
                invalidCandidateDecayStates,
                staleVisitedRefs,
                invalidNamedPlaces,
                List.copyOf(warnings),
                List.copyOf(errors)
        );
    }

    private static boolean hasDisplayableName(NamedPlace place) {
        if (place.manuallyRenamed() && !place.manualName().isBlank()) {
            return true;
        }
        NameRecipe recipe = place.nameRecipe();
        return recipe != null && (!recipe.patternKey().isBlank() || !recipe.fallbackResolvedName().isBlank());
    }

    private static boolean hasResolvableRecipe(NamedPlace place) {
        if (place == null || place.manuallyRenamed()) {
            return true;
        }
        NameRecipe recipe = place.nameRecipe();
        if (recipe == null || recipe.patternKey().isBlank()) {
            return true;
        }
        if (!recipe.fallbackResolvedName().isBlank()) {
            return true;
        }
        if (NameGenerator.isRuntimeMemorialPatternKey(recipe.styleId(), recipe.patternKey())
                && recipe.selectedTokenIds().stream().anyMatch(NameRecipe::isLiteralToken)) {
            return true;
        }
        NameDataPack pack = BuiltInNameData.packForStyle(recipe.styleId());
        for (NamePattern pattern : pack.patterns()) {
            if (pattern.translationKey().equals(recipe.patternKey())) {
                return true;
            }
        }
        return false;
    }

    private static boolean validUuid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public record ValidationReport(
            int worldStateVersion,
            int namedPlaces,
            int chunkStats,
            int deletedMarkers,
            int journalVisitedPlayers,
            int candidateDecayRecords,
            int duplicateIds,
            int invalidRadii,
            int missingNames,
            int invalidSourceChunks,
            int invalidChunkEntries,
            int invalidDeletedMarkers,
            int invalidCandidateDecayStates,
            int staleVisitedRefs,
            int invalidNamedPlaces,
            List<String> warnings,
            List<String> errors
    ) {
        public String result() {
            if (!errors.isEmpty() || duplicateIds > 0 || invalidNamedPlaces > 0 || invalidChunkEntries > 0) {
                return "FAIL";
            }
            if (!warnings.isEmpty()
                    || invalidRadii > 0
                    || missingNames > 0
                    || invalidSourceChunks > 0
                    || invalidDeletedMarkers > 0
                    || invalidCandidateDecayStates > 0
                    || staleVisitedRefs > 0) {
                return "WARN";
            }
            return "PASS";
        }

        public int warningCount() {
            return warnings.size()
                    + invalidRadii
                    + missingNames
                    + invalidSourceChunks
                    + invalidDeletedMarkers
                    + invalidCandidateDecayStates
                    + staleVisitedRefs;
        }

        public int errorCount() {
            return errors.size() + duplicateIds + invalidNamedPlaces + invalidChunkEntries;
        }

        public String format() {
            return "World Remembers data validate"
                    + " result=" + result()
                    + " worldStateVersion=" + worldStateVersion
                    + " namedPlaces=" + namedPlaces
                    + " chunkStats=" + chunkStats
                    + " deletedMarkers=" + deletedMarkers
                    + " journalVisitedPlayers=" + journalVisitedPlayers
                    + " candidateDecayRecords=" + candidateDecayRecords
                    + " duplicateIds=" + duplicateIds
                    + " invalidRadii=" + invalidRadii
                    + " missingNames=" + missingNames
                    + " invalidSourceChunks=" + invalidSourceChunks
                    + " invalidChunkEntries=" + invalidChunkEntries
                    + " invalidDeletedMarkers=" + invalidDeletedMarkers
                    + " invalidCandidateDecayStates=" + invalidCandidateDecayStates
                    + " staleVisitedRefs=" + staleVisitedRefs
                    + " invalidNamedPlaces=" + invalidNamedPlaces
                    + " warnings=" + warningCount()
                    + " errors=" + errorCount();
        }

        public String formatDetailed(int maxIssues) {
            StringBuilder builder = new StringBuilder(format());
            int emitted = 0;
            for (String error : errors) {
                if (emitted >= maxIssues) {
                    break;
                }
                builder.append('\n').append("ERROR: ").append(error);
                emitted++;
            }
            for (String warning : warnings) {
                if (emitted >= maxIssues) {
                    break;
                }
                builder.append('\n').append("WARN: ").append(warning);
                emitted++;
            }
            int hidden = errors.size() + warnings.size() - emitted;
            if (hidden > 0) {
                builder.append('\n').append("... ").append(hidden).append(" more issues in latest.log.");
            }
            return builder.toString();
        }
    }
}
