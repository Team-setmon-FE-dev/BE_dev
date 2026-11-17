package com.example.setmon.notification;

import java.time.LocalDate;
import java.util.Collection;

/**
 * 푸시 발송 공용 함수.
 * 사용자 알림 설정(captureNotifyOn·completeNotifyOn)은 구현체가 확인한다.
 * 발송 실패가 있어도 예외를 던지지 않는다(호출부인 워커·스케줄러를 깨지 않기 위해).
 *
 * <pre>
 * pushSender.sendToUser(job.userId(), NotificationType.VLOG_DONE, job.date());
 * </pre>
 */
public interface PushSender {

    /** @param date 알림 대상 날짜(KST). data의 date 값으로 나간다. */
    void sendToUser(Long userId, NotificationType type, LocalDate date);

    /** @param date 알림 대상 날짜(KST). data의 date 값으로 나간다. */
    void sendToUsers(Collection<Long> userIds, NotificationType type, LocalDate date);
}
