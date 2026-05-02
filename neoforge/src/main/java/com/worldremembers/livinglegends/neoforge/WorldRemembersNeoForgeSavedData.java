package com.worldremembers.livinglegends.neoforge;

import com.worldremembers.livinglegends.DirtyChunkScoreQueue;
import com.worldremembers.livinglegends.WorldMemoryStorageData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public final class WorldRemembersNeoForgeSavedData extends SavedData {
    static final Factory<WorldRemembersNeoForgeSavedData> FACTORY = new Factory<>(
            WorldRemembersNeoForgeSavedData::new,
            WorldRemembersNeoForgeSavedData::load,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final WorldMemoryStorageData data;
    private final DirtyChunkScoreQueue scoreQueue = new DirtyChunkScoreQueue();
    private long lastCandidateDecayCheckGameTime;

    private WorldRemembersNeoForgeSavedData() {
        this(new WorldMemoryStorageData());
    }

    private WorldRemembersNeoForgeSavedData(WorldMemoryStorageData data) {
        this.data = data == null ? new WorldMemoryStorageData() : data;
    }

    public static WorldRemembersNeoForgeSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        try {
            return new WorldRemembersNeoForgeSavedData(WorldRemembersLivingLegendsNeoForgeNbt.read(tag));
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Could not load World Remembers NeoForge saved data", exception);
        }
    }

    public WorldMemoryStorageData data() {
        return data;
    }

    DirtyChunkScoreQueue scoreQueue() {
        return scoreQueue;
    }

    boolean shouldRunCandidateDecay(long currentGameTime, long intervalTicks) {
        long interval = Math.max(1L, intervalTicks);
        return lastCandidateDecayCheckGameTime <= 0L
                || currentGameTime - lastCandidateDecayCheckGameTime >= interval;
    }

    void recordCandidateDecayCheck(long currentGameTime) {
        lastCandidateDecayCheckGameTime = currentGameTime;
    }

    void markChanged() {
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return WorldRemembersLivingLegendsNeoForgeNbt.write(data, tag);
    }
}
