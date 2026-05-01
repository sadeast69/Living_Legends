package com.worldremembers.livinglegends;

import java.io.Serializable;
import java.util.Locale;

public record PlaceBounds(
        String dimensionId,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ,
        Shape shape,
        int centerX,
        int centerY,
        int centerZ,
        int radius
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public PlaceBounds(
            String dimensionId,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ
    ) {
        this(
                dimensionId,
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ,
                Shape.BOX,
                center(minX, maxX),
                center(minY, maxY),
                center(minZ, maxZ),
                horizontalRadius(minX, minZ, maxX, maxZ)
        );
    }

    public PlaceBounds {
        dimensionId = WorldPos.requireId(dimensionId, "dimensionId");

        int normalizedMinX = Math.min(minX, maxX);
        int normalizedMinY = Math.min(minY, maxY);
        int normalizedMinZ = Math.min(minZ, maxZ);
        int normalizedMaxX = Math.max(minX, maxX);
        int normalizedMaxY = Math.max(minY, maxY);
        int normalizedMaxZ = Math.max(minZ, maxZ);

        minX = normalizedMinX;
        minY = normalizedMinY;
        minZ = normalizedMinZ;
        maxX = normalizedMaxX;
        maxY = normalizedMaxY;
        maxZ = normalizedMaxZ;
        shape = shape == null ? Shape.BOX : shape;

        if (centerX < minX || centerX > maxX) {
            centerX = center(minX, maxX);
        }
        if (centerY < minY || centerY > maxY) {
            centerY = center(minY, maxY);
        }
        if (centerZ < minZ || centerZ > maxZ) {
            centerZ = center(minZ, maxZ);
        }

        radius = Math.max(0, radius);
        if (radius == 0 && shape != Shape.BOX) {
            radius = horizontalRadius(minX, minZ, maxX, maxZ);
        }
    }

    public static PlaceBounds around(WorldPos center, int horizontalRadius, int verticalRadius) {
        int horizontal = Math.max(0, horizontalRadius);
        int vertical = Math.max(0, verticalRadius);

        return new PlaceBounds(
                center.dimensionId(),
                subtractClamped(center.x(), horizontal),
                subtractClamped(center.y(), vertical),
                subtractClamped(center.z(), horizontal),
                addClamped(center.x(), horizontal),
                addClamped(center.y(), vertical),
                addClamped(center.z(), horizontal),
                Shape.CYLINDER,
                center.x(),
                center.y(),
                center.z(),
                horizontal
        );
    }

    public static PlaceBounds structureBounds(
            String dimensionId,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ
    ) {
        return new PlaceBounds(
                dimensionId,
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ,
                Shape.STRUCTURE_BOUNDS,
                center(minX, maxX),
                center(minY, maxY),
                center(minZ, maxZ),
                horizontalRadius(minX, minZ, maxX, maxZ)
        );
    }

    public boolean contains(WorldPos position) {
        if (position == null || !dimensionId.equals(position.dimensionId())) {
            return false;
        }

        return switch (shape) {
            case SPHERE -> squaredDistance(position, center()) <= (long) radius * radius;
            case CYLINDER -> horizontalDistanceSquared(position, center()) <= (long) radius * radius
                    && position.y() >= minY
                    && position.y() <= maxY;
            case BOX, STRUCTURE_BOUNDS -> position.x() >= minX
                    && position.x() <= maxX
                    && position.y() >= minY
                    && position.y() <= maxY
                    && position.z() >= minZ
                    && position.z() <= maxZ;
        };
    }

    public WorldPos center() {
        return new WorldPos(dimensionId, centerX, centerY, centerZ);
    }

    public PlaceBounds expanded(int horizontalRadius, int verticalRadius) {
        int horizontal = Math.max(0, horizontalRadius);
        int vertical = Math.max(0, verticalRadius);

        return new PlaceBounds(
                dimensionId,
                subtractClamped(minX, horizontal),
                subtractClamped(minY, vertical),
                subtractClamped(minZ, horizontal),
                addClamped(maxX, horizontal),
                addClamped(maxY, vertical),
                addClamped(maxZ, horizontal),
                shape,
                centerX,
                centerY,
                centerZ,
                addClamped(radius, horizontal)
        );
    }

    public String boundsIdString() {
        return dimensionId
                + "@bounds:"
                + shape.idString()
                + ":"
                + minX + "," + minY + "," + minZ
                + ".."
                + maxX + "," + maxY + "," + maxZ;
    }

    public boolean isStructureBounds() {
        return shape == Shape.STRUCTURE_BOUNDS;
    }

    private static long squaredDistance(WorldPos first, WorldPos second) {
        long dx = (long) first.x() - second.x();
        long dy = (long) first.y() - second.y();
        long dz = (long) first.z() - second.z();
        return dx * dx + dy * dy + dz * dz;
    }

    private static long horizontalDistanceSquared(WorldPos first, WorldPos second) {
        long dx = (long) first.x() - second.x();
        long dz = (long) first.z() - second.z();
        return dx * dx + dz * dz;
    }

    private static int center(int min, int max) {
        return Math.min(min, max) + Math.floorDiv(Math.abs(max - min), 2);
    }

    private static int horizontalRadius(int minX, int minZ, int maxX, int maxZ) {
        int centerX = center(minX, maxX);
        int centerZ = center(minZ, maxZ);
        return Math.max(
                Math.max(Math.abs(centerX - minX), Math.abs(maxX - centerX)),
                Math.max(Math.abs(centerZ - minZ), Math.abs(maxZ - centerZ))
        );
    }

    private static int addClamped(int value, int amount) {
        long result = (long) value + amount;
        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }

    private static int subtractClamped(int value, int amount) {
        long result = (long) value - amount;
        return result < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) result;
    }

    public enum Shape {
        SPHERE("sphere"),
        CYLINDER("cylinder"),
        BOX("box"),
        STRUCTURE_BOUNDS("structure_bounds");

        private final String id;

        Shape(String id) {
            this.id = id;
        }

        public String idString() {
            return id;
        }

        public static Shape fromId(String id) {
            if (id == null || id.isBlank()) {
                return BOX;
            }

            String normalized = id.trim().toLowerCase(Locale.ROOT);
            for (Shape shape : values()) {
                if (shape.id.equals(normalized) || shape.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                    return shape;
                }
            }
            return BOX;
        }
    }
}
