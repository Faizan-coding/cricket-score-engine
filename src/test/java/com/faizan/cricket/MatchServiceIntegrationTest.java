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
        Long id = service.create(new CreateMatchRequest("A", "B", overs, null)).id();
        service.start(id);
        return id;
    }

    private static BallEventRequest ball(String eventId, EventType type, int runs) {
        return new BallEventRequest(eventId, type, runs, "Batter", "Bowler");
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
    void ballsAfterTheLastOverAreRejected() {
        Long id = liveMatch(1);
        for (int i = 0; i < 6; i++) service.submit(id, ball("e" + i, EventType.RUNS, 1));

        assertThrows(InvalidEventException.class, () -> service.submit(id, ball("e7", EventType.RUNS, 1)));
        assertEquals("COMPLETED", service.getState(id).status().name());
    }

    @Test
    void cannotScoreBeforeMatchStarts() {
        Long id = service.create(new CreateMatchRequest("A", "B", 5, null)).id();
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
    void matchEndsWhenTargetIsChased() {
        Long id = service.create(new CreateMatchRequest("A", "B", 5, 6)).id();
        service.start(id);
        service.submit(id, ball("t1", EventType.RUNS, 6));
        assertEquals("COMPLETED", service.getState(id).status().name());
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
        assertTrue(service.verify(id).consistent(), "replaying the log must equal the stored score");
    }

    @Test
    void replayOfEventLogMatchesStoredScore() {
        Long id = liveMatch(5);
        service.submit(id, ball("r1", EventType.RUNS, 4));
        service.submit(id, ball("r2", EventType.WIDE, 1));
        service.submit(id, ball("r3", EventType.WICKET, 0));
        service.submit(id, ball("r4", EventType.NO_BALL, 6));

        VerifyResult v = service.verify(id);
        assertTrue(v.consistent());
        assertEquals(4, v.events());
        assertEquals(v.stored(), v.replayed());
    }
}
