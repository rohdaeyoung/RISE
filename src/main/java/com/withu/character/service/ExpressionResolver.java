package com.withu.character.service;

import com.withu.character.entity.Expression;
import com.withu.character.entity.ExpressionPolicy;
import com.withu.character.repository.CharacterRepository;
import com.withu.mission.entity.Mission;
import com.withu.mission.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 오늘 달성률과 그룹 내 순위로 캐릭터 표정을 계산한다.
 *
 * 표정은 미션을 완료할 때마다 바뀌는 파생 값이라 조회 시점에 계산하는 것을 원본으로 삼는다.
 * DB의 characters.expression 컬럼은 캐릭터 단건 조회(/api/characters/me)를 위한 캐시라서,
 * 미션이 완료될 때 {@link #refreshGroup(List)}로 함께 갱신해준다.
 */
@Component
@RequiredArgsConstructor
public class ExpressionResolver {

    private final MissionRepository missionRepository;
    private final CharacterRepository characterRepository;

    /** 그룹원들의 오늘 기준 표정을 한 번에 계산한다. */
    public Map<Long, Expression> todayExpressions(List<Long> userIds) {
        Map<Long, Integer> rateByUser = todayRates(userIds);

        List<Long> rankedUserIds = userIds.stream()
                .sorted(Comparator.comparingInt((Long uid) -> rateByUser.getOrDefault(uid, 0)).reversed())
                .toList();

        Map<Long, Expression> result = new HashMap<>();
        for (int i = 0; i < rankedUserIds.size(); i++) {
            Long uid = rankedUserIds.get(i);
            result.put(uid, ExpressionPolicy.fromRank(i + 1, rankedUserIds.size(), rateByUser.getOrDefault(uid, 0)));
        }
        return result;
    }

    /** 미션 완료 등으로 달성률이 바뀐 뒤, 그룹 전체의 표정 캐시를 다시 계산해 저장한다. */
    @Transactional
    public void refreshGroup(List<Long> userIds) {
        Map<Long, Expression> expressions = todayExpressions(userIds);
        characterRepository.findByUserIdIn(userIds)
                .forEach(character -> {
                    Expression next = expressions.get(character.getUserId());
                    if (next != null) {
                        character.changeExpression(next);
                    }
                });
    }

    /** 오늘 미션 기준 달성률(%) — 미션이 없으면 0%. */
    public Map<Long, Integer> todayRates(List<Long> userIds) {
        Map<Long, List<Mission>> byUser = missionRepository
                .findByUserIdInAndMissionDate(userIds, LocalDate.now()).stream()
                .collect(Collectors.groupingBy(Mission::getUserId));

        return userIds.stream().collect(Collectors.toMap(
                uid -> uid,
                uid -> rateOf(byUser.getOrDefault(uid, List.of()))));
    }

    private int rateOf(List<Mission> missions) {
        if (missions.isEmpty()) {
            return 0;
        }
        long done = missions.stream().filter(Mission::isDone).count();
        return (int) Math.round(100.0 * done / missions.size());
    }
}
