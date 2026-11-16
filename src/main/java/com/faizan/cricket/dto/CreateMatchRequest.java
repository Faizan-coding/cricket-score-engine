package com.faizan.cricket.dto;

import jakarta.validation.constraints.*;

public record CreateMatchRequest(
        @NotBlank @Size(max = 80) String teamA,
        @NotBlank @Size(max = 80) String teamB,
        @Min(1) @Max(50) int oversLimit,
        @Min(1) Integer target) {
}
