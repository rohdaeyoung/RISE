package com.withu.file.service;

/**
 * JPEG의 EXIF 회전값(Orientation, TIFF 태그 0x0112)만 읽어낸다.
 *
 * <p>폰 카메라는 사진을 센서가 읽은 방향 그대로 저장하고, "화면에 띄울 땐 이만큼 돌려라"를 EXIF에만 적어둔다.
 * 브라우저는 그 값을 보고 알아서 세워주지만 {@code ImageIO.read}는 무시하기 때문에,
 * 축소 후 다시 저장하면 EXIF가 사라지면서 눕힌 사진이 그대로 굳어버린다.
 * 그래서 축소 전에 이 값을 읽어 픽셀 자체를 돌려놔야 한다.
 *
 * <p>회전 태그 하나만 필요해서 외부 EXIF 라이브러리를 쓰지 않고 직접 훑는다.
 */
final class ExifOrientation {

    /** 회전할 필요가 없는 정상 방향. 태그가 없거나 읽지 못하면 이 값으로 본다. */
    static final int NORMAL = 1;

    private static final int JPEG_MARKER = 0xFF;
    private static final int APP1 = 0xE1;
    private static final int SOS = 0xDA;
    private static final int ORIENTATION_TAG = 0x0112;

    private ExifOrientation() {
    }

    static int read(byte[] jpeg) {
        try {
            return scan(jpeg);
        } catch (RuntimeException e) {
            // 잘린 파일이나 EXIF가 없는 이미지 — 회전 정보가 없다고 보고 원본 방향을 유지한다.
            return NORMAL;
        }
    }

    private static int scan(byte[] b) {
        if (b.length < 4 || u8(b, 0) != JPEG_MARKER || u8(b, 1) != 0xD8) {
            return NORMAL; // JPEG이 아니면 EXIF도 없다 (PNG 등)
        }

        int i = 2;
        while (i + 4 <= b.length) {
            if (u8(b, i) != JPEG_MARKER) {
                return NORMAL;
            }
            int marker = u8(b, i + 1);
            if (marker == SOS) {
                return NORMAL; // 여기서부터는 압축된 화소 데이터라 더 볼 것이 없다
            }
            int segmentLength = u16(b, i + 2, true);
            if (segmentLength < 2) {
                return NORMAL;
            }
            if (marker == APP1 && isExifHeader(b, i + 4)) {
                return readFromTiff(b, i + 10, i + 2 + segmentLength);
            }
            i += 2 + segmentLength;
        }
        return NORMAL;
    }

    private static boolean isExifHeader(byte[] b, int p) {
        return p + 6 <= b.length
                && b[p] == 'E' && b[p + 1] == 'x' && b[p + 2] == 'i' && b[p + 3] == 'f'
                && b[p + 4] == 0 && b[p + 5] == 0;
    }

    /** TIFF 헤더가 시작되는 위치(tiff)부터 IFD0을 훑어 Orientation 태그를 찾는다. */
    private static int readFromTiff(byte[] b, int tiff, int end) {
        if (tiff + 8 > end || end > b.length) {
            return NORMAL;
        }
        boolean bigEndian;
        if (b[tiff] == 'M' && b[tiff + 1] == 'M') {
            bigEndian = true;
        } else if (b[tiff] == 'I' && b[tiff + 1] == 'I') {
            bigEndian = false;
        } else {
            return NORMAL;
        }

        int ifd = tiff + (int) u32(b, tiff + 4, bigEndian);
        if (ifd + 2 > end) {
            return NORMAL;
        }
        int entryCount = u16(b, ifd, bigEndian);
        for (int n = 0; n < entryCount; n++) {
            int entry = ifd + 2 + n * 12;
            if (entry + 12 > end) {
                return NORMAL;
            }
            if (u16(b, entry, bigEndian) == ORIENTATION_TAG) {
                int value = u16(b, entry + 8, bigEndian); // SHORT 값은 앞 2바이트에 들어있다
                return value >= 1 && value <= 8 ? value : NORMAL;
            }
        }
        return NORMAL;
    }

    private static int u8(byte[] b, int i) {
        return b[i] & 0xFF;
    }

    private static int u16(byte[] b, int i, boolean bigEndian) {
        int hi = u8(b, bigEndian ? i : i + 1);
        int lo = u8(b, bigEndian ? i + 1 : i);
        return (hi << 8) | lo;
    }

    private static long u32(byte[] b, int i, boolean bigEndian) {
        return ((long) u16(b, bigEndian ? i : i + 2, bigEndian) << 16)
                | u16(b, bigEndian ? i + 2 : i, bigEndian);
    }
}
