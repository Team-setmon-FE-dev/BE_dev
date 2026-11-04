package com.example.setmon.vlog.controller;

import com.example.setmon.vlog.dto.VlogStatusResponse;
import com.example.setmon.vlog.service.VlogGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "브이로그")
@RestController
@RequiredArgsConstructor
public class VlogController {

    private final VlogGenerationService vlogGenerationService;

    @Operation(summary = "오늘 브이로그 지금 만들기 (F-15)", description = """
            오늘(KST) 브이로그를 PENDING으로 만들고 202.
            이미 PENDING·PROCESSING이면 그대로, 이미 최신 DONE이면 DONE을 돌려준다.
            결과는 하루 상세 조회로 확인. 에러: NO_CLIPS""")
    @PostMapping("/api/users/me/days/today/vlog")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public VlogStatusResponse generateToday(@Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        return vlogGenerationService.generateToday(userId);
    }
}
