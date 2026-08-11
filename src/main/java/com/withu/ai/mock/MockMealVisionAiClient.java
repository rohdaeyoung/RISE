package com.withu.ai.mock;

import com.withu.ai.MealVisionAiClient;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 랜덤 달성/미달성 판정 mock — 프론트 mealApi.js와 동일한 확률(good 60% / normal 25% / bad 15%).
 * 실제 OpenAI 키가 오면 OpenAiMealVisionClient(GPT-4o Vision)로 교체한다.
 */
@Component
public class MockMealVisionAiClient implements MealVisionAiClient {

    @Override
    public MealAnalysisResult analyze(MultipartFile photo, String foodName, String portion, String goal) {
        double r = ThreadLocalRandom.current().nextDouble();
        InternalFit fit = r < 0.6 ? InternalFit.GOOD : r < 0.85 ? InternalFit.NORMAL : InternalFit.BAD;
        boolean achieved = fit != InternalFit.BAD;
        return new MealAnalysisResult(achieved, fit);
    }
}
