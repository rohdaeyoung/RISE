package com.withu.ai;

import org.springframework.web.multipart.MultipartFile;

/**
 * 식단 사진 AI 분석 포트. 실제 연동 시 OpenAiMealVisionClient(GPT-4o Vision) 구현체로 교체한다
 * (PRD 6. AI 분석 상세). internalFit은 UI에 노출하지 않고 다음 미션 생성 기준으로만 사용한다.
 */
public interface MealVisionAiClient {

    /**
     * @param missionTitle 이 사진으로 인증하려는 오늘의 식단 미션 제목. 남은 미션이 없으면 null.
     *                     achieved는 이 미션을 기준으로 판단해야 한다.
     */
    MealAnalysisResult analyze(MultipartFile photo, String foodName, String portion, String goal, String missionTitle);

    /**
     * @param food AI가 사진에서 무엇을 봤는지 (예: "단백질 음료"). 판정 결과와 함께 사용자에게 보여준다 —
     *             "달성/미달성"만 돌려주면 AI가 사진을 실제로 읽었는지 알 수 없어서, 초코우유를 올렸는데
     *             통과하면 사진을 본 것인지 아무거나 통과시킨 것인지 구분이 안 된다.
     */
    record MealAnalysisResult(boolean achieved, InternalFit internalFit, String food) {
    }

    enum InternalFit {
        GOOD, NORMAL, BAD
    }
}
