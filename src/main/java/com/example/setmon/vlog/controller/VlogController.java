package com.example.setmon.vlog.controller;

import com.example.setmon.common.exception.BusinessException;
import com.example.setmon.common.exception.ErrorCode;
import com.example.setmon.vlog.dto.VlogStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "브이로그")
@RestController
public class VlogController {

    @Operation(summary = "[미구현] 오늘 브이로그 지금 만들기 (F-15)",
            description = "PENDING으로 만들고 202. 이미 PENDING·PROCESSING이면 그대로 202. 에러: NO_CLIPS")
    @PostMapping("/api/users/me/days/today/vlog")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public VlogStatusResponse generateToday(@Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }
}
