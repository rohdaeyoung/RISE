package com.withu.file.entity;

import com.withu.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 업로드된 인증 사진. 배포 환경(컨테이너)에서는 재배포 시 디스크가 초기화되므로
 * 파일을 서버 디스크가 아니라 DB에 저장한다. 저장 전에 서버에서 축소하므로 용량은 수백 KB 수준.
 */
@Entity
@Table(name = "stored_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoredFile extends BaseTimeEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes;

    /**
     * 업로드된 원본 파일의 SHA-256. 같은 사진을 다시 인증에 쓰는 것을 막는 데 쓴다.
     * 축소 후가 아니라 원본 기준이라야 사용자가 올린 파일이 같은지 정확히 판단할 수 있다.
     */
    @Column(name = "checksum", length = 64)
    private String checksum;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] data;

    @Builder
    private StoredFile(String id, String contentType, byte[] data, String checksum) {
        this.id = id;
        this.contentType = contentType;
        this.data = data;
        this.sizeBytes = data.length;
        this.checksum = checksum;
    }
}
