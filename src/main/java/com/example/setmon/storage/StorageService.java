package com.example.setmon.storage;

import java.nio.file.Path;
import java.time.Duration;

/** 파일 저장소(S3). 키 규칙은 {@link StorageKeys}. */
public interface StorageService {

    String presignPut(String key, String contentType, Duration ttl);

    String presignGet(String key, Duration ttl);

    boolean exists(String key);

    void upload(String key, Path file, String contentType);

    void download(String key, Path target);

    void delete(String key);

    void deleteByPrefix(String prefix);
}
