package com.faizan.cricket.dto;

import com.faizan.cricket.domain.MatchProgress;

/** Result of rebuilding the whole match (both innings and the result) from the raw event log. */
public record VerifyResult(boolean consistent, int events, MatchProgress replayed, MatchProgress stored) {
}
