package com.worldremembers.livinglegends;

public final class WorldRemembersDataVersions {
    public static final int CURRENT_WORLD_STATE_VERSION = 13;
    public static final int CURRENT_NAMED_PLACE_VERSION = 1;
    public static final int CURRENT_NAME_RECIPE_VERSION = 1;
    public static final int CURRENT_CHUNK_MEMORY_VERSION = 1;
    public static final int CURRENT_DELETED_MARKER_VERSION = 1;
    public static final int CURRENT_FIRST_EVENT_VERSION = 1;
    public static final int CURRENT_JOURNAL_VERSION = 1;
    public static final int CURRENT_CANDIDATE_DECAY_VERSION = 1;
    public static final int CURRENT_EXPORT_VERSION = 1;

    private WorldRemembersDataVersions() {
    }

    public static String debugSummary() {
        return "worldStateVersion=" + CURRENT_WORLD_STATE_VERSION
                + " namedPlaceVersion=" + CURRENT_NAMED_PLACE_VERSION
                + " nameRecipeVersion=" + CURRENT_NAME_RECIPE_VERSION
                + " chunkMemoryVersion=" + CURRENT_CHUNK_MEMORY_VERSION
                + " deletedMarkerVersion=" + CURRENT_DELETED_MARKER_VERSION
                + " firstEventVersion=" + CURRENT_FIRST_EVENT_VERSION
                + " journalVersion=" + CURRENT_JOURNAL_VERSION
                + " candidateDecayVersion=" + CURRENT_CANDIDATE_DECAY_VERSION
                + " exportVersion=" + CURRENT_EXPORT_VERSION;
    }
}
