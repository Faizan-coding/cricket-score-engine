package com.faizan.cricket.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "cricket_match")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String teamA;

    @Column(nullable = false, length = 80)
    private String teamB;

    @Column(nullable = false)
    private int oversLimit;

    /** Optional chase target (second innings). */
    private Integer target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MatchStatus status = MatchStatus.SCHEDULED;

    private int runs;
    private int wickets;
    private int legalBalls;
    private int extras;

    /** Number of accepted events; also the next event's sequence number - 1. */
    private int eventCount;

    /** Optimistic lock: a concurrent writer makes the UPDATE match 0 rows and fail. */
    @Version
    private long version;

    private Instant createdAt = Instant.now();

    protected Match() {}

    public Match(String teamA, String teamB, int oversLimit, Integer target) {
        this.teamA = teamA;
        this.teamB = teamB;
        this.oversLimit = oversLimit;
        this.target = target;
    }

    public void start() {
        this.status = MatchStatus.LIVE;
    }

    public ScoreState score() {
        return new ScoreState(runs, wickets, legalBalls, extras);
    }

    /** Applies the next derived state after one accepted event. */
    public void applyScore(ScoreState s) {
        this.runs = s.runs();
        this.wickets = s.wickets();
        this.legalBalls = s.legalBalls();
        this.extras = s.extras();
        this.eventCount++;
        boolean chased = target != null && runs >= target;
        if (s.isInningsOver(oversLimit) || chased) {
            this.status = MatchStatus.COMPLETED;
        }
    }

    public Long getId() { return id; }
    public String getTeamA() { return teamA; }
    public String getTeamB() { return teamB; }
    public int getOversLimit() { return oversLimit; }
    public Integer getTarget() { return target; }
    public MatchStatus getStatus() { return status; }
    public int getEventCount() { return eventCount; }
}
