package com.example.setmon.clip.service;

import com.example.setmon.clip.repository.ClipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClipService {

    private final ClipRepository clipRepository;

    public long countRecordDays(Long userId) {
        return clipRepository.countRecordDays(userId);
    }

    // TODO(2주차): presigned URL 발급, 클립 등록(시간 슬롯·교체·stale), 클립 삭제
}
