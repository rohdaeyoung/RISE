package com.withu.group.entity;

import com.withu.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 7일 단위로 운영되는 건강 챌린지 그룹. 2~4인, 방장이 미션 시작 시간을 정하지만
 * 이후에는 그룹원 누구나 변경 가능하다 (PRD 7. 그룹 시스템).
 */
@Entity
@Table(name = "study_groups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Group extends BaseTimeEntity {

    public static final int DEFAULT_MISSION_HOUR = 9;
    public static final int DEFAULT_MISSION_MINUTE = 0;
    public static final int MAX_NAME_LENGTH = 10;
    public static final int CHALLENGE_LENGTH_DAYS = 7;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 6)
    private String code;

    @Column(nullable = false, length = 10)
    private String name;

    @Column(name = "host_user_id", nullable = false)
    private Long hostUserId;

    @Column(name = "mission_hour", nullable = false)
    private int missionHour;

    @Column(name = "mission_minute", nullable = false)
    private int missionMinute;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Builder
    private Group(String code, String name, Long hostUserId, Integer missionHour, Integer missionMinute) {
        this.code = code;
        this.name = normalizeName(name);
        this.hostUserId = hostUserId;
        this.missionHour = missionHour != null ? missionHour : DEFAULT_MISSION_HOUR;
        this.missionMinute = missionMinute != null ? missionMinute : DEFAULT_MISSION_MINUTE;
        this.startedAt = LocalDateTime.now();
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            return "건강한 친구들";
        }
        return name.trim().length() > MAX_NAME_LENGTH ? name.trim().substring(0, MAX_NAME_LENGTH) : name.trim();
    }

    public void rename(String name) {
        this.name = normalizeName(name);
    }

    public void changeMissionTime(int hour, int minute) {
        this.missionHour = hour;
        this.missionMinute = minute;
    }

    public void restartCycle() {
        this.startedAt = LocalDateTime.now();
    }

    public int currentDay() {
        long daysElapsed = java.time.Duration.between(startedAt, LocalDateTime.now()).toDays();
        return (int) Math.min(CHALLENGE_LENGTH_DAYS, daysElapsed + 1);
    }

    public boolean isLastDay() {
        return currentDay() >= CHALLENGE_LENGTH_DAYS;
    }
}
