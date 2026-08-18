package com.withu.mission.controller;

import com.withu.global.common.ApiResponse;
import com.withu.global.security.SecurityUtil;
import com.withu.mission.dto.MissionDto.Response;
import com.withu.mission.dto.MissionDto.TodaySummary;
import com.withu.mission.service.MissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    @PostMapping("/today")
    public ApiResponse<TodaySummary> generateToday() {
        return ApiResponse.success(missionService.generateToday(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/today")
    public ApiResponse<TodaySummary> getToday() {
        return ApiResponse.success(missionService.getToday(SecurityUtil.getCurrentUserId()));
    }

    /** 생활습관 미션 인증 — 사진이 미션과 맞는지 AI가 판정한 뒤 완료 처리한다. */
    @PostMapping(value = "/{missionId}/verify", consumes = "multipart/form-data")
    public ApiResponse<Response> verify(@PathVariable Long missionId, @RequestPart("photo") MultipartFile photo) {
        return ApiResponse.success(
                missionService.verifyLifestyleMission(SecurityUtil.getCurrentUserId(), missionId, photo));
    }
}
