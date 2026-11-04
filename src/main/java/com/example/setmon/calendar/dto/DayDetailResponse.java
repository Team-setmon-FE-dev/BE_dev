package com.example.setmon.calendar.dto;

import com.example.setmon.vlog.entity.VlogStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DayDetailResponse(
        Long userId,
        LocalDate date,
        @Schema(description = "타임라인 칸 범위(달력 주인의 알림 시간대)") int notifyStartHour,
        int notifyEndHour,
        VlogInfo vlog,
        @Schema(description = "slotHour 오름차순") List<ClipItem> clips
) {
    public record VlogInfo(
            VlogStatus status,
            boolean stale,
            @Schema(description = "DONE일 때만, 1시간 유효") String videoUrl,
            String thumbnailUrl,
            @Schema(description = "FAILED일 때만") String failReason
    ) {
    }

    public record ClipItem(
            Long id,
            int slotHour,
            Instant recordedAt,
            int durationMs,
            @Schema(description = "1시간 유효") String videoUrl,
            String thumbnailUrl
    ) {
    }
}
