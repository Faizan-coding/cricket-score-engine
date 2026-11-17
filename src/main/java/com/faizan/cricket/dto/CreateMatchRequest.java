package com.faizan.cricket.dto;

import jakarta.validation.constraints.*;

/** Team A bats first. The chase target and the result are worked out by the server. */
public record CreateMatchRequest(
        @NotBlank @Size(max = 80) String teamA,
        @NotBlank @Size(max = 80) String teamB,
        @Min(1) @Max(50) int oversLimit) {
}
