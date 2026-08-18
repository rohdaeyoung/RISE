package com.withu.file.controller;

import com.withu.file.entity.StoredFile;
import com.withu.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * 사진 조회. <img src="...">로 직접 불러야 해서 토큰 헤더를 붙일 수 없으므로 인증을 요구하지 않는다.
     * 파일 이름이 임의의 UUID라 주소를 모르면 접근할 수 없다.
     */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> get(@PathVariable String id) {
        StoredFile file = fileStorageService.load(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(file.getData());
    }
}
