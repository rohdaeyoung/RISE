package com.withu.ai.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.withu.ai.MissionAiClient;
import com.withu.ai.MissionAiClient.RecentMeal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GPT-4o(mini) 기반 개인 맞춤 미션 생성 (PRD 5. AI 개인 맞춤 미션).
 * 목표/성별/나이/키/몸무게/이전 달성률을 입력받아 식단 미션 2개 + 생활습관 미션 1개를 생성한다.
 */
@Slf4j
@RequiredArgsConstructor
public class OpenAiMissionClient implements MissionAiClient {

    private static final String SYSTEM_PROMPT = """
            너는 건강관리 앱 WITHU의 AI 코치야. 사용자의 건강 목표와 신체 정보, 그리고 어제까지의
            수행 결과를 바탕으로 오늘 하루 실천 가능한 미션을 만들어.

            중요: 어제 식단 분석 결과가 주어지면 반드시 그걸 반영해. 예를 들어 어제 저녁이 목표에
            맞지 않았다면(BAD) 오늘 저녁 미션을 그 부분에 초점을 맞춰 만들고, 어제 거른 끼니가 있으면
            오늘은 그 끼니를 챙기는 미션을 넣어. 어제와 똑같은 미션을 그대로 반복하지 마.

            난이도 지시:
            - UP: 어제 잘 해냈으니 한 단계 더 도전적인 미션으로 (양·강도·구체성을 높임)
            - KEEP: 지금 수준을 유지
            - DOWN: 부담을 낮춰 더 쉽고 작은 행동으로

            반드시 아래 JSON 형식으로만 응답해:
            {"missions": [{"type": "DIET", "title": "..."}, {"type": "LIFESTYLE", "title": "..."}]}
            type은 DIET(식사 관련) 또는 LIFESTYLE(물 마시기/걷기/스트레칭 등 간단한 습관)이야.
            title은 한국어로 20자 이내, 구체적이고 실천 가능한 표현으로 작성해.
            """;

    private final RestClient openAiRestClient;
    private final ObjectMapper objectMapper;
    private final String model;

    @Override
    public List<GeneratedMission> generateDailyMissions(GenerateMissionCommand command) {
        try {
            String userPrompt = """
                    건강 목표: %s
                    성별: %s, 나이: %d세, 키: %dcm, 몸무게: %dkg
                    직전 미션 달성률: %.0f%%
                    난이도 지시: %s
                    만들 미션 개수: 정확히 %d개 (더도 덜도 말고 이 개수만큼.
                    식단 미션을 우선하고, 2개 이상이면 마지막 1개는 생활습관 미션으로)
                    어제 식단 분석 결과: %s
                    """.formatted(
                    command.goal(), command.gender(), command.age(), command.height(), command.weight(),
                    command.previousAchievementRate(), command.difficulty(), command.missionCount(),
                    describeRecentMeals(command.recentMeals()));

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.putObject("response_format").put("type", "json_object");
            ArrayNode messages = requestBody.putArray("messages");
            messages.addObject().put("role", "system").put("content", SYSTEM_PROMPT);
            messages.addObject().put("role", "user").put("content", userPrompt);

            String requestJson = objectMapper.writeValueAsString(requestBody);
            // Spring 7은 Jackson 3을 쓰지만 이 클래스는 Jackson 2 API로 파싱하므로,
            // 메시지 컨버터가 관여하지 않도록 응답을 String으로 받아 직접 읽는다.
            String responseJson = openAiRestClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestJson)
                    .retrieve()
                    .body(String.class);

            JsonNode response = objectMapper.readTree(responseJson);
            String content = response.at("/choices/0/message/content").asText();
            JsonNode parsed = objectMapper.readTree(content);

            List<GeneratedMission> missions = new ArrayList<>();
            for (JsonNode node : parsed.path("missions")) {
                MissionType type = MissionType.valueOf(node.path("type").asText("LIFESTYLE").toUpperCase());
                String title = node.path("title").asText();
                if (!title.isBlank()) {
                    missions.add(new GeneratedMission(type, title));
                }
            }
            if (missions.isEmpty()) {
                throw new IllegalStateException("OpenAI가 빈 미션 목록을 반환함");
            }
            // 개수는 서버가 정한 값(난이도 규칙의 결과)이 기준 — AI가 더 많이 만들어도 잘라낸다.
            return missions.size() > command.missionCount()
                    ? missions.subList(0, command.missionCount())
                    : missions;
        } catch (Exception e) {
            // 여기서 던진 예외는 MissionSetCreator가 받아 고정 풀 미션으로 대체한다.
            // 사용량 한도에 걸려도 미션은 나와야 앱이 멈추지 않는다.
            throw OpenAiErrors.translate("AI 미션 생성", e);
        }
    }

    /** 어제 식단을 AI가 읽을 수 있는 한 줄로 정리한다. 기록이 없으면 첫날이라고 알려준다. */
    private String describeRecentMeals(List<RecentMeal> meals) {
        if (meals == null || meals.isEmpty()) {
            return "기록 없음 (첫날이거나 어제 인증하지 않음)";
        }
        return meals.stream()
                .map(meal -> "%s: 목표적합도 %s, 미션달성 %s".formatted(
                        meal.slot(), meal.fit(), meal.achieved() ? "성공" : "실패"))
                .collect(Collectors.joining(" / "));
    }
}
