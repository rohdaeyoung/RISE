package com.withu.onboarding.repository;

import com.withu.onboarding.entity.Onboarding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OnboardingRepository extends JpaRepository<Onboarding, Long> {

    Optional<Onboarding> findByUserIdAndGroupId(Long userId, Long groupId);
}
