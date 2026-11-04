package com.example.setmon.user.controller;

import com.example.setmon.user.dto.DeviceTokenRequest;
import com.example.setmon.user.dto.MeResponse;
import com.example.setmon.user.dto.UpdateSettingsRequest;
import com.example.setmon.user.service.UserService;
import com.example.setmon.user.service.WithdrawService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "내 정보")
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final WithdrawService withdrawService;

    @Operation(summary = "내 정보 + 총 기록 일수 (S-12)")
    @GetMapping
    public MeResponse getMe(@Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        return userService.getMe(userId);
    }

    @Operation(summary = "탈퇴 (F-04)", description = "클립·브이로그(S3 포함), 친구 관계·요청, 기기 토큰, 사용자 정보 모두 삭제. 204. 앱은 토큰을 지우고 로그인 화면으로")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        withdrawService.withdraw(userId);
    }

    @Operation(summary = "알림 설정 (F-13, F-28)", description = "에러: INVALID_INPUT(시작 > 끝)")
    @PutMapping("/settings")
    public MeResponse updateSettings(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                                     @Valid @RequestBody UpdateSettingsRequest request) {
        return userService.updateSettings(userId, request);
    }

    @Operation(summary = "FCM 기기 토큰 등록·갱신 (F-13)", description = "로그인 직후, 토큰이 바뀔 때 호출. 204")
    @PutMapping("/device-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerDeviceToken(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                                    @Valid @RequestBody DeviceTokenRequest request) {
        userService.registerDeviceToken(userId, request.token());
    }
}
