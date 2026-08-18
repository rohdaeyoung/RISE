package com.withu.challenge.controller;

import com.withu.challenge.dto.ChallengeDto.SummaryResponse;
import com.withu.challenge.service.ChallengeService;
import com.withu.global.common.ApiResponse;
import com.withu.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    /** 7일 챌린지 종료 + 순위별 보상 지급. 이미 정산됐으면 저장된 결과를 반환한다. */
    @PostMapping("/end")
    public ApiResponse<SummaryResponse> end() {
        return ApiResponse.success(challengeService.endChallenge(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/summary")
    public ApiResponse<SummaryResponse> summary() {
        return ApiResponse.success(challengeService.getSummary(SecurityUtil.getCurrentUserId()));
    }

    /** 같은 그룹으로 새 7일 사이클 시작. */
    @PostMapping("/continue")
    public ApiResponse<Void> continueChallenge() {
        challengeService.continueChallenge(SecurityUtil.getCurrentUserId());
        return ApiResponse.empty();
    }
}
