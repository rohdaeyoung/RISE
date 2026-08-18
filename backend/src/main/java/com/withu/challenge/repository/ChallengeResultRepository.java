package com.withu.challenge.repository;

import com.withu.challenge.entity.ChallengeResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChallengeResultRepository extends JpaRepository<ChallengeResult, Long> {

    boolean existsByGroupIdAndCycleStartedAt(Long groupId, LocalDateTime cycleStartedAt);

    List<ChallengeResult> findByGroupIdAndCycleStartedAt(Long groupId, LocalDateTime cycleStartedAt);

    Optional<ChallengeResult> findByUserIdAndGroupIdAndCycleStartedAt(Long userId, Long groupId, LocalDateTime cycleStartedAt);

    List<ChallengeResult> findByUserIdOrderByIdDesc(Long userId);

    void deleteByUserId(Long userId);
}
