package com.withu.global.security;

import com.withu.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 토큰이 없거나 만료됐을 때 <b>401</b>과 함께 {@code AUTH_003}을 내려준다.
 *
 * <p>이게 없으면 Spring Security 기본 동작이 <b>403</b>을 내려주는데, 우리 API에서 403은
 * "로그인은 됐지만 그 그룹의 그룹원이 아니다"({@code GROUP_005})라는 전혀 다른 뜻으로 이미
 * 쓰고 있다. 두 경우가 같은 상태 코드로 내려오면 프론트가 구분할 방법이 없어, 토큰이 만료된
 * 것뿐인데 "그룹에서 나갔다"로 처리해 로컬 그룹 정보를 지워버리는 일이 생긴다.
 *
 * <p>실제로 이 구분이 없어서 다음 증상이 있었다. 토큰 유효기간(24시간)이 지나면 모든 요청이
 * 403으로 거절되고, 프론트는 그 실패를 조용히 삼킨 뒤 localStorage에 남은 <b>어제 화면</b>을
 * 계속 보여줬다. 서버에는 오늘 미션이 정상적으로 만들어져 있는데도 "다음날이 됐는데 미션이
 * 안 바뀐다"로 보이는 원인이었다.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * 응답 본문은 손으로 만든다. Spring 7은 Jackson 3을 쓰는데 이 프로젝트의 나머지 코드는
     * Jackson 2 API를 직접 부르고 있어, 여기서 ObjectMapper를 끌어오면 어느 쪽이 관여하는지가
     * 모호해진다. 필드가 셋뿐인 고정 형식이라 직접 쓰는 편이 안전하다.
     * ({@link com.withu.global.common.ApiResponse}의 error 형태와 같은 모양을 유지한다.)
     */
    private static final String BODY = """
            {"success":false,"data":null,"error":{"code":"%s","message":"%s","field":null}}"""
            .formatted(ErrorCode.INVALID_TOKEN.getCode(), ErrorCode.INVALID_TOKEN.getMessage());

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(BODY);
    }
}
