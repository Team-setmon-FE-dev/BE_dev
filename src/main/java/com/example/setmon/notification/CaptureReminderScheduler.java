package com.example.setmon.notification;

import com.example.setmon.common.config.TimeConfig;
import com.example.setmon.user.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매시 정각(KST) 촬영 알림.
 * 대상: captureNotifyOn=true 이고 notifyStartHour <= 현재 시 <= notifyEndHour 인 사용자.
 * notification.capture.enabled=false(CAPTURE_NOTIFY_ENABLED=false)면 등록되지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.capture.enabled", havingValue = "true", matchIfMissing = true)
public class CaptureReminderScheduler {

    private final UserRepository userRepository;
    private final PushSender pushSender;
    private final Clock clock;

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void sendHourlyReminder() {
        try {
            ZonedDateTime now = ZonedDateTime.now(clock.withZone(TimeConfig.KST));
            int hour = now.getHour();
            LocalDate today = now.toLocalDate();
            List<Long> userIds = userRepository.findCaptureNotifyTargetIds(hour);
            log.info("촬영 알림 hour={} date={} targets={}", hour, today, userIds.size());
            if (userIds.isEmpty()) {
                return;
            }
            pushSender.sendToUsers(userIds, NotificationType.CAPTURE, today);
        } catch (Exception e) {
            log.error("촬영 알림 스케줄 실패", e);
        }
    }
}
