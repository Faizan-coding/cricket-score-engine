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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MatchStatus status = MatchStatus.SCHEDULED;

    /** 1 = Team A batting, 2 = Team B chasing. */
    private int innings = 1;

    // Innings in progress (or the final score of innings 2 once the match is complete).
    private int runs;
    private int wickets;
    private int legalBalls;
    private int extras;

    // Final score of the first innings, filled when innings 2 starts.
    private int firstRuns;
    private int firstWickets;
    private int firstLegalBalls;
    private int firstExtras;

    /** Winner / margin text, e.g. "India won by 12 runs". Null until the match is complete. */
    @Column(length = 160)
    private String result;

    /** Number of accepted events across both innings. */
    private int eventCount;

    /** Optimistic lock: a concurrent writer makes the UPDATE match 0 rows and fail. */
    @Version
    private long version;

    private Instant createdAt = Instant.now();

    protected Match() {}

    public Match(String teamA, String teamB, int oversLimit) {
        this.teamA = teamA;
        this.teamB = teamB;
        this.oversLimit = oversLimit;
    }

    public void start() {
        this.status = MatchStatus.LIVE;
    }

    public ScoreState score() {
        return new ScoreState(runs, wickets, legalBalls, extras);
    }

    /** The pure view of this match used by MatchEngine. */
    public MatchProgress progress() {
        ScoreState first = innings >= 2
                ? new ScoreState(firstRuns, firstWickets, firstLegalBalls, firstExtras) : null;
        return new MatchProgress(innings, first, score(), status, result);
    }

    /** Stores the state produced by MatchEngine after one accepted event. */
    public void applyProgress(MatchProgress p) {
        this.innings = p.innings();
        if (p.first() != null) {
            this.firstRuns = p.first().runs();
            this.firstWickets = p.first().wickets();
            this.firstLegalBalls = p.first().legalBalls();
            this.firstExtras = p.first().extras();
        }
        ScoreState s = p.current();
        this.runs = s.runs();
        this.wickets = s.wickets();
        this.legalBalls = s.legalBalls();
        this.extras = s.extras();
        this.status = p.status();
        this.result = p.result();
        this.eventCount++;
    }

    public Long getId() { return id; }
    public String getTeamA() { return teamA; }
    public String getTeamB() { return teamB; }
    public int getOversLimit() { return oversLimit; }
    public MatchStatus getStatus() { return status; }
    public int getEventCount() { return eventCount; }
}
