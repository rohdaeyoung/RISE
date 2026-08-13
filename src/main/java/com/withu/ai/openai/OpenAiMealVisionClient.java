package com.withu.ai.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.withu.ai.MealVisionAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;

/**
 * GPT-4o(mini) Vision 기반 식단 사진 분석 (PRD 6. AI 분석 상세).
 * 칼로리를 정확히 계산하기보다, 사용자 건강 목표에 적합한 식단인지 방향성만 판단한다.
 * internalFit(목표적합도)은 다음 미션 생성 내부 기준으로만 쓰이고 사용자에게 노출하지 않는다.
 */
@Slf4j
@RequiredArgsConstructor
public class OpenAiMealVisionClient implements MealVisionAiClient {

    private static final String SYSTEM_PROMPT = """
            너는 건강관리 앱 WITHU의 식단 분석 AI야. 사용자가 올린 식단 사진과 정보를 보고
            음식 종류, 구성, 영양 균형, 건강 목표 적합도를 종합 판단해.
            칼로리를 정확한 수치로 계산하려 하지 말고 방향성만 판단해.

            반드시 아래 JSON 형식으로만 응답해:
            {"achieved": true|false, "internalFit": "GOOD"|"NORMAL"|"BAD"}

            achieved: 주어진 "오늘의 식단 미션"을 이 식사로 수행했다고 볼 수 있으면 true.
            판정 기준은 이렇게 잡아:
            - 사진이 사람이 먹는 음식이고 미션 취지에 어긋나지 않으면 true로 본다.
            - 미션 조건을 완벽히 만족하지 않아도, 그 방향으로 노력한 흔적이 보이면 true다.
              (예: 미션이 "채소 두 가지 이상"인데 한 가지만 보여도 채소를 챙겼으면 true)
            - false는 미션에 명백히 반하는 경우에만 쓴다.
              (예: 미션이 "야식 대신 물"인데 치킨과 맥주 사진)
            - 음식이 전혀 보이지 않는 사진이면 false.
            - 미션이 "-"로 비어 있으면 건강한 식사인지만 보고 판단한다.
            사용자는 습관을 만드는 중이지 시험을 보는 게 아니다. 애매하면 true 쪽으로 판단해.

            internalFit: 건강 목표 적합도. 사용자에게 보여주지 않고 다음 미션 난이도 조절에만 쓰므로
            achieved와 무관하게 솔직하게 매겨. 목표에 잘 맞으면 GOOD, 보통이면 NORMAL, 어긋나면 BAD.
            """;

    private final RestClient openAiRestClient;
    private final ObjectMapper objectMapper;
    private final String model;

    @Override
    public MealAnalysisResult analyze(MultipartFile photo, String foodName, String portion, String goal,
                                      String missionTitle) {
        try {
            String imageDataUri = toDataUri(photo);
            String userText = """
                    오늘의 식단 미션: %s
                    건강 목표: %s
                    음식 이름: %s
                    섭취량: %s
                    """.formatted(blankToDash(missionTitle), goal, blankToDash(foodName), blankToDash(portion));

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.putObject("response_format").put("type", "json_object");
            ArrayNode messages = requestBody.putArray("messages");
            messages.addObject().put("role", "system").put("content", SYSTEM_PROMPT);

            ObjectNode userMessage = messages.addObject().put("role", "user");
            ArrayNode contentArray = userMessage.putArray("content");
            contentArray.addObject().put("type", "text").put("text", userText);
            contentArray.addObject().put("type", "image_url")
                    .putObject("image_url").put("url", imageDataUri);

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

            boolean achieved = parsed.path("achieved").asBoolean(false);
            InternalFit fit = InternalFit.valueOf(parsed.path("internalFit").asText("NORMAL").toUpperCase());
            return new MealAnalysisResult(achieved, fit);
        } catch (Exception e) {
            log.error("OpenAI 식단 분석 실패", e);
            throw new IllegalStateException("AI 식단 분석에 실패했습니다", e);
        }
    }

    private String toDataUri(MultipartFile photo) {
        try {
            String contentType = StringUtils.hasText(photo.getContentType()) ? photo.getContentType() : "image/jpeg";
            String base64 = Base64.getEncoder().encodeToString(photo.getBytes());
            return "data:" + contentType + ";base64," + base64;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String blankToDash(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }
}
