package com.example.setmon.user.entity;

import com.example.setmon.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    public static final int DEFAULT_NOTIFY_START_HOUR = 9;
    public static final int DEFAULT_NOTIFY_END_HOUR = 23;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 10)
    private String nickname;

    @Column(nullable = false, unique = true, length = 8)
    private String friendCode;

    /** 촬영 알림 시작 시각(KST, 0~23, 포함) */
    @Column(nullable = false)
    private int notifyStartHour;

    /** 촬영 알림 끝 시각(KST, 0~23, 포함) */
    @Column(nullable = false)
    private int notifyEndHour;

    @Column(nullable = false)
    private boolean captureNotifyOn;

    @Column(nullable = false)
    private boolean completeNotifyOn;

    public static User create(String email, String passwordHash, String nickname, String friendCode) {
        User user = new User();
        user.email = email;
        user.passwordHash = passwordHash;
        user.nickname = nickname;
        user.friendCode = friendCode;
        user.notifyStartHour = DEFAULT_NOTIFY_START_HOUR;
        user.notifyEndHour = DEFAULT_NOTIFY_END_HOUR;
        user.captureNotifyOn = true;
        user.completeNotifyOn = true;
        return user;
    }

    public void updateSettings(int notifyStartHour, int notifyEndHour, boolean captureNotifyOn, boolean completeNotifyOn) {
        this.notifyStartHour = notifyStartHour;
        this.notifyEndHour = notifyEndHour;
        this.captureNotifyOn = captureNotifyOn;
        this.completeNotifyOn = completeNotifyOn;
    }
}
