package com.faizan.cricket;

import static org.junit.jupiter.api.Assertions.*;

import com.faizan.cricket.domain.EventType;
import com.faizan.cricket.domain.InvalidEventException;
import com.faizan.cricket.dto.*;
import com.faizan.cricket.service.ConcurrentUpdateException;
import com.faizan.cricket.service.MatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MatchServiceIntegrationTest {

    @Autowired
    MatchService service;

    private Long liveMatch(int overs) {
        Long id = service.create(new CreateMatchRequest("A", "B", overs)).id();
        service.start(id);
        return id;
    }

    private static BallEventRequest ball(String eventId, EventType type, int runs) {
        return new BallEventRequest(eventId, type, runs, "Batter", "Bowler");
    }

    private void play(Long id, String prefix, EventType type, int runs, int balls) {
        for (int i = 0; i < balls; i++) service.submit(id, ball(prefix + i, type, runs));
    }

    @Test
    void sameEventIdIsCountedOnlyOnce() {
        Long id = liveMatch(5);

        SubmitResult first = service.submit(id, ball("evt-1", EventType.RUNS, 4));
        SubmitResult retry = service.submit(id, ball("evt-1", EventType.RUNS, 4));

        assertFalse(first.duplicate());
        assertTrue(retry.duplicate());
        MatchState s = service.getState(id);
        assertEquals(4, s.runs());
        assertEquals(1, s.eventCount());
        assertEquals(1, service.getEvents(id).size());
    }

    @Test
    void cannotScoreBeforeMatchStarts() {
        Long id = service.create(new CreateMatchRequest("A", "B", 5)).id();
        assertThrows(InvalidEventException.class, () -> service.submit(id, ball("e1", EventType.RUNS, 1)));
    }

    @Test
    void wideDoesNotConsumeALegalBall() {
        Long id = liveMatch(5);
        service.submit(id, ball("w1", EventType.WIDE, 0));
        MatchState s = service.getState(id);
        assertEquals(1, s.runs());
        assertEquals("0.0", s.overs());
    }

    @Test
    void firstInningsEndsAndSecondInningsStartsWithTarget() {
        Long id = liveMatch(1);
        play(id, "a", EventType.RUNS, 1, 6);

        MatchState s = service.getState(id);
        assertEquals("LIVE", s.status().name());
        assertEquals(2, s.innings());
        assertEquals("B", s.battingTeam());
        assertEquals(7, s.target());
        assertEquals(0, s.runs());
        assertEquals(6, s.firstInnings().runs());
        assertEquals("A", s.firstInnings().team());
    }

    @Test
    void allOutEndsTheFirstInnings() {
        Long id = liveMatch(20);
        play(id, "w", EventType.WICKET, 0, 10);
        MatchState s = service.getState(id);
        assertEquals(2, s.innings());
        assertEquals(10, s.firstInnings().wickets());
    }

    @Test
    void chasingTeamWinsByWickets() {
        Long id = liveMatch(1);
        play(id, "a", EventType.RUNS, 1, 6);              // A: 6, target 7
        service.submit(id, ball("b1", EventType.RUNS, 6));
        service.submit(id, ball("b2", EventType.RUNS, 1)); // B: 7

        MatchState s = service.getState(id);
        assertEquals("COMPLETED", s.status().name());
        assertEquals("B won by 10 wickets", s.result());
    }

    @Test
    void defendingTeamWinsByRuns() {
        Long id = liveMatch(1);
        play(id, "a", EventType.RUNS, 1, 6);
        play(id, "b", EventType.RUNS, 0, 6);

        MatchState s = service.getState(id);
        assertEquals("COMPLETED", s.status().name());
        assertEquals("A won by 6 runs", s.result());
    }

    @Test
    void matchCanBeTied() {
        Long id = liveMatch(1);
        play(id, "a", EventType.RUNS, 1, 6);
        play(id, "b", EventType.RUNS, 1, 6);
        assertEquals("Match tied", service.getState(id).result());
    }

    @Test
    void ballsAfterTheMatchIsCompleteAreRejected() {
        Long id = liveMatch(1);
        play(id, "a", EventType.RUNS, 1, 6);
        play(id, "b", EventType.RUNS, 0, 6);
        assertThrows(InvalidEventException.class, () -> service.submit(id, ball("late", EventType.RUNS, 1)));
    }

    @Test
    void concurrentScorersNeverLoseOrDuplicateAnUpdate() throws Exception {
        Long id = liveMatch(20);
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    service.submit(id, ball(UUID.randomUUID().toString(), EventType.RUNS, 1));
                    accepted.incrementAndGet();
                } catch (ConcurrentUpdateException ignored) {
                    // allowed under heavy contention; the invariant below must still hold
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        MatchState s = service.getState(id);
        assertTrue(accepted.get() > 0);
        assertEquals(accepted.get(), s.runs(), "every accepted event must be reflected exactly once");
        assertEquals(accepted.get(), s.eventCount());
        assertTrue(service.verify(id).consistent(), "replaying the log must equal the stored match");
    }

    @Test
    void replayOfBothInningsMatchesStoredMatch() {
        Long id = liveMatch(1);
        play(id, "a", EventType.RUNS, 1, 6);                  // innings 1 complete
        service.submit(id, ball("b1", EventType.WIDE, 0));     // innings 2 has started
        service.submit(id, ball("b2", EventType.RUNS, 4));

        VerifyResult v = service.verify(id);
        assertTrue(v.consistent());
        assertEquals(8, v.events());
        assertEquals(v.stored(), v.replayed());
        assertEquals(1, service.getEvents(id).get(0).innings());
        assertEquals(2, service.getEvents(id).get(7).innings());
    }
}
