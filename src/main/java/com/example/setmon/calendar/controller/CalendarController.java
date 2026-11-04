package com.example.setmon.calendar.controller;

import com.example.setmon.calendar.dto.DayDetailResponse;
import com.example.setmon.calendar.dto.MonthResponse;
import com.example.setmon.calendar.service.CalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "달력")
@RestController
@RequestMapping("/api/users/{userId}/days")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @Operation(summary = "월별 조회 (F-05, F-06)",
            description = "기록 있는 날짜만 내려준다. URL은 1시간 유효. 본인 또는 수락된 친구만. 에러: FORBIDDEN, USER_NOT_FOUND")
    @GetMapping
    public MonthResponse getMonth(@Parameter(hidden = true) @AuthenticationPrincipal Long viewerId,
                                  @PathVariable Long userId,
                                  @Parameter(example = "2026-11") @RequestParam YearMonth month) {
        return calendarService.getMonth(viewerId, userId, month);
    }

    @Operation(summary = "하루 상세 (F-18)",
            description = "슬롯별 클립 + 브이로그 상태. 생성 중이면 몇 초마다 다시 조회. 본인 또는 수락된 친구만. 에러: FORBIDDEN, USER_NOT_FOUND")
    @GetMapping("/{date}")
    public DayDetailResponse getDay(@Parameter(hidden = true) @AuthenticationPrincipal Long viewerId,
                                    @PathVariable Long userId,
                                    @Parameter(example = "2026-11-04") @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return calendarService.getDay(viewerId, userId, date);
    }
}
