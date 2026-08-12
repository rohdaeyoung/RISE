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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Slf4j
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

        requireImage(photo);

        String goal = currentGoal(userId);
        MealAnalysisResult result = analyzeSafely(photo, foodName, portion, goal);
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

    /**
     * 사진이 아닌 파일은 AI를 부르기 전에 걸러낸다. 그대로 넘기면 OpenAI가 MIME 타입을 거절하면서
     * 요청 비용만 쓰고 사용자에게는 500이 나간다. 사용자가 파일을 잘못 고른 것뿐이니 400이 맞다.
     */
    private void requireImage(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new CustomException(ErrorCode.NOT_AN_IMAGE);
        }
        String contentType = photo.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new CustomException(ErrorCode.NOT_AN_IMAGE);
        }
    }

    /**
     * OpenAI가 느려지거나 잠깐 죽어도 사용자에게 스택트레이스 섞인 500을 주지 않는다.
     * 식단 판정은 AI가 실제로 봐야 의미가 있으므로 mock으로 대체해 아무 값이나 지어내지 않고,
     * "잠시 후 다시" 라고 명확히 알려 재시도하게 한다.
     */
    private MealAnalysisResult analyzeSafely(MultipartFile photo, String foodName, String portion, String goal) {
        try {
            return mealVisionAiClient.analyze(photo, foodName, portion, goal);
        } catch (CustomException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("식단 사진 분석에 실패했습니다. goal={}", goal, e);
            throw new CustomException(ErrorCode.MEAL_ANALYSIS_FAILED);
        }
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
