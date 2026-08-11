package com.withu.ai.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.withu.ai.MissionAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * GPT-4o(mini) 기반 개인 맞춤 미션 생성 (PRD 5. AI 개인 맞춤 미션).
 * 목표/성별/나이/키/몸무게/이전 달성률을 입력받아 식단 미션 2개 + 생활습관 미션 1개를 생성한다.
 */
@Slf4j
@RequiredArgsConstructor
public class OpenAiMissionClient implements MissionAiClient {

    private static final String SYSTEM_PROMPT = """
            너는 건강관리 앱 WITHU의 AI 코치야. 사용자의 건강 목표와 신체 정보를 바탕으로
            하루 동안 실천 가능한 미션을 만들어. 식단 미션 2개(식사 관련, 구체적인 행동), 생활습관 미션 1개
            (물 마시기/걷기/스트레칭 등 간단한 습관)를 생성해.
            반드시 아래 JSON 형식으로만 응답해:
            {"missions": [{"type": "DIET", "title": "..."}, {"type": "DIET", "title": "..."}, {"type": "LIFESTYLE", "title": "..."}]}
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
                    최근 미션 달성률: %.0f%%
                    """.formatted(
                    command.goal(), command.gender(), command.age(), command.height(), command.weight(),
                    command.previousAchievementRate());

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
            return missions;
        } catch (Exception e) {
            log.error("OpenAI 미션 생성 실패, fallback 없이 예외 전파", e);
            throw new IllegalStateException("AI 미션 생성에 실패했습니다", e);
        }
    }
}
