package com.example.setmon.storage;

import java.time.LocalDate;
import java.util.UUID;

/**
 * S3 키 규칙. 사용자별 접두어(users/{userId}/) 아래에만 둔다 → 탈퇴 시 접두어 하나만 지우면 끝.
 */
public final class StorageKeys {

    public static final String VIDEO_EXT = ".mp4";
    public static final String THUMB_EXT = ".jpg";

    private StorageKeys() {
    }

    public static String userPrefix(Long userId) {
        return "users/" + userId + "/";
    }

    public static String clipPrefix(Long userId) {
        return userPrefix(userId) + "clips/";
    }

    public static String newClipBase(Long userId, LocalDate date) {
        return clipPrefix(userId) + date + "/" + UUID.randomUUID();
    }

    public static String newVlogBase(Long userId, LocalDate date) {
        return userPrefix(userId) + "vlogs/" + date + "/" + UUID.randomUUID();
    }

    /** 앱이 보낸 키가 본인 클립 경로이고 확장자가 맞는지 */
    public static boolean isOwnClipKey(Long userId, String key, String ext) {
        return key != null
                && key.startsWith(clipPrefix(userId))
                && key.endsWith(ext)
                && !key.contains("..")
                && !key.contains("//");
    }
}
