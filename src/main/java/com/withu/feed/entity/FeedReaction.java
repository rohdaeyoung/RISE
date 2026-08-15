package com.withu.feed.entity;

import com.withu.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 그룹 피드에서 그룹원의 오늘 인증에 남긴 반응 (PRD 8. 그룹 피드).
 *
 * <p>한 사람이 같은 상대에게 하루에 하나의 반응만 남긴다 — 프론트가 이미 그렇게 동작한다
 * (다른 이모지를 누르면 이전 것이 바뀌고, 같은 것을 다시 누르면 취소). 유니크 제약으로 강제한다.
 *
 * <p>날짜를 함께 저장하는 이유는 피드가 "오늘의 인증"이기 때문이다. 어제 반응이 오늘 카드에
 * 남아 있으면 안 된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "feed_reactions", uniqueConstraints = @UniqueConstraint(
        name = "uk_reaction_group_date_actor_target",
        columnNames = {"group_id", "feed_date", "actor_user_id", "target_user_id"}))
public class FeedReaction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "feed_date", nullable = false)
    private LocalDate feedDate;

    /** 반응을 남긴 사람. */
    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    /** 반응을 받은 사람. */
    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    /** 이모지 한 글자 — 종류는 프론트가 정한다. 길이는 이모지가 4바이트 이상인 경우를 감안해 넉넉히 둔다. */
    @Column(nullable = false, length = 16)
    private String emoji;

    @Builder
    private FeedReaction(Long groupId, LocalDate feedDate, Long actorUserId, Long targetUserId, String emoji) {
        this.groupId = groupId;
        this.feedDate = feedDate;
        this.actorUserId = actorUserId;
        this.targetUserId = targetUserId;
        this.emoji = emoji;
    }

    public void changeEmoji(String emoji) {
        this.emoji = emoji;
    }
}
