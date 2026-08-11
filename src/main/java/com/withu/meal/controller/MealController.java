package com.withu.meal.controller;

import com.withu.global.common.ApiResponse;
import com.withu.global.security.SecurityUtil;
import com.withu.meal.dto.MealDto.Response;
import com.withu.meal.dto.MealDto.TodayResponse;
import com.withu.meal.entity.MealSlot;
import com.withu.meal.service.MealService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;

    @PostMapping(value = "/{slot}/analyze", consumes = "multipart/form-data")
    public ApiResponse<Response> analyze(
            @PathVariable MealSlot slot,
            @RequestPart("photo") MultipartFile photo,
            @RequestPart(value = "foodName", required = false) String foodName,
            @RequestPart(value = "portion", required = false) String portion) {
        return ApiResponse.success(
                mealService.analyze(SecurityUtil.getCurrentUserId(), slot, photo, foodName, portion));
    }

    @GetMapping("/today")
    public ApiResponse<TodayResponse> getToday() {
        return ApiResponse.success(mealService.getToday(SecurityUtil.getCurrentUserId()));
    }
}
