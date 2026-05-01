package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.NameRecipe;
import com.worldremembers.livinglegends.NameTokenForm;
import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

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
) implements CustomPayload {
    public static final Id<WorldJournalS2CPayload> ID =
            new Id<>(Identifier.of(WorldRemembersLivingLegends.MOD_ID, "world_journal_s2c"));
    public static final PacketCodec<RegistryByteBuf, WorldJournalS2CPayload> CODEC =
            CustomPayload.codecOf(WorldJournalS2CPayload::write, WorldJournalS2CPayload::new);

    private static boolean registered;

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

    private WorldJournalS2CPayload(RegistryByteBuf buf) {
        this(
                buf.readEnumConstant(Action.class),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                readEntries(buf),
                buf.readString(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readString(),
                buf.readString()
        );
    }

    static synchronized void registerType() {
        if (registered) {
            return;
        }
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
        registered = true;
    }

    static WorldJournalS2CPayload open(int pageSize, boolean canManage, boolean canTeleport, boolean showExactCoordinates) {
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

    static WorldJournalS2CPayload error(String messageKey, String messageText) {
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
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private void write(RegistryByteBuf buf) {
        buf.writeEnumConstant(action);
        buf.writeVarInt(totalCount);
        buf.writeVarInt(page);
        buf.writeVarInt(pageSize);
        writeEntries(buf, places);
        buf.writeString(selectedPlaceId);
        buf.writeBoolean(canManage);
        buf.writeBoolean(canTeleport);
        buf.writeBoolean(showExactCoordinates);
        buf.writeString(messageKey);
        buf.writeString(messageText);
    }

    private static void writeEntries(RegistryByteBuf buf, List<Entry> entries) {
        List<Entry> resolved = entries == null ? List.of() : entries;
        buf.writeVarInt(resolved.size());
        for (Entry entry : resolved) {
            entry.write(buf);
        }
    }

    private static List<Entry> readEntries(RegistryByteBuf buf) {
        int size = Math.max(0, Math.min(128, buf.readVarInt()));
        List<Entry> entries = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            entries.add(new Entry(buf));
        }
        return entries;
    }

    private static void writeRecipe(RegistryByteBuf buf, NameRecipe recipe) {
        NameRecipe resolved = recipe == null ? NameRecipe.empty() : recipe;
        buf.writeString(resolved.styleId());
        buf.writeString(resolved.patternKey());
        writeStringList(buf, resolved.selectedTokenIds());
        List<String> forms = new ArrayList<>();
        for (NameTokenForm form : resolved.requestedTokenForms()) {
            forms.add((form == null ? NameTokenForm.BASE : form).idString());
        }
        writeStringList(buf, forms);
        buf.writeLong(resolved.seed());
        buf.writeString(resolved.fallbackResolvedName());
    }

    private static NameRecipe readRecipe(RegistryByteBuf buf) {
        String styleId = buf.readString();
        String patternKey = buf.readString();
        List<String> tokenIds = readStringList(buf);
        List<String> formIds = readStringList(buf);
        List<NameTokenForm> forms = new ArrayList<>();
        for (String formId : formIds) {
            forms.add(NameTokenForm.fromId(formId));
        }
        long seed = buf.readLong();
        String fallback = buf.readString();
        return new NameRecipe(styleId, patternKey, tokenIds, forms, seed, fallback);
    }

    private static void writeStringList(RegistryByteBuf buf, List<String> values) {
        List<String> resolved = values == null ? List.of() : values;
        buf.writeVarInt(resolved.size());
        for (String value : resolved) {
            buf.writeString(value == null ? "" : value);
        }
    }

    private static List<String> readStringList(RegistryByteBuf buf) {
        int size = Math.max(0, Math.min(64, buf.readVarInt()));
        List<String> result = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            result.add(buf.readString());
        }
        return result;
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

        private Entry(RegistryByteBuf buf) {
            this(
                    buf.readString(),
                    buf.readString(),
                    buf.readString(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readLong(),
                    buf.readLong(),
                    buf.readDouble(),
                    buf.readString(),
                    buf.readBoolean(),
                    buf.readString(),
                    readRecipe(buf),
                    buf.readString()
            );
        }

        private void write(RegistryByteBuf buf) {
            buf.writeString(placeId);
            buf.writeString(placeType);
            buf.writeString(dimension);
            buf.writeInt(centerX);
            buf.writeInt(centerY);
            buf.writeInt(centerZ);
            buf.writeInt(radius);
            buf.writeLong(createdGameTime);
            buf.writeLong(updatedGameTime);
            buf.writeDouble(distanceToPlayer);
            buf.writeString(nameStyle);
            buf.writeBoolean(manualName);
            buf.writeString(manualNameText);
            writeRecipe(buf, nameRecipe);
            buf.writeString(serverResolvedFallbackName);
        }
    }
}
