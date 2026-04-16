package com.keroleap.immerreader.Service;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.keroleap.immerreader.ImmerRest;

import static org.junit.jupiter.api.Assertions.*;

class ImmerAnalyzerServiceTest {

    private ImmerAnalyzerService service;

    // Image large enough to accommodate all pixel reads/writes used by getImmerRestData
    // with offsetX=0, offsetY=0. Furthest writes reach x+5=500, y+5=275.
    private static final int IMG_WIDTH = 600;
    private static final int IMG_HEIGHT = 400;

    @BeforeEach
    void setUp() {
        service = new ImmerAnalyzerService();
    }

    // -------------------------------------------------------------------------
    // getNumber – segment decoding (7-segment display logic)
    // -------------------------------------------------------------------------

    @Test
    void getNumber_digit0() {
        // 0: d1,d2,d3,d4,d5,d6 on; d7 off
        assertEquals(0, service.getNumber(true, true, true, true, true, true, false));
    }

    @Test
    void getNumber_digit1() {
        // 1: d5,d6 on; rest off
        assertEquals(1, service.getNumber(false, false, false, false, true, true, false));
    }

    @Test
    void getNumber_digit2() {
        // 2: d1,d3,d4,d6,d7 on; d2,d5 off
        assertEquals(2, service.getNumber(true, false, true, true, false, true, true));
    }

    @Test
    void getNumber_digit3() {
        // 3: d1,d4,d5,d6,d7 on; d2,d3 off
        assertEquals(3, service.getNumber(true, false, false, true, true, true, true));
    }

    @Test
    void getNumber_digit4() {
        // 4: d2,d5,d6,d7 on; d1,d3,d4 off
        assertEquals(4, service.getNumber(false, true, false, false, true, true, true));
    }

    @Test
    void getNumber_digit5() {
        // 5: d1,d2,d4,d5,d7 on; d3,d6 off
        assertEquals(5, service.getNumber(true, true, false, true, true, false, true));
    }

    @Test
    void getNumber_digit6() {
        // 6: d1,d2,d3,d4,d5,d7 on; d6 off
        assertEquals(6, service.getNumber(true, true, true, true, true, false, true));
    }

    @Test
    void getNumber_digit7() {
        // 7: d1,d5,d6 on; rest off
        assertEquals(7, service.getNumber(true, false, false, false, true, true, false));
    }

    @Test
    void getNumber_digit8() {
        // 8: all segments on
        assertEquals(8, service.getNumber(true, true, true, true, true, true, true));
    }

    @Test
    void getNumber_digit9() {
        // 9: d1,d2,d4,d5,d6,d7 on; d3 off
        assertEquals(9, service.getNumber(true, true, false, true, true, true, true));
    }

    @Test
    void getNumber_unknownPatternReturns1000() {
        // All off is not a recognised digit
        assertEquals(1000, service.getNumber(false, false, false, false, false, false, false));
    }

    @Test
    void getNumber_anotherUnknownPatternReturns1000() {
        // Only d1 on is not a recognised digit
        assertEquals(1000, service.getNumber(true, false, false, false, false, false, false));
    }

    // -------------------------------------------------------------------------
    // getImmerRestData – full image-based analysis
    // -------------------------------------------------------------------------

    /**
     * Creates a BufferedImage of the required size filled with black pixels.
     * Black pixels (getRGB returns 0xFF000000 = -16777216) are below the
     * LIGHT_THRESHOLD of -2500000, so getLightValueAnnDrawRedCross returns false.
     */
    private BufferedImage createBlackImage() {
        BufferedImage img = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_RGB);
        // TYPE_INT_RGB is zeroed on creation; getRGB packs 0xFF alpha → -16777216 (black)
        return img;
    }

    /**
     * Sets a region of pixels to white around (cx, cy) so that the cross-scan
     * performed by getLightValueAnnDrawRedCross yields a value above the threshold
     * (i.e. detected = true).
     */
    private void setRegionWhite(BufferedImage img, int cx, int cy) {
        int WHITE = 0xFFFFFF; // stored as RGB; getRGB returns 0xFFFFFFFF = -1
        for (int dx = -5; dx <= 5; dx++) {
            int px = cx + dx;
            if (px >= 0 && px < img.getWidth()) {
                img.setRGB(px, cy, WHITE);
            }
        }
        for (int dy = -5; dy <= 5; dy++) {
            int py = cy + dy;
            if (py >= 0 && py < img.getHeight()) {
                img.setRGB(cx, py, WHITE);
            }
        }
    }

    @Test
    void getImmerRestData_noHeating_allZero() {
        // All pixels black → heating=false → temperature forced to 0, throttle=0
        BufferedImage img = createBlackImage();
        ImmerRest result = service.getImmerRestData(img, 0, 0);

        assertFalse(result.isHeating());
        assertFalse(result.isBoilerOn());
        assertEquals(0, result.getThrottle());
        assertEquals(0, result.getTemperaute());
    }

    @Test
    void getImmerRestData_heatingOn_boilerOn_throttleOne() {
        BufferedImage img = createBlackImage();

        // heating indicator at (495, 215)
        setRegionWhite(img, 495, 215);
        // boilerOn indicator at (490, 120)
        setRegionWhite(img, 490, 120);
        // throttle level 1 at (305, 150); levels 2-4 remain black
        setRegionWhite(img, 305, 150);

        // Leave temperature digits all black → digits decode to 1000,
        // number > 500 → falls back to previousTempValue (0)
        ImmerRest result = service.getImmerRestData(img, 0, 0);

        assertTrue(result.isHeating());
        assertTrue(result.isBoilerOn());
        assertEquals(1, result.getThrottle());
        // temperature 0 falls outside (20,56) range → falls back to previousTempValue=0,
        // and heating=true so it is not forced to 0
        assertEquals(0, result.getTemperaute());
    }

    @Test
    void getImmerRestData_throttleFour() {
        BufferedImage img = createBlackImage();
        setRegionWhite(img, 495, 215); // heating on

        // All four throttle levels lit → last assignment wins → throttle=4
        setRegionWhite(img, 305, 150);
        setRegionWhite(img, 334, 150);
        setRegionWhite(img, 362, 150);
        setRegionWhite(img, 390, 150);

        ImmerRest result = service.getImmerRestData(img, 0, 0);
        assertEquals(4, result.getThrottle());
    }

    @Test
    void getImmerRestData_throttleTwo() {
        BufferedImage img = createBlackImage();
        setRegionWhite(img, 495, 215); // heating on

        // Level 1 and 2 lit, 3 and 4 dark → throttle=2
        setRegionWhite(img, 305, 150);
        setRegionWhite(img, 334, 150);

        ImmerRest result = service.getImmerRestData(img, 0, 0);
        assertEquals(2, result.getThrottle());
    }

    @Test
    void getImmerRestData_validTemperature() {
        // Encode temperature "35":
        //   digit1 = 3 → segments: d1=T,d2=F,d3=F,d4=T,d5=T,d6=T,d7=T
        //   digit2 = 5 → segments: d1=T,d2=T,d3=F,d4=T,d5=T,d6=F,d7=T
        // Result: 3*10 + 5 = 35 which is in range (20, 56).
        BufferedImage img = createBlackImage();
        setRegionWhite(img, 495, 215); // heating on

        // Digit 1 segments (all offsets relative to offsetX=0, offsetY=0)
        setRegionWhite(img, 306, 178);  // d1_1 on
        // d1_2 (291,199) left black → off
        // d1_3 (291,243) left black → off
        setRegionWhite(img, 306, 269);  // d1_4 on
        setRegionWhite(img, 324, 243);  // d1_5 on
        setRegionWhite(img, 324, 199);  // d1_6 on
        setRegionWhite(img, 304, 224);  // d1_7 on

        // Digit 2 segments
        setRegionWhite(img, 360, 178);  // d2_1 on
        setRegionWhite(img, 344, 199);  // d2_2 on
        // d2_3 (344,243) left black → off
        setRegionWhite(img, 360, 268);  // d2_4 on
        setRegionWhite(img, 377, 243);  // d2_5 on
        // d2_6 (377,199) left black → off
        setRegionWhite(img, 360, 224);  // d2_7 on

        ImmerRest result = service.getImmerRestData(img, 0, 0);

        assertTrue(result.isHeating());
        assertEquals(35, result.getTemperaute());
    }

    @Test
    void getImmerRestData_outOfRangeTemperatureFallsBackToPrevious() {
        // No digit segments lit → both digits decode to 1000 → number = 10000 > 500
        // → falls back to previousTempValue which starts at 0.
        // heating=true so temperature is not forced to 0, but fallback value is 0.
        BufferedImage img = createBlackImage();
        setRegionWhite(img, 495, 215); // heating on

        ImmerRest result = service.getImmerRestData(img, 0, 0);
        assertEquals(0, result.getTemperaute());
    }

    @Test
    void getImmerRestData_withPositiveOffset() {
        // Verify that offsetX and offsetY shift the sampling coordinates correctly.
        int ox = 10;
        int oy = 5;
        BufferedImage img = createBlackImage();

        // Heating at (495+ox, 215+oy)
        setRegionWhite(img, 495 + ox, 215 + oy);
        // BoilerOn at (490+ox, 120+oy)
        setRegionWhite(img, 490 + ox, 120 + oy);

        ImmerRest result = service.getImmerRestData(img, ox, oy);

        assertTrue(result.isHeating());
        assertTrue(result.isBoilerOn());
    }
}
