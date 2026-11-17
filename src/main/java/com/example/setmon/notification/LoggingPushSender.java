package com.example.setmon.notification;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * FirebaseMessaging 빈이 없을 때(fcm.key-path 비어 있음) 쓰는 대체 구현.
 * 실제로 보내지 않고 보낼 내용만 로그로 남긴다. 로컬·테스트용.
 */
@Slf4j
@Component
@Conditional(FcmCondition.Disabled.class)
@RequiredArgsConstructor
public class LoggingPushSender implements PushSender {

    private final Clock clock;

    @Override
    public void sendToUser(Long userId, NotificationType type, LocalDate date) {
        sendToUsers(userId == null ? List.of() : List.of(userId), type, date);
    }

    @Override
    public void sendToUsers(Collection<Long> userIds, NotificationType type, LocalDate date) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        log.info("[푸시-로그전용] users={} data={}", userIds, PushMessages.data(type, date, clock));
    }
}
