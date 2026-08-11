package com.withu.ranking.service;

import com.withu.auth.entity.User;
import com.withu.auth.repository.UserRepository;
import com.withu.character.entity.Character;
import com.withu.character.repository.CharacterRepository;
import com.withu.global.error.CustomException;
import com.withu.global.error.ErrorCode;
import com.withu.group.entity.GroupMember;
import com.withu.group.repository.GroupMemberRepository;
import com.withu.mission.entity.Mission;
import com.withu.mission.repository.MissionRepository;
import com.withu.ranking.dto.RankingDto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankingService {

    private final GroupMemberRepository groupMemberRepository;
    private final MissionRepository missionRepository;
    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;

    /** 그룹 내 오늘 달성률 기준 실시간 순위 (PRD 10. 랭킹 시스템 — 방 내 순위). */
    public GroupRankingResponse getGroupRanking(Long userId) {
        GroupMember me = groupMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
        Long groupId = me.getGroup().getId();
        List<Long> memberUserIds = groupMemberRepository.findByGroupId(groupId).stream()
                .map(GroupMember::getUserId)
                .toList();

        List<Mission> todayMissions = missionRepository.findByUserIdInAndMissionDate(memberUserIds, LocalDate.now());
        Map<Long, List<Mission>> missionsByUser = todayMissions.stream()
                .collect(Collectors.groupingBy(Mission::getUserId));

        Map<Long, User> usersById = userRepository.findAllById(memberUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Character> charactersByUserId = characterRepository.findByUserIdIn(memberUserIds).stream()
                .collect(Collectors.toMap(Character::getUserId, c -> c));

        List<MemberRate> rates = memberUserIds.stream()
                .map(uid -> new MemberRate(uid, achievementRate(missionsByUser.getOrDefault(uid, List.of()))))
                .sorted((a, b) -> b.rate() - a.rate())
                .toList();

        List<GroupRankingItem> result = new java.util.ArrayList<>();
        for (int i = 0; i < rates.size(); i++) {
            MemberRate memberRate = rates.get(i);
            Long uid = memberRate.userId();
            User user = usersById.get(uid);
            Character character = charactersByUserId.get(uid);
            result.add(new GroupRankingItem(
                    i + 1,
                    uid,
                    uid.equals(userId),
                    user != null ? user.getNickname() : null,
                    character != null ? character.getSpecies() : null,
                    character != null ? character.getExpression().name() : null,
                    memberRate.rate()
            ));
        }
        return new GroupRankingResponse(result);
    }

    /** 누적 코인 기준 전체 사용자 랭킹 (PRD 10. 랭킹 시스템 — 전체 랭킹). */
    public GlobalRankingResponse getGlobalRanking() {
        List<User> topUsers = userRepository.findTop100ByOrderByCoinsDesc();
        List<GlobalRankingItem> result = new java.util.ArrayList<>();
        for (int i = 0; i < topUsers.size(); i++) {
            User user = topUsers.get(i);
            result.add(new GlobalRankingItem(i + 1, user.getId(), user.getNickname(), user.getCoins()));
        }
        return new GlobalRankingResponse(result);
    }

    private record MemberRate(Long userId, int rate) {
    }

    private int achievementRate(List<Mission> missions) {
        if (missions.isEmpty()) {
            return 0;
        }
        long done = missions.stream().filter(Mission::isDone).count();
        return (int) Math.round(100.0 * done / missions.size());
    }
}
