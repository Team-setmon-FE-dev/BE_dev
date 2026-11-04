package com.example.setmon.user.service;

import com.example.setmon.clip.service.ClipService;
import com.example.setmon.common.exception.BusinessException;
import com.example.setmon.common.exception.ErrorCode;
import com.example.setmon.user.dto.MeResponse;
import com.example.setmon.user.dto.UpdateSettingsRequest;
import com.example.setmon.user.entity.DeviceToken;
import com.example.setmon.user.entity.User;
import com.example.setmon.user.repository.DeviceTokenRepository;
import com.example.setmon.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final ClipService clipService;

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    public MeResponse getMe(Long userId) {
        return MeResponse.of(getUser(userId), clipService.countRecordDays(userId));
    }

    @Transactional
    public MeResponse updateSettings(Long userId, UpdateSettingsRequest request) {
        if (request.notifyStartHour() > request.notifyEndHour()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "알림 시작 시각은 끝 시각보다 늦을 수 없습니다.");
        }
        User user = getUser(userId);
        user.updateSettings(request.notifyStartHour(), request.notifyEndHour(),
                request.captureNotifyOn(), request.completeNotifyOn());
        return MeResponse.of(user, clipService.countRecordDays(userId));
    }

    @Transactional
    public void registerDeviceToken(Long userId, String token) {
        deviceTokenRepository.findByToken(token).ifPresentOrElse(
                existing -> existing.assignTo(userId),
                () -> deviceTokenRepository.save(DeviceToken.create(userId, token)));
    }

    // TODO(3주차): 탈퇴 — S3(users/{id}/), 친구 관계, 기기 토큰, 클립·브이로그, 사용자 삭제
}
