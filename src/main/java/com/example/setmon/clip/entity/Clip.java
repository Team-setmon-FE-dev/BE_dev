package com.example.setmon.clip.entity;

import com.example.setmon.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "clips", uniqueConstraints = @UniqueConstraint(
        name = "uk_clips_user_date_slot", columnNames = {"user_id", "clip_date", "slot_hour"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Clip extends BaseTimeEntity {

    public static final int MIN_DURATION_MS = 1000;
    public static final int MAX_DURATION_MS = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** KST 기준 날짜 */
    @Column(name = "clip_date", nullable = false)
    private LocalDate clipDate;

    /** KST 기준 시(0~23) */
    @Column(name = "slot_hour", nullable = false)
    private int slotHour;

    @Column(nullable = false, length = 255)
    private String videoKey;

    @Column(nullable = false, length = 255)
    private String thumbKey;

    @Column(nullable = false)
    private int durationMs;

    /** 촬영 시각(UTC) */
    @Column(nullable = false)
    private Instant recordedAt;

    public static Clip create(Long userId, LocalDate clipDate, int slotHour,
                              String videoKey, String thumbKey, int durationMs, Instant recordedAt) {
        Clip clip = new Clip();
        clip.userId = userId;
        clip.clipDate = clipDate;
        clip.slotHour = slotHour;
        clip.replace(videoKey, thumbKey, durationMs, recordedAt);
        return clip;
    }

    /** 같은 슬롯 재촬영 시 교체 */
    public void replace(String videoKey, String thumbKey, int durationMs, Instant recordedAt) {
        this.videoKey = videoKey;
        this.thumbKey = thumbKey;
        this.durationMs = durationMs;
        this.recordedAt = recordedAt;
    }
}
