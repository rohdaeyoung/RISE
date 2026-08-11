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

    /** 닉네임은 랭킹·그룹 피드에서 사람을 구분하는 유일한 표시 이름이다 (프론트 MAX_NICKNAME_LENGTH=10과 일치). */
    public record NicknameRequest(
            @NotBlank @Size(max = 10, message = "닉네임은 10자 이내로 입력해주세요") String nickname
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
