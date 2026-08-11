package com.withu.file.service;

import com.withu.file.entity.StoredFile;
import com.withu.file.repository.StoredFileRepository;
import com.withu.global.error.CustomException;
import com.withu.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

/**
 * 업로드 파일을 DB에 저장하고 조회 URL을 돌려준다.
 * 컨테이너 재배포로 디스크가 초기화돼도 사진이 남도록 파일시스템을 쓰지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileStorageService {

    private static final String URL_PREFIX = "/api/files/";

    private final StoredFileRepository storedFileRepository;
    private final ImageDownscaler imageDownscaler;

    /** @return 저장된 파일을 조회할 수 있는 경로. 업로드가 없으면 null. */
    @Transactional
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        byte[] original;
        try {
            original = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType() : "image/jpeg";
        ImageDownscaler.Result resized = imageDownscaler.downscale(original, contentType);

        StoredFile stored = StoredFile.builder()
                .id(UUID.randomUUID().toString())
                .contentType(resized.contentType())
                .data(resized.data())
                .build();
        storedFileRepository.save(stored);
        return URL_PREFIX + stored.getId();
    }

    public StoredFile load(String id) {
        return storedFileRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));
    }
}
