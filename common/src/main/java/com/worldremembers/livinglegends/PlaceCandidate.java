package com.worldremembers.livinglegends;

import java.io.Serializable;

public record PlaceCandidate(
        String candidateId,
        PlaceType placeType,
        String sourceChunkId,
        PlaceBounds bounds,
        double score,
        double threshold,
        long activityCount,
        PlaceRarity rarity,
        String reason
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public PlaceCandidate {
        candidateId = WorldPos.requireId(candidateId, "candidateId");
        placeType = placeType == null ? PlaceType.UNKNOWN : placeType;
        sourceChunkId = WorldPos.requireId(sourceChunkId, "sourceChunkId");
        bounds = java.util.Objects.requireNonNull(bounds, "bounds");
        score = Math.max(0.0, score);
        threshold = Math.max(0.0, threshold);
        activityCount = Math.max(0L, activityCount);
        rarity = rarity == null ? PlaceRarity.fromScore(score) : rarity;
        reason = reason == null ? "" : reason;
    }
}
