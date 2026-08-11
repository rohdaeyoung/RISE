package com.withu.ai;

import org.springframework.web.multipart.MultipartFile;

/**
 * 식단 사진 AI 분석 포트. 실제 연동 시 OpenAiMealVisionClient(GPT-4o Vision) 구현체로 교체한다
 * (PRD 6. AI 분석 상세). internalFit은 UI에 노출하지 않고 다음 미션 생성 기준으로만 사용한다.
 */
public interface MealVisionAiClient {

    MealAnalysisResult analyze(MultipartFile photo, String foodName, String portion, String goal);

    record MealAnalysisResult(boolean achieved, InternalFit internalFit) {
    }

    enum InternalFit {
        GOOD, NORMAL, BAD
    }
}
