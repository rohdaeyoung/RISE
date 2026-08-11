package com.withu.global.common;

public record ApiResponse<T>(boolean success, T data, ApiError error) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> empty() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, null, new ApiError(code, message, null));
    }

    public static ApiResponse<Void> error(String code, String message, String field) {
        return new ApiResponse<>(false, null, new ApiError(code, message, field));
    }

    public record ApiError(String code, String message, String field) {
    }
}
