package com.withu.feed.controller;

import com.withu.feed.dto.FeedDto.CommentRequest;
import com.withu.feed.dto.FeedDto.ReactionRequest;
import com.withu.feed.dto.FeedDto.Response;
import com.withu.feed.service.FeedService;
import com.withu.global.common.ApiResponse;
import com.withu.global.security.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 그룹 피드의 반응·댓글. 모두 "오늘" 기준이며 같은 그룹원에게만 보인다.
 *
 * <p>세 엔드포인트 모두 갱신된 피드 전체를 돌려준다. 반응 하나를 누를 때마다 목록을 다시
 * 조회하는 왕복을 없애고, 다른 그룹원이 그 사이 남긴 반응까지 같이 받아가게 하기 위해서다.
 */
@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping
    public ApiResponse<Response> getFeed() {
        return ApiResponse.success(feedService.getFeed(SecurityUtil.getCurrentUserId()));
    }

    /** 같은 이모지를 다시 보내면 취소, 다른 이모지를 보내면 교체된다. */
    @PostMapping("/reactions")
    public ApiResponse<Response> react(@Valid @RequestBody ReactionRequest request) {
        return ApiResponse.success(
                feedService.toggleReaction(SecurityUtil.getCurrentUserId(), request.targetUserId(), request.emoji()));
    }

    @PostMapping("/comments")
    public ApiResponse<Response> comment(@Valid @RequestBody CommentRequest request) {
        return ApiResponse.success(feedService.addComment(SecurityUtil.getCurrentUserId(), request.text()));
    }
}
