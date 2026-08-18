package com.withu.challenge.dto;

import java.util.List;

public class ChallengeDto {

    /** 프론트 ChallengeSummarySheet가 그리는 참가자 카드 한 칸. */
    public record ParticipantResult(
            Long userId,
            boolean me,
            String label,
            String species,
            String expression,
            String outfit,
            int rank,
            int points,
            int achievementRate,
            int bonusCoins,
            String badgeAwarded
    ) {
    }

    public record MealPhoto(
            String mealKey,
            String mealLabel,
            String photoUrl
    ) {
    }

    public record SummaryResponse(
            String groupName,
            int days,
            int achievementRate,
            int coinsEarned,
            int rank,
            int totalParticipants,
            String badgeAwarded,
            List<ParticipantResult> ranking,
            List<MealPhoto> mealPhotos
    ) {
    }
}
