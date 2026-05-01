package com.worldremembers.livinglegends;

import java.io.Serializable;

public record PlayerPlaceVisitState(
        String playerId,
        String placeId,
        long firstVisitGameTime,
        long lastVisitGameTime,
        long lastKnownInsideGameTime,
        int visitCount,
        boolean currentlyInside
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public PlayerPlaceVisitState {
        playerId = WorldPos.requireId(playerId, "playerId");
        placeId = WorldPos.requireId(placeId, "placeId");
        firstVisitGameTime = Math.max(0L, firstVisitGameTime);
        lastVisitGameTime = Math.max(firstVisitGameTime, lastVisitGameTime);
        lastKnownInsideGameTime = Math.max(0L, lastKnownInsideGameTime);
        visitCount = Math.max(0, visitCount);
    }

    public static PlayerPlaceVisitState unvisited(String playerId, String placeId) {
        return new PlayerPlaceVisitState(playerId, placeId, 0L, 0L, 0L, 0, false);
    }

    public PlayerPlaceVisitState recordEnter(long gameTime) {
        long normalizedTime = Math.max(0L, gameTime);
        long firstVisit = firstVisitGameTime == 0L ? normalizedTime : Math.min(firstVisitGameTime, normalizedTime);

        return new PlayerPlaceVisitState(
                playerId,
                placeId,
                firstVisit,
                normalizedTime,
                normalizedTime,
                visitCount + 1,
                true
        );
    }

    public PlayerPlaceVisitState recordInside(long gameTime) {
        long normalizedTime = Math.max(0L, gameTime);

        return new PlayerPlaceVisitState(
                playerId,
                placeId,
                firstVisitGameTime,
                Math.max(lastVisitGameTime, normalizedTime),
                normalizedTime,
                visitCount,
                true
        );
    }

    public PlayerPlaceVisitState recordExit(long gameTime) {
        long normalizedTime = Math.max(0L, gameTime);

        return new PlayerPlaceVisitState(
                playerId,
                placeId,
                firstVisitGameTime,
                Math.max(lastVisitGameTime, normalizedTime),
                Math.max(lastKnownInsideGameTime, normalizedTime),
                visitCount,
                false
        );
    }

    public double basicScore() {
        return visitCount + (currentlyInside ? 0.5 : 0.0);
    }

    public String playerIdString() {
        return playerId;
    }

    public String placeIdString() {
        return placeId;
    }
}
