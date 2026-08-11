package com.withu.ai;

import java.util.List;

/**
 * AI 개인 맞춤 미션 생성 포트. 실제 연동 시 OpenAiMissionClient(GPT-4o) 구현체로 교체한다
 * (PRD 5. AI 개인 맞춤 미션 — 입력: 목표/신체정보/이전 수행결과, 출력: 식단 2~3 + 생활습관 1).
 */
public interface MissionAiClient {

    List<GeneratedMission> generateDailyMissions(GenerateMissionCommand command);

    record GenerateMissionCommand(
            String goal,
            String gender,
            int age,
            int height,
            int weight,
            double previousAchievementRate
    ) {
    }

    record GeneratedMission(MissionType type, String title) {
    }

    enum MissionType {
        DIET, LIFESTYLE
    }
}
