package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import com.worldremembers.livinglegends.map.MapPlaceDescriptor;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record MapDestinationS2CPayload(
        Action action,
        String placeId,
        MapPlaceDescriptor place
) implements CustomPayload {
    public static final Id<MapDestinationS2CPayload> ID =
            new Id<>(Identifier.of(WorldRemembersLivingLegends.MOD_ID, "map_destination_s2c"));
    public static final PacketCodec<RegistryByteBuf, MapDestinationS2CPayload> CODEC =
            CustomPayload.codecOf(MapDestinationS2CPayload::write, MapDestinationS2CPayload::new);

    private static boolean registered;

    public MapDestinationS2CPayload {
        action = action == null ? Action.REMOVE : action;
        placeId = placeId == null ? "" : placeId;
        place = place == null ? MapIntegrationS2CPayload.emptyPlace() : place;
    }

    private MapDestinationS2CPayload(RegistryByteBuf buf) {
        this(readAction(buf.readString()), buf.readString(), MapIntegrationS2CPayload.readPlace(buf));
    }

    static MapDestinationS2CPayload remove(String placeId) {
        return new MapDestinationS2CPayload(Action.REMOVE, placeId, MapIntegrationS2CPayload.emptyPlace());
    }

    static MapDestinationS2CPayload refresh(MapPlaceDescriptor place) {
        MapPlaceDescriptor resolved = place == null ? MapIntegrationS2CPayload.emptyPlace() : place;
        return new MapDestinationS2CPayload(Action.REFRESH, resolved.placeId(), resolved);
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
        buf.writeString(action.name());
        buf.writeString(placeId);
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
