package com.withu.file.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 폰으로 세워 찍은 사진이 그룹 피드에서 눕지 않는지 확인한다.
 * 회전은 눈으로 봐야 알 수 있는 종류의 버그라, 색으로 방향을 표시해 자동으로 검사한다.
 */
class ImageDownscalerTest {

    private final ImageDownscaler downscaler = new ImageDownscaler();

    @Test
    @DisplayName("EXIF 회전값이 없으면 원본 방향 그대로 둔다")
    void keepsOrientationWhenNoExif() throws IOException {
        byte[] jpeg = jpeg(landscape());

        BufferedImage result = read(downscaler.downscale(jpeg, "image/jpeg").data());

        assertThat(result.getWidth()).isGreaterThan(result.getHeight());
        assertThat(isRed(result, 0, 0)).isTrue();
    }

    @Test
    @DisplayName("EXIF 회전값 6(폰 세로 촬영)이면 세워서 저장한다")
    void rotatesQuarterTurnForPortraitPhotos() throws IOException {
        // 폰은 가로로 누운 화소를 저장하고 "시계로 90도 돌려 보여라"를 EXIF에만 적어둔다.
        byte[] jpeg = withExifOrientation(jpeg(landscape()), 6);

        var stored = downscaler.downscale(jpeg, "image/jpeg");
        BufferedImage result = read(stored.data());

        // 돌아갔으니 세로가 더 길어야 한다.
        assertThat(result.getHeight()).isGreaterThan(result.getWidth());
        // 왼쪽 위에 있던 빨강은 시계 90도 회전 뒤 오른쪽 위로 간다.
        assertThat(isRed(result, 0, 0)).isFalse();
        assertThat(isRed(result, result.getWidth() - 1, 0)).isTrue();
    }

    @Test
    @DisplayName("EXIF 회전값 3(뒤집힌 사진)이면 180도 돌려 저장한다")
    void rotatesHalfTurn() throws IOException {
        byte[] jpeg = withExifOrientation(jpeg(landscape()), 3);

        BufferedImage result = read(downscaler.downscale(jpeg, "image/jpeg").data());

        assertThat(result.getWidth()).isGreaterThan(result.getHeight());
        // 왼쪽 위 빨강이 오른쪽 아래로 간다.
        assertThat(isRed(result, result.getWidth() - 1, result.getHeight() - 1)).isTrue();
    }

    /** 왼쪽 위만 빨강으로 칠한 가로 이미지 — 회전 여부를 색 위치로 판별하기 위한 표식. */
    private BufferedImage landscape() {
        BufferedImage image = new BufferedImage(400, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 400, 200);
        g.setColor(Color.RED);
        g.fillRect(0, 0, 200, 100);
        g.dispose();
        return image;
    }

    /** JPEG은 손실 압축이라 색이 정확히 보존되지 않으므로, 빨강인지 파랑인지만 구분한다. */
    private boolean isRed(BufferedImage image, int x, int y) {
        Color c = new Color(image.getRGB(x, y));
        return c.getRed() > c.getBlue();
    }

    private byte[] jpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }

    private BufferedImage read(byte[] data) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(data));
    }

    /**
     * JPEG 앞부분에 Orientation 태그 하나만 담은 최소 EXIF(APP1) 세그먼트를 끼워 넣는다.
     * 폰이 만드는 파일과 같은 구조라, 이걸 읽어내면 실제 사진도 읽어낼 수 있다.
     */
    private byte[] withExifOrientation(byte[] jpeg, int orientation) {
        byte[] app1 = {
                // APP1, 길이 30 = 길이 필드 2 + Exif 식별자 6 + TIFF 헤더 8 + 항목 수 2 + 항목 12
                (byte) 0xFF, (byte) 0xE1, 0x00, 0x1E,
                'E', 'x', 'i', 'f', 0x00, 0x00,                     // EXIF 식별자
                'M', 'M', 0x00, 0x2A, 0x00, 0x00, 0x00, 0x08,       // 빅엔디안 TIFF 헤더, IFD0은 8바이트 뒤
                0x00, 0x01,                                         // 항목 1개
                0x01, 0x12,                                         // 태그 0x0112 = Orientation
                0x00, 0x03,                                         // 타입 SHORT
                0x00, 0x00, 0x00, 0x01,                             // 개수 1
                0x00, (byte) orientation, 0x00, 0x00                // 값 (SHORT는 앞 2바이트)
        };

        byte[] out = new byte[jpeg.length + app1.length];
        out[0] = jpeg[0];                                           // SOI 0xFFD8
        out[1] = jpeg[1];
        System.arraycopy(app1, 0, out, 2, app1.length);
        System.arraycopy(jpeg, 2, out, 2 + app1.length, jpeg.length - 2);
        return out;
    }
}
