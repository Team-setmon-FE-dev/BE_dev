package com.example.setmon.calendar.dto;

import com.example.setmon.vlog.entity.VlogStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

public record MonthResponse(
        Long userId,
        @Schema(example = "2026-11") String month,
        @Schema(description = "기록이 있는 날짜만") List<DaySummary> days
) {
    public record DaySummary(
            LocalDate date,
            @Schema(description = "브이로그 썸네일, 없으면 첫 클립 썸네일(1시간 유효)") String thumbnailUrl,
            int clipCount,
            VlogStatus vlogStatus
    ) {
    }
}
