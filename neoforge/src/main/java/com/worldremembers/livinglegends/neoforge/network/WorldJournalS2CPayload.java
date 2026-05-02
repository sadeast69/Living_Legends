package com.worldremembers.livinglegends.neoforge.network;

import com.worldremembers.livinglegends.NameRecipe;
import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record WorldJournalS2CPayload(
        Action action,
        int totalCount,
        int page,
        int pageSize,
        List<Entry> places,
        String selectedPlaceId,
        boolean canManage,
        boolean canTeleport,
        boolean showExactCoordinates,
        String messageKey,
        String messageText
) implements CustomPacketPayload {
    public static final Type<WorldJournalS2CPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WorldRemembersLivingLegends.MOD_ID, "world_journal_s2c")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, WorldJournalS2CPayload> STREAM_CODEC =
            CustomPacketPayload.codec(WorldJournalS2CPayload::write, WorldJournalS2CPayload::new);

    public WorldJournalS2CPayload {
        action = action == null ? Action.PAGE : action;
        totalCount = Math.max(0, totalCount);
        page = Math.max(0, page);
        pageSize = Math.max(1, pageSize);
        places = List.copyOf(places == null ? List.of() : places);
        selectedPlaceId = clean(selectedPlaceId);
        messageKey = clean(messageKey);
        messageText = clean(messageText);
    }

    private WorldJournalS2CPayload(RegistryFriendlyByteBuf buf) {
        this(
                buf.readEnum(Action.class),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                readEntries(buf),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf)
        );
    }

    public static WorldJournalS2CPayload open(
            int pageSize,
            boolean canManage,
            boolean canTeleport,
            boolean showExactCoordinates
    ) {
        return new WorldJournalS2CPayload(
                Action.OPEN,
                0,
                0,
                pageSize,
                List.of(),
                "",
                canManage,
                canTeleport,
                showExactCoordinates,
                "",
                ""
        );
    }

    public static WorldJournalS2CPayload error(String messageKey, String messageText) {
        return new WorldJournalS2CPayload(
                Action.ERROR,
                0,
                0,
                20,
                List.of(),
                "",
                false,
                false,
                true,
                messageKey,
                messageText
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeVarInt(totalCount);
        buf.writeVarInt(page);
        buf.writeVarInt(pageSize);
        writeEntries(buf, places);
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, selectedPlaceId);
        buf.writeBoolean(canManage);
        buf.writeBoolean(canTeleport);
        buf.writeBoolean(showExactCoordinates);
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, messageKey);
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, messageText);
    }

    private static void writeEntries(RegistryFriendlyByteBuf buf, List<Entry> entries) {
        List<Entry> resolved = entries == null ? List.of() : entries;
        buf.writeVarInt(resolved.size());
        for (Entry entry : resolved) {
            entry.write(buf);
        }
    }

    private static List<Entry> readEntries(RegistryFriendlyByteBuf buf) {
        int size = Math.max(0, Math.min(WorldRemembersLivingLegendsNeoForgeNetworkCodecs.MAX_JOURNAL_ENTRY_COUNT, buf.readVarInt()));
        List<Entry> entries = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            entries.add(new Entry(buf));
        }
        return entries;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public enum Action {
        OPEN,
        PAGE,
        UPDATED,
        DELETED,
        ERROR
    }

    public record Entry(
            String placeId,
            String placeType,
            String dimension,
            int centerX,
            int centerY,
            int centerZ,
            int radius,
            long createdGameTime,
            long updatedGameTime,
            double distanceToPlayer,
            String nameStyle,
            boolean manualName,
            String manualNameText,
            NameRecipe nameRecipe,
            String serverResolvedFallbackName
    ) {
        public Entry {
            placeId = clean(placeId);
            placeType = clean(placeType);
            dimension = clean(dimension);
            radius = Math.max(0, radius);
            createdGameTime = Math.max(0L, createdGameTime);
            updatedGameTime = Math.max(0L, updatedGameTime);
            distanceToPlayer = Double.isFinite(distanceToPlayer) ? Math.max(-1.0, distanceToPlayer) : -1.0;
            nameStyle = nameStyle == null || nameStyle.isBlank() ? "vanilla_adventure" : nameStyle.trim();
            manualNameText = clean(manualNameText);
            nameRecipe = nameRecipe == null ? NameRecipe.empty() : nameRecipe;
            serverResolvedFallbackName = clean(serverResolvedFallbackName);
        }

        private Entry(RegistryFriendlyByteBuf buf) {
            this(
                    WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                    WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                    WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readLong(),
                    buf.readLong(),
                    buf.readDouble(),
                    WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                    buf.readBoolean(),
                    WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                    WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readNameRecipe(buf),
                    WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf)
            );
        }

        private void write(RegistryFriendlyByteBuf buf) {
            WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, placeId);
            WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, placeType);
            WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, dimension);
            buf.writeInt(centerX);
            buf.writeInt(centerY);
            buf.writeInt(centerZ);
            buf.writeInt(radius);
            buf.writeLong(createdGameTime);
            buf.writeLong(updatedGameTime);
            buf.writeDouble(distanceToPlayer);
            WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, nameStyle);
            buf.writeBoolean(manualName);
            WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, manualNameText);
            WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeNameRecipe(buf, nameRecipe);
            WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, serverResolvedFallbackName);
        }
    }
}
