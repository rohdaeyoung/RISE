package com.withu.ai.mock;

import com.withu.ai.MissionAiClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 목표(goal) 하나만 보고 고정 풀에서 미션을 뽑는 mock 생성기 — 프론트 missionApi.js의
 * MISSION_POOL을 그대로 이식. 실제 OpenAI 키가 오면 OpenAiMissionClient로 교체한다.
 */
@Component
public class MockMissionAiClient implements MissionAiClient {

    private static final Map<String, List<String>> DIET_POOL = Map.of(
            "diet", List.of("아침 든든하게 챙겨 먹기", "점심 탄단지 균형 맞추기", "저녁 과식하지 않기", "야식 대신 물 마시기", "채소 반찬 한 가지 이상 먹기"),
            "bulk", List.of("고단백 식단 챙겨 먹기", "운동 후 30분 내 단백질 섭취하기", "삼시세끼 거르지 않기"),
            "health", List.of("채소 3가지 이상 포함해서 먹기", "삼시세끼 챙겨먹기", "가공식품 대신 자연식 선택하기")
    );

    private static final List<String> LIFESTYLE_POOL =
            List.of("물 8잔 마시기", "엘리베이터 대신 계단 이용하기", "자기 전 스트레칭 5분 하기");

    @Override
    public List<GeneratedMission> generateDailyMissions(GenerateMissionCommand command) {
        // mock은 AI 추론을 흉내낼 수 없지만, 개수 축소(3일 연속 실패 → 1개)만큼은 실제 구현과 맞춰둔다.
        // 이걸 지키지 않으면 키 없이 개발할 때 미션 개수가 실제 동작과 달라진다.
        int missionCount = Math.max(1, command.missionCount());
        int dietCount = missionCount == 1 ? 1 : missionCount - 1;

        List<String> dietPool = DIET_POOL.getOrDefault(command.goal(), DIET_POOL.get("health"));
        List<String> dietTitles = pickRandom(dietPool, Math.min(dietCount, dietPool.size()));

        List<GeneratedMission> missions = new ArrayList<>();
        dietTitles.forEach(title -> missions.add(new GeneratedMission(MissionType.DIET, title)));
        if (missionCount > 1) {
            pickRandom(LIFESTYLE_POOL, 1)
                    .forEach(title -> missions.add(new GeneratedMission(MissionType.LIFESTYLE, title)));
        }
        return missions;
    }

    private List<String> pickRandom(List<String> pool, int count) {
        List<String> shuffled = new ArrayList<>(pool);
        java.util.Collections.shuffle(shuffled, ThreadLocalRandom.current());
        return shuffled.subList(0, count);
    }
}
