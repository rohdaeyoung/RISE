package com.withu.mission.service;

import com.withu.ai.MissionAiClient;
import com.withu.ai.mock.MockMissionAiClient;
import com.withu.ai.MissionAiClient.GenerateMissionCommand;
import com.withu.ai.MissionAiClient.GeneratedMission;
import com.withu.global.error.CustomException;
import com.withu.global.error.ErrorCode;
import com.withu.group.entity.Group;
import com.withu.group.entity.GroupMember;
import com.withu.group.repository.GroupMemberRepository;
import com.withu.mission.entity.Mission;
import com.withu.mission.entity.MissionType;
import com.withu.mission.repository.MissionRepository;
import com.withu.mission.service.MissionHistoryAnalyzer.MissionPlan;
import com.withu.onboarding.entity.Onboarding;
import com.withu.onboarding.repository.OnboardingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 오늘의 미션 세트를 만들어 저장한다.
 *
 * <p>{@link MissionService}에서 분리한 이유는 트랜잭션 경계 때문이다. 유니크 제약(user_id, mission_date, seq)에
 * 걸리면 그 트랜잭션은 롤백 대상이 되어 같은 트랜잭션 안에서는 재조회조차 할 수 없다. 생성만 별도 트랜잭션으로
 * 떼어놓아야, 동시 요청에 밀린 쪽이 먼저 만들어진 세트를 깨끗하게 다시 읽어갈 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class MissionSetCreator {

    // 그룹에서 설정한 미션 시작 시간 기준 +0h/+3.5h/+7h/+11h 간격으로 하나씩 도착 (프론트 missionApi.js와 동일).
    private static final int[] UNLOCK_OFFSET_MINUTES = {0, 210, 420, 660};

    private final MissionRepository missionRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final OnboardingRepository onboardingRepository;
    private final MissionAiClient missionAiClient;
    /** AI 호출이 실패했을 때 쓰는 고정 풀 생성기. 키가 없으면 위 필드와 같은 인스턴스가 된다. */
    private final MockMissionAiClient fallbackMissionAiClient;
    private final MissionHistoryAnalyzer missionHistoryAnalyzer;

    @Transactional
    public void createTodaySet(Long userId, LocalDate today) {
        Group group = getGroupOfUser(userId);
        Onboarding onboarding = onboardingRepository.findByUserIdAndGroupId(userId, group.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.ONBOARDING_NOT_FOUND));

        // 어제까지의 미션 달성률과 식단 분석 결과를 읽어 오늘 미션의 난이도·개수를 정한다
        // (PRD 1·5·6 — 수행 결과를 분석해 다음 미션을 생성하는 루프).
        MissionPlan plan = missionHistoryAnalyzer.analyze(userId, today);

        GenerateMissionCommand command = new GenerateMissionCommand(
                onboarding.getGoal().name().toLowerCase(),
                onboarding.getGender().name().toLowerCase(),
                onboarding.getAge(),
                onboarding.getHeight(),
                onboarding.getWeight(),
                plan.previousAchievementRate(),
                plan.difficulty(),
                plan.missionCount(),
                plan.recentMeals()
        );
        List<GeneratedMission> generated = generateWithFallback(command);

        LocalTime base = LocalTime.of(group.getMissionHour(), group.getMissionMinute());
        for (int i = 0; i < generated.size(); i++) {
            GeneratedMission g = generated.get(i);
            LocalTime unlockTime = i == 0 ? null : base.plusMinutes(offsetFor(i));
            missionRepository.save(Mission.builder()
                    .userId(userId)
                    .groupId(group.getId())
                    .missionDate(today)
                    .seq(i)
                    .type(MissionType.valueOf(g.type().name()))
                    .title(g.title())
                    .unlockTime(unlockTime)
                    .build());
        }
        // 커밋까지 미루지 않고 여기서 제약 위반을 드러내, 호출자가 "이미 만들어졌다"로 처리할 수 있게 한다.
        missionRepository.flush();
    }

    /**
     * OpenAI가 잠깐 죽거나 느려져도 미션은 나와야 한다. 미션이 없으면 인증도, 그룹 피드도, 달성률도
     * 전부 멈춰 앱이 통째로 못 쓰게 된다. 개인화 품질은 떨어지지만 고정 풀에서라도 미션을 준다.
     *
     * <p>식단 분석은 반대로 mock 대체를 하지 않는다({@code MealService}). 사진을 보지도 않고
     * 달성 여부를 지어내면 사용자를 속이는 것이기 때문이다.
     */
    private List<GeneratedMission> generateWithFallback(GenerateMissionCommand command) {
        try {
            return missionAiClient.generateDailyMissions(command);
        } catch (RuntimeException e) {
            log.warn("AI 미션 생성에 실패해 기본 미션으로 대체합니다. goal={}", command.goal(), e);
            return fallbackMissionAiClient.generateDailyMissions(command);
        }
    }

    private int offsetFor(int index) {
        return index < UNLOCK_OFFSET_MINUTES.length
                ? UNLOCK_OFFSET_MINUTES[index]
                : UNLOCK_OFFSET_MINUTES[UNLOCK_OFFSET_MINUTES.length - 1];
    }

    private Group getGroupOfUser(Long userId) {
        GroupMember member = groupMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
        return member.getGroup();
    }
}
