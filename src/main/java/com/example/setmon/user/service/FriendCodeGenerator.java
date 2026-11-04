package com.example.setmon.user.service;

import com.example.setmon.user.repository.UserRepository;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 8자, 헷갈리는 글자(0 O 1 I L) 제외 */
@Component
@RequiredArgsConstructor
public class FriendCodeGenerator {

    private static final String CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 8;
    private static final int MAX_ATTEMPTS = 10;

    private final SecureRandom random = new SecureRandom();
    private final UserRepository userRepository;

    public String generate() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String code = randomCode();
            if (!userRepository.existsByFriendCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("친구 코드 생성 실패");
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
