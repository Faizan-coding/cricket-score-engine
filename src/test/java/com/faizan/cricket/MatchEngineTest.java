package com.faizan.cricket;

import static org.junit.jupiter.api.Assertions.*;

import com.faizan.cricket.domain.*;
import org.junit.jupiter.api.Test;

/** Pure unit tests for the two-innings rules (no Spring, no database). */
class MatchEngineTest {

    private static MatchProgress play(MatchProgress p, EventType type, int runs, int balls, int overs) {
        for (int i = 0; i < balls; i++) p = MatchEngine.apply(p, type, runs, overs, "A", "B");
        return p;
    }

    /** 1-over match where A scored 6, so B needs 7. */
    private static MatchProgress chaseStart() {
        return play(MatchProgress.start(), EventType.RUNS, 1, 6, 1);
    }

    @Test
    void firstInningsEndsAndChaseStartsWithATarget() {
        MatchProgress p = chaseStart();
        assertEquals(2, p.innings());
        assertEquals(6, p.first().runs());
        assertEquals(7, p.target());
        assertEquals(ScoreState.ZERO, p.current());
        assertEquals(MatchStatus.LIVE, p.status());
    }

    @Test
    void allOutEndsFirstInnings() {
        MatchProgress p = play(MatchProgress.start(), EventType.WICKET, 0, 10, 20);
        assertEquals(2, p.innings());
        assertEquals(10, p.first().wickets());
    }

    @Test
    void chasingTeamWinsByWicketsInHand() {
        MatchProgress p = play(chaseStart(), EventType.WICKET, 0, 2, 1);
        p = play(p, EventType.RUNS, 6, 1, 1);
        p = play(p, EventType.RUNS, 1, 1, 1);
        assertEquals(MatchStatus.COMPLETED, p.status());
        assertEquals("B won by 8 wickets", p.result());
    }

    @Test
    void defendingTeamWinsByRuns() {
        MatchProgress p = play(chaseStart(), EventType.RUNS, 0, 6, 1);
        assertEquals("A won by 6 runs", p.result());
    }

    @Test
    void winByOneRunIsSingular() {
        MatchProgress p = play(chaseStart(), EventType.RUNS, 0, 5, 1);
        p = play(p, EventType.RUNS, 5, 1, 1);
        assertEquals("A won by 1 run", p.result());
    }

    @Test
    void equalScoresAreATie() {
        MatchProgress p = play(chaseStart(), EventType.RUNS, 1, 6, 1);
        assertEquals("Match tied", p.result());
    }

    @Test
    void noBallsAreAcceptedAfterTheResult() {
        MatchProgress done = play(chaseStart(), EventType.RUNS, 0, 6, 1);
        assertThrows(InvalidEventException.class,
                () -> MatchEngine.apply(done, EventType.RUNS, 1, 1, "A", "B"));
    }
}
