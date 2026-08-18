package com.withu.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "요청 값이 올바르지 않습니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_002", "인증이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_003", "권한이 없습니다"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_004", "요청한 경로를 찾을 수 없습니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_005", "이 주소에서 지원하지 않는 요청 방식입니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 오류가 발생했습니다"),

    // auth
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH_001", "이미 가입된 이메일이에요"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_002", "이메일 또는 비밀번호가 올바르지 않아요"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "유효하지 않은 토큰입니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_004", "사용자를 찾을 수 없습니다"),

    // character
    CHARACTER_ALREADY_EXISTS(HttpStatus.CONFLICT, "CHAR_001", "이미 캐릭터가 존재합니다"),
    CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAR_002", "캐릭터가 없습니다"),
    INVALID_SPECIES(HttpStatus.BAD_REQUEST, "CHAR_003", "선택할 수 없는 캐릭터예요"),

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
    MISSION_PHOTO_MISMATCH(HttpStatus.BAD_REQUEST, "MISSION_004", "사진이 미션과 맞지 않아요. 미션을 수행한 사진으로 다시 올려주세요"),
    MISSION_VERIFY_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "MISSION_005", "인증 확인에 실패했어요. 잠시 후 다시 시도해주세요"),
    PHOTO_ALREADY_USED(HttpStatus.BAD_REQUEST, "FILE_004", "이미 인증에 사용한 사진이에요. 새로 찍은 사진으로 올려주세요"),

    // meal
    MEAL_ALREADY_LOGGED(HttpStatus.CONFLICT, "MEAL_001", "이미 인증한 식단입니다"),
    MEAL_ANALYSIS_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "MEAL_002",
            "지금은 사진을 분석할 수 없어요. 잠시 후 다시 시도해주세요"),

    // ai
    AI_QUOTA_EXCEEDED(HttpStatus.SERVICE_UNAVAILABLE, "AI_001",
            "오늘 AI 사용량을 다 썼어요. 잠시 후 다시 시도해주세요"),

    // challenge
    CHALLENGE_NOT_FINISHED(HttpStatus.CONFLICT, "CHALLENGE_001", "아직 챌린지가 끝나지 않았어요"),

    // file
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE_001", "파일을 찾을 수 없습니다"),
    FILE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "FILE_002", "사진 용량이 너무 커요. 10MB 이하로 올려주세요"),
    NOT_AN_IMAGE(HttpStatus.BAD_REQUEST, "FILE_003", "사진 파일만 올릴 수 있어요"),

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
