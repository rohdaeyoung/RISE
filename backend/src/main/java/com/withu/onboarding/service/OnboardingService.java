package com.withu.onboarding.service;

import com.withu.global.error.CustomException;
import com.withu.global.error.ErrorCode;
import com.withu.group.entity.GroupMember;
import com.withu.group.repository.GroupMemberRepository;
import com.withu.onboarding.dto.OnboardingDto.Request;
import com.withu.onboarding.dto.OnboardingDto.Response;
import com.withu.onboarding.entity.Onboarding;
import com.withu.onboarding.repository.OnboardingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnboardingService {

    private final OnboardingRepository onboardingRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Transactional
    public Response submit(Long userId, Request request) {
        Long groupId = getCurrentGroupId(userId);
        Onboarding onboarding = onboardingRepository.findByUserIdAndGroupId(userId, groupId)
                .orElse(null);

        if (onboarding == null) {
            onboarding = Onboarding.builder()
                    .userId(userId)
                    .groupId(groupId)
                    .goal(request.goal())
                    .gender(request.gender())
                    .age(request.age())
                    .height(request.height())
                    .weight(request.weight())
                    .build();
            onboardingRepository.save(onboarding);
        } else {
            onboarding.update(request.goal(), request.gender(), request.age(), request.height(), request.weight());
        }
        return Response.from(onboarding);
    }

    public Response getMine(Long userId) {
        Long groupId = getCurrentGroupId(userId);
        Onboarding onboarding = onboardingRepository.findByUserIdAndGroupId(userId, groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.ONBOARDING_NOT_FOUND));
        return Response.from(onboarding);
    }

    private Long getCurrentGroupId(Long userId) {
        GroupMember member = groupMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
        return member.getGroup().getId();
    }
}
