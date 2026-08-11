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
}
