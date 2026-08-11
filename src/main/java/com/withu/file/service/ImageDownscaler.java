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
            ImageIO.write(target, "jpg", out);
            return new Result(out.toByteArray(), JPEG);
        } catch (IOException e) {
            log.warn("이미지 축소 실패, 원본을 그대로 저장합니다", e);
            return new Result(original, contentType);
        }
    }
}
