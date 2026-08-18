package com.withu.ai.openai;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.withu.global.error.CustomException;
import com.withu.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * OpenAI 채팅 요청을 보내되, 사용량 한도에 걸리면 다음 모델로 이어서 시도한다.
 *
 * <p>무료 등급은 <b>모델마다</b> 하루 요청 수를 따로 센다(gpt-4o-mini는 50회). 그래서 쓰던 모델이
 * 막혀도 다른 모델은 멀쩡히 살아 있다. 같은 키·같은 계정이므로 추가 결제가 아니라 <b>이미 가진
 * 한도를 마저 쓰는 것</b>이다.
 *
 * <p>이게 없으면 한도가 차는 순간 사진 인증이 통째로 막힌다. 심사 도중에 그렇게 되면 손쓸 방법이
 * 없으므로, 서버가 알아서 다음 모델로 넘어가게 한다.
 *
 * <p>넘어가는 조건은 <b>429(사용량 한도)뿐</b>이다. 잘못된 요청이나 서버 오류로 모델을 바꿔가며
 * 재시도하면 같은 실패를 모델 수만큼 반복할 뿐이다.
 */
@Slf4j
public class OpenAiChatCaller {

    private final RestClient openAiRestClient;
    /** 앞에서부터 시도한다. 첫 번째가 기본 모델, 나머지가 한도에 걸렸을 때 쓸 모델. */
    private final List<String> models;

    public OpenAiChatCaller(RestClient openAiRestClient, String primaryModel, List<String> fallbackModels) {
        this.openAiRestClient = openAiRestClient;
        // 기본 모델이 fallback 목록에 또 들어 있으면 같은 모델을 두 번 부르게 되므로 중복을 없앤다.
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        ordered.add(primaryModel);
        fallbackModels.stream().map(String::trim).filter(m -> !m.isEmpty()).forEach(ordered::add);
        this.models = List.copyOf(ordered);
    }

    /**
     * @param what     로그에 남길 작업 이름 (예: "AI 식단 분석")
     * @param request  {@code model}을 제외한 나머지가 채워진 요청 본문. 모델은 여기서 채운다
     * @return 응답 본문(JSON 문자열). 파싱은 호출한 쪽이 한다
     */
    public String call(String what, ObjectNode request) {
        List<String> exhausted = new ArrayList<>();
        for (String model : models) {
            request.put("model", model);
            try {
                String response = openAiRestClient.post()
                        .uri("/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request.toString())
                        .retrieve()
                        .body(String.class);
                if (!exhausted.isEmpty()) {
                    log.info("{} — {} 한도 소진으로 {}(으)로 이어서 처리했습니다.", what, exhausted, model);
                }
                return response;
            } catch (HttpClientErrorException.TooManyRequests e) {
                exhausted.add(model);
                log.warn("{} — {} 사용량 한도에 걸렸습니다. 남은 대체 모델 {}개",
                        what, model, models.size() - exhausted.size());
            }
        }
        log.error("{} — 사용할 수 있는 모델의 한도가 모두 소진됐습니다. 시도한 모델={} "
                + "결제 수단 등록이나 키 교체가 필요합니다.", what, exhausted);
        throw new CustomException(ErrorCode.AI_QUOTA_EXCEEDED);
    }
}
