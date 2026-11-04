package com.example.setmon.vlog.service;

import com.example.setmon.clip.service.ClipService;
import com.example.setmon.common.config.TimeConfig;
import com.example.setmon.common.exception.BusinessException;
import com.example.setmon.common.exception.ErrorCode;
import com.example.setmon.vlog.dto.VlogStatusResponse;
import com.example.setmon.vlog.entity.DailyVlog;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** "지금 만들기". ClipService ↔ VlogService 순환을 피하려고 분리. */
@Service
@RequiredArgsConstructor
public class VlogGenerationService {

    private final ClipService clipService;
    private final VlogService vlogService;
    private final Clock clock;

    @Transactional
    public VlogStatusResponse generateToday(Long userId) {
        LocalDate today = LocalDate.ofInstant(clock.instant(), TimeConfig.KST);
        if (clipService.countClips(userId, today) == 0) {
            throw new BusinessException(ErrorCode.NO_CLIPS);
        }
        DailyVlog vlog = vlogService.requestGeneration(userId, today);
        return new VlogStatusResponse(today, vlog.getStatus());
    }
}
