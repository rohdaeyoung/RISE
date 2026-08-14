package com.withu.ai.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.withu.ai.LifestyleVisionAiClient;
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
 * GPT-4o(mini) Vision으로 생활습관 미션 인증 사진을 판정한다.
 *
 * <p>"이 사진이 미션을 수행했다는 증거가 되는가"만 본다. 사진이 예쁜지, 건강해 보이는지는 상관없다.
 * 걷기 미션에 음식 사진을 올리는 것처럼 미션과 무관한 사진은 걸러내는 것이 이 클래스의 존재 이유다.
 */
@Slf4j
@RequiredArgsConstructor
public class OpenAiLifestyleVisionClient implements LifestyleVisionAiClient {

    private static final String SYSTEM_PROMPT = """
            너는 건강관리 앱 WITHU의 생활습관 미션 인증 심사 AI야.
            사용자가 "오늘의 미션"을 실제로 했다는 증거로 사진을 올렸어.
            이 사진이 그 미션을 수행했다는 증거가 되는지 판단해.

            반드시 아래 JSON 형식으로만 응답해:
            {"seen": "사진에 보이는 것", "achieved": true|false}

            seen: 먼저 사진에 무엇이 보이는지 그대로 적어. 장소, 사물, 행동을 짧게 적으면 된다.
            판정은 이걸 적은 뒤에 해.

            achieved: 사진이 "그 행동을 하는 상황에서 찍을 법한 사진"인지만 보면 된다.
            그 행동을 하는 장면이 찍혀 있을 필요는 없다. 혼자 걸으면서 자기 모습을 찍을 수는 없으니,
            걷다가 본 풍경을 찍는 것이 자연스러운 인증 방식이다.

            먼저 미션이 어떤 장면과 어울리는지 떠올린 다음, 사진이 거기에 들어가면 true를 줘.
            - "걷기 / 산책 / 조깅 / 등산 / 외출" → 바깥 풍경, 길, 공원, 하늘, 거리, 나무, 산,
              운동화나 발, 걸음 수 화면. 사람이 안 보여도 바깥에서 찍은 사진이면 true.
            - "물 마시기" → 물병, 물컵, 정수기, 텀블러.
            - "스트레칭 / 운동 / 요가" → 스트레칭 자세, 운동 매트, 운동 기구, 운동하는 공간.
            - "일찍 자기 / 수면" → 침실, 잠자리, 취침 시각이 보이는 화면.
            - "계단 이용" → 계단.

            false는 이럴 때만 준다.
            - 사진 내용이 미션과 전혀 다른 범주일 때.
              (예: "걷기" 미션에 음식·채소·식탁·접시 사진 → 밥 먹은 사진이지 나간 사진이 아니다)
              (예: "물 마시기" 미션에 바깥 풍경 사진)
            - 무엇인지 알아볼 수 없을 때. 단색, 노이즈, 심하게 흐려서 내용을 말할 수 없는 사진.

            판단은 사진에 보이는 것만 근거로 삼되, 화질이나 구도로 트집 잡지 마.
            그림이나 화면 캡처여도 내용이 미션과 이어지면 true다.
            사용자는 습관을 만드는 중이지 알리바이를 제출하는 게 아니다.
            seen에 적은 것이 위 목록의 어느 항목과 이어진다면 주저 없이 true를 줘.
            """;

    private final RestClient openAiRestClient;
    private final ObjectMapper objectMapper;
    private final String model;

    @Override
    public LifestyleVerification verify(MultipartFile photo, String missionTitle) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.putObject("response_format").put("type", "json_object");
            ArrayNode messages = requestBody.putArray("messages");
            messages.addObject().put("role", "system").put("content", SYSTEM_PROMPT);

            ObjectNode userMessage = messages.addObject().put("role", "user");
            ArrayNode content = userMessage.putArray("content");
            content.addObject().put("type", "text").put("text", "오늘의 미션: " + missionTitle);
            content.addObject().put("type", "image_url")
                    .putObject("image_url").put("url", toDataUri(photo));

            // 응답을 String으로 받아 직접 파싱한다 (OpenAiMealVisionClient와 같은 이유 — Jackson 버전 차이).
            String responseJson = openAiRestClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(requestBody))
                    .retrieve()
                    .body(String.class);

            JsonNode parsed = objectMapper.readTree(
                    objectMapper.readTree(responseJson).at("/choices/0/message/content").asText());

            boolean achieved = parsed.path("achieved").asBoolean(false);
            String seen = parsed.path("seen").asText("-");
            log.info("생활습관 미션 인증 판정. 미션={} 인식={} 달성={}", missionTitle, seen, achieved);
            return new LifestyleVerification(achieved, seen);
        } catch (Exception e) {
            log.error("OpenAI 생활습관 미션 인증 판정 실패", e);
            throw new IllegalStateException("AI 인증 판정에 실패했습니다", e);
        }
    }

    private String toDataUri(MultipartFile photo) {
        try {
            String contentType = StringUtils.hasText(photo.getContentType()) ? photo.getContentType() : "image/jpeg";
            return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(photo.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
