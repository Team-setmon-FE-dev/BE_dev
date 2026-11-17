package com.example.setmon.notification;

import com.example.setmon.common.config.TimeConfig;
import com.example.setmon.user.entity.DeviceToken;
import com.example.setmon.user.entity.User;
import com.example.setmon.user.repository.DeviceTokenRepository;
import com.example.setmon.user.repository.UserRepository;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FcmPushSenderTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 17);

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    private FcmPushSender sender;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(ZonedDateTime.of(2026, 11, 17, 9, 0, 0, 0, TimeConfig.KST).toInstant(), ZoneOffset.UTC);
        sender = new FcmPushSender(firebaseMessaging, userRepository, deviceTokenRepository, clock);
    }

    @Test
    @DisplayName("UNREGISTERED로 실패한 토큰은 DB에서 삭제하고, 일반 실패 토큰은 남긴다")
    void deletesUnregisteredTokens() throws Exception {
        given(userRepository.findAllById(List.of(1L))).willReturn(List.of(user(1L, true, true)));
        given(deviceTokenRepository.findAllByUserIdIn(List.of(1L))).willReturn(List.of(
                DeviceToken.create(1L, "ok-token-aaaaaaaa"),
                DeviceToken.create(1L, "dead-token-bbbbbbbb"),
                DeviceToken.create(1L, "busy-token-cccccccc")));
        BatchResponse batch = mock(BatchResponse.class);
        List<SendResponse> responses = List.of(
                success(),
                failure(MessagingErrorCode.UNREGISTERED),
                failure(MessagingErrorCode.UNAVAILABLE));
        given(batch.getResponses()).willReturn(responses);
        given(firebaseMessaging.sendEach(anyList())).willReturn(batch);
        given(deviceTokenRepository.deleteByTokenIn(anyCollection())).willReturn(1);

        sender.sendToUser(1L, NotificationType.CAPTURE, DATE);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> deleted = ArgumentCaptor.forClass(Collection.class);
        verify(deviceTokenRepository).deleteByTokenIn(deleted.capture());
        assertThat(deleted.getValue()).containsExactly("dead-token-bbbbbbbb");
    }

    @Test
    @DisplayName("INVALID_ARGUMENT 토큰도 삭제한다")
    void deletesInvalidArgumentTokens() throws Exception {
        given(userRepository.findAllById(List.of(1L))).willReturn(List.of(user(1L, true, true)));
        given(deviceTokenRepository.findAllByUserIdIn(List.of(1L)))
                .willReturn(List.of(DeviceToken.create(1L, "bad-token-dddddddd")));
        BatchResponse batch = mock(BatchResponse.class);
        List<SendResponse> responses = List.of(failure(MessagingErrorCode.INVALID_ARGUMENT));
        given(batch.getResponses()).willReturn(responses);
        given(firebaseMessaging.sendEach(anyList())).willReturn(batch);

        sender.sendToUser(1L, NotificationType.VLOG_DONE, DATE);

        verify(deviceTokenRepository).deleteByTokenIn(List.of("bad-token-dddddddd"));
    }

    @Test
    @DisplayName("captureNotifyOn=false면 촬영 알림을 보내지 않는다")
    void skipsCaptureWhenOff() throws Exception {
        given(userRepository.findAllById(List.of(1L))).willReturn(List.of(user(1L, false, true)));

        sender.sendToUser(1L, NotificationType.CAPTURE, DATE);

        verify(deviceTokenRepository, never()).findAllByUserIdIn(anyCollection());
        verify(firebaseMessaging, never()).sendEach(anyList());
    }

    @Test
    @DisplayName("completeNotifyOn=false면 완성 알림을 보내지 않고, 켜진 사용자에게만 보낸다")
    void skipsVlogDoneWhenOff() throws Exception {
        given(userRepository.findAllById(List.of(1L, 2L)))
                .willReturn(List.of(user(1L, true, false), user(2L, true, true)));
        given(deviceTokenRepository.findAllByUserIdIn(List.of(2L)))
                .willReturn(List.of(DeviceToken.create(2L, "token-of-user-2xxxx")));
        BatchResponse batch = mock(BatchResponse.class);
        List<SendResponse> responses = List.of(success());
        given(batch.getResponses()).willReturn(responses);
        given(firebaseMessaging.sendEach(anyList())).willReturn(batch);

        sender.sendToUsers(List.of(1L, 2L), NotificationType.VLOG_DONE, DATE);

        verify(deviceTokenRepository).findAllByUserIdIn(List.of(2L));
        verify(firebaseMessaging).sendEach(anyList());
        verify(deviceTokenRepository, never()).deleteByTokenIn(anyCollection());
    }

    @Test
    @DisplayName("토큰이 500개를 넘으면 나눠서 보낸다")
    void sendsInChunksOf500() throws Exception {
        given(userRepository.findAllById(List.of(1L))).willReturn(List.of(user(1L, true, true)));
        List<DeviceToken> tokens = IntStream.range(0, 501)
                .mapToObj(i -> DeviceToken.create(1L, "token-%05d-xxxxxxxx".formatted(i)))
                .toList();
        given(deviceTokenRepository.findAllByUserIdIn(List.of(1L))).willReturn(tokens);
        given(firebaseMessaging.sendEach(anyList())).willAnswer(inv -> {
            List<Message> messages = inv.getArgument(0);
            BatchResponse batch = mock(BatchResponse.class);
            List<SendResponse> responses = messages.stream().map(m -> success()).toList();
            given(batch.getResponses()).willReturn(responses);
            return batch;
        });

        sender.sendToUser(1L, NotificationType.CAPTURE, DATE);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
        verify(firebaseMessaging, times(2)).sendEach(captor.capture());
        assertThat(captor.getAllValues()).extracting(List::size).containsExactly(500, 1);
    }

    @Test
    @DisplayName("FCM 호출이 예외를 던져도 밖으로 전파하지 않는다")
    void swallowsErrors() throws Exception {
        given(userRepository.findAllById(List.of(1L))).willReturn(List.of(user(1L, true, true)));
        given(deviceTokenRepository.findAllByUserIdIn(List.of(1L)))
                .willReturn(List.of(DeviceToken.create(1L, "token-eeeeeeeeeeee")));
        FirebaseMessagingException ex = mock(FirebaseMessagingException.class);
        given(firebaseMessaging.sendEach(anyList())).willThrow(ex);

        assertThatCode(() -> sender.sendToUser(1L, NotificationType.CAPTURE, DATE)).doesNotThrowAnyException();
        verify(deviceTokenRepository, never()).deleteByTokenIn(any());
    }

    @Test
    @DisplayName("data 페이로드: type·date(KST)·title·body")
    void payload() {
        Clock clock = Clock.fixed(ZonedDateTime.of(2026, 11, 17, 9, 0, 0, 0, TimeConfig.KST).toInstant(), ZoneOffset.UTC);

        assertThat(PushMessages.data(NotificationType.CAPTURE, DATE, clock)).containsExactlyEntriesOf(
                orderedMap("CAPTURE", "2026-11-17", "지금 이 순간을 찍어주세요", "09시의 1초를 남겨요"));
        assertThat(PushMessages.data(NotificationType.VLOG_DONE, DATE, clock)).containsExactlyEntriesOf(
                orderedMap("VLOG_DONE", "2026-11-17", "오늘의 브이로그가 완성됐어요", "11월 17일 브이로그를 확인해보세요"));
    }

    private static java.util.Map<String, String> orderedMap(String type, String date, String title, String body) {
        java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
        m.put("type", type);
        m.put("date", date);
        m.put("title", title);
        m.put("body", body);
        return m;
    }

    private static User user(Long id, boolean captureOn, boolean completeOn) {
        User user = User.create("u" + id + "@test.com", "hash", "u" + id, "CODE000" + id);
        user.updateSettings(9, 23, captureOn, completeOn);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static SendResponse success() {
        SendResponse r = mock(SendResponse.class);
        given(r.isSuccessful()).willReturn(true);
        return r;
    }

    private static SendResponse failure(MessagingErrorCode code) {
        FirebaseMessagingException ex = mock(FirebaseMessagingException.class);
        given(ex.getMessagingErrorCode()).willReturn(code);
        SendResponse r = mock(SendResponse.class);
        given(r.isSuccessful()).willReturn(false);
        given(r.getException()).willReturn(ex);
        return r;
    }
}
