package com.withu.challenge.entity;

import com.withu.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 7일 챌린지 사이클이 끝난 시점의 개인 결과 스냅샷 (PRD 10. 랭킹 시스템 — 방 종료 결과).
 * 사이클마다 한 건씩 쌓이며, 결과 화면과 뱃지 이력의 근거가 된다.
 */
@Entity
@Table(name = "challenge_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChallengeResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    /** 이 사이클이 시작된 시각 — 같은 그룹의 여러 사이클을 구분하는 키. */
    @Column(name = "cycle_started_at", nullable = false)
    private LocalDateTime cycleStartedAt;

    @Column(name = "final_rank", nullable = false)
    private int finalRank;

    @Column(name = "total_participants", nullable = false)
    private int totalParticipants;

    @Column(nullable = false)
    private int points;

    @Column(name = "achievement_rate", nullable = false)
    private int achievementRate;

    @Column(name = "coins_earned", nullable = false)
    private int coinsEarned;

    @Column(name = "bonus_coins", nullable = false)
    private int bonusCoins;

    @Column(name = "badge_awarded")
    private String badgeAwarded;

    @Builder
    private ChallengeResult(Long userId, Long groupId, LocalDateTime cycleStartedAt, int finalRank,
                           int totalParticipants, int points, int achievementRate, int coinsEarned,
                           int bonusCoins, String badgeAwarded) {
        this.userId = userId;
        this.groupId = groupId;
        this.cycleStartedAt = cycleStartedAt;
        this.finalRank = finalRank;
        this.totalParticipants = totalParticipants;
        this.points = points;
        this.achievementRate = achievementRate;
        this.coinsEarned = coinsEarned;
        this.bonusCoins = bonusCoins;
        this.badgeAwarded = badgeAwarded;
    }
}
