package com.withu.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "요청 값이 올바르지 않습니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_002", "인증이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_003", "권한이 없습니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 오류가 발생했습니다"),

    // auth
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH_001", "이미 가입된 이메일이에요"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_002", "이메일 또는 비밀번호가 올바르지 않아요"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "유효하지 않은 토큰입니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_004", "사용자를 찾을 수 없습니다"),

    // character
    CHARACTER_ALREADY_EXISTS(HttpStatus.CONFLICT, "CHAR_001", "이미 캐릭터가 존재합니다"),
    CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAR_002", "캐릭터가 없습니다"),

    // group
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "GROUP_001", "그룹을 찾을 수 없습니다"),
    GROUP_FULL(HttpStatus.CONFLICT, "GROUP_002", "그룹 인원이 가득 찼습니다"),
    GROUP_ALREADY_JOINED(HttpStatus.CONFLICT, "GROUP_003", "이미 그룹에 참여 중입니다"),
    INVALID_GROUP_CODE(HttpStatus.BAD_REQUEST, "GROUP_004", "그룹 코드는 6자리예요"),
    NOT_GROUP_MEMBER(HttpStatus.FORBIDDEN, "GROUP_005", "그룹원만 접근할 수 있습니다"),

    // onboarding
    ONBOARDING_NOT_FOUND(HttpStatus.NOT_FOUND, "ONB_001", "온보딩 정보가 없습니다"),

    // mission
    MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "MISSION_001", "미션을 찾을 수 없습니다"),
    MISSION_ALREADY_DONE(HttpStatus.CONFLICT, "MISSION_002", "이미 완료한 미션입니다"),
    MISSION_LOCKED(HttpStatus.CONFLICT, "MISSION_003", "아직 도착하지 않은 미션입니다"),

    // meal
    MEAL_ALREADY_LOGGED(HttpStatus.CONFLICT, "MEAL_001", "이미 인증한 식단입니다"),

    // shop
    OUTFIT_NOT_FOUND(HttpStatus.NOT_FOUND, "SHOP_001", "존재하지 않는 의상입니다"),
    OUTFIT_ALREADY_OWNED(HttpStatus.CONFLICT, "SHOP_002", "이미 보유한 의상입니다"),
    OUTFIT_NOT_OWNED(HttpStatus.CONFLICT, "SHOP_003", "보유하지 않은 의상입니다"),
    NOT_ENOUGH_COINS(HttpStatus.CONFLICT, "SHOP_004", "코인이 부족합니다");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
