package com.example.setmon.notification;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * fcm.key-path(FIREBASE_KEY_PATH)가 있을 때만 FirebaseApp·FirebaseMessaging 빈을 만든다.
 * 없으면 빈이 없고 {@link LoggingPushSender}가 대신 쓰인다(로컬·테스트).
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@Conditional(FcmCondition.Enabled.class)
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp(@Value("${" + FcmCondition.KEY_PATH_PROPERTY + "}") String keyPath) throws IOException {
        for (FirebaseApp app : FirebaseApp.getApps()) {
            if (FirebaseApp.DEFAULT_APP_NAME.equals(app.getName())) {
                return app; // 이미 초기화돼 있으면 재사용 (테스트 컨텍스트 재생성 등)
            }
        }
        try (InputStream in = Files.newInputStream(Path.of(keyPath.trim()))) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .build();
            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("Firebase 초기화 완료");
            return app;
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
