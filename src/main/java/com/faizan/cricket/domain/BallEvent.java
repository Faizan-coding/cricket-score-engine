package com.faizan.cricket.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Append-only log of what happened. The match score is derived from these rows,
 * so it can always be rebuilt and audited by replaying them.
 */
@Entity
@Table(name = "ball_event", uniqueConstraints = {
        // Idempotency: a retried request with the same eventId can never be stored twice.
        @UniqueConstraint(name = "uk_match_event_id", columnNames = {"match_id", "event_id"}),
        // Ordering: two writers can never claim the same sequence number.
        @UniqueConstraint(name = "uk_match_sequence", columnNames = {"match_id", "sequence_no"})
})
public class BallEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    private int overNumber;
    private int ballInOver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EventType type;

    private int runs;

    @Column(length = 80)
    private String batter;

    @Column(length = 80)
    private String bowler;

    private Instant createdAt = Instant.now();

    protected BallEvent() {}

    public BallEvent(Long matchId, String eventId, int sequenceNo, int overNumber, int ballInOver,
                     EventType type, int runs, String batter, String bowler) {
        this.matchId = matchId;
        this.eventId = eventId;
        this.sequenceNo = sequenceNo;
        this.overNumber = overNumber;
        this.ballInOver = ballInOver;
        this.type = type;
        this.runs = runs;
        this.batter = batter;
        this.bowler = bowler;
    }

    public Long getId() { return id; }
    public Long getMatchId() { return matchId; }
    public String getEventId() { return eventId; }
    public int getSequenceNo() { return sequenceNo; }
    public int getOverNumber() { return overNumber; }
    public int getBallInOver() { return ballInOver; }
    public EventType getType() { return type; }
    public int getRuns() { return runs; }
    public String getBatter() { return batter; }
    public String getBowler() { return bowler; }
    public Instant getCreatedAt() { return createdAt; }
}
