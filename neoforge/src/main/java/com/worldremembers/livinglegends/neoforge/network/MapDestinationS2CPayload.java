package com.worldremembers.livinglegends.neoforge.network;

import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import com.worldremembers.livinglegends.map.MapPlaceDescriptor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MapDestinationS2CPayload(
        Action action,
        String placeId,
        MapPlaceDescriptor place
) implements CustomPacketPayload {
    public static final Type<MapDestinationS2CPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WorldRemembersLivingLegends.MOD_ID, "map_destination_s2c")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, MapDestinationS2CPayload> STREAM_CODEC =
            CustomPacketPayload.codec(MapDestinationS2CPayload::write, MapDestinationS2CPayload::new);

    public MapDestinationS2CPayload {
        action = action == null ? Action.REMOVE : action;
        placeId = placeId == null ? "" : placeId;
        place = place == null ? MapIntegrationS2CPayload.emptyPlace() : place;
    }

    private MapDestinationS2CPayload(RegistryFriendlyByteBuf buf) {
        this(readAction(WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf)),
                WorldRemembersLivingLegendsNeoForgeNetworkCodecs.readString(buf),
                MapIntegrationS2CPayload.readPlace(buf));
    }

    public static MapDestinationS2CPayload remove(String placeId) {
        return new MapDestinationS2CPayload(Action.REMOVE, placeId, MapIntegrationS2CPayload.emptyPlace());
    }

    public static MapDestinationS2CPayload refresh(MapPlaceDescriptor place) {
        MapPlaceDescriptor resolved = place == null ? MapIntegrationS2CPayload.emptyPlace() : place;
        return new MapDestinationS2CPayload(Action.REFRESH, resolved.placeId(), resolved);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buf) {
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, action.name());
        WorldRemembersLivingLegendsNeoForgeNetworkCodecs.writeString(buf, placeId);
        MapIntegrationS2CPayload.writePlace(buf, place);
    }

    private static Action readAction(String value) {
        if (value != null) {
            for (Action action : Action.values()) {
                if (action.name().equalsIgnoreCase(value.trim())) {
                    return action;
                }
            }
        }
        return Action.REMOVE;
    }

    public enum Action {
        REMOVE,
        REFRESH
    }
}
