package com.withu.group.entity;

import com.withu.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 한 사용자는 동시에 하나의 그룹에만 속한다 — user_id에 유니크 제약을 걸어
 * 중복 요청(네트워크 재시도 등)이 들어와도 두 그룹에 동시에 소속되지 않도록 DB에서 막는다.
 */
@Entity
@Table(name = "group_members", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"group_id", "user_id"}),
        @UniqueConstraint(name = "uk_group_member_user", columnNames = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 이번 7일 사이클 동안 모은 점수. 챌린지 종료 시 최종 순위 기준이 되고,
     * 새 사이클이 시작되면 0으로 초기화된다 (프론트 AppContext.jsx의 challengeCoins와 같은 개념).
     */
    @Column(name = "cycle_points", nullable = false)
    private int cyclePoints;

    @Builder
    private GroupMember(Group group, Long userId) {
        this.group = group;
        this.userId = userId;
        this.cyclePoints = 0;
    }

    public void addCyclePoints(int amount) {
        this.cyclePoints += amount;
    }

    public void resetCyclePoints() {
        this.cyclePoints = 0;
    }
}
