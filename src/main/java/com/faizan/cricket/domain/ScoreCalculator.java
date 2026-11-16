package com.faizan.cricket.domain;

/**
 * Pure cricket rules. No framework, no I/O: given a state and a ball event it returns
 * the next state or throws InvalidEventException. Easy to unit test and to replay.
 */
public final class ScoreCalculator {

    public static final int BALLS_PER_OVER = 6;
    public static final int MAX_WICKETS = 10;

    private ScoreCalculator() {}

    public static ScoreState apply(ScoreState s, EventType type, int runs, int oversLimit) {
        if (s.wickets() >= MAX_WICKETS) {
            throw new InvalidEventException("Innings is over: team is all out");
        }
        if (s.legalBalls() >= oversLimit * BALLS_PER_OVER) {
            throw new InvalidEventException("Innings is over: all overs bowled");
        }
        validateRuns(type, runs);

        return switch (type) {
            case RUNS -> new ScoreState(s.runs() + runs, s.wickets(), s.legalBalls() + 1, s.extras());
            case WICKET -> new ScoreState(s.runs() + runs, s.wickets() + 1, s.legalBalls() + 1, s.extras());
            case WIDE -> new ScoreState(s.runs() + 1 + runs, s.wickets(), s.legalBalls(), s.extras() + 1 + runs);
            case NO_BALL -> new ScoreState(s.runs() + 1 + runs, s.wickets(), s.legalBalls(), s.extras() + 1);
            case BYE, LEG_BYE -> new ScoreState(s.runs() + runs, s.wickets(), s.legalBalls() + 1, s.extras() + runs);
        };
    }

    private static void validateRuns(EventType type, int runs) {
        int min;
        int max;
        switch (type) {
            case RUNS, NO_BALL -> { min = 0; max = 6; }
            case WICKET -> { min = 0; max = 3; }
            case WIDE -> { min = 0; max = 4; }
            case BYE, LEG_BYE -> { min = 1; max = 4; }
            default -> throw new IllegalStateException("Unhandled type " + type);
        }
        if (runs < min || runs > max) {
            throw new InvalidEventException(type + " must have between " + min + " and " + max + " runs, got " + runs);
        }
    }
}
