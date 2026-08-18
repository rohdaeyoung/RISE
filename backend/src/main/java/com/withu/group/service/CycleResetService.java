package com.withu.group.service;

import com.withu.meal.repository.MealRepository;
import com.withu.mission.repository.MissionRepository;
import com.withu.onboarding.repository.OnboardingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 7일 사이클이 끝났을 때 그 사이클에 쌓인 기록을 비운다.
 *
 * <p>그룹을 나갈 때와 "계속하기"로 새 사이클을 시작할 때 똑같이 필요하다. 한쪽에만 넣었더니
 * 계속하기에서는 지난 사이클의 미션과 달성 상태가 그대로 이어져 Day 1인데 100%로 시작했다.
 *
 * <p>미션과 식단은 그룹이 아니라 "사용자 + 날짜"로 저장되어 있어서, 그룹원 자격만 정리해서는
 * 남는다. 온보딩은 (사용자, 그룹) 단위라 그룹은 그대로인 계속하기에서는 지워야 다시 입력받는다.
 * PRD상 목표·신체정보는 사이클마다 다시 받는 값이다.
 *
 * <p>누적 코인과 지난 챌린지 결과·뱃지는 사이클 기록이 아니라 개인 이력이므로 건드리지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CycleResetService {

    private final MissionRepository missionRepository;
    private final MealRepository mealRepository;
    private final OnboardingRepository onboardingRepository;

    @Transactional
    public void reset(Long userId, Long groupId, LocalDate cycleStart) {
        missionRepository.deleteByUserIdAndMissionDateGreaterThanEqual(userId, cycleStart);
        mealRepository.deleteByUserIdAndMealDateGreaterThanEqual(userId, cycleStart);
        onboardingRepository.deleteByUserIdAndGroupId(userId, groupId);
    }
}
