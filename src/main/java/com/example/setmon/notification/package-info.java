/**
 * 푸시 알림(FCM).
 * <ul>
 *   <li>{@link com.example.setmon.notification.PushSender} — 공용 발송 함수. 워커(완성 알림)·스케줄러(촬영 알림)가 호출한다.
 *       발송 실패는 로그만 남기고 예외를 던지지 않는다.</li>
 *   <li>{@link com.example.setmon.notification.FcmPushSender} — {@code fcm.key-path}가 있을 때 실제 FCM 발송(data-only).
 *       사용자 알림 설정 확인, 500개 단위 sendEach, 무효 토큰(UNREGISTERED·INVALID_ARGUMENT) 삭제.</li>
 *   <li>{@link com.example.setmon.notification.LoggingPushSender} — 키가 없을 때(로컬·테스트) 로그만 남기는 대체 구현.</li>
 *   <li>{@link com.example.setmon.notification.CaptureReminderScheduler} — 매시 정각(KST) 촬영 알림.
 *       {@code notification.capture.enabled=false}로 끈다.</li>
 * </ul>
 * 앱 쪽 data 키·라우팅은 docs/FCM.md 참고.
 */
package com.example.setmon.notification;
