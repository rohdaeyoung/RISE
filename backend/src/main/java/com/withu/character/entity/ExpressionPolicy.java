package com.withu.character.entity;

/**
 * 캐릭터 표정 결정 규칙 (프론트 AppContext.jsx expressionFromRank와 동일한 규칙).
 *
 * 달성률이 0%면 순위와 무관하게 무조건 슬픔 — 아무것도 안 했는데 등수만으로 기쁜 표정을 짓는 건
 * 어색하다(특히 그룹원이 없어 항상 "나 혼자 1등"인 경우). 뭔가 했으면 그때부터 그룹 내 순위로
 * 세분화한다: 상위권은 기쁨, 꼴찌 바로 위는 무표정, 꼴찌는 슬픔.
 * 그룹 인원이 최대 4인이라 "1~2등 기쁨 / 3등 무표정 / 4등 슬픔" 구성과 맞아떨어진다.
 */
public final class ExpressionPolicy {

    private ExpressionPolicy() {
    }

    public static Expression fromRank(int rank, int total, int rate) {
        if (rate <= 0) {
            return Expression.BAD;
        }
        if (total <= 1) {
            return rate >= 50 ? Expression.GOOD : Expression.NORMAL;
        }
        if (rank >= total) {
            return Expression.BAD;
        }
        if (total >= 3 && rank == total - 1) {
            return Expression.NORMAL;
        }
        return Expression.GOOD;
    }
}
