package com.withu.ai;

import java.util.List;

/**
 * AI 개인 맞춤 미션 생성 포트. 실제 연동 시 OpenAiMissionClient(GPT-4o) 구현체로 교체한다
 * (PRD 5. AI 개인 맞춤 미션 — 입력: 목표/신체정보/이전 수행결과/이전 식단 분석 결과).
 */
public interface MissionAiClient {

    List<GeneratedMission> generateDailyMissions(GenerateMissionCommand command);

    /**
     * @param previousAchievementRate 직전 활동일의 미션 달성률(%)
     * @param difficulty              PRD 6. 난이도 조절 규칙으로 계산된 이번 미션의 난이도 방향
     * @param missionCount            이번에 생성할 미션 개수 (3일 연속 실패 시 1개로 축소)
     * @param recentMeals             최근 식단 분석 결과 — AI가 다음 미션을 정할 때 참고하는 핵심 입력
     */
    record GenerateMissionCommand(
            String goal,
            String gender,
            int age,
            int height,
            int weight,
            double previousAchievementRate,
            Difficulty difficulty,
            int missionCount,
            List<RecentMeal> recentMeals
    ) {
    }

    /** 최근 식단 한 끼의 분석 결과. fit은 건강 목표 적합도(GOOD/NORMAL/BAD). */
    record RecentMeal(String slot, boolean achieved, String fit) {
    }

    /** 난이도 조절 방향 (PRD 6. 난이도 조절). */
    enum Difficulty {
        UP, KEEP, DOWN
    }

    record GeneratedMission(MissionType type, String title) {
    }

    enum MissionType {
        DIET, LIFESTYLE
    }
}
