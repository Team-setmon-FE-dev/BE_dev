package com.example.setmon.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateSettingsRequest(
        @Schema(description = "촬영 알림 시작 시(KST, 포함)", example = "9") @NotNull @Min(0) @Max(23) Integer notifyStartHour,
        @Schema(description = "촬영 알림 끝 시(KST, 포함)", example = "23") @NotNull @Min(0) @Max(23) Integer notifyEndHour,
        @NotNull Boolean captureNotifyOn,
        @NotNull Boolean completeNotifyOn
) {
}
