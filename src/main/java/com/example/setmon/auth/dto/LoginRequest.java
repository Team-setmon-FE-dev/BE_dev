package com.example.setmon.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "user@example.com") @NotBlank String email,
        @Schema(example = "password123") @NotBlank String password
) {
}
