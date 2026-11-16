package com.faizan.cricket;

import static org.junit.jupiter.api.Assertions.*;

import com.faizan.cricket.domain.*;
import org.junit.jupiter.api.Test;

class ScoreCalculatorTest {

    private static final int OVERS = 5;

    @Test
    void runsOffTheBatCountAsLegalBall() {
        ScoreState s = ScoreCalculator.apply(ScoreState.ZERO, EventType.RUNS, 4, OVERS);
        assertEquals(new ScoreState(4, 0, 1, 0), s);
    }

    @Test
    void wideAddsPenaltyAndDoesNotUseALegalBall() {
        ScoreState s = ScoreCalculator.apply(ScoreState.ZERO, EventType.WIDE, 2, OVERS);
        assertEquals(new ScoreState(3, 0, 0, 3), s);
    }

    @Test
    void noBallCountsBatRunsAsRunsNotExtras() {
        ScoreState s = ScoreCalculator.apply(ScoreState.ZERO, EventType.NO_BALL, 6, OVERS);
        assertEquals(new ScoreState(7, 0, 0, 1), s);
    }

    @Test
    void wicketIncrementsWicketsAndBall() {
        ScoreState s = ScoreCalculator.apply(ScoreState.ZERO, EventType.WICKET, 0, OVERS);
        assertEquals(new ScoreState(0, 1, 1, 0), s);
    }

    @Test
    void byeAndLegByeAreExtrasOnALegalBall() {
        ScoreState s = ScoreCalculator.apply(ScoreState.ZERO, EventType.BYE, 2, OVERS);
        s = ScoreCalculator.apply(s, EventType.LEG_BYE, 1, OVERS);
        assertEquals(new ScoreState(3, 0, 2, 3), s);
    }

    @Test
    void overRollsOverAfterSixLegalBalls() {
        ScoreState s = ScoreState.ZERO;
        for (int i = 0; i < 6; i++) s = ScoreCalculator.apply(s, EventType.RUNS, 1, OVERS);
        assertEquals("1.0", s.overs());
    }

    @Test
    void seventhLegalBallOfLastOverIsRejected() {
        ScoreState s = ScoreState.ZERO;
        for (int i = 0; i < 6; i++) s = ScoreCalculator.apply(s, EventType.RUNS, 0, 1);
        ScoreState done = s;
        assertThrows(InvalidEventException.class, () -> ScoreCalculator.apply(done, EventType.RUNS, 0, 1));
    }

    @Test
    void noBallsAfterAllOutAreRejected() {
        ScoreState s = ScoreState.ZERO;
        for (int i = 0; i < 10; i++) s = ScoreCalculator.apply(s, EventType.WICKET, 0, 20);
        ScoreState done = s;
        assertThrows(InvalidEventException.class, () -> ScoreCalculator.apply(done, EventType.RUNS, 1, 20));
    }

    @Test
    void invalidRunValuesAreRejected() {
        assertThrows(InvalidEventException.class, () -> ScoreCalculator.apply(ScoreState.ZERO, EventType.RUNS, 7, OVERS));
        assertThrows(InvalidEventException.class, () -> ScoreCalculator.apply(ScoreState.ZERO, EventType.BYE, 0, OVERS));
        assertThrows(InvalidEventException.class, () -> ScoreCalculator.apply(ScoreState.ZERO, EventType.WICKET, 4, OVERS));
    }

    @Test
    void runRateIsRunsPerOver() {
        ScoreState s = new ScoreState(30, 1, 18, 0);
        assertEquals(10.0, s.runRate());
    }
}
