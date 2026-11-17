package com.example.setmon.notification;

import com.example.setmon.common.config.TimeConfig;
import com.example.setmon.user.entity.User;
import com.example.setmon.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 대상 사용자 필터링(JPQL)까지 검증하려고 실제 리포지토리(H2)를 쓴다. PushSender는 mock.
 */
@SpringBootTest
@Transactional
class CaptureReminderSchedulerTest {

    @Autowired
    private UserRepository userRepository;

    /** 이름 → id */
    private Map<String, Long> users;

    @BeforeEach
    void setUp() {
        users = Map.of(
                "day", save(9, 23, true),       // 기본값 09~23
                "dawn", save(0, 8, true),       // 00~08
                "off", save(9, 23, false),      // 촬영 알림 꺼짐
                "edge", save(8, 9, true));      // 08~09
    }

    @ParameterizedTest(name = "KST {0}시 → {1}")
    @CsvSource({
            "8,  dawn|edge",
            "9,  day|edge",
            "23, day",
            "0,  dawn",
    })
    @DisplayName("현재 KST 시가 알림 시간대(양끝 포함)에 들어가고 촬영 알림이 켜진 사용자에게만 보낸다")
    void filtersTargets(int hour, String expected) {
        PushSender pushSender = mock(PushSender.class);
        Clock clock = kstClock(hour);

        new CaptureReminderScheduler(userRepository, pushSender, clock).sendHourlyReminder();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> ids = ArgumentCaptor.forClass(Collection.class);
        verify(pushSender).sendToUsers(ids.capture(), eq(NotificationType.CAPTURE), eq(LocalDate.of(2026, 11, 17)));

        List<Long> ours = ids.getValue().stream().filter(users::containsValue).toList();
        List<Long> expectedIds = Arrays.stream(expected.split("\\|")).map(users::get).toList();
        assertThat(ours).containsExactlyInAnyOrderElementsOf(expectedIds);
        assertThat(ids.getValue()).doesNotContain(users.get("off"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("captureNotifyOn=false 사용자는 시간대 안이어도 빠진다")
    void excludesCaptureOff() {
        assertThat(userRepository.findCaptureNotifyTargetIds(12))
                .contains(users.get("day"))
                .doesNotContain(users.get("off"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("대상이 없으면 발송하지 않는다")
    void noTargets() {
        userRepository.deleteAll();
        PushSender pushSender = mock(PushSender.class);

        new CaptureReminderScheduler(userRepository, pushSender, kstClock(12)).sendHourlyReminder();

        verify(pushSender, never()).sendToUsers(anyCollection(), any(), any());
    }

    /** 2026-11-17 hour:00 KST (00시는 UTC로는 전날이지만 날짜는 KST 기준이어야 한다) */
    private static Clock kstClock(int hour) {
        return Clock.fixed(ZonedDateTime.of(2026, 11, 17, hour, 0, 0, 0, TimeConfig.KST).toInstant(), ZoneOffset.UTC);
    }

    private Long save(int start, int end, boolean captureOn) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = User.create(suffix + "@test.com", "hash", "u" + suffix.substring(0, 5), suffix.toUpperCase());
        user.updateSettings(start, end, captureOn, true);
        return userRepository.save(user).getId();
    }
}
