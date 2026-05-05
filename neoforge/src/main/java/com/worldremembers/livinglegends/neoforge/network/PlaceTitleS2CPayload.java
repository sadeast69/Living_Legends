package com.worldremembers.livinglegends.neoforge.network;

import com.worldremembers.livinglegends.NameRecipe;
import com.worldremembers.livinglegends.PlaceType;
import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import com.worldremembers.livinglegends.visual.PlaceVisualTheme;
import com.worldremembers.livinglegends.visual.PlaceVisualThemeResolver;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

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
) implements CustomPacketPayload {
    public static final Type<PlaceTitleS2CPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WorldRemembersLivingLegends.MOD_ID, "place_title")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, PlaceTitleS2CPayload> STREAM_CODEC =
            CustomPacketPayload.codec(PlaceTitleS2CPayload::write, PlaceTitleS2CPayload::new);

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

    private PlaceTitleS2CPayload(RegistryFriendlyByteBuf buf) {
        this(
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                buf.readBoolean(),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readNameRecipe(buf),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readEnum(Reason.class),
                readTheme(buf)
        );
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
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buf) {
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, placeId);
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, placeType);
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, nameStyle);
        buf.writeBoolean(manualName);
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, manualNameText);
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeNameRecipe(buf, nameRecipe);
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, serverResolvedFallbackName);
        buf.writeInt(centerX);
        buf.writeInt(centerY);
        buf.writeInt(centerZ);
        buf.writeInt(radius);
        buf.writeEnum(reason);
        writeTheme(buf, visualTheme);
    }

    private static void writeTheme(RegistryFriendlyByteBuf buf, PlaceVisualTheme theme) {
        PlaceVisualTheme resolved = theme == null ? PlaceVisualTheme.DEFAULT : theme;
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, resolved.toneKey());
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, resolved.mapLabelStyleKey());
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, resolved.titleOverlayStyleKey());
        buf.writeInt(resolved.mainColor());
        buf.writeInt(resolved.secondaryColor());
        buf.writeInt(resolved.accentColor());
        buf.writeInt(resolved.lineColor());
        buf.writeInt(resolved.outlineColor());
        buf.writeInt(resolved.shadowColor());
        buf.writeInt(resolved.tooltipAccentColor());
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, resolved.glyph());
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, resolved.glyphPlacement());
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, resolved.lineStyle());
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, resolved.labelWeight());
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, resolved.curveStyle());
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, resolved.shadowStyle());
        buf.writeVarInt(resolved.emphasis());
    }

    private static PlaceVisualTheme readTheme(RegistryFriendlyByteBuf buf) {
        return new PlaceVisualTheme(
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                buf.readVarInt()
        );
    }

    public enum Reason {
        ENTERED,
        CREATED,
        DEBUG,
        CLEAR
    }
}
