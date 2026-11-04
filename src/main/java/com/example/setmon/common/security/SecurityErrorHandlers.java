package com.example.setmon.common.security;

import com.example.setmon.common.exception.ErrorCode;
import com.example.setmon.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorHandlers {

    public AuthenticationEntryPoint entryPoint() {
        return (request, response, ex) -> {
            Object attr = request.getAttribute(JwtAuthenticationFilter.ERROR_ATTRIBUTE);
            ErrorCode code = attr instanceof ErrorCode ec ? ec : ErrorCode.UNAUTHORIZED;
            write(response, code);
        };
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> write(response, ErrorCode.FORBIDDEN);
    }

    private void write(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(ErrorResponse.of(code).toJson());
    }
}
