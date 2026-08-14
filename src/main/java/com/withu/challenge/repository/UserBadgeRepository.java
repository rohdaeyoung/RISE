package com.withu.challenge.repository;

import com.withu.challenge.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    Optional<UserBadge> findByUserIdAndBadgeId(Long userId, String badgeId);

    List<UserBadge> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
