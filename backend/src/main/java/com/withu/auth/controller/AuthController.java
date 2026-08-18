package com.withu.auth.controller;

import com.withu.auth.dto.AuthDto.*;
import com.withu.auth.service.AccountDeletionService;
import com.withu.auth.service.AuthService;
import com.withu.global.common.ApiResponse;
import com.withu.global.security.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AccountDeletionService accountDeletionService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        return ApiResponse.success(authService.signUp(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me() {
        return ApiResponse.success(authService.getMe(SecurityUtil.getCurrentUserId()));
    }

    /** 계정 탈퇴. 이 사용자의 데이터를 DB에서 전부 지운다 — 되돌릴 수 없다. */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount() {
        accountDeletionService.delete(SecurityUtil.getCurrentUserId());
    }

    @PatchMapping("/me/nickname")
    public ApiResponse<MeResponse> changeNickname(@Valid @RequestBody NicknameRequest request) {
        return ApiResponse.success(authService.changeNickname(SecurityUtil.getCurrentUserId(), request));
    }
}
