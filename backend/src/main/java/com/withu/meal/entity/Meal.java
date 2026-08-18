package com.withu.meal.entity;

import com.withu.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 하루 3끼(아침/점심/저녁) 식단 인증. 사용자에게는 achieved(달성 여부)만 노출하고,
 * internalFit(목표적합도)은 다음 미션 생성 기준으로만 내부에서 사용한다 (PRD 6. AI 분석 상세).
 */
@Entity
@Table(name = "meals", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "meal_date", "slot"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meal extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MealSlot slot;

    @Column(nullable = false)
    private boolean achieved;

    @Enumerated(EnumType.STRING)
    @Column(name = "internal_fit", nullable = false, length = 10)
    private InternalFit internalFit;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Builder
    private Meal(Long userId, LocalDate mealDate, MealSlot slot, boolean achieved, InternalFit internalFit, String photoUrl) {
        this.userId = userId;
        this.mealDate = mealDate;
        this.slot = slot;
        this.achieved = achieved;
        this.internalFit = internalFit;
        this.photoUrl = photoUrl;
    }

    public enum InternalFit {
        GOOD, NORMAL, BAD
    }
}
