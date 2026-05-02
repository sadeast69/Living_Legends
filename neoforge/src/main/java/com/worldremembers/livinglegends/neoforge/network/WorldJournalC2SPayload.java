package com.worldremembers.livinglegends.neoforge.network;

import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WorldJournalC2SPayload(
        Action action,
        int page,
        int pageSize,
        String tab,
        String searchQuery,
        String placeTypeFilter,
        String dimensionFilter,
        String sortMode,
        String sortDirection,
        String placeId,
        String text
) implements CustomPacketPayload {
    public static final Type<WorldJournalC2SPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WorldRemembersLivingLegends.MOD_ID, "world_journal_c2s")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, WorldJournalC2SPayload> STREAM_CODEC =
            CustomPacketPayload.codec(WorldJournalC2SPayload::write, WorldJournalC2SPayload::new);

    public WorldJournalC2SPayload {
        action = action == null ? Action.PAGE : action;
        page = Math.max(0, page);
        pageSize = Math.max(1, pageSize);
        tab = clean(tab);
        searchQuery = clean(searchQuery);
        placeTypeFilter = clean(placeTypeFilter);
        dimensionFilter = clean(dimensionFilter);
        sortMode = clean(sortMode);
        sortDirection = clean(sortDirection);
        placeId = clean(placeId);
        text = clean(text);
    }

    private WorldJournalC2SPayload(RegistryFriendlyByteBuf buf) {
        this(
                buf.readEnum(Action.class),
                buf.readVarInt(),
                buf.readVarInt(),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf)
        );
    }

    public static WorldJournalC2SPayload page(
            int page,
            int pageSize,
            String tab,
            String searchQuery,
            String placeTypeFilter,
            String dimensionFilter,
            String sortMode,
            String sortDirection,
            String selectedPlaceId
    ) {
        return new WorldJournalC2SPayload(
                Action.PAGE,
                page,
                pageSize,
                tab,
                searchQuery,
                placeTypeFilter,
                dimensionFilter,
                sortMode,
                sortDirection,
                selectedPlaceId,
                ""
        );
    }

    public static WorldJournalC2SPayload action(Action action, String placeId, String text) {
        return new WorldJournalC2SPayload(action, 0, 20, "", "", "", "", "", "", placeId, text);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeVarInt(page);
        buf.writeVarInt(pageSize);
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, tab);
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, searchQuery);
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, placeTypeFilter);
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, dimensionFilter);
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, sortMode);
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, sortDirection);
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, placeId);
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, text);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public enum Action {
        OPEN,
        PAGE,
        RENAME,
        DELETE,
        RESTORE_GENERATED,
        TELEPORT,
        CREATE_CUSTOM,
        REFRESH
    }
}
