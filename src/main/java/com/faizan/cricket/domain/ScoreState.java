package com.faizan.cricket.domain;

/** Immutable innings state. Everything shown to users is derived from this. */
public record ScoreState(int runs, int wickets, int legalBalls, int extras) {

    public static final ScoreState ZERO = new ScoreState(0, 0, 0, 0);

    public String overs() {
        return (legalBalls / ScoreCalculator.BALLS_PER_OVER) + "." + (legalBalls % ScoreCalculator.BALLS_PER_OVER);
    }

    public boolean isInningsOver(int oversLimit) {
        return wickets >= ScoreCalculator.MAX_WICKETS
                || legalBalls >= oversLimit * ScoreCalculator.BALLS_PER_OVER;
    }

    public double runRate() {
        if (legalBalls == 0) return 0.0;
        return Math.round(runs * 6.0 / legalBalls * 100.0) / 100.0;
    }
}
