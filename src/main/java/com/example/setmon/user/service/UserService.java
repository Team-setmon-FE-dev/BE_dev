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
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    public Optional<User> findByFriendCode(String friendCode) {
        return userRepository.findByFriendCode(friendCode);
    }

    public Map<Long, User> getUsersByIds(Collection<Long> ids) {
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
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

    /** 탈퇴용: 기기 토큰과 사용자 행 삭제 (나머지는 WithdrawService가 순서대로 처리) */
    @Transactional
    public void deleteAccount(Long userId) {
        deviceTokenRepository.deleteAllOfUser(userId);
        userRepository.delete(getUser(userId));
    }
}
