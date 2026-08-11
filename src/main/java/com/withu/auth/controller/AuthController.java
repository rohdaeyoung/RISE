package com.withu.auth.controller;

import com.withu.auth.dto.AuthDto.*;
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

    @PatchMapping("/me/nickname")
    public ApiResponse<MeResponse> changeNickname(@Valid @RequestBody NicknameRequest request) {
        return ApiResponse.success(authService.changeNickname(SecurityUtil.getCurrentUserId(), request));
    }
}
