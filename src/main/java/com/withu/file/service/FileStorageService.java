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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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
                .checksum(checksumOf(original))
                .build();
        storedFileRepository.save(stored);
        return URL_PREFIX + stored.getId();
    }

    /**
     * 예전에 인증에 쓰인 적이 있는 사진인지 확인한다.
     *
     * <p>인터넷에서 받은 사진이나 캡처 이미지를 여러 번 우려먹는 것을 막는다. 완벽한 방어는 아니다 —
     * 다른 사진을 새로 구해오면 그만이다. 다만 같은 파일을 반복해서 쓰는 가장 쉬운 편법은 막힌다.
     */
    public boolean isAlreadyUsed(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        try {
            return storedFileRepository.existsByChecksum(checksumOf(file.getBytes()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String checksumOf(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 모든 JVM이 반드시 제공한다. 여기 오면 실행 환경 자체가 잘못된 것이다.
            throw new IllegalStateException(e);
        }
    }

    public StoredFile load(String id) {
        return storedFileRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));
    }
}
