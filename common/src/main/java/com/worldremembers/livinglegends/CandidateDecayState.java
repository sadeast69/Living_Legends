package com.worldremembers.livinglegends;

import java.io.Serializable;

public record CandidateDecayState(
        double score,
        long lastRelevantEventGameTime,
        long lastDecayGameTime
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public CandidateDecayState {
        score = Math.max(0.0, score);
        lastRelevantEventGameTime = Math.max(0L, lastRelevantEventGameTime);
        lastDecayGameTime = Math.max(0L, lastDecayGameTime);
    }

    public CandidateDecayState refreshed(double freshScore, long gameTime) {
        long refreshedTime = Math.max(lastRelevantEventGameTime, Math.max(0L, gameTime));
        return new CandidateDecayState(
                Math.max(score, Math.max(0.0, freshScore)),
                refreshedTime,
                lastDecayGameTime
        );
    }

    public CandidateDecayState withDecay(double decayedScore, long decayGameTime) {
        return new CandidateDecayState(decayedScore, lastRelevantEventGameTime, decayGameTime);
    }
}
