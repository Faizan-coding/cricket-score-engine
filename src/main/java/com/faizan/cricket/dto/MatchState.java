package com.faizan.cricket.dto;

import com.faizan.cricket.domain.*;

/** Read model served to clients, cached and pushed over SSE. */
public record MatchState(
        Long id, String teamA, String teamB, MatchStatus status,
        int oversLimit, Integer target,
        int runs, int wickets, int extras, int legalBalls, String overs,
        double runRate, Double requiredRunRate, int eventCount) {

    public static MatchState from(Match m) {
        ScoreState s = m.score();
        Double rrr = null;
        if (m.getTarget() != null && s.runs() < m.getTarget()) {
            int ballsLeft = m.getOversLimit() * ScoreCalculator.BALLS_PER_OVER - s.legalBalls();
            if (ballsLeft > 0) {
                rrr = Math.round((m.getTarget() - s.runs()) * 6.0 / ballsLeft * 100.0) / 100.0;
            }
        }
        return new MatchState(m.getId(), m.getTeamA(), m.getTeamB(), m.getStatus(),
                m.getOversLimit(), m.getTarget(),
                s.runs(), s.wickets(), s.extras(), s.legalBalls(), s.overs(),
                s.runRate(), rrr, m.getEventCount());
    }
}
