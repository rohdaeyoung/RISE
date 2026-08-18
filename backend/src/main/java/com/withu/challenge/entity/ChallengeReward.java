package com.withu.challenge.entity;

/**
 * 순위별 보상 규칙 (PRD 10. 랭킹 시스템 — 보상).
 * 1위: 뱃지 + 코인 + 점수 / 2·3위: 코인 + 점수 / 4위: 점수
 */
public final class ChallengeReward {

    private ChallengeReward() {
    }

    public static final String FIRST_PLACE_BADGE = "champion";

    private static final int[] BONUS_COINS_BY_RANK = {100, 60, 30, 0};
    private static final int[] BONUS_POINTS_BY_RANK = {50, 30, 20, 10};

    public static int bonusCoinsFor(int rank) {
        return valueFor(BONUS_COINS_BY_RANK, rank);
    }

    public static int bonusPointsFor(int rank) {
        return valueFor(BONUS_POINTS_BY_RANK, rank);
    }

    public static String badgeFor(int rank) {
        return rank == 1 ? FIRST_PLACE_BADGE : null;
    }

    private static int valueFor(int[] table, int rank) {
        if (rank < 1) {
            return 0;
        }
        return rank <= table.length ? table[rank - 1] : table[table.length - 1];
    }
}
