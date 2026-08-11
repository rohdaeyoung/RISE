package com.withu.challenge.service;

import com.withu.auth.entity.User;
import com.withu.auth.repository.UserRepository;
import com.withu.challenge.dto.ChallengeDto.MealPhoto;
import com.withu.challenge.dto.ChallengeDto.ParticipantResult;
import com.withu.challenge.dto.ChallengeDto.SummaryResponse;
import com.withu.challenge.entity.ChallengeResult;
import com.withu.challenge.entity.ChallengeReward;
import com.withu.challenge.entity.UserBadge;
import com.withu.challenge.repository.ChallengeResultRepository;
import com.withu.challenge.repository.UserBadgeRepository;
import com.withu.character.entity.Character;
import com.withu.character.repository.CharacterRepository;
import com.withu.global.error.CustomException;
import com.withu.global.error.ErrorCode;
import com.withu.group.entity.Group;
import com.withu.group.entity.GroupMember;
import com.withu.group.repository.GroupMemberRepository;
import com.withu.meal.entity.Meal;
import com.withu.meal.entity.MealSlot;
import com.withu.meal.repository.MealRepository;
import com.withu.mission.entity.Mission;
import com.withu.mission.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 7일 챌린지 종료 처리 (PRD 8. 방 운영 / 10. 랭킹 시스템).
 * 종료 시 사이클 점수 기준으로 최종 순위를 매기고 순위별 보상을 지급한 뒤 결과를 스냅샷으로 남긴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengeService {

    private static final Map<MealSlot, String> MEAL_LABELS = Map.of(
            MealSlot.BREAKFAST, "아침",
            MealSlot.LUNCH, "점심",
            MealSlot.DINNER, "저녁"
    );

    private final ChallengeResultRepository challengeResultRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final MissionRepository missionRepository;
    private final MealRepository mealRepository;
    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;

    /**
     * 챌린지를 종료하고 보상을 지급한다. 이미 이번 사이클이 정산된 경우에는 저장된 결과를 그대로 돌려주므로
     * 여러 그룹원이 각자 호출해도 보상이 중복 지급되지 않는다.
     */
    @Transactional
    public SummaryResponse endChallenge(Long userId) {
        GroupMember myMembership = getMembership(userId);
        Group group = myMembership.getGroup();

        if (!group.isLastDay()) {
            throw new CustomException(ErrorCode.CHALLENGE_NOT_FINISHED);
        }
        if (!challengeResultRepository.existsByGroupIdAndCycleStartedAt(group.getId(), group.getStartedAt())) {
            settle(group);
        }
        return buildSummary(userId, group);
    }

    /** 이미 종료된 사이클의 결과 조회 (결과 화면 재진입용). */
    public SummaryResponse getSummary(Long userId) {
        Group group = getMembership(userId).getGroup();
        if (!challengeResultRepository.existsByGroupIdAndCycleStartedAt(group.getId(), group.getStartedAt())) {
            throw new CustomException(ErrorCode.CHALLENGE_NOT_FINISHED);
        }
        return buildSummary(userId, group);
    }

    /**
     * 같은 그룹으로 새 사이클 시작 (PRD 8. 방 운영 — 계속하기).
     * 사이클 점수만 초기화하고 캐릭터/누적 점수/코인은 그대로 유지한다.
     */
    @Transactional
    public void continueChallenge(Long userId) {
        Group group = getMembership(userId).getGroup();
        if (!challengeResultRepository.existsByGroupIdAndCycleStartedAt(group.getId(), group.getStartedAt())) {
            throw new CustomException(ErrorCode.CHALLENGE_NOT_FINISHED);
        }
        groupMemberRepository.findByGroupId(group.getId()).forEach(GroupMember::resetCyclePoints);
        group.restartCycle();
    }

    /** 사이클 점수 기준으로 순위를 매기고 보상 지급 + 결과 스냅샷 저장. */
    private void settle(Group group) {
        List<GroupMember> members = groupMemberRepository.findByGroupId(group.getId());
        LocalDate from = group.getStartedAt().toLocalDate();
        LocalDate to = LocalDate.now();

        List<Long> userIds = members.stream().map(GroupMember::getUserId).toList();
        Map<Long, Integer> rateByUser = achievementRatesOf(userIds, from, to);

        List<GroupMember> ranked = members.stream()
                .sorted(Comparator.comparingInt(GroupMember::getCyclePoints).reversed())
                .toList();

        for (int i = 0; i < ranked.size(); i++) {
            GroupMember member = ranked.get(i);
            int rank = i + 1;
            int bonusCoins = ChallengeReward.bonusCoinsFor(rank);
            int bonusPoints = ChallengeReward.bonusPointsFor(rank);
            String badge = ChallengeReward.badgeFor(rank);

            User user = userRepository.findById(member.getUserId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            user.addCoins(bonusCoins);
            user.addPoints(member.getCyclePoints() + bonusPoints);

            if (badge != null) {
                awardBadge(member.getUserId(), badge);
            }

            challengeResultRepository.save(ChallengeResult.builder()
                    .userId(member.getUserId())
                    .groupId(group.getId())
                    .cycleStartedAt(group.getStartedAt())
                    .finalRank(rank)
                    .totalParticipants(ranked.size())
                    .points(member.getCyclePoints())
                    .achievementRate(rateByUser.getOrDefault(member.getUserId(), 0))
                    .coinsEarned(member.getCyclePoints())
                    .bonusCoins(bonusCoins)
                    .badgeAwarded(badge)
                    .build());
        }
    }

    private void awardBadge(Long userId, String badgeId) {
        userBadgeRepository.findByUserIdAndBadgeId(userId, badgeId)
                .ifPresentOrElse(
                        UserBadge::increase,
                        () -> userBadgeRepository.save(UserBadge.builder().userId(userId).badgeId(badgeId).build()));
    }

    private SummaryResponse buildSummary(Long userId, Group group) {
        List<ChallengeResult> results = challengeResultRepository
                .findByGroupIdAndCycleStartedAt(group.getId(), group.getStartedAt()).stream()
                .sorted(Comparator.comparingInt(ChallengeResult::getFinalRank))
                .toList();

        List<Long> userIds = results.stream().map(ChallengeResult::getUserId).toList();
        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Character> charactersByUserId = characterRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(Character::getUserId, c -> c));

        List<ParticipantResult> ranking = new ArrayList<>();
        for (ChallengeResult result : results) {
            User user = usersById.get(result.getUserId());
            Character character = charactersByUserId.get(result.getUserId());
            boolean me = result.getUserId().equals(userId);
            ranking.add(new ParticipantResult(
                    result.getUserId(),
                    me,
                    labelOf(user, me),
                    character != null ? character.getSpecies() : null,
                    character != null ? character.getExpression().name() : null,
                    character != null ? character.getOutfit() : null,
                    result.getFinalRank(),
                    result.getPoints(),
                    result.getAchievementRate(),
                    result.getBonusCoins(),
                    result.getBadgeAwarded()
            ));
        }

        ChallengeResult mine = challengeResultRepository
                .findByUserIdAndGroupIdAndCycleStartedAt(userId, group.getId(), group.getStartedAt())
                .orElseThrow(() -> new CustomException(ErrorCode.CHALLENGE_NOT_FINISHED));

        return new SummaryResponse(
                group.getName(),
                Group.CHALLENGE_LENGTH_DAYS,
                mine.getAchievementRate(),
                mine.getCoinsEarned() + mine.getBonusCoins(),
                mine.getFinalRank(),
                mine.getTotalParticipants(),
                mine.getBadgeAwarded(),
                ranking,
                mealPhotosOf(userId, group)
        );
    }

    private List<MealPhoto> mealPhotosOf(Long userId, Group group) {
        return mealRepository
                .findByUserIdAndMealDateBetweenOrderByIdAsc(userId, group.getStartedAt().toLocalDate(), LocalDate.now())
                .stream()
                .filter(meal -> meal.getPhotoUrl() != null)
                .map(meal -> new MealPhoto(
                        meal.getSlot().name().toLowerCase(),
                        MEAL_LABELS.get(meal.getSlot()),
                        meal.getPhotoUrl()))
                .toList();
    }

    /** 사이클 전체 미션 기준 달성률 — 오늘 하루가 아니라 7일 전체를 본다. */
    private Map<Long, Integer> achievementRatesOf(List<Long> userIds, LocalDate from, LocalDate to) {
        Map<Long, List<Mission>> byUser = missionRepository.findByUserIdInAndMissionDateBetween(userIds, from, to)
                .stream()
                .collect(Collectors.groupingBy(Mission::getUserId));
        return userIds.stream().collect(Collectors.toMap(
                uid -> uid,
                uid -> {
                    List<Mission> missions = byUser.getOrDefault(uid, List.of());
                    if (missions.isEmpty()) {
                        return 0;
                    }
                    long done = missions.stream().filter(Mission::isDone).count();
                    return (int) Math.round(100.0 * done / missions.size());
                }));
    }

    private String labelOf(User user, boolean me) {
        if (user != null && user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        return me ? "나" : "그룹원";
    }

    private GroupMember getMembership(Long userId) {
        return groupMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
    }
}
