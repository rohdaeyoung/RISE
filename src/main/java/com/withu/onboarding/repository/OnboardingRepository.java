package com.withu.onboarding.repository;

import com.withu.onboarding.entity.Onboarding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OnboardingRepository extends JpaRepository<Onboarding, Long> {

    Optional<Onboarding> findByUserIdAndGroupId(Long userId, Long groupId);

    /** 그룹을 떠날 때 그 그룹에서 입력한 목표·신체정보를 지운다 (온보딩은 그룹 사이클마다 다시 받는다). */
    void deleteByUserIdAndGroupId(Long userId, Long groupId);

    void deleteByUserId(Long userId);
}
