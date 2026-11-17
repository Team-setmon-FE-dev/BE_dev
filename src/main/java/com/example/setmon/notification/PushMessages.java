package com.example.setmon.notification;

import com.example.setmon.common.config.TimeConfig;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/** 알림 종류별 data 페이로드(type·date·title·body). FCM·로그 구현이 같이 쓴다. */
final class PushMessages {

    static final String KEY_TYPE = "type";
    static final String KEY_DATE = "date";
    static final String KEY_TITLE = "title";
    static final String KEY_BODY = "body";

    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH");
    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("M월 d일");

    private PushMessages() {
    }

    static Map<String, String> data(NotificationType type, LocalDate date, Clock clock) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(KEY_TYPE, type.name());
        data.put(KEY_DATE, date.toString()); // yyyy-MM-dd (KST)
        switch (type) {
            case CAPTURE -> {
                data.put(KEY_TITLE, "지금 이 순간을 찍어주세요");
                data.put(KEY_BODY, LocalTime.now(clock.withZone(TimeConfig.KST)).format(HOUR) + "시의 1초를 남겨요");
            }
            case VLOG_DONE -> {
                data.put(KEY_TITLE, "오늘의 브이로그가 완성됐어요");
                data.put(KEY_BODY, date.format(MONTH_DAY) + " 브이로그를 확인해보세요");
            }
        }
        return data;
    }

    /** 로그용: 토큰은 앞 8자리만 남긴다. */
    static String mask(String token) {
        if (token == null) {
            return "null";
        }
        return (token.length() <= 8 ? token : token.substring(0, 8)) + "...";
    }
}
