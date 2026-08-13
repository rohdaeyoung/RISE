package com.withu.file.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * DB에 원본을 그대로 넣으면 사진 한 장에 수 MB가 쌓이므로, 저장 전에 긴 변 기준으로 축소한다.
 * 1024px면 AI 분석과 화면 표시 모두에 충분하다.
 */
@Slf4j
@Component
public class ImageDownscaler {

    private static final int MAX_EDGE = 1024;
    private static final String JPEG = "image/jpeg";

    public record Result(byte[] data, String contentType) {
    }

    public Result downscale(byte[] original, String contentType) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));
            if (source == null) {
                // 이미지로 해석되지 않으면(예: 알 수 없는 포맷) 원본을 그대로 둔다.
                return new Result(original, contentType);
            }

            int longestEdge = Math.max(source.getWidth(), source.getHeight());
            double ratio = longestEdge > MAX_EDGE ? (double) MAX_EDGE / longestEdge : 1.0;
            int width = (int) Math.round(source.getWidth() * ratio);
            int height = (int) Math.round(source.getHeight() * ratio);

            // JPEG은 투명도를 표현하지 못해 PNG의 투명 영역이 검게 나오므로 흰 배경을 먼저 깔아준다.
            BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = target.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.drawImage(source, 0, 0, width, height, null);
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(applyOrientation(target, ExifOrientation.read(original)), "jpg", out);
            return new Result(out.toByteArray(), JPEG);
        } catch (IOException e) {
            log.warn("이미지 축소 실패, 원본을 그대로 저장합니다", e);
            return new Result(original, contentType);
        }
    }

    /**
     * EXIF 회전값대로 픽셀을 실제로 돌려놓는다.
     *
     * <p>다시 인코딩한 JPEG에는 EXIF가 남지 않으므로, 여기서 굽지 않으면 폰으로 세워서 찍은 사진이
     * 그룹 피드에서 옆으로 누워 보인다. 올린 본인은 브라우저가 EXIF를 반영해 만든 썸네일을 보기 때문에
     * 멀쩡해 보이고, 저장된 사진을 받아 보는 다른 그룹원에게만 돌아가 보였다.
     */
    private BufferedImage applyOrientation(BufferedImage image, int orientation) {
        if (orientation == ExifOrientation.NORMAL) {
            return image;
        }

        int w = image.getWidth();
        int h = image.getHeight();
        // 90도 회전 계열(5~8)은 가로세로가 뒤바뀐다.
        boolean swapped = orientation >= 5;
        BufferedImage rotated = new BufferedImage(swapped ? h : w, swapped ? w : h, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = rotated.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        switch (orientation) {
            case 2 -> { g.translate(w, 0); g.scale(-1, 1); }                        // 좌우 반전
            case 3 -> { g.translate(w, h); g.rotate(Math.PI); }                     // 180도
            case 4 -> { g.translate(0, h); g.scale(1, -1); }                        // 상하 반전
            case 5 -> { g.rotate(Math.PI / 2); g.scale(1, -1); }                    // 전치
            case 6 -> { g.translate(h, 0); g.rotate(Math.PI / 2); }                 // 시계 90도 (폰 세로 촬영)
            case 7 -> { g.translate(h, w); g.rotate(Math.PI / 2); g.scale(-1, 1); } // 역전치
            case 8 -> { g.translate(0, w); g.rotate(-Math.PI / 2); }                // 반시계 90도
            default -> { }
        }
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return rotated;
    }
}
