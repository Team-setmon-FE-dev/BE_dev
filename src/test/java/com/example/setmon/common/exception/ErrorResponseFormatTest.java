package com.example.setmon.common.exception;

import com.example.setmon.auth.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 프레임워크 단 에러도 {code, message} 모양으로 나가는지 */
@SpringBootTest
@AutoConfigureMockMvc
class ErrorResponseFormatTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    private String bearer;

    @BeforeEach
    void setUp() {
        bearer = "Bearer " + jwtProvider.createAccessToken(999_999L);
    }

    @Test
    @DisplayName("토큰 없음 → 401 UNAUTHORIZED")
    void unauthorized() throws Exception {
        expect(mockMvc.perform(get("/api/users/me")), 401, "UNAUTHORIZED");
    }

    @Test
    @DisplayName("잘못된 토큰 → 401 INVALID_TOKEN")
    void invalidToken() throws Exception {
        expect(mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer abc.def.ghi")), 401, "INVALID_TOKEN");
    }

    @Test
    @DisplayName("없는 경로 → 404 NOT_FOUND")
    void notFound() throws Exception {
        expect(mockMvc.perform(get("/api/nope").header("Authorization", bearer)), 404, "NOT_FOUND");
    }

    @Test
    @DisplayName("지원하지 않는 메서드 → 405 METHOD_NOT_ALLOWED")
    void methodNotAllowed() throws Exception {
        expect(mockMvc.perform(get("/api/clips").header("Authorization", bearer)), 405, "METHOD_NOT_ALLOWED");
    }

    @Test
    @DisplayName("잘못된 JSON 바디 → 400 INVALID_INPUT")
    void malformedJson() throws Exception {
        expect(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{bad")),
                400, "INVALID_INPUT");
    }

    @Test
    @DisplayName("validation 실패 → 400 INVALID_INPUT")
    void validation() throws Exception {
        expect(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}")),
                400, "INVALID_INPUT");
    }

    @Test
    @DisplayName("JSON이 아닌 Content-Type → 415 UNSUPPORTED_MEDIA_TYPE")
    void unsupportedMediaType() throws Exception {
        expect(mockMvc.perform(put("/api/users/me/settings").header("Authorization", bearer)
                .contentType(MediaType.TEXT_PLAIN).content("x")), 415, "UNSUPPORTED_MEDIA_TYPE");
    }

    @Test
    @DisplayName("필수 쿼리 파라미터 누락·형식 오류 → 400 INVALID_INPUT")
    void missingOrBadParam() throws Exception {
        expect(mockMvc.perform(get("/api/users/999999/days").header("Authorization", bearer)), 400, "INVALID_INPUT");
        expect(mockMvc.perform(get("/api/users/999999/days").param("month", "2026-13").header("Authorization", bearer)),
                400, "INVALID_INPUT");
    }

    @Test
    @DisplayName("친구가 아닌 사람 달력 → 403 FORBIDDEN")
    void forbidden() throws Exception {
        expect(mockMvc.perform(get("/api/users/1/days/2026-11-17").header("Authorization", bearer)), 403, "FORBIDDEN");
    }

    private static void expect(ResultActions result, int status, String code) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").isString());
    }
}
