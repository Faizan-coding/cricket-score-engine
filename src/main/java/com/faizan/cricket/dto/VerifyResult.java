package com.faizan.cricket.dto;

import com.faizan.cricket.domain.ScoreState;

/** Result of rebuilding the score from the raw event log and comparing it with the stored score. */
public record VerifyResult(boolean consistent, int events, ScoreState replayed, ScoreState stored) {
}
