package com.example.setmon.calendar.controller;

import com.example.setmon.calendar.dto.DayDetailResponse;
import com.example.setmon.calendar.dto.MonthResponse;
import com.example.setmon.common.exception.BusinessException;
import com.example.setmon.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.YearMonth;
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
public class CalendarController {

    @Operation(summary = "[미구현] 월별 조회 (F-05, F-06)", description = "본인 또는 수락된 친구만. 에러: FORBIDDEN")
    @GetMapping
    public MonthResponse getMonth(@Parameter(hidden = true) @AuthenticationPrincipal Long viewerId,
                                  @PathVariable Long userId,
                                  @Parameter(example = "2026-11") @RequestParam YearMonth month) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Operation(summary = "[미구현] 하루 상세 (F-18)", description = "슬롯별 클립 + 브이로그 상태. 본인 또는 수락된 친구만. 에러: FORBIDDEN")
    @GetMapping("/{date}")
    public DayDetailResponse getDay(@Parameter(hidden = true) @AuthenticationPrincipal Long viewerId,
                                    @PathVariable Long userId,
                                    @Parameter(example = "2026-11-04") @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }
}
