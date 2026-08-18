package com.withu.group.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 혼동되기 쉬운 문자(0/O, 1/I 등)를 제외한 문자셋으로 6자리 그룹 코드를 만든다.
 * (프론트 groupApi.js의 CODE_CHARS와 동일)
 */
@Component
public class GroupCodeGenerator {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
