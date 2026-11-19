package com.faizan.cricket.domain;

/**
 * Pure snapshot of a match: which innings, the finished first innings (once innings 2 starts),
 * the innings in progress, the match status and the result text once completed.
 * Team A always bats first.
 */
public record MatchProgress(int innings, ScoreState first, ScoreState current, MatchStatus status, String result) {

    public static MatchProgress start() {
        return new MatchProgress(1, null, ScoreState.ZERO, MatchStatus.LIVE, null);
    }

    /** Runs the chasing team needs to win, or null during the first innings. */
    public Integer target() {
        return innings == 2 && first != null ? first.runs() + 1 : null;
    }
}
