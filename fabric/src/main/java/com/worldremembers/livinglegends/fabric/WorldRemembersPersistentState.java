package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.WorldMemoryStorageData;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.PersistentState;

final class WorldRemembersPersistentState extends PersistentState {
    static final Type<WorldRemembersPersistentState> TYPE = new Type<>(
            WorldRemembersPersistentState::new,
            WorldRemembersPersistentState::read,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final WorldRemembersLivingLegendsFabricStorage.PersistentStateDelegate delegate;

    private WorldRemembersPersistentState() {
        this(new WorldMemoryStorageData());
    }

    private WorldRemembersPersistentState(WorldMemoryStorageData data) {
        this.delegate = new WorldRemembersLivingLegendsFabricStorage.PersistentStateDelegate(data);
    }

    private static WorldRemembersPersistentState read(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        try {
            return new WorldRemembersPersistentState(WorldRemembersLivingLegendsFabricNbt.read(nbt));
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Could not load World Remembers persistent state", exception);
        }
    }

    WorldRemembersLivingLegendsFabricStorage.PersistentStateDelegate delegate() {
        return delegate;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        return WorldRemembersLivingLegendsFabricNbt.write(delegate.data(), nbt);
    }
}
