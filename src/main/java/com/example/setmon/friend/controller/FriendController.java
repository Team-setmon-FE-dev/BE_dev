package com.example.setmon.friend.controller;

import com.example.setmon.friend.dto.FriendRequestCreateRequest;
import com.example.setmon.friend.dto.FriendRequestResponse;
import com.example.setmon.friend.dto.FriendRequestResultResponse;
import com.example.setmon.friend.dto.FriendResponse;
import com.example.setmon.friend.service.FriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "친구")
@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @Operation(summary = "친구 코드로 요청", description = """
            코드는 대소문자 무관. 상대가 이미 나에게 요청해 뒀다면 바로 친구가 된다(status=ACCEPTED). 201.
            에러: FRIEND_CODE_NOT_FOUND, SELF_FRIEND_REQUEST, ALREADY_FRIEND, DUPLICATE_REQUEST""")
    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public FriendRequestResultResponse sendRequest(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                                                   @Valid @RequestBody FriendRequestCreateRequest request) {
        return friendService.sendRequest(userId, request.code());
    }

    @Operation(summary = "받은 요청 목록", description = "대기 중인 요청만, 최신순")
    @GetMapping("/requests/received")
    public List<FriendRequestResponse> getReceivedRequests(@Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        return friendService.getReceivedRequests(userId);
    }

    @Operation(summary = "요청 수락", description = "나에게 온 대기 요청만. 204. 에러: REQUEST_NOT_FOUND")
    @PostMapping("/requests/{requestId}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(@Parameter(hidden = true) @AuthenticationPrincipal Long userId, @PathVariable Long requestId) {
        friendService.accept(userId, requestId);
    }

    @Operation(summary = "요청 거절", description = "요청 삭제(상대에게 알림 없음, 다시 요청 가능). 204. 에러: REQUEST_NOT_FOUND")
    @PostMapping("/requests/{requestId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@Parameter(hidden = true) @AuthenticationPrincipal Long userId, @PathVariable Long requestId) {
        friendService.reject(userId, requestId);
    }

    @Operation(summary = "친구 목록", description = "닉네임순")
    @GetMapping
    public List<FriendResponse> getFriends(@Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        return friendService.getFriends(userId);
    }

    @Operation(summary = "친구 삭제", description = "양쪽 모두 끊김. 204. 에러: FRIEND_NOT_FOUND")
    @DeleteMapping("/{friendUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFriend(@Parameter(hidden = true) @AuthenticationPrincipal Long userId, @PathVariable Long friendUserId) {
        friendService.deleteFriend(userId, friendUserId);
    }
}
