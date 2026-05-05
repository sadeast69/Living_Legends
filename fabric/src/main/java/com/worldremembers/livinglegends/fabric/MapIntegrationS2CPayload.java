package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.NameRecipe;
import com.worldremembers.livinglegends.NameTokenForm;
import com.worldremembers.livinglegends.PlaceType;
import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import com.worldremembers.livinglegends.map.MapDestinationSettings;
import com.worldremembers.livinglegends.map.MapIntegrationSettings;
import com.worldremembers.livinglegends.map.MapLabelSettings;
import com.worldremembers.livinglegends.map.MapPlaceDescriptor;
import com.worldremembers.livinglegends.visual.PlaceVisualTheme;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record MapIntegrationS2CPayload(
        MapIntegrationSettings settings,
        List<MapPlaceDescriptor> places
) implements CustomPayload {
    public static final Id<MapIntegrationS2CPayload> ID =
            new Id<>(Identifier.of(WorldRemembersLivingLegends.MOD_ID, "map_integration_s2c"));
    public static final PacketCodec<RegistryByteBuf, MapIntegrationS2CPayload> CODEC =
            CustomPayload.codecOf(MapIntegrationS2CPayload::write, MapIntegrationS2CPayload::new);

    private static boolean registered;

    public MapIntegrationS2CPayload {
        settings = settings == null ? MapIntegrationSettings.disabled() : settings;
        places = List.copyOf(places == null ? List.of() : places);
    }

    private MapIntegrationS2CPayload(RegistryByteBuf buf) {
        this(readSettings(buf), readPlaces(buf));
    }

    static synchronized void registerType() {
        if (registered) {
            return;
        }
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
        registered = true;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private void write(RegistryByteBuf buf) {
        writeSettings(buf, settings);
        writePlaces(buf, places);
    }

    private static void writeSettings(RegistryByteBuf buf, MapIntegrationSettings settings) {
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

    private static MapIntegrationSettings readSettings(RegistryByteBuf buf) {
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

    private static void writePlaces(RegistryByteBuf buf, List<MapPlaceDescriptor> places) {
        List<MapPlaceDescriptor> resolved = places == null ? List.of() : places;
        buf.writeVarInt(resolved.size());
        for (MapPlaceDescriptor place : resolved) {
            writePlace(buf, place);
        }
    }

    private static List<MapPlaceDescriptor> readPlaces(RegistryByteBuf buf) {
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

    static void writePlace(RegistryByteBuf buf, MapPlaceDescriptor place) {
        MapPlaceDescriptor resolved = place == null ? emptyPlace() : place;
        buf.writeString(resolved.placeId());
        buf.writeString(resolved.displayName());
        buf.writeString(resolved.placeType().idString());
        buf.writeString(resolved.dimensionId());
        buf.writeInt(resolved.centerX());
        buf.writeInt(resolved.centerY());
        buf.writeInt(resolved.centerZ());
        buf.writeVarInt(resolved.radius());
        buf.writeBoolean(resolved.manualName());
        buf.writeString(resolved.manualNameText());
        writeRecipe(buf, resolved.nameRecipe());
        buf.writeString(resolved.serverResolvedFallbackName());
        buf.writeString(resolved.tooltip());
        writeTheme(buf, resolved.visualTheme());
    }

    static MapPlaceDescriptor readPlace(RegistryByteBuf buf) {
        return new MapPlaceDescriptor(
                buf.readString(),
                buf.readString(),
                PlaceType.fromId(buf.readString()),
                buf.readString(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readString(),
                readRecipe(buf),
                buf.readString(),
                buf.readString(),
                readTheme(buf)
        );
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
}
