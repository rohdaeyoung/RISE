package com.withu.onboarding.entity;

import com.withu.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 목표/신체정보는 그룹(챌린지) 사이클마다 갱신될 수 있어 (userId, groupId) 단위로 저장한다
 * (PRD 4. 온보딩 — "그룹이 시작될 때마다 새로운 목표를 설정").
 */
@Entity
@Table(name = "onboardings", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "group_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Onboarding extends BaseTimeEntity {

    private static final int AGE_MIN = 1;
    private static final int AGE_MAX = 100;
    private static final int HEIGHT_MIN = 100;
    private static final int WEIGHT_MIN = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Goal goal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Column(nullable = false)
    private int age;

    @Column(nullable = false)
    private int height;

    @Column(nullable = false)
    private int weight;

    @Builder
    private Onboarding(Long userId, Long groupId, Goal goal, Gender gender, int age, int height, int weight) {
        this.userId = userId;
        this.groupId = groupId;
        this.goal = goal;
        this.gender = gender;
        this.age = clamp(age, AGE_MIN, AGE_MAX);
        this.height = Math.max(height, HEIGHT_MIN);
        this.weight = Math.max(weight, WEIGHT_MIN);
    }

    public void update(Goal goal, Gender gender, int age, int height, int weight) {
        this.goal = goal;
        this.gender = gender;
        this.age = clamp(age, AGE_MIN, AGE_MAX);
        this.height = Math.max(height, HEIGHT_MIN);
        this.weight = Math.max(weight, WEIGHT_MIN);
    }

    private static int clamp(int value, int min, int max) {
        return Math.min(max, Math.max(min, value));
    }
}
