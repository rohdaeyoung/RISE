package com.withu.global.error;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String field;
    /** 이 상황에서만 쓰는 안내 문구. null이면 ErrorCode에 적힌 기본 문구가 나간다. */
    private final String detail;

    public CustomException(ErrorCode errorCode) {
        this(errorCode, null, null);
    }

    public CustomException(ErrorCode errorCode, String field) {
        this(errorCode, field, null);
    }

    /**
     * 같은 오류라도 상황마다 알려줄 내용이 다를 때 쓴다. 예를 들어 사진이 미션과 맞지 않을 때,
     * AI가 그 사진을 무엇으로 봤는지까지 알려주면 사용자가 무엇을 다시 찍어야 할지 알 수 있다.
     * 코드(errorCode)는 그대로 두므로 프론트의 분기 처리는 영향받지 않는다.
     */
    public CustomException(ErrorCode errorCode, String field, String detail) {
        super(detail != null ? detail : errorCode.getMessage());
        this.errorCode = errorCode;
        this.field = field;
        this.detail = detail;
    }

    /** 사용자에게 내보낼 문구. */
    public String getUserMessage() {
        return detail != null ? detail : errorCode.getMessage();
    }
}
