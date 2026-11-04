package com.example.setmon.user.service;

import com.example.setmon.clip.service.ClipService;
import com.example.setmon.common.tx.AfterCommit;
import com.example.setmon.friend.service.FriendService;
import com.example.setmon.storage.StorageKeys;
import com.example.setmon.storage.StorageService;
import com.example.setmon.vlog.service.VlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 탈퇴 (F-04). UserService ↔ FriendService 순환을 피하려고 분리.
 * DB는 한 트랜잭션으로 지우고, S3(users/{id}/)는 커밋이 끝난 뒤에 지운다.
 */
@Service
@RequiredArgsConstructor
public class WithdrawService {

    private final UserService userService;
    private final FriendService friendService;
    private final ClipService clipService;
    private final VlogService vlogService;
    private final StorageService storageService;

    @Transactional
    public void withdraw(Long userId) {
        userService.getUser(userId);
        friendService.deleteAllOf(userId);
        vlogService.deleteAllOf(userId);
        clipService.deleteAllOf(userId);
        userService.deleteAccount(userId);
        AfterCommit.run(() -> storageService.deleteByPrefix(StorageKeys.userPrefix(userId)));
    }
}
