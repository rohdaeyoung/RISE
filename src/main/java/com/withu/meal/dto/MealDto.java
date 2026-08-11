package com.withu.meal.dto;

import com.withu.meal.entity.Meal;
import com.withu.meal.entity.MealSlot;

import java.util.List;

public class MealDto {

    public record Response(
            MealSlot slot,
            boolean achieved,
            String photoUrl
    ) {
        public static Response from(Meal meal) {
            return new Response(meal.getSlot(), meal.isAchieved(), meal.getPhotoUrl());
        }
    }

    public record TodayResponse(
            List<Response> meals
    ) {
    }
}
