package com.withu.meal.service;

import com.withu.ai.MealVisionAiClient;
import com.withu.ai.MealVisionAiClient.MealAnalysisResult;
import com.withu.file.service.FileStorageService;
import com.withu.global.error.CustomException;
import com.withu.global.error.ErrorCode;
import com.withu.group.entity.GroupMember;
import com.withu.group.repository.GroupMemberRepository;
import com.withu.meal.dto.MealDto.Response;
import com.withu.meal.dto.MealDto.TodayResponse;
import com.withu.meal.entity.Meal;
import com.withu.meal.entity.MealSlot;
import com.withu.meal.repository.MealRepository;
import com.withu.mission.service.MissionService;
import com.withu.onboarding.entity.Onboarding;
import com.withu.onboarding.repository.OnboardingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MealService {

    private final MealRepository mealRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final OnboardingRepository onboardingRepository;
    private final MealVisionAiClient mealVisionAiClient;
    private final MissionService missionService;
    private final FileStorageService fileStorageService;

    @Transactional
    public Response analyze(Long userId, MealSlot slot, MultipartFile photo, String foodName, String portion) {
        if (mealRepository.existsByUserIdAndMealDateAndSlot(userId, LocalDate.now(), slot)) {
            throw new CustomException(ErrorCode.MEAL_ALREADY_LOGGED);
        }

        String goal = currentGoal(userId);
        MealAnalysisResult result = mealVisionAiClient.analyze(photo, foodName, portion, goal);
        String photoUrl = fileStorageService.store(photo);

        Meal meal = Meal.builder()
                .userId(userId)
                .mealDate(LocalDate.now())
                .slot(slot)
                .achieved(result.achieved())
                .internalFit(Meal.InternalFit.valueOf(result.internalFit().name()))
                .photoUrl(photoUrl)
                .build();
        mealRepository.save(meal);

        if (result.achieved()) {
            missionService.completeFirstPendingDietMission(userId);
        }
        return Response.from(meal);
    }

    public TodayResponse getToday(Long userId) {
        var meals = mealRepository.findByUserIdAndMealDate(userId, LocalDate.now()).stream()
                .map(Response::from)
                .toList();
        return new TodayResponse(meals);
    }

    private String currentGoal(Long userId) {
        GroupMember member = groupMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
        Onboarding onboarding = onboardingRepository
                .findByUserIdAndGroupId(userId, member.getGroup().getId())
                .orElseThrow(() -> new CustomException(ErrorCode.ONBOARDING_NOT_FOUND));
        return onboarding.getGoal().name().toLowerCase();
    }
}
