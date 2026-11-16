package com.faizan.cricket.dto;

/** duplicate = true means this eventId was already processed; nothing was changed. */
public record SubmitResult(boolean duplicate, BallEventView event, MatchState state) {
}
