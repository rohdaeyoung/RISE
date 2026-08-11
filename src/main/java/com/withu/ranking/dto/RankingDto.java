package com.withu.ranking.dto;

import java.util.List;

public class RankingDto {

    public record GroupRankingItem(
            int rank,
            Long userId,
            boolean me,
            String nickname,
            String species,
            String expression,
            int achievementRate
    ) {
    }

    public record GlobalRankingItem(
            int rank,
            Long userId,
            String nickname,
            int coins
    ) {
    }

    public record GroupRankingResponse(List<GroupRankingItem> ranking) {
    }

    public record GlobalRankingResponse(List<GlobalRankingItem> ranking) {
    }
}
