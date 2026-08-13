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

    record MealAnalysisResult(boolean achieved, InternalFit internalFit) {
    }

    enum InternalFit {
        GOOD, NORMAL, BAD
    }
}
