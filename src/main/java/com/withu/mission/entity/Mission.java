package com.withu.mission.entity;

import com.withu.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 하루 단위 개인 맞춤 미션. 그룹에서 정한 "미션 시작 시간" 기준으로 시간대별 unlock 되며
 * (unlockTime == null이면 즉시 노출), 식단 미션은 meal 도메인에서, 생활습관 미션은
 * 직접 인증(verify)으로 완료 처리된다 (PRD 5. AI 개인 맞춤 미션).
 */
@Entity
@Table(name = "missions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mission extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "mission_date", nullable = false)
    private LocalDate missionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MissionType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private boolean done;

    @Column(name = "unlock_time")
    private LocalTime unlockTime;

    @Builder
    private Mission(Long userId, Long groupId, LocalDate missionDate, MissionType type, String title, LocalTime unlockTime) {
        this.userId = userId;
        this.groupId = groupId;
        this.missionDate = missionDate;
        this.type = type;
        this.title = title;
        this.unlockTime = unlockTime;
        this.done = false;
    }

    public boolean isUnlocked(LocalTime now) {
        return unlockTime == null || done || !now.isBefore(unlockTime);
    }

    public void complete() {
        this.done = true;
    }
}
