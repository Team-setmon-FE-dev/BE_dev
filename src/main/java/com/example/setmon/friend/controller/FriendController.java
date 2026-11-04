package com.example.setmon.friend.controller;

import com.example.setmon.common.exception.BusinessException;
import com.example.setmon.common.exception.ErrorCode;
import com.example.setmon.friend.dto.FriendRequestCreateRequest;
import com.example.setmon.friend.dto.FriendRequestResponse;
import com.example.setmon.friend.dto.FriendResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
public class FriendController {

    @Operation(summary = "[미구현] 친구 코드로 요청",
            description = "상대가 이미 나에게 요청했다면 바로 수락 처리. 201. 에러: FRIEND_CODE_NOT_FOUND, SELF_FRIEND_REQUEST, ALREADY_FRIEND, DUPLICATE_REQUEST")
    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public void sendRequest(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                            @Valid @RequestBody FriendRequestCreateRequest request) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Operation(summary = "[미구현] 받은 요청 목록")
    @GetMapping("/requests/received")
    public List<FriendRequestResponse> getReceivedRequests(@Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Operation(summary = "[미구현] 요청 수락", description = "204. 에러: REQUEST_NOT_FOUND")
    @PostMapping("/requests/{requestId}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(@Parameter(hidden = true) @AuthenticationPrincipal Long userId, @PathVariable Long requestId) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Operation(summary = "[미구현] 요청 거절", description = "요청 행 삭제. 204. 에러: REQUEST_NOT_FOUND")
    @PostMapping("/requests/{requestId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@Parameter(hidden = true) @AuthenticationPrincipal Long userId, @PathVariable Long requestId) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Operation(summary = "[미구현] 친구 목록")
    @GetMapping
    public List<FriendResponse> getFriends(@Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Operation(summary = "[미구현] 친구 삭제", description = "양쪽 모두 끊김. 204. 에러: FRIEND_NOT_FOUND")
    @DeleteMapping("/{friendUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFriend(@Parameter(hidden = true) @AuthenticationPrincipal Long userId, @PathVariable Long friendUserId) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }
}
