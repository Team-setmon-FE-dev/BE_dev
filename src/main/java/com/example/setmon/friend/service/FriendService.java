package com.example.setmon.friend.service;

import com.example.setmon.common.exception.BusinessException;
import com.example.setmon.common.exception.ErrorCode;
import com.example.setmon.friend.dto.FriendRequestResponse;
import com.example.setmon.friend.dto.FriendRequestResultResponse;
import com.example.setmon.friend.dto.FriendResponse;
import com.example.setmon.friend.entity.Friendship;
import com.example.setmon.friend.entity.FriendshipStatus;
import com.example.setmon.friend.repository.FriendshipRepository;
import com.example.setmon.user.entity.User;
import com.example.setmon.user.service.UserService;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserService userService;

    public boolean areFriends(Long userId, Long otherId) {
        return friendshipRepository.existsBetweenWithStatus(userId, otherId, FriendshipStatus.ACCEPTED);
    }

    /** 친구 코드로 요청. 상대가 이미 나에게 요청해 뒀다면 바로 수락한다. */
    @Transactional
    public FriendRequestResultResponse sendRequest(Long userId, String code) {
        User target = userService.findByFriendCode(code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_CODE_NOT_FOUND));
        if (target.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.SELF_FRIEND_REQUEST);
        }
        Optional<Friendship> existing = friendshipRepository.findBetween(userId, target.getId());
        if (existing.isPresent()) {
            Friendship f = existing.get();
            if (f.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new BusinessException(ErrorCode.ALREADY_FRIEND);
            }
            if (f.getRequesterId().equals(userId)) {
                throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
            }
            f.accept(); // 상대가 보낸 요청이 대기 중 → 맞요청 = 수락
            return new FriendRequestResultResponse(f.getId(), f.getStatus());
        }
        try {
            Friendship saved = friendshipRepository.saveAndFlush(Friendship.request(userId, target.getId()));
            return new FriendRequestResultResponse(saved.getId(), saved.getStatus());
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
        }
    }

    public List<FriendRequestResponse> getReceivedRequests(Long userId) {
        List<Friendship> requests =
                friendshipRepository.findAllByAddresseeIdAndStatusOrderByCreatedAtDesc(userId, FriendshipStatus.PENDING);
        Map<Long, User> users = userService.getUsersByIds(requests.stream().map(Friendship::getRequesterId).toList());
        return requests.stream()
                .filter(f -> users.containsKey(f.getRequesterId()))
                .map(f -> new FriendRequestResponse(f.getId(), f.getRequesterId(),
                        users.get(f.getRequesterId()).getNickname(), f.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void accept(Long userId, Long requestId) {
        getReceivedPending(userId, requestId).accept();
    }

    @Transactional
    public void reject(Long userId, Long requestId) {
        friendshipRepository.delete(getReceivedPending(userId, requestId));
    }

    public List<FriendResponse> getFriends(Long userId) {
        List<Friendship> friendships = friendshipRepository.findAllOfUserWithStatus(userId, FriendshipStatus.ACCEPTED);
        Map<Long, User> users = userService.getUsersByIds(friendships.stream().map(f -> otherOf(f, userId)).toList());
        return friendships.stream()
                .filter(f -> users.containsKey(otherOf(f, userId)))
                .map(f -> {
                    User friend = users.get(otherOf(f, userId));
                    return new FriendResponse(friend.getId(), friend.getNickname(), f.getUpdatedAt());
                })
                .sorted(Comparator.comparing(FriendResponse::nickname))
                .toList();
    }

    /** 친구 삭제 — 행을 지워서 양쪽 모두 끊긴다. */
    @Transactional
    public void deleteFriend(Long userId, Long friendUserId) {
        Friendship f = friendshipRepository.findBetween(userId, friendUserId)
                .filter(x -> x.getStatus() == FriendshipStatus.ACCEPTED)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_NOT_FOUND));
        friendshipRepository.delete(f);
    }

    /** 탈퇴용: 보낸·받은 요청과 친구 관계 모두 삭제 */
    @Transactional
    public void deleteAllOf(Long userId) {
        friendshipRepository.deleteAllOfUser(userId);
    }

    private Friendship getReceivedPending(Long userId, Long requestId) {
        return friendshipRepository.findById(requestId)
                .filter(f -> f.getAddresseeId().equals(userId) && f.getStatus() == FriendshipStatus.PENDING)
                .orElseThrow(() -> new BusinessException(ErrorCode.REQUEST_NOT_FOUND));
    }

    private static Long otherOf(Friendship f, Long userId) {
        return f.getRequesterId().equals(userId) ? f.getAddresseeId() : f.getRequesterId();
    }
}
