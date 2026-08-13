package com.withu.mission.service;

import com.withu.auth.entity.User;
import com.withu.auth.repository.UserRepository;
import com.withu.character.service.ExpressionResolver;
import com.withu.global.common.GameConstants;
import com.withu.global.error.CustomException;
import com.withu.global.error.ErrorCode;
import com.withu.group.entity.GroupMember;
import com.withu.group.repository.GroupMemberRepository;
import com.withu.mission.dto.MissionDto.Response;
import com.withu.mission.dto.MissionDto.TodaySummary;
import com.withu.mission.entity.Mission;
import com.withu.mission.entity.MissionType;
import com.withu.mission.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissionService {

    private final MissionRepository missionRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final ExpressionResolver expressionResolver;
    private final MissionSetCreator missionSetCreator;

    /**
     * 오늘 미션을 가져오되, 아직 없으면 만들어서 준다.
     *
     * <p>트랜잭션 밖에서 실행한다. 세트 생성은 {@link MissionSetCreator}가 자기 트랜잭션에서 처리하므로,
     * 동시 요청에 밀려 유니크 제약에 걸리더라도 이쪽 트랜잭션이 오염되지 않고 먼저 만들어진 세트를 다시 읽을 수 있다.
     * (브라우저가 15초마다 폴링해서 실제로 겹친다.)
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public TodaySummary generateToday(Long userId) {
        LocalDate today = LocalDate.now();
        if (!missionRepository.existsByUserIdAndMissionDate(userId, today)) {
            try {
                missionSetCreator.createTodaySet(userId, today);
            } catch (DataIntegrityViolationException e) {
                log.info("오늘 미션 세트가 이미 생성되어 있어 기존 세트를 사용합니다. userId={}", userId);
            }
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
     * 이 사진으로 인증하게 될 식단 미션의 제목. 없으면 null.
     *
     * <p>AI에게 "오늘의 식단 미션을 달성했는지" 물으려면 그 미션이 무엇인지 알려줘야 한다.
     * 제목 없이 물어보면 AI가 기준을 스스로 지어내서, 목표에 맞는 식사인데도 미달성으로 판정되곤 했다.
     */
    public String pendingDietMissionTitle(Long userId) {
        return missionRepository
                .findFirstByUserIdAndMissionDateAndTypeAndDoneFalseOrderByIdAsc(userId, LocalDate.now(), MissionType.DIET)
                .map(Mission::getTitle)
                .orElse(null);
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

}
