package com.withu.ranking.controller;

import com.withu.global.common.ApiResponse;
import com.withu.global.security.SecurityUtil;
import com.withu.ranking.dto.RankingDto.GlobalRankingResponse;
import com.withu.ranking.dto.RankingDto.GroupRankingResponse;
import com.withu.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping("/group")
    public ApiResponse<GroupRankingResponse> getGroupRanking() {
        return ApiResponse.success(rankingService.getGroupRanking(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/global")
    public ApiResponse<GlobalRankingResponse> getGlobalRanking() {
        return ApiResponse.success(rankingService.getGlobalRanking());
    }
}
