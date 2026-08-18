package com.withu.ai;

import org.springframework.web.multipart.MultipartFile;

/**
 * 생활습관 미션 인증 사진 판정 포트 (PRD 6. AI 분석 상세).
 *
 * <p>식단 분석과 나누어 둔 이유는 물어보는 것이 다르기 때문이다. 식단은 "이 음식이 목표에 맞는가"를
 * 보지만, 생활습관은 "이 사진이 그 행동을 했다는 증거가 되는가"를 본다. 걷기 미션에 채소 사진을 올리면
 * 음식으로는 훌륭해도 걸었다는 증거는 아니므로 미달성이어야 한다.
 */
public interface LifestyleVisionAiClient {

    /**
     * @param missionTitle 인증하려는 미션 제목 (예: "하루 30분 걷기")
     * @return 판정 결과. seen은 AI가 사진에서 무엇을 봤는지 — 오판을 추적하는 용도로만 쓰고
     *         사용자에게는 노출하지 않는다.
     */
    LifestyleVerification verify(MultipartFile photo, String missionTitle);

    record LifestyleVerification(boolean achieved, String seen) {
    }
}
