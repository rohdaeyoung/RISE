package com.withu.feed.repository;

import com.withu.feed.entity.FeedComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FeedCommentRepository extends JpaRepository<FeedComment, Long> {

    List<FeedComment> findByGroupIdAndFeedDateOrderByIdAsc(Long groupId, LocalDate feedDate);

    void deleteByAuthorUserId(Long authorUserId);

    void deleteByGroupId(Long groupId);
}
