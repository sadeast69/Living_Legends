package com.worldremembers.livinglegends;

import java.io.Serializable;
import java.util.Objects;

public record WorldPos(String dimensionId, int x, int y, int z) implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int CHUNK_SIZE = 16;

    public WorldPos {
        dimensionId = requireId(dimensionId, "dimensionId");
    }

    public int chunkX() {
        return Math.floorDiv(x, CHUNK_SIZE);
    }

    public int chunkZ() {
        return Math.floorDiv(z, CHUNK_SIZE);
    }

    public boolean sameDimension(WorldPos other) {
        return other != null && dimensionId.equals(other.dimensionId);
    }

    public long squaredDistanceTo(WorldPos other) {
        Objects.requireNonNull(other, "other");

        if (!sameDimension(other)) {
            return Long.MAX_VALUE;
        }

        long dx = (long) x - other.x;
        long dy = (long) y - other.y;
        long dz = (long) z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public String blockIdString() {
        return dimensionId + "@" + x + "," + y + "," + z;
    }

    public String chunkIdString() {
        return chunkIdString(dimensionId, chunkX(), chunkZ());
    }

    public static String chunkIdString(String dimensionId, int chunkX, int chunkZ) {
        return requireId(dimensionId, "dimensionId") + "@chunk:" + chunkX + "," + chunkZ;
    }

    static String requireId(String id, String fieldName) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return id.trim();
    }

    static String optionalId(String id) {
        if (id == null || id.isBlank()) {
            return "";
        }

        return id.trim();
    }
}
