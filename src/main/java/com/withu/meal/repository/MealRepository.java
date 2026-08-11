package com.withu.meal.repository;

import com.withu.meal.entity.Meal;
import com.withu.meal.entity.MealSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealRepository extends JpaRepository<Meal, Long> {

    List<Meal> findByUserIdAndMealDate(Long userId, LocalDate mealDate);

    Optional<Meal> findByUserIdAndMealDateAndSlot(Long userId, LocalDate mealDate, MealSlot slot);

    boolean existsByUserIdAndMealDateAndSlot(Long userId, LocalDate mealDate, MealSlot slot);

    /** 그룹 피드에 그룹원들의 오늘 인증 사진을 함께 보여주기 위한 조회. */
    List<Meal> findByUserIdInAndMealDateOrderByIdAsc(List<Long> userIds, LocalDate mealDate);

    /** 챌린지 사이클 동안 인증한 식단 사진 — 결과 화면의 사진 모아보기에 사용. */
    List<Meal> findByUserIdAndMealDateBetweenOrderByIdAsc(Long userId, LocalDate from, LocalDate to);
}
