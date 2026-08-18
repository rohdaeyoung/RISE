package com.withu.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.withu.ai.openai.OpenAiChatCaller;
import com.withu.ai.openai.OpenAiLifestyleVisionClient;
import com.withu.ai.openai.OpenAiMealVisionClient;
import com.withu.ai.openai.OpenAiMissionClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * OPENAI_API_KEY가 설정된 경우에만 실제 OpenAI 연동 Bean을 등록해 mock 구현체(ai.mock 패키지, 항상
 * 등록됨) 대신 사용한다. 키가 없으면 이 설정은 아무것도 등록하지 않고 mock이 그대로 동작한다.
 */
@Configuration
public class OpenAiClientConfig {

    // Boot 4의 webmvc 스타터만으로는 Jackson 자동 설정이 붙지 않는 경우가 있어 안전하게 직접 등록.
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnExpression("!'${openai.api-key:}'.isBlank()")
    public RestClient openAiRestClient(@Value("${openai.api-key}") String apiKey) {
        return RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 미션 생성용 호출기. 기본 모델의 하루 한도가 차면 fallback 목록의 모델로 이어서 시도한다.
     * 같은 키·같은 계정이고, 한도가 모델마다 따로 잡혀 있어서 가능한 방식이다.
     */
    @Bean
    @ConditionalOnBean(RestClient.class)
    public OpenAiChatCaller missionChatCaller(
            RestClient openAiRestClient,
            @Value("${openai.mission-model}") String model,
            @Value("${openai.fallback-models}") List<String> fallbackModels) {
        return new OpenAiChatCaller(openAiRestClient, model, fallbackModels);
    }

    /** 사진 판정용 호출기. fallback 모델도 <b>이미지를 볼 수 있어야</b> 한다. */
    @Bean
    @ConditionalOnBean(RestClient.class)
    public OpenAiChatCaller visionChatCaller(
            RestClient openAiRestClient,
            @Value("${openai.vision-model}") String model,
            @Value("${openai.fallback-models}") List<String> fallbackModels) {
        return new OpenAiChatCaller(openAiRestClient, model, fallbackModels);
    }

    @Bean
    @Primary
    @ConditionalOnBean(RestClient.class)
    public OpenAiMissionClient openAiMissionClient(OpenAiChatCaller missionChatCaller, ObjectMapper objectMapper) {
        return new OpenAiMissionClient(missionChatCaller, objectMapper);
    }

    @Bean
    @Primary
    @ConditionalOnBean(RestClient.class)
    public OpenAiMealVisionClient openAiMealVisionClient(OpenAiChatCaller visionChatCaller, ObjectMapper objectMapper) {
        return new OpenAiMealVisionClient(visionChatCaller, objectMapper);
    }

    @Bean
    @Primary
    @ConditionalOnBean(RestClient.class)
    public OpenAiLifestyleVisionClient openAiLifestyleVisionClient(
            OpenAiChatCaller visionChatCaller, ObjectMapper objectMapper) {
        return new OpenAiLifestyleVisionClient(visionChatCaller, objectMapper);
    }
}
