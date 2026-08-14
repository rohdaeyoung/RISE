package com.withu.ai.mock;

import com.withu.ai.LifestyleVisionAiClient;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * OpenAI 키가 없을 때 쓰는 mock. 사진 내용을 볼 수 없으므로 항상 통과시킨다.
 *
 * <p>여기서 무작위로 실패시키면 키 없이 개발하는 사람이 미션을 아예 진행할 수 없게 된다.
 * 실제 판정은 키가 있을 때 {@code OpenAiLifestyleVisionClient}가 한다.
 */
@Component
public class MockLifestyleVisionAiClient implements LifestyleVisionAiClient {

    @Override
    public LifestyleVerification verify(MultipartFile photo, String missionTitle) {
        return new LifestyleVerification(true, "mock - 사진을 판별하지 않음");
    }
}
