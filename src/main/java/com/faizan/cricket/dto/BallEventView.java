package com.faizan.cricket.dto;

import com.faizan.cricket.domain.BallEvent;
import com.faizan.cricket.domain.EventType;
import java.time.Instant;

public record BallEventView(
        int sequence, int innings, String eventId, int over, int ball,
        EventType type, int runs, String batter, String bowler, Instant at) {

    public static BallEventView from(BallEvent e) {
        return new BallEventView(e.getSequenceNo(), e.getInnings(), e.getEventId(), e.getOverNumber(),
                e.getBallInOver(), e.getType(), e.getRuns(), e.getBatter(), e.getBowler(), e.getCreatedAt());
    }
}
