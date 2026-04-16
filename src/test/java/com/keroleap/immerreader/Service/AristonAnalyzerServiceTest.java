package com.keroleap.immerreader.Service;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.keroleap.immerreader.AristonRest;

import static org.junit.jupiter.api.Assertions.*;

class AristonAnalyzerServiceTest {

    private AristonAnalyzerService service;

    // The sampling coordinates in AristonAnalyzerService reach up to x=220, y=180.
    // getLightValueAndDrawRedCross writes up to x+5=225, y+5=185, so the image must
    // be at least 226 wide and 186 tall.
    private static final int IMG_WIDTH = 300;
    private static final int IMG_HEIGHT = 250;

    // x and y coordinate arrays mirrored from AristonAnalyzerService
    private static final int[] PERCENT_X = {
        160, 163, 166, 169, 172, 175, 178, 181, 184, 187,
        190, 193, 196, 199, 202, 205, 208, 211, 214, 217, 220
    };
    private static final int[] PERCENT_Y = {
        160, 161, 162, 163, 164, 165, 166, 167, 168, 169,
        170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180
    };

    @BeforeEach
    void setUp() {
        service = new AristonAnalyzerService();
    }

    /**
     * Returns a BufferedImage filled with white pixels.
     * White pixels (getRGB = 0xFFFFFFFF = -1) are above the LIGHT_THRESHOLD of
     * -7000000, so getLightValueAndDrawRedCross returns false (no segment detected).
     */
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
     * Makes a small region around (cx, cy) black so that
     * getLightValueAndDrawRedCross detects it (average RGB < -7000000).
     */
    private void setRegionBlack(BufferedImage img, int cx, int cy) {
        int BLACK = 0x000000; // getRGB returns 0xFF000000 = -16777216
        for (int dx = -5; dx <= 5; dx++) {
            int px = cx + dx;
            if (px >= 0 && px < img.getWidth()) {
                img.setRGB(px, cy, BLACK);
            }
        }
        for (int dy = -5; dy <= 5; dy++) {
            int py = cy + dy;
            if (py >= 0 && py < img.getHeight()) {
                img.setRGB(cx, py, BLACK);
            }
        }
    }

    @Test
    void getAristonRestData_allWhite_percentageIsZero() {
        // No dark pixels → no point detected → percentage stays at 0
        BufferedImage img = createWhiteImage();
        AristonRest result = service.getAristonRestData(img);
        assertEquals(0, result.getPercentage());
    }

    @Test
    void getAristonRestData_onlyFirstPointDark_percentage0() {
        // Only index 0 dark → percentage = 0 * 5 = 0
        BufferedImage img = createWhiteImage();
        setRegionBlack(img, PERCENT_X[0], PERCENT_Y[0]);
        AristonRest result = service.getAristonRestData(img);
        assertEquals(0, result.getPercentage());
    }

    @Test
    void getAristonRestData_firstTenPointsDark_percentage45() {
        // Points 0–9 dark; points 10–20 white → last detected is index 9 → 9*5=45
        BufferedImage img = createWhiteImage();
        for (int i = 0; i <= 9; i++) {
            setRegionBlack(img, PERCENT_X[i], PERCENT_Y[i]);
        }
        AristonRest result = service.getAristonRestData(img);
        assertEquals(45, result.getPercentage());
    }

    @Test
    void getAristonRestData_onlyLastPointDark_percentage100() {
        // Only the last index (20) dark → 20*5=100
        BufferedImage img = createWhiteImage();
        setRegionBlack(img, PERCENT_X[20], PERCENT_Y[20]);
        AristonRest result = service.getAristonRestData(img);
        assertEquals(100, result.getPercentage());
    }

    @Test
    void getAristonRestData_allPointsDark_percentage100() {
        // All points dark → last detected index is 20 → 100%
        BufferedImage img = createWhiteImage();
        for (int i = 0; i < PERCENT_X.length; i++) {
            setRegionBlack(img, PERCENT_X[i], PERCENT_Y[i]);
        }
        AristonRest result = service.getAristonRestData(img);
        assertEquals(100, result.getPercentage());
    }

    @Test
    void getAristonRestData_halfwayPointDark_percentage50() {
        // Only index 10 dark (all others white) → 10*5=50
        BufferedImage img = createWhiteImage();
        setRegionBlack(img, PERCENT_X[10], PERCENT_Y[10]);
        AristonRest result = service.getAristonRestData(img);
        assertEquals(50, result.getPercentage());
    }
}
