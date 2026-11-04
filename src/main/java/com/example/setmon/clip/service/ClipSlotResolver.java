package com.example.setmon.clip.service;

import com.example.setmon.common.config.TimeConfig;
import com.example.setmon.common.exception.BusinessException;
import com.example.setmon.common.exception.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 촬영 시각(KST)으로 날짜·시간 슬롯을 정한다.
 * - 지금 슬롯만 허용. 단, 정각 후 GRACE 동안은 직전 슬롯도 허용(59분에 찍고 정각 넘어 업로드한 경우).
 * - 서버 시각보다 FUTURE_TOLERANCE 넘게 앞서면 거절.
 */
public final class ClipSlotResolver {

    static final Duration GRACE = Duration.ofMinutes(2);
    static final Duration FUTURE_TOLERANCE = Duration.ofSeconds(30);

    private ClipSlotResolver() {
    }

    public record Slot(LocalDate date, int hour) {
    }

    public static Slot resolve(Instant recordedAt, Instant now) {
        if (recordedAt.isAfter(now.plus(FUTURE_TOLERANCE))) {
            throw new BusinessException(ErrorCode.FUTURE_RECORDED_AT);
        }
        ZonedDateTime recordedSlot = recordedAt.atZone(TimeConfig.KST).truncatedTo(ChronoUnit.HOURS);
        ZonedDateTime nowZoned = now.atZone(TimeConfig.KST);
        ZonedDateTime currentSlot = nowZoned.truncatedTo(ChronoUnit.HOURS);

        // 미래 허용치 때문에 다음 슬롯으로 넘어간 경우도 현재 슬롯과 같은 것으로 본다
        boolean isCurrent = recordedSlot.equals(currentSlot) || recordedSlot.equals(currentSlot.plusHours(1));
        boolean isPreviousInGrace = recordedSlot.equals(currentSlot.minusHours(1))
                && nowZoned.isBefore(currentSlot.plus(GRACE));
        if (!isCurrent && !isPreviousInGrace) {
            throw new BusinessException(ErrorCode.PAST_SLOT);
        }
        return new Slot(recordedSlot.toLocalDate(), recordedSlot.getHour());
    }
}
