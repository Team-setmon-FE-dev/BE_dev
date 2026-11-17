package com.example.setmon.notification;

import com.example.setmon.user.entity.DeviceToken;
import com.example.setmon.user.entity.User;
import com.example.setmon.user.repository.DeviceTokenRepository;
import com.example.setmon.user.repository.UserRepository;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * 실제 FCM 발송. FirebaseMessaging 빈과 같은 조건(fcm.key-path 있음)에서만 등록된다.
 * data-only 메시지(notification 필드 없음) — 앱이 onMessageReceived에서 직접 알림을 만든다.
 */
@Slf4j
@Component
@Conditional(FcmCondition.Enabled.class)
@RequiredArgsConstructor
public class FcmPushSender implements PushSender {

    /** FCM sendEach 한 번에 보낼 수 있는 최대 메시지 수 */
    static final int BATCH_SIZE = 500;

    private static final Set<MessagingErrorCode> INVALID_TOKEN_CODES =
            Set.of(MessagingErrorCode.UNREGISTERED, MessagingErrorCode.INVALID_ARGUMENT);

    private final FirebaseMessaging firebaseMessaging;
    private final UserRepository userRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final Clock clock;

    @Override
    public void sendToUser(Long userId, NotificationType type, LocalDate date) {
        if (userId == null) {
            return;
        }
        sendToUsers(List.of(userId), type, date);
    }

    @Override
    public void sendToUsers(Collection<Long> userIds, NotificationType type, LocalDate date) {
        try {
            doSend(userIds, type, date);
        } catch (Exception e) {
            // 호출부(워커·스케줄러)를 깨지 않는다
            log.error("푸시 발송 중 오류 type={} date={} users={}", type, date, userIds == null ? 0 : userIds.size(), e);
        }
    }

    private void doSend(Collection<Long> userIds, NotificationType type, LocalDate date) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        List<Long> targets = userRepository.findAllById(userIds).stream()
                .filter(u -> isEnabled(u, type))
                .map(User::getId)
                .toList();
        if (targets.isEmpty()) {
            log.info("푸시 대상 없음(설정 꺼짐) type={} date={} requested={}", type, date, userIds.size());
            return;
        }
        List<String> tokens = deviceTokenRepository.findAllByUserIdIn(targets).stream()
                .map(DeviceToken::getToken)
                .distinct()
                .toList();
        if (tokens.isEmpty()) {
            log.info("푸시 대상 기기 없음 type={} date={} users={}", type, date, targets.size());
            return;
        }

        Map<String, String> data = PushMessages.data(type, date, clock);
        AndroidConfig android = AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setTtl(ttl(type).toMillis())
                .build();

        int success = 0;
        int failure = 0;
        List<String> invalidTokens = new ArrayList<>();
        for (int from = 0; from < tokens.size(); from += BATCH_SIZE) {
            List<String> chunk = tokens.subList(from, Math.min(from + BATCH_SIZE, tokens.size()));
            List<Message> messages = chunk.stream()
                    .map(token -> Message.builder()
                            .setToken(token)
                            .putAllData(data)
                            .setAndroidConfig(android)
                            .build())
                    .toList();
            try {
                BatchResponse response = firebaseMessaging.sendEach(messages);
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    SendResponse r = responses.get(i);
                    if (r.isSuccessful()) {
                        success++;
                        continue;
                    }
                    failure++;
                    String token = chunk.get(i);
                    FirebaseMessagingException ex = r.getException();
                    MessagingErrorCode code = ex == null ? null : ex.getMessagingErrorCode();
                    if (code != null && INVALID_TOKEN_CODES.contains(code)) {
                        invalidTokens.add(token);
                    } else {
                        log.warn("푸시 실패 type={} token={} code={} msg={}", type, PushMessages.mask(token), code,
                                ex == null ? null : ex.getMessage());
                    }
                }
            } catch (FirebaseMessagingException e) {
                failure += chunk.size();
                log.warn("푸시 묶음 발송 실패 type={} size={} code={} msg={}",
                        type, chunk.size(), e.getMessagingErrorCode(), e.getMessage());
            }
        }

        int deleted = deleteInvalidTokens(invalidTokens);
        log.info("푸시 발송 type={} date={} users={} tokens={} success={} failure={} deletedTokens={}",
                type, date, targets.size(), tokens.size(), success, failure, deleted);
    }

    private int deleteInvalidTokens(List<String> invalidTokens) {
        if (invalidTokens.isEmpty()) {
            return 0;
        }
        try {
            int deleted = deviceTokenRepository.deleteByTokenIn(invalidTokens);
            log.info("무효 토큰 삭제 {}개 {}", deleted, invalidTokens.stream().map(PushMessages::mask).toList());
            return deleted;
        } catch (Exception e) {
            log.warn("무효 토큰 삭제 실패 count={}", invalidTokens.size(), e);
            return 0;
        }
    }

    private static boolean isEnabled(User user, NotificationType type) {
        return switch (type) {
            case CAPTURE -> user.isCaptureNotifyOn();
            case VLOG_DONE -> user.isCompleteNotifyOn();
        };
    }

    private static Duration ttl(NotificationType type) {
        return switch (type) {
            case CAPTURE -> Duration.ofMinutes(10);
            case VLOG_DONE -> Duration.ofDays(1);
        };
    }
}
