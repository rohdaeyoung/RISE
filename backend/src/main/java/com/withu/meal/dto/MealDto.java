package com.withu.meal.dto;

import com.withu.meal.entity.Meal;
import com.withu.meal.entity.MealSlot;

import java.util.List;

public class MealDto {

    /**
     * @param recognized AI가 사진에서 본 것 (예: "단백질 음료"). 방금 분석한 응답에만 담기고,
     *                   나중에 목록으로 다시 불러올 때는 null이다 — 판정 근거는 DB에 남기지 않는다.
     */
    public record Response(
            MealSlot slot,
            boolean achieved,
            String photoUrl,
            String recognized
    ) {
        public static Response from(Meal meal) {
            return new Response(meal.getSlot(), meal.isAchieved(), meal.getPhotoUrl(), null);
        }

        public static Response of(Meal meal, String recognized) {
            return new Response(meal.getSlot(), meal.isAchieved(), meal.getPhotoUrl(), recognized);
        }
    }

    public record TodayResponse(
            List<Response> meals
    ) {
    }
}
