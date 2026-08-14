package com.keroleap.immerreader.Service;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.keroleap.immerreader.AristonRest;

import static org.junit.jupiter.api.Assertions.*;

class AristonAnalyzerServiceTest {

    private static final int POINT_COUNT = 50;

    private AristonAnalyzerService service;

    // Use a large image so 50 interpolated points are well spaced and
    // do not cause out-of-bounds when crosses are drawn.
    private static final int IMG_WIDTH = 800;
    private static final int IMG_HEIGHT = 800;

    // Horizontal line with ~14 px spacing between points (700 px / 49 gaps)
    private static final int START_X = 50;
    private static final int START_Y = 400;
    private static final int CONTROL_X = 400;
    private static final int CONTROL_Y = 200;
    private static final int END_X = 750;
    private static final int END_Y = 400;

    @BeforeEach
    void setUp() {
        service = new AristonAnalyzerService();
    }

    private BufferedImage createWhiteImage() {
        BufferedImage img = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_RGB);
        int WHITE = 0xFFFFFF;
        for (int y = 0; y < IMG_HEIGHT; y++) {
            for (int x = 0; x < IMG_WIDTH; x++) {
                img.setRGB(x, y, WHITE);
            }
        }
        return img;
    }

    /**
     * Blacken a generous filled square around (cx, cy) so that even after
     * nearby 10-pixel-wide crosses are drawn by the analyzer there is still
     * enough black left for detection.
     */
    private void setRegionBlack(BufferedImage img, int cx, int cy) {
        int BLACK = 0x000000; // getRGB returns -16777216
        for (int dx = -7; dx <= 7; dx++) {
            for (int dy = -7; dy <= 7; dy++) {
                int px = cx + dx;
                int py = cy + dy;
                if (px >= 0 && px < img.getWidth() && py >= 0 && py < img.getHeight()) {
                    img.setRGB(px, py, BLACK);
                }
            }
        }
    }

    private void setPointIndexBlack(BufferedImage img, int index) {
        double t = (double) index / (POINT_COUNT - 1);
        double oneMinusT = 1 - t;
        int x = (int) Math.round(oneMinusT * oneMinusT * START_X
                + 2 * oneMinusT * t * CONTROL_X
                + t * t * END_X);
        int y = (int) Math.round(oneMinusT * oneMinusT * START_Y
                + 2 * oneMinusT * t * CONTROL_Y
                + t * t * END_Y);
        setRegionBlack(img, x, y);
    }

    @Test
    void getAristonRestData_allWhite_percentageIsZero() {
        BufferedImage img = createWhiteImage();
        AristonRest result = service.getAristonRestData(img, START_X, START_Y, CONTROL_X, CONTROL_Y, END_X, END_Y);
        assertEquals(0, result.getPercentage());
    }

    @Test
    void getAristonRestData_onlyFirstPointDark_percentage0() {
        BufferedImage img = createWhiteImage();
        setPointIndexBlack(img, 0);
        AristonRest result = service.getAristonRestData(img, START_X, START_Y, CONTROL_X, CONTROL_Y, END_X, END_Y);
        assertEquals(0, result.getPercentage());
    }

    @Test
    void getAristonRestData_onlyLastPointDark_percentage100() {
        BufferedImage img = createWhiteImage();
        setPointIndexBlack(img, POINT_COUNT - 1);
        setPointIndexBlack(img, POINT_COUNT - 2);
        setPointIndexBlack(img, POINT_COUNT - 3);
        AristonRest result = service.getAristonRestData(img, START_X, START_Y, CONTROL_X, CONTROL_Y, END_X, END_Y);
        assertEquals(100, result.getPercentage());
    }

    @Test
    void getAristonRestData_allPointsDark_percentage100() {
        BufferedImage img = createWhiteImage();
        for (int i = 0; i < POINT_COUNT; i++) {
            setPointIndexBlack(img, i);
        }
        AristonRest result = service.getAristonRestData(img, START_X, START_Y, CONTROL_X, CONTROL_Y, END_X, END_Y);
        assertEquals(100, result.getPercentage());
    }

    @Test
    void getAristonRestData_firstHalfDark_percentage49() {
        BufferedImage img = createWhiteImage();
        int lastDarkIndex = (POINT_COUNT - 1) / 2; // 24
        for (int i = 0; i <= lastDarkIndex; i++) {
            setPointIndexBlack(img, i);
        }
        AristonRest result = service.getAristonRestData(img, START_X, START_Y, CONTROL_X, CONTROL_Y, END_X, END_Y);
        int expected = (int) Math.round(lastDarkIndex * 100.0 / (POINT_COUNT - 1));
        assertEquals(expected, result.getPercentage());
    }

    @Test
    void getAristonRestData_customLine_interpolatesCorrectly() {
        BufferedImage img = createWhiteImage();
        // Vertical line with ~12 px spacing (600 px / 49 gaps)
        int startX = 400;
        int startY = 50;
        int endX = 400;
        int endY = 650;

        // Darken a short run around index 25 (~midpoint)
        int controlX = startX;
        int controlY = (startY + endY) / 2;
        for (int i = 24; i <= 26; i++) {
            double t = i / (double) (POINT_COUNT - 1);
            double oneMinusT = 1 - t;
            int px = (int) Math.round(oneMinusT * oneMinusT * startX
                    + 2 * oneMinusT * t * controlX
                    + t * t * endX);
            int py = (int) Math.round(oneMinusT * oneMinusT * startY
                    + 2 * oneMinusT * t * controlY
                    + t * t * endY);
            setRegionBlack(img, px, py);
        }

        AristonRest result = service.getAristonRestData(img, startX, startY, controlX, controlY, endX, endY);
        int expectedPercentage = (int) Math.round(26 * 100.0 / (POINT_COUNT - 1));
        assertEquals(expectedPercentage, result.getPercentage());
    }
}
