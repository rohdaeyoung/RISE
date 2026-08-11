package com.withu.mission.controller;

import com.withu.global.common.ApiResponse;
import com.withu.global.security.SecurityUtil;
import com.withu.mission.dto.MissionDto.Response;
import com.withu.mission.dto.MissionDto.TodaySummary;
import com.withu.mission.service.MissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/{missionId}/verify")
    public ApiResponse<Response> verify(@PathVariable Long missionId) {
        return ApiResponse.success(missionService.verifyLifestyleMission(SecurityUtil.getCurrentUserId(), missionId));
    }
}
