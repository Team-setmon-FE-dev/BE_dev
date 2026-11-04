package com.example.setmon.clip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.setmon.common.exception.BusinessException;
import com.example.setmon.common.exception.ErrorCode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class ClipSlotResolverTest {

    private static Instant kst(String dateTime) {
        return ZonedDateTime.parse(dateTime + "+09:00[Asia/Seoul]").toInstant();
    }

    @Test
    void 지금_슬롯이면_통과() {
        var slot = ClipSlotResolver.resolve(kst("2026-11-04T14:20:00"), kst("2026-11-04T14:20:05"));
        assertThat(slot).isEqualTo(new ClipSlotResolver.Slot(LocalDate.of(2026, 11, 4), 14));
    }

    @Test
    void 정각_후_2분_안에는_직전_슬롯_인정() {
        var slot = ClipSlotResolver.resolve(kst("2026-11-04T13:59:58"), kst("2026-11-04T14:01:30"));
        assertThat(slot.hour()).isEqualTo(13);
    }

    @Test
    void 정각_후_2분이_지나면_직전_슬롯_거절() {
        assertThatThrownBy(() -> ClipSlotResolver.resolve(kst("2026-11-04T13:59:58"), kst("2026-11-04T14:02:01")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAST_SLOT);
    }

    @Test
    void 자정_넘어_올리면_전날_23시_슬롯() {
        var slot = ClipSlotResolver.resolve(kst("2026-11-04T23:59:59"), kst("2026-11-05T00:00:40"));
        assertThat(slot).isEqualTo(new ClipSlotResolver.Slot(LocalDate.of(2026, 11, 4), 23));
    }

    @Test
    void 미래_시각은_거절() {
        assertThatThrownBy(() -> ClipSlotResolver.resolve(kst("2026-11-04T14:30:00"), kst("2026-11-04T14:20:00")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FUTURE_RECORDED_AT);
    }

    @Test
    void 기기_시계가_조금_빨라_다음_슬롯이어도_허용() {
        var slot = ClipSlotResolver.resolve(kst("2026-11-04T15:00:10"), kst("2026-11-04T14:59:50"));
        assertThat(slot.hour()).isEqualTo(15);
    }
}
