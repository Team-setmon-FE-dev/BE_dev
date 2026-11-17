package com.example.setmon.common.exception;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 컨트롤러 밖(필터 등)에서 난 에러가 /error로 넘어와도 {code, message} 모양을 유지한다.
 * (Spring Boot 기본 BasicErrorController 대체 — 기본은 {timestamp, status, error, path})
 */
@Hidden
@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ErrorResponse> error(HttpServletRequest request) {
        Object attr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = attr instanceof Integer i ? i : HttpStatus.INTERNAL_SERVER_ERROR.value();
        ErrorCode code = toErrorCode(status);
        HttpStatus httpStatus = HttpStatus.resolve(status);
        if (httpStatus == null || httpStatus.is5xxServerError() || !httpStatus.isError()) {
            httpStatus = code.getStatus();
        }
        return ResponseEntity.status(httpStatus).body(ErrorResponse.of(code));
    }

    private static ErrorCode toErrorCode(int status) {
        return switch (status) {
            case 400 -> ErrorCode.INVALID_INPUT;
            case 401 -> ErrorCode.UNAUTHORIZED;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404 -> ErrorCode.NOT_FOUND;
            case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
            case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            default -> status >= 400 && status < 500 ? ErrorCode.INVALID_INPUT : ErrorCode.INTERNAL_ERROR;
        };
    }
}
