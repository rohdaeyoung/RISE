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

    private final MissionRepository missionRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final OnboardingRepository onboardingRepository;
    private final MissionAiClient missionAiClient;
    /** AI 호출이 실패했을 때 쓰는 고정 풀 생성기. 키가 없으면 위 필드와 같은 인스턴스가 된다. */
    private final MockMissionAiClient fallbackMissionAiClient;
    private final MissionHistoryAnalyzer missionHistoryAnalyzer;

    @Transactional
    public void createTodaySet(Long userId, LocalDate today) {
        GroupMember member = getMembership(userId);
        Group group = member.getGroup();

        // 방을 만들 때 정한 미션 시각 전에는 만들지 않는다. 예전에는 사용자가 MY 화면을 여는
        // 순간 시각과 무관하게 만들어버려서, 오후 9시로 설정해두고 아침에 앱을 열면 그냥 미션이
        // 나왔다. 그러면 미션 시각 설정이 아무 의미가 없다 (PRD 8 — 설정된 시간에 생성).
        //
        // 단 첫날은 예외다. 방을 만들거나 참여한 사람은 그 자리에서 미션을 받아야 한다.
        // 아니면 오후 9시로 설정하고 아침에 방을 만든 사람은 하루를 빈 화면으로 보낸다.
        if (!isFirstDay(group, member, today) && LocalTime.now().isBefore(missionTimeOf(group))) {
            return;
        }

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

        for (int i = 0; i < generated.size(); i++) {
            GeneratedMission g = generated.get(i);
            missionRepository.save(Mission.builder()
                    .userId(userId)
                    .groupId(group.getId())
                    .missionDate(today)
                    .seq(i)
                    .type(MissionType.valueOf(g.type().name()))
                    .title(g.title())
                    // 하루치 미션은 세트가 만들어지는 순간 전부 함께 열린다 (PRD 8 — "설정된 시간에
                    // 모든 그룹원의 개인 맞춤 미션이 동시에 생성된다"). 예전에는 +3.5h/+7h로 나눠
                    // 열어서, 미션 하나만 보이고 "다음 미션은 오후 12:30에 도착해요"가 떴다.
                    .unlockTime(null)
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

    static LocalTime missionTimeOf(Group group) {
        return LocalTime.of(group.getMissionHour(), group.getMissionMinute());
    }

    /**
     * 이 사람에게 오늘이 이번 사이클의 첫날인가.
     *
     * <p>사이클 시작일과 참여일을 모두 본다. 방을 만든 사람은 사이클 시작일이 오늘이고,
     * 중간에 코드로 들어온 사람은 참여일이 오늘이다. "계속하기"로 새 사이클을 시작하면
     * 시작일이 다시 오늘이 되므로 그때도 바로 미션을 받는다.
     */
    private boolean isFirstDay(Group group, GroupMember member, LocalDate today) {
        return group.getStartedAt().toLocalDate().equals(today)
                || member.getCreatedAt().toLocalDate().equals(today);
    }

    private GroupMember getMembership(Long userId) {
        return groupMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
    }
}
