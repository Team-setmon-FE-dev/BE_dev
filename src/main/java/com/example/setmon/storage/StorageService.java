package com.example.setmon.storage;

import java.time.Duration;

/**
 * S3 접근 (2주차 구현).
 * 키 규칙: users/{userId}/clips/{yyyy-MM-dd}/{uuid}.mp4|jpg, users/{userId}/vlogs/{yyyy-MM-dd}/{uuid}.mp4|jpg
 * → 탈퇴 시 users/{userId}/ 접두어만 지우면 된다.
 */
public interface StorageService {

    String presignPut(String key, String contentType, Duration ttl);

    String presignGet(String key, Duration ttl);

    boolean exists(String key);

    void delete(String key);

    void deleteByPrefix(String prefix);
}
