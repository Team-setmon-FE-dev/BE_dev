# 에러 코드

모든 에러 응답은 같은 모양이다.

```json
{ "code": "FORBIDDEN", "message": "접근 권한이 없습니다." }
```

- 앱은 **`code`로 분기**하고, `message`는 그대로 보여줘도 된다.
- `message`는 기본 문구와 다를 수 있다. 예: validation 실패 → `"email: 올바른 형식의 이메일 주소여야 합니다"`, 알림 시간 오류 → `"알림 시작 시각은 끝 시각보다 늦을 수 없습니다."`
- 인증이 필요한 API는 Spring Security가 먼저 검사한다. 토큰이 없거나 잘못됐으면 경로가 없더라도 404가 아니라 401이 나간다.

## 공통

| 코드 | HTTP | 기본 메시지 | 발생 API |
|------|------|-------------|----------|
| `INVALID_INPUT` | 400 | 요청 값이 올바르지 않습니다. | 전체. 바디 validation 실패, JSON 형식 오류, 경로·쿼리 값 형식 오류(예: `month=2026-13`), 필수 쿼리 누락(예: 월별 조회의 `month`). `PUT /api/users/me/settings`에서 시작 시각 > 끝 시각 |
| `UNAUTHORIZED` | 401 | 로그인이 필요합니다. | `/api/auth/**`를 뺀 전체. `Authorization` 헤더가 없을 때 |
| `TOKEN_EXPIRED` | 401 | 토큰이 만료되었습니다. | 인증이 필요한 전체(Access 만료). `POST /api/auth/refresh`(Refresh 만료) |
| `INVALID_TOKEN` | 401 | 유효하지 않은 토큰입니다. | 인증이 필요한 전체(위조, 형식 오류, Refresh 토큰을 Access로 사용). `POST /api/auth/refresh`(토큰 오류, 탈퇴한 사용자) |
| `FORBIDDEN` | 403 | 접근 권한이 없습니다. | `GET /api/users/{userId}/days`, `GET /api/users/{userId}/days/{date}`(본인도 수락된 친구도 아닐 때), `DELETE /api/clips/{clipId}`(남의 클립) |
| `NOT_FOUND` | 404 | 요청한 경로를 찾을 수 없습니다. | 없는 경로 |
| `METHOD_NOT_ALLOWED` | 405 | 지원하지 않는 HTTP 메서드입니다. | 경로는 있지만 메서드가 다를 때(예: `GET /api/clips`) |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | 지원하지 않는 Content-Type입니다. application/json으로 보내주세요. | 바디가 있는 API에 JSON이 아닌 Content-Type을 보냈을 때 |
| `NOT_IMPLEMENTED` | 501 | 아직 구현되지 않은 API입니다. | 현재 쓰는 곳 없음 |
| `INTERNAL_ERROR` | 500 | 서버 오류가 발생했습니다. | 전체(예상하지 못한 서버 오류) |

## 계정

| 코드 | HTTP | 기본 메시지 | 발생 API |
|------|------|-------------|----------|
| `USER_NOT_FOUND` | 404 | 사용자를 찾을 수 없습니다. | `GET /api/users/me`, `PUT /api/users/me/settings`, `DELETE /api/users/me`(탈퇴 직후의 토큰 등 사용자 행이 없을 때). `GET /api/users/{userId}/days`, `GET /api/users/{userId}/days/{date}`(본인 id인데 사용자가 없을 때. 남의 id면 FORBIDDEN이 먼저 나감) |
| `EMAIL_DUPLICATED` | 409 | 이미 사용 중인 이메일입니다. | `POST /api/auth/signup` |
| `INVALID_CREDENTIALS` | 401 | 이메일 또는 비밀번호가 올바르지 않습니다. | `POST /api/auth/login` |

## 클립

| 코드 | HTTP | 기본 메시지 | 발생 API |
|------|------|-------------|----------|
| `PAST_SLOT` | 400 | 지금 시간대에만 올릴 수 있습니다. | `POST /api/clips` |
| `CLIP_TOO_SHORT` | 400 | 1초 이상 찍어주세요. | `POST /api/clips` |
| `CLIP_TOO_LONG` | 400 | 2초 이하로 찍어주세요. | `POST /api/clips` |
| `INVALID_UPLOAD_KEY` | 400 | 업로드 키가 올바르지 않습니다. | `POST /api/clips`(본인 경로가 아니거나 확장자가 틀린 키, S3에 파일이 없을 때 — 이 경우 메시지는 "S3에 파일이 없습니다. 업로드 후 등록해주세요.") |
| `CLIP_NOT_FOUND` | 404 | 클립을 찾을 수 없습니다. | `DELETE /api/clips/{clipId}` |
| `FUTURE_RECORDED_AT` | 400 | 촬영 시각이 현재보다 미래입니다. | `POST /api/clips` |
| `CLIP_UPLOAD_CONFLICT` | 409 | 같은 시간대에 동시에 업로드되었습니다. 다시 시도해주세요. | `POST /api/clips` |

## 브이로그

| 코드 | HTTP | 기본 메시지 | 발생 API |
|------|------|-------------|----------|
| `NO_CLIPS` | 400 | 오늘 올린 클립이 없습니다. | `POST /api/users/me/days/today/vlog` |

## 친구

| 코드 | HTTP | 기본 메시지 | 발생 API |
|------|------|-------------|----------|
| `FRIEND_CODE_NOT_FOUND` | 404 | 존재하지 않는 친구 코드입니다. | `POST /api/friends/requests` |
| `SELF_FRIEND_REQUEST` | 400 | 자기 자신에게는 친구 요청을 보낼 수 없습니다. | `POST /api/friends/requests` |
| `ALREADY_FRIEND` | 409 | 이미 친구입니다. | `POST /api/friends/requests` |
| `DUPLICATE_REQUEST` | 409 | 이미 친구 요청을 보냈거나 받았습니다. | `POST /api/friends/requests` |
| `REQUEST_NOT_FOUND` | 404 | 친구 요청을 찾을 수 없습니다. | `POST /api/friends/requests/{requestId}/accept`, `POST /api/friends/requests/{requestId}/reject`(없거나, 나에게 온 요청이 아니거나, 이미 처리된 요청) |
| `FRIEND_NOT_FOUND` | 404 | 친구 관계가 아닙니다. | `DELETE /api/friends/{friendUserId}` |
