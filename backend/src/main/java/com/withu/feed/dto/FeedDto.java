package com.withu.feed.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public class FeedDto {

    /**
     * 오늘 그룹 피드의 반응·댓글.
     *
     * @param reactions 그룹원별 반응. 키는 userId 문자열
     * @param comments  오래된 순
     */
    public record Response(
            Map<String, MemberReactions> reactions,
            List<Comment> comments
    ) {
    }

    /**
     * @param counts   이모지별 개수
     * @param myEmoji  내가 이 사람에게 남긴 반응. 없으면 null
     */
    public record MemberReactions(
            Map<String, Long> counts,
            String myEmoji
    ) {
    }

    public record Comment(
            Long id,
            Long authorUserId,
            String authorNickname,
            boolean me,
            String text,
            String createdAt
    ) {
    }

    public record ReactionRequest(
            @NotNull(message = "반응을 남길 그룹원을 선택해주세요") Long targetUserId,
            @NotBlank(message = "이모지를 선택해주세요") @Size(max = 16) String emoji
    ) {
    }

    public record CommentRequest(
            @NotBlank(message = "댓글을 입력해주세요") @Size(max = 200, message = "댓글은 200자까지 쓸 수 있어요") String text
    ) {
    }
}
