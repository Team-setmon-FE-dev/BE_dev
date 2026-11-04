package com.example.setmon.clip.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateClipRequest(
        @NotBlank String videoKey,
        @NotBlank String thumbKey,
        @Schema(description = "2000~4000", example = "3200") @NotNull Integer durationMs,
        @Schema(description = "촬영 시각(UTC)", example = "2026-11-04T05:00:03Z") @NotNull Instant recordedAt
) {
}
