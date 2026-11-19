package com.faizan.cricket.service;

import com.faizan.cricket.cache.MatchStateCache;
import com.faizan.cricket.domain.*;
import com.faizan.cricket.dto.*;
import com.faizan.cricket.repo.BallEventRepository;
import com.faizan.cricket.repo.MatchRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class MatchService {

    private static final int MAX_ATTEMPTS = 10;

    private final MatchRepository matches;
    private final BallEventRepository events;
    private final MatchStateCache cache;
    private final LiveUpdateService live;
    private final TransactionTemplate tx;

    public MatchService(MatchRepository matches, BallEventRepository events, MatchStateCache cache,
                        LiveUpdateService live, PlatformTransactionManager txManager) {
        this.matches = matches;
        this.events = events;
        this.cache = cache;
        this.live = live;
        this.tx = new TransactionTemplate(txManager);
    }

    @Transactional
    public MatchState create(CreateMatchRequest r) {
        Match m = matches.save(new Match(r.teamA(), r.teamB(), r.oversLimit()));
        return MatchState.from(m);
    }

    @Transactional
    public MatchState start(Long id) {
        Match m = find(id);
        if (m.getStatus() != MatchStatus.SCHEDULED) {
            throw new InvalidEventException("Match has already started");
        }
        m.start();
        matches.saveAndFlush(m);
        MatchState state = MatchState.from(m);
        afterCommit(state);
        return state;
    }

    /**
     * Records one ball. Safe to retry (idempotent on eventId) and safe under concurrent scorers
     * (optimistic locking + unique constraints, retried a few times with a short random backoff).
     * The retry loop sits OUTSIDE the transaction, so every attempt starts from fresh data.
     */
    public SubmitResult submit(Long matchId, BallEventRequest req) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return tx.execute(status -> doSubmit(matchId, req));
            } catch (ConcurrencyFailureException | DataIntegrityViolationException e) {
                // Lost an optimistic-lock race, or another request inserted the same eventId/sequence.
                // On retry the duplicate check below will find the already stored event.
                last = e;
                backoff(attempt);
            }
        }
        throw new ConcurrentUpdateException("Too many concurrent updates, retry with the same eventId", last);
    }

    private SubmitResult doSubmit(Long matchId, BallEventRequest req) {
        Match m = find(matchId);

        Optional<BallEvent> existing = events.findByMatchIdAndEventId(matchId, req.eventId());
        if (existing.isPresent()) {
            return new SubmitResult(true, BallEventView.from(existing.get()), MatchState.from(m));
        }
        if (m.getStatus() != MatchStatus.LIVE) {
            throw new InvalidEventException("Match is not live (status " + m.getStatus() + ")");
        }

        MatchProgress before = m.progress();
        MatchProgress after = MatchEngine.apply(before, req.type(), req.runs(), m.getOversLimit(),
                m.getTeamA(), m.getTeamB());

        // Over and ball are counted inside the innings the ball belongs to.
        int legalBefore = before.current().legalBalls();
        int over = legalBefore / ScoreCalculator.BALLS_PER_OVER;
        int ball = legalBefore % ScoreCalculator.BALLS_PER_OVER + 1;
        BallEvent event = events.save(new BallEvent(matchId, req.eventId(), m.getEventCount() + 1,
                before.innings(), over, ball, req.type(), req.runs(), req.batter(), req.bowler()));

        m.applyProgress(after);
        matches.saveAndFlush(m); // flush now so a version conflict surfaces inside this attempt

        MatchState state = MatchState.from(m);
        afterCommit(state);
        return new SubmitResult(false, BallEventView.from(event), state);
    }

    /** Cache-aside read: Redis/in-memory first, database on a miss. */
    @Transactional(readOnly = true)
    public MatchState getState(Long id) {
        return cache.get(id).orElseGet(() -> {
            MatchState state = MatchState.from(find(id));
            cache.put(state);
            return state;
        });
    }

    @Transactional(readOnly = true)
    public List<BallEventView> getEvents(Long id) {
        find(id);
        return events.findByMatchIdOrderBySequenceNoAsc(id).stream().map(BallEventView::from).toList();
    }

    /** Rebuilds both innings and the result from the raw event log and compares with the stored match. */
    @Transactional(readOnly = true)
    public VerifyResult verify(Long id) {
        Match m = find(id);
        List<BallEvent> log = events.findByMatchIdOrderBySequenceNoAsc(id);
        MatchProgress replayed = MatchProgress.start();
        for (BallEvent e : log) {
            replayed = MatchEngine.apply(replayed, e.getType(), e.getRuns(), m.getOversLimit(),
                    m.getTeamA(), m.getTeamB());
        }
        MatchProgress stored = m.progress();
        boolean consistent = log.size() == m.getEventCount()
                && (m.getStatus() == MatchStatus.SCHEDULED ? log.isEmpty() : replayed.equals(stored));
        return new VerifyResult(consistent, log.size(), replayed, stored);
    }

    private Match find(Long id) {
        return matches.findById(id).orElseThrow(() -> new NotFoundException("Match " + id + " not found"));
    }

    /** Cache + push happen only after the DB commit, so readers never see state that was rolled back. */
    private void afterCommit(MatchState state) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cache.put(state);
                live.publish(state);
            }
        });
    }

    private static void backoff(int attempt) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(1, 5 * attempt + 1));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
