package com.withu.ai.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.withu.global.error.CustomException;
import com.withu.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 사용량 한도(429)에 걸리면 다음 모델로 이어서 시도하는지 확인한다.
 *
 * <p>실제 OpenAI를 부르지 않는다. 한도 소진은 하루 50회를 다 써야 재현되는데, 그걸 기다렸다가
 * 테스트할 수는 없기 때문이다. 요청을 가로채 원하는 응답을 돌려주는 가짜 서버를 끼워 넣는다.
 */
class OpenAiChatCallerTest {

    private static final String OK_BODY = "{\"choices\":[{\"message\":{\"content\":\"{}\"}}]}";

    /** 호출된 모델을 순서대로 기록하면서, 지정한 모델에는 429를 돌려주는 가짜 서버. */
    private static class FakeOpenAi implements ClientHttpRequestFactory {
        private final List<String> quotaExhausted;
        private final List<String> called = new ArrayList<>();

        FakeOpenAi(String... quotaExhausted) {
            this.quotaExhausted = List.of(quotaExhausted);
        }

        @Override
        public ClientHttpRequest createRequest(URI uri, HttpMethod method) {
            return new ClientHttpRequest() {
                private final ByteArrayOutputStream body = new ByteArrayOutputStream();
                private final HttpHeaders headers = new HttpHeaders();

                @Override
                public OutputStream getBody() {
                    return body;
                }

                @Override
                public HttpHeaders getHeaders() {
                    return headers;
                }

                @Override
                public HttpMethod getMethod() {
                    return method;
                }

                @Override
                public URI getURI() {
                    return uri;
                }

                @Override
                public Map<String, Object> getAttributes() {
                    return new HashMap<>();
                }

                @Override
                public ClientHttpResponse execute() {
                    String sent = body.toString(StandardCharsets.UTF_8);
                    String model = sent.replaceAll("(?s).*\"model\"\\s*:\\s*\"([^\"]+)\".*", "$1");
                    called.add(model);
                    boolean blocked = quotaExhausted.contains(model);
                    MockClientHttpResponse response = new MockClientHttpResponse(
                            (blocked ? "{\"error\":{\"message\":\"Rate limit reached\"}}" : OK_BODY)
                                    .getBytes(StandardCharsets.UTF_8),
                            blocked ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.OK);
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    return response;
                }
            };
        }
    }

    private OpenAiChatCaller caller(FakeOpenAi fake, String primary, List<String> fallbacks) {
        RestClient client = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .requestFactory(fake)
                .build();
        return new OpenAiChatCaller(client, primary, fallbacks);
    }

    private ObjectNode request() {
        return new ObjectMapper().createObjectNode().put("messages", "…");
    }

    @Test
    @DisplayName("한도에 안 걸리면 기본 모델만 부른다")
    void usesPrimaryModelWhenAvailable() {
        FakeOpenAi fake = new FakeOpenAi();
        String response = caller(fake, "gpt-4o-mini", List.of("gpt-4.1-mini")).call("테스트", request());

        assertThat(response).isEqualTo(OK_BODY);
        assertThat(fake.called).containsExactly("gpt-4o-mini");
    }

    @Test
    @DisplayName("기본 모델의 한도가 차면 다음 모델로 이어서 처리한다")
    void fallsBackWhenQuotaExhausted() {
        FakeOpenAi fake = new FakeOpenAi("gpt-4o-mini");
        String response = caller(fake, "gpt-4o-mini", List.of("gpt-4.1-mini", "gpt-4.1-nano")).call("테스트", request());

        assertThat(response).isEqualTo(OK_BODY);
        // 막힌 모델을 먼저 시도한 뒤 다음 모델로 넘어가고, 성공했으면 그 뒤는 부르지 않는다.
        assertThat(fake.called).containsExactly("gpt-4o-mini", "gpt-4.1-mini");
    }

    @Test
    @DisplayName("모든 모델의 한도가 차면 사용자에게 보여줄 오류로 바꿔 던진다")
    void throwsWhenAllModelsExhausted() {
        FakeOpenAi fake = new FakeOpenAi("gpt-4o-mini", "gpt-4.1-mini");
        OpenAiChatCaller caller = caller(fake, "gpt-4o-mini", List.of("gpt-4.1-mini"));

        assertThatThrownBy(() -> caller.call("테스트", request()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_QUOTA_EXCEEDED);
        assertThat(fake.called).containsExactly("gpt-4o-mini", "gpt-4.1-mini");
    }

    @Test
    @DisplayName("기본 모델이 대체 목록에 또 있어도 두 번 부르지 않는다")
    void doesNotCallSameModelTwice() {
        FakeOpenAi fake = new FakeOpenAi("gpt-4o-mini");
        caller(fake, "gpt-4o-mini", List.of("gpt-4o-mini", "gpt-4.1-mini")).call("테스트", request());

        assertThat(fake.called).containsExactly("gpt-4o-mini", "gpt-4.1-mini");
    }
}
