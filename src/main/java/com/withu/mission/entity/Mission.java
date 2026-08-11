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
@Table(name = "missions", uniqueConstraints =
        // 같은 날짜에 대해 미션 생성 요청이 동시에 들어와도 세트가 두 번 만들어지지 않도록
        // (사용자, 날짜, 순번)에 유니크 제약을 걸어 DB에서 막는다.
        @UniqueConstraint(name = "uk_mission_user_date_seq", columnNames = {"user_id", "mission_date", "seq"}))
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

    /** 그날 미션 세트 안에서의 순번(0부터). 중복 생성 방지용 유니크 키의 일부. */
    @Column(nullable = false)
    private int seq;

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
    private Mission(Long userId, Long groupId, LocalDate missionDate, int seq, MissionType type, String title, LocalTime unlockTime) {
        this.userId = userId;
        this.groupId = groupId;
        this.missionDate = missionDate;
        this.seq = seq;
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
