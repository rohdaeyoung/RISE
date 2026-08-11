package com.withu.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Bean
    @Primary
    @ConditionalOnBean(RestClient.class)
    public OpenAiMissionClient openAiMissionClient(
            RestClient openAiRestClient, ObjectMapper objectMapper,
            @Value("${openai.mission-model}") String model) {
        return new OpenAiMissionClient(openAiRestClient, objectMapper, model);
    }

    @Bean
    @Primary
    @ConditionalOnBean(RestClient.class)
    public OpenAiMealVisionClient openAiMealVisionClient(
            RestClient openAiRestClient, ObjectMapper objectMapper,
            @Value("${openai.vision-model}") String model) {
        return new OpenAiMealVisionClient(openAiRestClient, objectMapper, model);
    }
}
