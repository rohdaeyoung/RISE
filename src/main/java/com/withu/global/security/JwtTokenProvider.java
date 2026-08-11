package com.withu.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    /**
     * application.yml에 적혀 있는(=저장소에 공개된) 개발용 기본 키. 이 값으로 서명하면
     * 저장소를 본 누구나 원하는 userId의 토큰을 위조할 수 있으므로 로컬에서만 허용한다.
     */
    static final String INSECURE_DEFAULT_SECRET = "local-dev-secret-key-change-me-please-32bytes-min";

    private final SecretKey key;
    private final long validityMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-ms}") long validityMs,
            @Value("${spring.profiles.active:local}") String activeProfile) {
        requireSecureSecret(secret, activeProfile);
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.validityMs = validityMs;
    }

    /**
     * 배포 환경에서 JWT_SECRET을 깜빡하면 서버는 아무 경고 없이 공개된 기본 키로 떠버린다.
     * 조용히 뚫린 채 도는 것보다 아예 뜨지 않는 편이 낫다.
     */
    private void requireSecureSecret(String secret, String activeProfile) {
        if ("local".equals(activeProfile)) {
            return;
        }
        if (INSECURE_DEFAULT_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "JWT_SECRET 환경변수를 설정해야 합니다. 개발용 기본 키는 저장소에 공개돼 있어 "
                            + "그대로 배포하면 누구나 토큰을 위조할 수 있습니다.");
        }
        // HS384 서명에는 최소 48바이트가 필요하다. 짧으면 jjwt가 예외를 던지므로 이유를 먼저 알려준다.
        if (secret.getBytes().length < 48) {
            throw new IllegalStateException("JWT_SECRET은 48바이트(영문 기준 48자) 이상이어야 합니다.");
        }
    }

    public String createToken(Long userId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityMs);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
