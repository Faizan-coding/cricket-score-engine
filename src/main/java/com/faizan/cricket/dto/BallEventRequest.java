package com.faizan.cricket.dto;

import com.faizan.cricket.domain.EventType;
import jakarta.validation.constraints.*;

/** eventId is chosen by the client and makes the call safe to retry. */
public record BallEventRequest(
        @NotBlank @Size(max = 64) String eventId,
        @NotNull EventType type,
        @Min(0) @Max(6) int runs,
        @Size(max = 80) String batter,
        @Size(max = 80) String bowler) {
}
