package com.withu.global.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * 로컬 디스크에 업로드 파일을 저장하는 임시 구현. 실제 배포 시 S3 등 오브젝트 스토리지로 교체 예정.
 */
@Slf4j
@Service
public class FileStorageService {

    private final Path baseDir;

    public FileStorageService(@Value("${file.upload-dir:./uploads}") String uploadDir) {
        this.baseDir = Path.of(uploadDir);
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String ext = extensionOf(file.getOriginalFilename());
        String filename = UUID.randomUUID() + ext;
        try {
            Files.copy(file.getInputStream(), baseDir.resolve(filename));
        } catch (IOException e) {
            log.error("파일 저장 실패", e);
            throw new UncheckedIOException(e);
        }
        return "/uploads/" + filename;
    }

    private String extensionOf(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}
