package com.example.setmon.auth.controller;

import com.example.setmon.auth.dto.LoginRequest;
import com.example.setmon.auth.dto.RefreshRequest;
import com.example.setmon.auth.dto.SignupRequest;
import com.example.setmon.auth.dto.TokenResponse;
import com.example.setmon.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증")
@SecurityRequirements
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입 (F-01)", description = "가입 후 바로 토큰 발급. 에러: INVALID_INPUT, EMAIL_DUPLICATED")
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @Operation(summary = "로그인 (F-02)", description = "에러: INVALID_INPUT, INVALID_CREDENTIALS")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "토큰 재발급 (F-03)", description = "Access·Refresh 모두 새로 발급. 에러: TOKEN_EXPIRED, INVALID_TOKEN")
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }
}
