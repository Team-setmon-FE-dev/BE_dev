package com.example.setmon.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.setmon.common.exception.BusinessException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtMzItYnl0ZXMtbWluaW11bS0hIQ==";

    private final JwtProvider provider =
            new JwtProvider(new JwtProperties(SECRET, Duration.ofHours(1), Duration.ofDays(14)));

    @Test
    void access_토큰에서_userId를_꺼낸다() {
        String token = provider.createAccessToken(42L);
        assertThat(provider.parseUserId(token, TokenType.ACCESS)).isEqualTo(42L);
    }

    @Test
    void refresh_토큰을_access로_쓰면_거절한다() {
        String refresh = provider.createRefreshToken(42L);
        assertThatThrownBy(() -> provider.parseUserId(refresh, TokenType.ACCESS))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 만료된_토큰은_거절한다() {
        JwtProvider expired = new JwtProvider(new JwtProperties(SECRET, Duration.ofSeconds(-1), Duration.ofDays(14)));
        String token = expired.createAccessToken(1L);
        assertThatThrownBy(() -> provider.parseUserId(token, TokenType.ACCESS))
                .isInstanceOf(BusinessException.class);
    }
}
