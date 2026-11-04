package com.example.setmon.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_IMPLEMENTED(HttpStatus.NOT_IMPLEMENTED, "아직 구현되지 않은 API입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // 계정
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),

    // 클립
    PAST_SLOT(HttpStatus.BAD_REQUEST, "지금 시간대에만 올릴 수 있습니다."),
    CLIP_TOO_SHORT(HttpStatus.BAD_REQUEST, "2초 이상 찍어주세요."),
    CLIP_TOO_LONG(HttpStatus.BAD_REQUEST, "4초 이하로 찍어주세요."),
    INVALID_UPLOAD_KEY(HttpStatus.BAD_REQUEST, "업로드 키가 올바르지 않습니다."),
    CLIP_NOT_FOUND(HttpStatus.NOT_FOUND, "클립을 찾을 수 없습니다."),

    // 브이로그
    NO_CLIPS(HttpStatus.BAD_REQUEST, "오늘 올린 클립이 없습니다."),

    // 친구
    FRIEND_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 친구 코드입니다."),
    SELF_FRIEND_REQUEST(HttpStatus.BAD_REQUEST, "자기 자신에게는 친구 요청을 보낼 수 없습니다."),
    ALREADY_FRIEND(HttpStatus.CONFLICT, "이미 친구입니다."),
    DUPLICATE_REQUEST(HttpStatus.CONFLICT, "이미 친구 요청을 보냈거나 받았습니다."),
    REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "친구 요청을 찾을 수 없습니다."),
    FRIEND_NOT_FOUND(HttpStatus.NOT_FOUND, "친구 관계가 아닙니다.");

    private final HttpStatus status;
    private final String message;
}
