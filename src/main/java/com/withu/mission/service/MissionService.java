package com.withu.mission.service;

import com.withu.ai.MissionAiClient;
import com.withu.ai.MissionAiClient.GenerateMissionCommand;
import com.withu.ai.MissionAiClient.GeneratedMission;
import com.withu.auth.entity.User;
import com.withu.auth.repository.UserRepository;
import com.withu.character.service.ExpressionResolver;
import com.withu.global.common.GameConstants;
import com.withu.global.error.CustomException;
import com.withu.global.error.ErrorCode;
import com.withu.group.entity.Group;
import com.withu.group.entity.GroupMember;
import com.withu.group.repository.GroupMemberRepository;
import com.withu.mission.dto.MissionDto.Response;
import com.withu.mission.dto.MissionDto.TodaySummary;
import com.withu.mission.entity.Mission;
import com.withu.mission.entity.MissionType;
import com.withu.mission.repository.MissionRepository;
import com.withu.onboarding.entity.Onboarding;
import com.withu.onboarding.repository.OnboardingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissionService {

    // 그룹에서 설정한 미션 시작 시간 기준 +0h/+3.5h/+7h/+11h 간격으로 하나씩 도착 (프론트 missionApi.js와 동일).
    private static final int[] UNLOCK_OFFSET_MINUTES = {0, 210, 420, 660};

    private final MissionRepository missionRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final OnboardingRepository onboardingRepository;
    private final UserRepository userRepository;
    private final MissionAiClient missionAiClient;
    private final ExpressionResolver expressionResolver;

    @Transactional
    public TodaySummary generateToday(Long userId) {
        LocalDate today = LocalDate.now();
        if (missionRepository.existsByUserIdAndMissionDate(userId, today)) {
            return getToday(userId);
        }

        Group group = getGroupOfUser(userId);
        Onboarding onboarding = onboardingRepository.findByUserIdAndGroupId(userId, group.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.ONBOARDING_NOT_FOUND));

        List<GeneratedMission> generated = missionAiClient.generateDailyMissions(new GenerateMissionCommand(
                onboarding.getGoal().name().toLowerCase(),
                onboarding.getGender().name().toLowerCase(),
                onboarding.getAge(),
                onboarding.getHeight(),
                onboarding.getWeight(),
                0
        ));

        LocalTime base = LocalTime.of(group.getMissionHour(), group.getMissionMinute());
        for (int i = 0; i < generated.size(); i++) {
            GeneratedMission g = generated.get(i);
            LocalTime unlockTime = i == 0 ? null : base.plusMinutes(offsetFor(i));
            Mission mission = Mission.builder()
                    .userId(userId)
                    .groupId(group.getId())
                    .missionDate(today)
                    .seq(i)
                    .type(MissionType.valueOf(g.type().name()))
                    .title(g.title())
                    .unlockTime(unlockTime)
                    .build();
            missionRepository.save(mission);
        }
        try {
            missionRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // 동시에 들어온 다른 요청이 이미 오늘 세트를 만든 경우 — 먼저 만들어진 쪽을 그대로 쓴다.
            log.info("오늘 미션 세트가 이미 생성되어 있어 기존 세트를 사용합니다. userId={}", userId);
        }
        return getToday(userId);
    }

    public TodaySummary getToday(Long userId) {
        List<Mission> missions = missionRepository.findByUserIdAndMissionDateOrderByIdAsc(userId, LocalDate.now());
        LocalTime now = LocalTime.now();
        List<Response> responses = missions.stream().map(m -> Response.from(m, now)).toList();
        int rate = missions.isEmpty() ? 0 : (int) Math.round(
                100.0 * missions.stream().filter(Mission::isDone).count() / missions.size());
        return new TodaySummary(responses, rate);
    }

    @Transactional
    public Response verifyLifestyleMission(Long userId, Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));
        if (!mission.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (mission.isDone()) {
            throw new CustomException(ErrorCode.MISSION_ALREADY_DONE);
        }
        if (!mission.isUnlocked(LocalTime.now())) {
            throw new CustomException(ErrorCode.MISSION_LOCKED);
        }
        // 생활습관 미션은 목표적합도 판단이 필요 없는 단순 완료 인증이라 mock에서는 항상 성공 처리.
        mission.complete();
        rewardCoins(userId);
        return Response.from(mission, LocalTime.now());
    }

    /**
     * 식단 인증이 달성으로 판정되면 오늘 미완료 식단 미션 중 하나를 완료 처리한다
     * (프론트 AppContext.jsx LOG_MEAL 리듀서와 동일한 규칙).
     */
    @Transactional
    public boolean completeFirstPendingDietMission(Long userId) {
        Mission mission = missionRepository
                .findFirstByUserIdAndMissionDateAndTypeAndDoneFalseOrderByIdAsc(userId, LocalDate.now(), MissionType.DIET)
                .orElse(null);
        if (mission == null) {
            return false;
        }
        mission.complete();
        rewardCoins(userId);
        return true;
    }

    /** 미션 1개 완료 보상 — 누적 코인과 이번 챌린지 사이클 점수에 함께 반영한다. */
    @Transactional
    public void rewardCoins(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.addCoins(GameConstants.MISSION_COIN_REWARD);
        groupMemberRepository.findByUserId(userId).ifPresent(member -> {
            member.addCyclePoints(GameConstants.MISSION_COIN_REWARD);
            // 달성률이 바뀌면 그룹원 전체의 순위가 흔들리므로 표정 캐시도 그룹 단위로 다시 계산한다.
            List<Long> groupUserIds = groupMemberRepository.findByGroupId(member.getGroup().getId()).stream()
                    .map(GroupMember::getUserId)
                    .toList();
            expressionResolver.refreshGroup(groupUserIds);
        });
    }

    private int offsetFor(int index) {
        return index < UNLOCK_OFFSET_MINUTES.length ? UNLOCK_OFFSET_MINUTES[index] : UNLOCK_OFFSET_MINUTES[UNLOCK_OFFSET_MINUTES.length - 1];
    }

    private Group getGroupOfUser(Long userId) {
        GroupMember member = groupMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
        return member.getGroup();
    }
}
