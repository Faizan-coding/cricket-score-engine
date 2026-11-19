package com.faizan.cricket.dto;

import com.faizan.cricket.domain.*;

/** Read model served to clients, cached and pushed over SSE. */
public record MatchState(
        Long id, String teamA, String teamB, MatchStatus status, int oversLimit,
        int innings, String battingTeam, Integer target,
        int runs, int wickets, int extras, int legalBalls, String overs,
        double runRate, Double requiredRunRate,
        InningsSummary firstInnings, String result, int eventCount) {

    /** Final score of a finished innings. */
    public record InningsSummary(String team, int runs, int wickets, String overs, int extras) {}

    public static MatchState from(Match m) {
        MatchProgress p = m.progress();
        ScoreState s = p.current();
        Integer target = p.target();

        Double rrr = null;
        if (target != null && p.status() == MatchStatus.LIVE && s.runs() < target) {
            int ballsLeft = m.getOversLimit() * ScoreCalculator.BALLS_PER_OVER - s.legalBalls();
            if (ballsLeft > 0) {
                rrr = Math.round((target - s.runs()) * 6.0 / ballsLeft * 100.0) / 100.0;
            }
        }
        InningsSummary first = p.first() == null ? null
                : new InningsSummary(m.getTeamA(), p.first().runs(), p.first().wickets(),
                        p.first().overs(), p.first().extras());

        return new MatchState(m.getId(), m.getTeamA(), m.getTeamB(), p.status(), m.getOversLimit(),
                p.innings(), p.innings() == 1 ? m.getTeamA() : m.getTeamB(), target,
                s.runs(), s.wickets(), s.extras(), s.legalBalls(), s.overs(),
                s.runRate(), rrr, first, p.result(), m.getEventCount());
    }
}
