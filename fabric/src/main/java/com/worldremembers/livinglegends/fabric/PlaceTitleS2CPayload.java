package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.NameRecipe;
import com.worldremembers.livinglegends.NameTokenForm;
import com.worldremembers.livinglegends.PlaceType;
import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import com.worldremembers.livinglegends.visual.PlaceVisualTheme;
import com.worldremembers.livinglegends.visual.PlaceVisualThemeResolver;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record PlaceTitleS2CPayload(
        String placeId,
        String placeType,
        String nameStyle,
        boolean manualName,
        String manualNameText,
        NameRecipe nameRecipe,
        String serverResolvedFallbackName,
        int centerX,
        int centerY,
        int centerZ,
        int radius,
        Reason reason,
        PlaceVisualTheme visualTheme
) implements CustomPayload {
    public static final Id<PlaceTitleS2CPayload> ID =
            new Id<>(Identifier.of(WorldRemembersLivingLegends.MOD_ID, "place_title"));
    public static final PacketCodec<RegistryByteBuf, PlaceTitleS2CPayload> CODEC =
            CustomPayload.codecOf(PlaceTitleS2CPayload::write, PlaceTitleS2CPayload::new);

    private static boolean registered;

    public PlaceTitleS2CPayload {
        placeId = placeId == null ? "" : placeId;
        placeType = placeType == null ? "custom" : placeType;
        nameStyle = nameStyle == null || nameStyle.isBlank() ? "vanilla_adventure" : nameStyle;
        manualNameText = manualNameText == null ? "" : manualNameText;
        nameRecipe = nameRecipe == null ? NameRecipe.empty() : nameRecipe;
        serverResolvedFallbackName = serverResolvedFallbackName == null ? "" : serverResolvedFallbackName;
        reason = reason == null ? Reason.DEBUG : reason;
        visualTheme = visualTheme == null
                ? PlaceVisualThemeResolver.resolve(PlaceType.fromId(placeType), "", "", "")
                : visualTheme;
    }

    public PlaceTitleS2CPayload(
            String placeId,
            String placeType,
            String nameStyle,
            boolean manualName,
            String manualNameText,
            NameRecipe nameRecipe,
            String serverResolvedFallbackName,
            int centerX,
            int centerY,
            int centerZ,
            int radius,
            Reason reason
    ) {
        this(
                placeId,
                placeType,
                nameStyle,
                manualName,
                manualNameText,
                nameRecipe,
                serverResolvedFallbackName,
                centerX,
                centerY,
                centerZ,
                radius,
                reason,
                PlaceVisualThemeResolver.resolve(PlaceType.fromId(placeType), "", "", "")
        );
    }

    public PlaceTitleS2CPayload(RegistryByteBuf buf) {
        this(
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readBoolean(),
                buf.readString(),
                readRecipe(buf),
                buf.readString(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readEnumConstant(Reason.class),
                readTheme(buf)
        );
    }

    public static synchronized void registerType() {
        if (registered) {
            return;
        }
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
        registered = true;
    }

    public static PlaceTitleS2CPayload clear() {
        return new PlaceTitleS2CPayload(
                "",
                "custom",
                "vanilla_adventure",
                true,
                "",
                NameRecipe.empty(),
                "",
                0,
                0,
                0,
                0,
                Reason.CLEAR,
                PlaceVisualTheme.DEFAULT
        );
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private void write(RegistryByteBuf buf) {
        buf.writeString(placeId);
        buf.writeString(placeType);
        buf.writeString(nameStyle);
        buf.writeBoolean(manualName);
        buf.writeString(manualNameText);
        writeRecipe(buf, nameRecipe);
        buf.writeString(serverResolvedFallbackName);
        buf.writeInt(centerX);
        buf.writeInt(centerY);
        buf.writeInt(centerZ);
        buf.writeInt(radius);
        buf.writeEnumConstant(reason);
        writeTheme(buf, visualTheme);
    }

    private static void writeTheme(RegistryByteBuf buf, PlaceVisualTheme theme) {
        PlaceVisualTheme resolved = theme == null ? PlaceVisualTheme.DEFAULT : theme;
        buf.writeString(resolved.toneKey());
        buf.writeString(resolved.mapLabelStyleKey());
        buf.writeString(resolved.titleOverlayStyleKey());
        buf.writeInt(resolved.mainColor());
        buf.writeInt(resolved.secondaryColor());
        buf.writeInt(resolved.accentColor());
        buf.writeInt(resolved.lineColor());
        buf.writeInt(resolved.outlineColor());
        buf.writeInt(resolved.shadowColor());
        buf.writeInt(resolved.tooltipAccentColor());
        buf.writeString(resolved.glyph());
        buf.writeString(resolved.glyphPlacement());
        buf.writeString(resolved.lineStyle());
        buf.writeString(resolved.labelWeight());
        buf.writeString(resolved.curveStyle());
        buf.writeString(resolved.shadowStyle());
        buf.writeVarInt(resolved.emphasis());
    }

    private static PlaceVisualTheme readTheme(RegistryByteBuf buf) {
        return new PlaceVisualTheme(
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readVarInt()
        );
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

    public enum Reason {
        ENTERED,
        CREATED,
        DEBUG,
        CLEAR
    }
}
