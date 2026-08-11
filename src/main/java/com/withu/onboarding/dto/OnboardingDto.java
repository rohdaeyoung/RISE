package com.withu.onboarding.dto;

import com.withu.onboarding.entity.Gender;
import com.withu.onboarding.entity.Goal;
import com.withu.onboarding.entity.Onboarding;
import jakarta.validation.constraints.NotNull;

public class OnboardingDto {

    public record Request(
            @NotNull Goal goal,
            @NotNull Gender gender,
            @NotNull Integer age,
            @NotNull Integer height,
            @NotNull Integer weight
    ) {
    }

    public record Response(
            Long id,
            Goal goal,
            Gender gender,
            int age,
            int height,
            int weight
    ) {
        public static Response from(Onboarding onboarding) {
            return new Response(
                    onboarding.getId(),
                    onboarding.getGoal(),
                    onboarding.getGender(),
                    onboarding.getAge(),
                    onboarding.getHeight(),
                    onboarding.getWeight()
            );
        }
    }
}
