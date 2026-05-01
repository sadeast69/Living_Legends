package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

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
) implements CustomPayload {
    public static final Id<WorldJournalC2SPayload> ID =
            new Id<>(Identifier.of(WorldRemembersLivingLegends.MOD_ID, "world_journal_c2s"));
    public static final PacketCodec<RegistryByteBuf, WorldJournalC2SPayload> CODEC =
            CustomPayload.codecOf(WorldJournalC2SPayload::write, WorldJournalC2SPayload::new);

    private static boolean registered;

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

    private WorldJournalC2SPayload(RegistryByteBuf buf) {
        this(
                buf.readEnumConstant(Action.class),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readString()
        );
    }

    static synchronized void registerType() {
        if (registered) {
            return;
        }
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
        registered = true;
    }

    static WorldJournalC2SPayload page(
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

    static WorldJournalC2SPayload action(Action action, String placeId, String text) {
        return new WorldJournalC2SPayload(action, 0, 20, "", "", "", "", "", "", placeId, text);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private void write(RegistryByteBuf buf) {
        buf.writeEnumConstant(action);
        buf.writeVarInt(page);
        buf.writeVarInt(pageSize);
        buf.writeString(tab);
        buf.writeString(searchQuery);
        buf.writeString(placeTypeFilter);
        buf.writeString(dimensionFilter);
        buf.writeString(sortMode);
        buf.writeString(sortDirection);
        buf.writeString(placeId);
        buf.writeString(text);
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
