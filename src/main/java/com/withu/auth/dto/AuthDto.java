package com.withu.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDto {

    public record SignUpRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, message = "비밀번호는 8자 이상이어야 해요") String password
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record AuthResponse(
            Long userId,
            String email,
            String accessToken
    ) {
    }

    public record MeResponse(
            Long userId,
            String email,
            String nickname,
            int coins
    ) {
    }
}
