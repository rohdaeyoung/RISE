package com.withu.onboarding.controller;

import com.withu.global.common.ApiResponse;
import com.withu.global.security.SecurityUtil;
import com.withu.onboarding.dto.OnboardingDto.Request;
import com.withu.onboarding.dto.OnboardingDto.Response;
import com.withu.onboarding.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @PostMapping
    public ApiResponse<Response> submit(@Valid @RequestBody Request request) {
        return ApiResponse.success(onboardingService.submit(SecurityUtil.getCurrentUserId(), request));
    }

    @GetMapping("/me")
    public ApiResponse<Response> getMine() {
        return ApiResponse.success(onboardingService.getMine(SecurityUtil.getCurrentUserId()));
    }
}
