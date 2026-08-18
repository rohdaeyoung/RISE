package com.withu.feed.repository;

import com.withu.feed.entity.FeedReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FeedReactionRepository extends JpaRepository<FeedReaction, Long> {

    List<FeedReaction> findByGroupIdAndFeedDate(Long groupId, LocalDate feedDate);

    Optional<FeedReaction> findByGroupIdAndFeedDateAndActorUserIdAndTargetUserId(
            Long groupId, LocalDate feedDate, Long actorUserId, Long targetUserId);

    void deleteByActorUserId(Long actorUserId);

    void deleteByTargetUserId(Long targetUserId);

    void deleteByGroupId(Long groupId);
}
