package com.example.setmon.vlog.entity;

import com.example.setmon.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "daily_vlogs",
        uniqueConstraints = @UniqueConstraint(name = "uk_daily_vlogs_user_date", columnNames = {"user_id", "vlog_date"}),
        indexes = @Index(name = "idx_daily_vlogs_status", columnList = "status"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyVlog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** KST 기준 날짜 */
    @Column(name = "vlog_date", nullable = false)
    private LocalDate vlogDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VlogStatus status;

    /** 클립이 추가·교체·삭제되어 다시 만들어야 하는지 */
    @Column(nullable = false)
    private boolean stale;

    @Column(length = 255)
    private String videoKey;

    @Column(length = 255)
    private String thumbKey;

    @Column(length = 1000)
    private String failReason;

    /** PROCESSING 시작 시각(10분 타임아웃 판단용) */
    private Instant startedAt;

    @Column(nullable = false)
    private int retryCount;

    public static DailyVlog create(Long userId, LocalDate vlogDate) {
        DailyVlog vlog = new DailyVlog();
        vlog.userId = userId;
        vlog.vlogDate = vlogDate;
        vlog.status = VlogStatus.NONE;
        vlog.stale = false;
        vlog.retryCount = 0;
        return vlog;
    }

    public void markStale() {
        this.stale = true;
    }

    /** 생성 요청(배치 또는 지금 만들기). 이미 대기·진행 중이면 그대로 둔다. */
    public void requestGeneration() {
        if (status == VlogStatus.PENDING || status == VlogStatus.PROCESSING) {
            return;
        }
        this.status = VlogStatus.PENDING;
        this.failReason = null;
        this.retryCount = 0;
    }

    /** 워커가 가져갈 때. stale을 내려서, 처리 중 바뀐 클립은 다시 stale이 되게 한다. */
    public void startProcessing(Instant now) {
        this.status = VlogStatus.PROCESSING;
        this.stale = false;
        this.startedAt = now;
    }

    public void complete(String videoKey, String thumbKey) {
        this.status = VlogStatus.DONE;
        this.videoKey = videoKey;
        this.thumbKey = thumbKey;
        this.failReason = null;
    }

    public void fail(String reason) {
        this.status = VlogStatus.FAILED;
        this.failReason = reason;
        this.retryCount++;
    }

    /** 그날 클립이 모두 지워진 경우: 결과를 비우고 생성 전 상태로 */
    public void clear() {
        this.status = VlogStatus.NONE;
        this.videoKey = null;
        this.thumbKey = null;
        this.failReason = null;
        this.stale = false;
    }

    public boolean isUpToDate() {
        return status == VlogStatus.DONE && !stale;
    }
}
