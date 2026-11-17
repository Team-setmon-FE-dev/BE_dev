package com.example.setmon.notification;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/** fcm.key-path가 비어 있지 않으면 실제 FCM, 비어 있으면 로그 대체 구현. */
final class FcmCondition {

    static final String KEY_PATH_PROPERTY = "fcm.key-path";

    private FcmCondition() {
    }

    static boolean configured(ConditionContext context) {
        return StringUtils.hasText(context.getEnvironment().getProperty(KEY_PATH_PROPERTY));
    }

    static class Enabled implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return configured(context);
        }
    }

    static class Disabled implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return !configured(context);
        }
    }
}
