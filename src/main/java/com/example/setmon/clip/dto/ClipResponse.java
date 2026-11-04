package com.example.setmon.clip.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record ClipResponse(
        Long id,
        @Schema(description = "KST 날짜") LocalDate clipDate,
        @Schema(description = "KST 시(0~23)") int slotHour,
        int durationMs,
        @Schema(description = "같은 슬롯의 기존 클립을 교체했으면 true") boolean replaced
) {
}
