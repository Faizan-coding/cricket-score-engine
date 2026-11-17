package com.faizan.cricket.domain;

/**
 * Pure two-innings rules: innings change-over, chase target, and the result.
 * No framework and no I/O, so it is easy to unit test and to replay from the event log.
 */
public final class MatchEngine {

    private MatchEngine() {}

    public static MatchProgress apply(MatchProgress p, EventType type, int runs, int oversLimit,
                                      String teamA, String teamB) {
        if (p.status() == MatchStatus.COMPLETED) {
            throw new InvalidEventException("Match is already completed");
        }
        ScoreState next = ScoreCalculator.apply(p.current(), type, runs, oversLimit);

        if (p.innings() == 1) {
            if (next.isInningsOver(oversLimit)) {
                // First innings finished: store it and start the chase from zero.
                return new MatchProgress(2, next, ScoreState.ZERO, MatchStatus.LIVE, null);
            }
            return new MatchProgress(1, null, next, MatchStatus.LIVE, null);
        }

        int firstRuns = p.first().runs();
        if (next.runs() > firstRuns) {
            int wicketsInHand = ScoreCalculator.MAX_WICKETS - next.wickets();
            return completed(p, next, teamB + " won by " + plural(wicketsInHand, "wicket"));
        }
        if (next.isInningsOver(oversLimit)) {
            if (next.runs() == firstRuns) {
                return completed(p, next, "Match tied");
            }
            return completed(p, next, teamA + " won by " + plural(firstRuns - next.runs(), "run"));
        }
        return new MatchProgress(2, p.first(), next, MatchStatus.LIVE, null);
    }

    private static MatchProgress completed(MatchProgress p, ScoreState next, String result) {
        return new MatchProgress(2, p.first(), next, MatchStatus.COMPLETED, result);
    }

    private static String plural(int n, String word) {
        return n + " " + word + (n == 1 ? "" : "s");
    }
}
