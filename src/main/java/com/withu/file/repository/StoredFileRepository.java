package com.withu.file.repository;

import com.withu.file.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, String> {

    /** 이미 인증에 쓰인 사진인지 확인한다 (같은 파일 재사용 차단). */
    boolean existsByChecksum(String checksum);
}
