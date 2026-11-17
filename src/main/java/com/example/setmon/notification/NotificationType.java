package com.example.setmon.notification;

/** FCM data의 type 값. 앱은 이 값으로 알림 문구·이동 화면을 정한다(docs/FCM.md). */
public enum NotificationType {
    /** 매시 촬영 알림 → 촬영 화면(S-04) */
    CAPTURE,
    /** 브이로그 완성 알림 → 하루 상세 */
    VLOG_DONE
}
