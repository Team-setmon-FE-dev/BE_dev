package com.example.setmon.user.dto;

import com.example.setmon.user.entity.User;

public record MeResponse(
        Long id,
        String email,
        String nickname,
        String friendCode,
        int notifyStartHour,
        int notifyEndHour,
        boolean captureNotifyOn,
        boolean completeNotifyOn,
        long totalRecordDays
) {
    public static MeResponse of(User user, long totalRecordDays) {
        return new MeResponse(user.getId(), user.getEmail(), user.getNickname(), user.getFriendCode(),
                user.getNotifyStartHour(), user.getNotifyEndHour(),
                user.isCaptureNotifyOn(), user.isCompleteNotifyOn(), totalRecordDays);
    }
}
