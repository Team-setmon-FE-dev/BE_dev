package com.example.setmon.vlog.service;

import com.example.setmon.vlog.entity.DailyVlog;
import com.example.setmon.vlog.repository.DailyVlogRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VlogService {

    private final DailyVlogRepository dailyVlogRepository;

    public Optional<DailyVlog> find(Long userId, LocalDate date) {
        return dailyVlogRepository.findByUserIdAndVlogDate(userId, date);
    }

    public List<DailyVlog> findBetween(Long userId, LocalDate from, LocalDate to) {
        return dailyVlogRepository.findAllByUserIdAndVlogDateBetween(userId, from, to);
    }

    /** 클립이 추가·교체·삭제되면 호출. 행이 없으면 아직 만든 적이 없으니 할 일 없음. */
    @Transactional
    public void markStale(Long userId, LocalDate date) {
        dailyVlogRepository.findByUserIdAndVlogDate(userId, date).ifPresent(DailyVlog::markStale);
    }

    /** 생성 요청. 이미 최신(DONE·stale 아님)이거나 대기·진행 중이면 그대로 둔다. */
    @Transactional
    public DailyVlog requestGeneration(Long userId, LocalDate date) {
        DailyVlog vlog = dailyVlogRepository.findByUserIdAndVlogDate(userId, date)
                .orElseGet(() -> dailyVlogRepository.save(DailyVlog.create(userId, date)));
        if (!vlog.isUpToDate()) {
            vlog.requestGeneration();
        }
        return vlog;
    }

    /** 탈퇴용 */
    @Transactional
    public void deleteAllOf(Long userId) {
        dailyVlogRepository.deleteAllOfUser(userId);
    }
}
