package com.example.setmon.auth.service;

import com.example.setmon.auth.dto.LoginRequest;
import com.example.setmon.auth.dto.SignupRequest;
import com.example.setmon.auth.dto.TokenResponse;
import com.example.setmon.auth.jwt.JwtProvider;
import com.example.setmon.auth.jwt.TokenType;
import com.example.setmon.common.exception.BusinessException;
import com.example.setmon.common.exception.ErrorCode;
import com.example.setmon.user.entity.User;
import com.example.setmon.user.repository.UserRepository;
import com.example.setmon.user.service.FriendCodeGenerator;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final FriendCodeGenerator friendCodeGenerator;

    @Transactional
    public TokenResponse signup(SignupRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }
        User user = User.create(email, passwordEncoder.encode(request.password()),
                request.nickname().trim(), friendCodeGenerator.generate());
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 이메일로 가입한 경우
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }
        return issue(user.getId());
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalize(request.email()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return issue(user.getId());
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        Long userId = jwtProvider.parseUserId(refreshToken, TokenType.REFRESH);
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return issue(userId);
    }

    private TokenResponse issue(Long userId) {
        return new TokenResponse(userId, jwtProvider.createAccessToken(userId), jwtProvider.createRefreshToken(userId));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
