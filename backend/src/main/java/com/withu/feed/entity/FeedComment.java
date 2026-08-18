package com.withu.feed.entity;

import com.withu.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 그룹 피드에 남긴 댓글 (PRD 8. 그룹 피드).
 *
 * <p>반응과 마찬가지로 "오늘의 피드"에 달리므로 날짜를 함께 저장한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "feed_comments")
public class FeedComment extends BaseTimeEntity {

    /** 프론트 AppContext.jsx의 MAX_COMMENT_LENGTH와 같은 값이어야 한다. */
    public static final int MAX_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "feed_date", nullable = false)
    private LocalDate feedDate;

    @Column(name = "author_user_id", nullable = false)
    private Long authorUserId;

    @Column(nullable = false, length = MAX_LENGTH)
    private String text;

    @Builder
    private FeedComment(Long groupId, LocalDate feedDate, Long authorUserId, String text) {
        this.groupId = groupId;
        this.feedDate = feedDate;
        this.authorUserId = authorUserId;
        this.text = text;
    }
}
