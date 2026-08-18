package com.withu.challenge.entity;

import com.withu.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 챌린지 1위로 획득한 뱃지. 같은 뱃지를 여러 번 딸 수 있어 획득 횟수를 함께 관리한다.
 */
@Entity
@Table(name = "user_badges", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "badge_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBadge extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "badge_id", nullable = false, length = 30)
    private String badgeId;

    @Column(nullable = false)
    private int count;

    @Builder
    private UserBadge(Long userId, String badgeId) {
        this.userId = userId;
        this.badgeId = badgeId;
        this.count = 1;
    }

    public void increase() {
        this.count += 1;
    }
}
