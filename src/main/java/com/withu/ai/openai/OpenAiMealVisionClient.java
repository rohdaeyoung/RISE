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
            {"food": "사진에 보이는 것", "achieved": true|false, "internalFit": "GOOD"|"NORMAL"|"BAD"}

            food: 먼저 사진에 무엇이 보이는지 그대로 적어. 음식이 아니면 그 사실을 적어
            ("음식 아님 - 하늘 사진", "음식 아님 - 단색 이미지"처럼). 판정은 이걸 적은 뒤에 해.

            achieved: 아래 순서로 판단해. 앞 단계에서 false가 나오면 거기서 끝이다.
            1) 사진에 실제 음식이나 음료가 보이지 않으면 false.
               사람, 풍경, 화면 캡처, 사물, 단색·무늬 이미지, 무엇인지 알아볼 수 없는 사진은 모두 false다.
               음식 사진이 아닌데 true를 주면 인증이 아무 의미가 없어진다.
            2) 음식이 맞다면, "오늘의 식단 미션"을 이 식사로 수행했다고 볼 수 있는지 본다.
               미션이 요구하는 핵심 요소가 사진에 보이면 true.
               (예: 미션이 "채소 반찬 두 가지 이상"인데 채소 반찬이 하나뿐이면 아쉬워도 true,
                아예 채소가 없고 고기와 밥만 있으면 false)
            3) 미션 취지에 정면으로 어긋나면 false.
               (예: 미션이 "야식 대신 물 마시기"인데 치킨과 맥주,
                미션이 "탄수화물 줄이기"인데 라면과 공기밥)
            4) 미션이 "-"로 비어 있으면, 건강 목표에 맞는 식사인지만 보고 판단한다.


            그리고 직접 찍은 사진이 아닌 것은 걸러내야 한다. 아래에 해당하면 achieved는 false다.
            - 화면 캡처: 상태 표시줄(시계·배터리·통신), 앱이나 웹페이지의 버튼·메뉴·검색창,
              마우스 커서, 브라우저 주소창, 스크롤바가 보이면 캡처다.
            - 화면을 카메라로 다시 찍은 사진: 모니터·휴대폰 테두리, 화면의 반사·물결무늬(모아레),
              화면 밖 책상이나 벽이 함께 찍힌 경우.
            - 인터넷에서 가져온 이미지: 워터마크, 로고, 자막이나 설명 글자가 얹혀 있는 경우,
              스튜디오 조명에 배경이 완벽히 정돈된 광고·요리책 같은 사진.
            직접 찍은 생활 사진은 보통 배경이 어수선하고 조명이 고르지 않다. 그런 사진이 정상이다.
            판단이 서지 않으면 캡처로 몰지 말고 내용으로 판단해.

            판정은 사진에 실제로 보이는 것만 근거로 삼아. 사용자가 적어낸 음식 이름이 사진과 다르면
            사진을 믿어라. 애매하면 미션의 핵심 요소가 보이는지를 기준으로 결정해.

            internalFit: 건강 목표 적합도. 사용자에게 보여주지 않고 다음 미션 난이도 조절에만 쓰므로
            achieved와 무관하게 솔직하게 매겨. 목표에 잘 맞으면 GOOD, 보통이면 NORMAL, 어긋나면 BAD.
            음식이 아닌 사진은 BAD.
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
            // 오판 신고가 들어왔을 때 AI가 사진을 무엇으로 봤는지 확인할 수 있어야 한다.
            // 사진 자체는 남기지 않고 판단 근거만 남긴다.
            log.info("식단 분석 결과. 미션={} 인식={} 달성={} 적합도={}",
                    missionTitle, parsed.path("food").asText("-"), achieved, parsed.path("internalFit").asText("-"));
            InternalFit fit = InternalFit.valueOf(parsed.path("internalFit").asText("NORMAL").toUpperCase());
            return new MealAnalysisResult(achieved, fit);
        } catch (Exception e) {
            throw OpenAiErrors.translate("AI 식단 분석", e);
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
