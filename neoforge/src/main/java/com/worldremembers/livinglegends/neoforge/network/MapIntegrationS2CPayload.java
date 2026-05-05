package com.worldremembers.livinglegends.neoforge.network;

import com.worldremembers.livinglegends.NameRecipe;
import com.worldremembers.livinglegends.PlaceType;
import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import com.worldremembers.livinglegends.map.MapDestinationSettings;
import com.worldremembers.livinglegends.map.MapIntegrationSettings;
import com.worldremembers.livinglegends.map.MapLabelSettings;
import com.worldremembers.livinglegends.map.MapPlaceDescriptor;
import com.worldremembers.livinglegends.visual.PlaceVisualTheme;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record MapIntegrationS2CPayload(
        MapIntegrationSettings settings,
        List<MapPlaceDescriptor> places
) implements CustomPacketPayload {
    public static final Type<MapIntegrationS2CPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WorldRemembersLivingLegends.MOD_ID, "map_integration_s2c")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, MapIntegrationS2CPayload> STREAM_CODEC =
            CustomPacketPayload.codec(MapIntegrationS2CPayload::write, MapIntegrationS2CPayload::new);

    public MapIntegrationS2CPayload {
        settings = settings == null ? MapIntegrationSettings.disabled() : settings;
        places = List.copyOf(places == null ? List.of() : places);
    }

    private MapIntegrationS2CPayload(RegistryFriendlyByteBuf buf) {
        this(readSettings(buf), readPlaces(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buf) {
        writeSettings(buf, settings);
        writePlaces(buf, places);
    }

    private static void writeSettings(RegistryFriendlyByteBuf buf, MapIntegrationSettings settings) {
        MapIntegrationSettings resolved = settings == null ? MapIntegrationSettings.disabled() : settings;
        MapLabelSettings labels = resolved.placeLabels();
        MapDestinationSettings destinations = resolved.destinations();
        buf.writeBoolean(resolved.enabled());
        buf.writeBoolean(resolved.journeyMapEnabled());
        buf.writeBoolean(resolved.xaeroMapEnabled());
        buf.writeBoolean(resolved.ftbChunksMapEnabled());
        buf.writeBoolean(labels.enabled());
        buf.writeBoolean(labels.showGeneralLandmarks());
        buf.writeBoolean(labels.showTooltips());
        buf.writeBoolean(labels.showCoordinatesInTooltip());
        buf.writeBoolean(labels.showDimensionInTooltip());
        buf.writeBoolean(labels.showPlaceTypeInTooltip());
        buf.writeBoolean(destinations.enabled());
        buf.writeBoolean(destinations.onlyOneActiveDestination());
        buf.writeBoolean(destinations.clearWhenEnteringPlaceRadius());
        buf.writeVarInt(destinations.fallbackClearDistanceBlocks());
    }

    private static MapIntegrationSettings readSettings(RegistryFriendlyByteBuf buf) {
        return new MapIntegrationSettings(
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                new MapLabelSettings(
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readBoolean()
                ),
                new MapDestinationSettings(
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        Math.max(1, buf.readVarInt())
                )
        );
    }

    private static void writePlaces(RegistryFriendlyByteBuf buf, List<MapPlaceDescriptor> places) {
        List<MapPlaceDescriptor> resolved = places == null ? List.of() : places;
        buf.writeVarInt(resolved.size());
        for (MapPlaceDescriptor place : resolved) {
            writePlace(buf, place);
        }
    }

    private static List<MapPlaceDescriptor> readPlaces(RegistryFriendlyByteBuf buf) {
        int size = Math.max(0, Math.min(2048, buf.readVarInt()));
        List<MapPlaceDescriptor> places = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            places.add(readPlace(buf));
        }
        return places;
    }

    static MapPlaceDescriptor emptyPlace() {
        return new MapPlaceDescriptor("", "", PlaceType.UNKNOWN, "", 0, 64, 0, 0, false, "", NameRecipe.empty(), "", "");
    }

    static void writePlace(RegistryFriendlyByteBuf buf, MapPlaceDescriptor place) {
        MapPlaceDescriptor resolved = place == null ? emptyPlace() : place;
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, resolved.placeId());
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, resolved.displayName());
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, resolved.placeType().idString());
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, resolved.dimensionId());
        buf.writeInt(resolved.centerX());
        buf.writeInt(resolved.centerY());
        buf.writeInt(resolved.centerZ());
        buf.writeVarInt(resolved.radius());
        buf.writeBoolean(resolved.manualName());
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, resolved.manualNameText());
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeNameRecipe(buf, resolved.nameRecipe());
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, resolved.serverResolvedFallbackName());
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, resolved.tooltip());
        writeTheme(buf, resolved.visualTheme());
    }

    static MapPlaceDescriptor readPlace(RegistryFriendlyByteBuf buf) {
        return new MapPlaceDescriptor(
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                PlaceType.fromId(WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf)),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readNameRecipe(buf),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                readTheme(buf)
        );
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
}
