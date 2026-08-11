package com.withu.mission.service;

import com.withu.ai.MissionAiClient.Difficulty;
import com.withu.ai.MissionAiClient.RecentMeal;
import com.withu.meal.entity.Meal;
import com.withu.meal.repository.MealRepository;
import com.withu.mission.entity.Mission;
import com.withu.mission.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 지난 미션·식단 기록을 읽어 오늘 미션의 난이도와 개수를 정한다 (PRD 6. 난이도 조절).
 *
 * <p>PRD의 규칙은 다음과 같다.
 * <pre>
 *   달성률 90% 이상 → 난이도 상승
 *   달성률 50~90%   → 현재 유지
 *   달성률 50% 미만 → 난이도 하향
 *   3일 연속 실패    → 미션 1개로 축소
 * </pre>
 *
 * <p>PRD가 "실패"를 수치로 정의하지 않아, 난이도 하향 기준과 같은 <b>달성률 50% 미만</b>인 날을
 * 실패한 날로 본다. 미션이 아예 없던 날(가입 전, 그룹 없던 날)은 실패도 성공도 아니므로 세지 않는다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissionHistoryAnalyzer {

    /** 기본 미션 개수 — 식단 2 + 생활습관 1 (PRD 5. 하루 3~4개). */
    static final int DEFAULT_MISSION_COUNT = 3;
    /** 3일 연속 실패하면 부담을 줄이기 위해 미션을 1개만 준다. */
    static final int REDUCED_MISSION_COUNT = 1;
    private static final int CONSECUTIVE_FAILURE_LIMIT = 3;

    private static final int FAILURE_RATE_THRESHOLD = 50;
    private static final int DIFFICULTY_UP_THRESHOLD = 90;

    /** 며칠 치 기록을 볼지 — 챌린지 한 사이클(7일)이면 난이도 판단에 충분하다. */
    private static final int LOOKBACK_DAYS = 7;

    private final MissionRepository missionRepository;
    private final MealRepository mealRepository;

    public MissionPlan analyze(Long userId, LocalDate today) {
        NavigableMap<LocalDate, Integer> ratesByDate = dailyRatesBefore(userId, today);

        Integer previousRate = ratesByDate.isEmpty() ? null : ratesByDate.lastEntry().getValue();
        int consecutiveFailures = countTrailingFailures(ratesByDate);

        Difficulty difficulty = difficultyFor(previousRate);
        int missionCount = consecutiveFailures >= CONSECUTIVE_FAILURE_LIMIT
                ? REDUCED_MISSION_COUNT
                : DEFAULT_MISSION_COUNT;

        return new MissionPlan(
                previousRate == null ? 0 : previousRate,
                difficulty,
                missionCount,
                recentMealsBefore(userId, today));
    }

    /**
     * 오늘 이전 날짜별 달성률(%). 미션이 있었던 날만 담기며, 날짜 오름차순이다.
     */
    private NavigableMap<LocalDate, Integer> dailyRatesBefore(Long userId, LocalDate today) {
        List<Mission> missions = missionRepository.findByUserIdInAndMissionDateBetween(
                List.of(userId), today.minusDays(LOOKBACK_DAYS), today.minusDays(1));

        return missions.stream()
                .collect(Collectors.groupingBy(Mission::getMissionDate, TreeMap::new,
                        Collectors.collectingAndThen(Collectors.toList(), this::rateOf)));
    }

    /** 가장 최근 날짜부터 거슬러 올라가며 연속으로 실패한 날 수를 센다. */
    private int countTrailingFailures(NavigableMap<LocalDate, Integer> ratesByDate) {
        List<Integer> newestFirst = ratesByDate.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, Integer>comparingByKey().reversed())
                .map(Map.Entry::getValue)
                .toList();

        int count = 0;
        for (int rate : newestFirst) {
            if (rate >= FAILURE_RATE_THRESHOLD) {
                break;
            }
            count++;
        }
        return count;
    }

    /** 기록이 아예 없는 첫날은 기준이 없으므로 유지(KEEP)로 시작한다. */
    private Difficulty difficultyFor(Integer previousRate) {
        if (previousRate == null) {
            return Difficulty.KEEP;
        }
        if (previousRate >= DIFFICULTY_UP_THRESHOLD) {
            return Difficulty.UP;
        }
        if (previousRate < FAILURE_RATE_THRESHOLD) {
            return Difficulty.DOWN;
        }
        return Difficulty.KEEP;
    }

    /**
     * 직전 활동일의 식단 분석 결과. AI가 "어제 뭘 먹었는지"를 보고 오늘 미션을 정하도록 넘긴다
     * (PRD 1·5·6 — 식단 수행 결과를 분석해 다음 미션을 생성하는 루프의 입력).
     */
    private List<RecentMeal> recentMealsBefore(Long userId, LocalDate today) {
        List<Meal> meals = mealRepository.findByUserIdAndMealDateBetweenOrderByIdAsc(
                userId, today.minusDays(LOOKBACK_DAYS), today.minusDays(1));
        if (meals.isEmpty()) {
            return List.of();
        }

        LocalDate lastDate = meals.stream()
                .map(Meal::getMealDate)
                .max(Comparator.naturalOrder())
                .orElseThrow();

        return meals.stream()
                .filter(meal -> meal.getMealDate().equals(lastDate))
                .map(meal -> new RecentMeal(
                        meal.getSlot().name().toLowerCase(),
                        meal.isAchieved(),
                        meal.getInternalFit().name()))
                .toList();
    }

    private int rateOf(List<Mission> missions) {
        if (missions.isEmpty()) {
            return 0;
        }
        long done = missions.stream().filter(Mission::isDone).count();
        return (int) Math.round(100.0 * done / missions.size());
    }

    /** 오늘 미션을 어떻게 만들지에 대한 결론. */
    public record MissionPlan(
            double previousAchievementRate,
            Difficulty difficulty,
            int missionCount,
            List<RecentMeal> recentMeals
    ) {
    }
}
