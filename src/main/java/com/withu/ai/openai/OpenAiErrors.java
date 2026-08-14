package com.withu.ai.openai;

import com.withu.global.error.CustomException;
import com.withu.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;

/**
 * OpenAI 호출 실패를 사용자에게 보여줄 오류로 옮긴다.
 *
 * <p>사용량 한도(429)를 따로 구분하는 이유는, 이게 서버 버그처럼 보이기 때문이다. 무료 등급은
 * 하루 요청 수가 정해져 있어서(gpt-4o-mini 기준 50) 한도를 넘기면 그 순간부터 모든 사진 인증이
 * 실패한다. 앞선 몇 번은 멀쩡히 되다가 갑자기 안 되니 "두 번째 업로드부터 고장난다"로 보였다.
 * 로그와 응답 코드가 나뉘어 있어야 결제/키 문제라는 걸 바로 알 수 있다.
 */
@Slf4j
final class OpenAiErrors {

    private OpenAiErrors() {
    }

    static RuntimeException translate(String what, Exception e) {
        if (e instanceof HttpClientErrorException.TooManyRequests tooMany) {
            log.error("OpenAI 사용량 한도에 걸렸습니다 — 결제 수단 등록이나 키 교체가 필요합니다. 작업={} 응답={}",
                    what, tooMany.getResponseBodyAsString());
            return new CustomException(ErrorCode.AI_QUOTA_EXCEEDED);
        }
        log.error("OpenAI 호출 실패. 작업={}", what, e);
        return new IllegalStateException(what + "에 실패했습니다", e);
    }
}
