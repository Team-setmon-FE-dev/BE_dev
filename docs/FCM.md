# FCM 푸시 알림 (A 앱 연동용)

서버는 **data-only 메시지**만 보낸다(`notification` 필드 없음).
앱이 꺼져 있거나 백그라운드에 있어도 `FirebaseMessagingService.onMessageReceived`가 호출되므로,
**앱이 직접 알림(Notification)을 만들어 띄우고, 탭했을 때 이동할 화면을 정해야 한다.**
(시스템이 대신 알림을 띄워 주지 않는다.)

## 알림 종류

| type | 언제 | 탭하면 이동 | TTL |
|------|------|------------|-----|
| `CAPTURE` | 매시 정각(KST). 사용자의 알림 시간대(`notifyStartHour` ~ `notifyEndHour`, 양끝 포함)이고 `captureNotifyOn=true`일 때 | S-04 촬영 화면 — route `capture` | 10분 |
| `VLOG_DONE` | 그날 브이로그 합성이 끝났을 때. `completeNotifyOn=true`일 때 | 하루 상세 — route `day/{myUserId}/{date}` | 1일 |

- 두 메시지 모두 Android priority `HIGH`.
- 알림 설정이 꺼진 사용자에게는 서버가 보내지 않으므로, 앱에서 설정을 다시 확인할 필요는 없다.
- TTL이 지나도록 기기가 오프라인이면 FCM이 버린다(지난 시간의 촬영 알림이 늦게 뜨지 않게).

## data 키

모든 값은 문자열이다(FCM data는 `Map<String, String>`).

| 키 | 값 | 설명 |
|----|----|------|
| `type` | `CAPTURE` \| `VLOG_DONE` | 알림 종류. 라우팅 기준 |
| `date` | `yyyy-MM-dd` | 대상 날짜(**KST**). `VLOG_DONE`이면 `day/{myUserId}/{date}`의 `date` |
| `title` | 문자열 | 알림 제목. 그대로 표시하면 된다 |
| `body` | 문자열 | 알림 본문. 그대로 표시하면 된다 |

- `myUserId`는 data에 없다. 로그인한 내 id(GET `/api/users/me`의 `id`)를 쓴다.
- 알 수 없는 `type`이 오면 무시한다(나중에 종류가 늘어날 수 있음).

### 예시 1 — CAPTURE

```json
{
  "type": "CAPTURE",
  "date": "2026-11-17",
  "title": "지금 이 순간을 찍어주세요",
  "body": "09시의 1초를 남겨요"
}
```

### 예시 2 — VLOG_DONE

```json
{
  "type": "VLOG_DONE",
  "date": "2026-11-17",
  "title": "오늘의 브이로그가 완성됐어요",
  "body": "11월 17일 브이로그를 확인해보세요"
}
```

## 앱 구현 체크리스트

1. `onMessageReceived(remoteMessage)`에서 `remoteMessage.data`를 읽는다.
2. `title`·`body`로 알림을 만든다. Android 13+는 `POST_NOTIFICATIONS` 권한이 필요하다.
3. 탭 `PendingIntent`에 `type`·`date`를 넣고, 앱 진입 시 아래처럼 이동한다.
   - `CAPTURE` → `capture`
   - `VLOG_DONE` → `day/{myUserId}/{date}`
4. 같은 종류 알림이 쌓이지 않게 하려면 notification id를 `type`별로 고정하면 된다(선택).

## 기기 토큰 등록

`PUT /api/users/me/device-token` (인증 필요, 204)

```json
{ "token": "<FCM registration token>" }
```

다음 두 시점에 호출한다.

- **로그인(회원가입) 직후** — `FirebaseMessaging.getInstance().token`으로 받은 토큰
- **`onNewToken(token)`이 호출될 때** — 토큰이 바뀐 경우. 로그인 상태일 때만 보낸다.

같은 토큰을 여러 번 보내도 된다. 같은 기기에서 다른 계정으로 로그인하면 토큰의 소유자가 새 계정으로 바뀐다.
FCM이 무효(`UNREGISTERED`, `INVALID_ARGUMENT`)라고 응답한 토큰은 서버가 자동으로 삭제한다.
탈퇴하면 그 사용자의 토큰은 모두 삭제된다.

## 서버 설정 (참고)

| 환경변수 | 기본값 | 설명 |
|----------|--------|------|
| `FIREBASE_KEY_PATH` | (빈 값) | 서비스 계정 JSON 절대경로. 비어 있으면 실제로 보내지 않고 서버 로그에만 남긴다(로컬·테스트) |
| `CAPTURE_NOTIFY_ENABLED` | `true` | `false`면 매시 촬영 알림 스케줄러를 끈다 |

키 파일은 저장소 밖에 둔다(`*firebase*.json`은 `.gitignore` 대상).
