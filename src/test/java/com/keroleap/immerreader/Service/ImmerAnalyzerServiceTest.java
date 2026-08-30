package com.keroleap.immerreader.Service;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.keroleap.immerreader.ImmerRest;
import com.keroleap.immerreader.SharedData.ImmerManagerData;

import static org.junit.jupiter.api.Assertions.*;

class ImmerAnalyzerServiceTest {

    private ImmerAnalyzerService service;

    // Image large enough to accommodate all pixel reads/writes used by getImmerRestData
    // with the default point coordinates. Furthest writes reach x+5=500, y+5=275.
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

    private int[] defaultXs() {
        ImmerManagerData data = new ImmerManagerData();
        return data.getXs();
    }

    private int[] defaultYs() {
        ImmerManagerData data = new ImmerManagerData();
        return data.getYs();
    }

    @Test
    void getImmerRestData_noHeating_allZero() {
        // All pixels black → heating=false → temperature forced to 0, throttle=0
        BufferedImage img = createBlackImage();
        ImmerRest result = service.getImmerRestData(img, defaultXs(), defaultYs());

        assertFalse(result.isHeating());
        assertFalse(result.isBoilerOn());
        assertEquals(0, result.getThrottle());
        assertEquals(0, result.getTemperaute());
    }

    @Test
    void getImmerRestData_heatingOn_boilerOn_throttleOne() {
        BufferedImage img = createBlackImage();
        int[] xs = defaultXs();
        int[] ys = defaultYs();

        // heating indicator at default (495, 215)
        setRegionWhite(img, xs[ImmerManagerData.HEATING], ys[ImmerManagerData.HEATING]);
        // boilerOn indicator at default (490, 120)
        setRegionWhite(img, xs[ImmerManagerData.BOILER], ys[ImmerManagerData.BOILER]);
        // throttle level 1 at default (305, 150); levels 2-4 remain black
        setRegionWhite(img, xs[ImmerManagerData.LEVEL_ONE], ys[ImmerManagerData.LEVEL_ONE]);

        // Leave temperature digits all black → digits decode to 1000,
        // number > 500 → falls back to previousTempValue (0)
        ImmerRest result = service.getImmerRestData(img, xs, ys);

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
        int[] xs = defaultXs();
        int[] ys = defaultYs();
        setRegionWhite(img, xs[ImmerManagerData.HEATING], ys[ImmerManagerData.HEATING]);

        // All four throttle levels lit → last assignment wins → throttle=4
        setRegionWhite(img, xs[ImmerManagerData.LEVEL_ONE], ys[ImmerManagerData.LEVEL_ONE]);
        setRegionWhite(img, xs[ImmerManagerData.LEVEL_TWO], ys[ImmerManagerData.LEVEL_TWO]);
        setRegionWhite(img, xs[ImmerManagerData.LEVEL_THREE], ys[ImmerManagerData.LEVEL_THREE]);
        setRegionWhite(img, xs[ImmerManagerData.LEVEL_FOUR], ys[ImmerManagerData.LEVEL_FOUR]);

        ImmerRest result = service.getImmerRestData(img, xs, ys);
        assertEquals(4, result.getThrottle());
    }

    @Test
    void getImmerRestData_throttleTwo() {
        BufferedImage img = createBlackImage();
        int[] xs = defaultXs();
        int[] ys = defaultYs();
        setRegionWhite(img, xs[ImmerManagerData.HEATING], ys[ImmerManagerData.HEATING]);

        // Level 1 and 2 lit, 3 and 4 dark → throttle=2
        setRegionWhite(img, xs[ImmerManagerData.LEVEL_ONE], ys[ImmerManagerData.LEVEL_ONE]);
        setRegionWhite(img, xs[ImmerManagerData.LEVEL_TWO], ys[ImmerManagerData.LEVEL_TWO]);

        ImmerRest result = service.getImmerRestData(img, xs, ys);
        assertEquals(2, result.getThrottle());
    }

    @Test
    void getImmerRestData_validTemperature() {
        // Encode temperature "35":
        //   digit1 = 3 → segments: d1=T,d2=F,d3=F,d4=T,d5=T,d6=T,d7=T
        //   digit2 = 5 → segments: d1=T,d2=T,d3=F,d4=T,d5=T,d6=F,d7=T
        // Result: 3*10 + 5 = 35 which is in range (20, 56).
        BufferedImage img = createBlackImage();
        int[] xs = defaultXs();
        int[] ys = defaultYs();
        setRegionWhite(img, xs[ImmerManagerData.HEATING], ys[ImmerManagerData.HEATING]);

        // Digit 1 segments
        setRegionWhite(img, xs[ImmerManagerData.DIGIT1_SEG1], ys[ImmerManagerData.DIGIT1_SEG1]);  // d1_1 on
        // d1_2 left black → off
        // d1_3 left black → off
        setRegionWhite(img, xs[ImmerManagerData.DIGIT1_SEG4], ys[ImmerManagerData.DIGIT1_SEG4]);  // d1_4 on
        setRegionWhite(img, xs[ImmerManagerData.DIGIT1_SEG5], ys[ImmerManagerData.DIGIT1_SEG5]);  // d1_5 on
        setRegionWhite(img, xs[ImmerManagerData.DIGIT1_SEG6], ys[ImmerManagerData.DIGIT1_SEG6]);  // d1_6 on
        setRegionWhite(img, xs[ImmerManagerData.DIGIT1_SEG7], ys[ImmerManagerData.DIGIT1_SEG7]);  // d1_7 on

        // Digit 2 segments
        setRegionWhite(img, xs[ImmerManagerData.DIGIT2_SEG1], ys[ImmerManagerData.DIGIT2_SEG1]);  // d2_1 on
        setRegionWhite(img, xs[ImmerManagerData.DIGIT2_SEG2], ys[ImmerManagerData.DIGIT2_SEG2]);  // d2_2 on
        // d2_3 left black → off
        setRegionWhite(img, xs[ImmerManagerData.DIGIT2_SEG4], ys[ImmerManagerData.DIGIT2_SEG4]);  // d2_4 on
        setRegionWhite(img, xs[ImmerManagerData.DIGIT2_SEG5], ys[ImmerManagerData.DIGIT2_SEG5]);  // d2_5 on
        // d2_6 left black → off
        setRegionWhite(img, xs[ImmerManagerData.DIGIT2_SEG7], ys[ImmerManagerData.DIGIT2_SEG7]);  // d2_7 on

        ImmerRest result = service.getImmerRestData(img, xs, ys);

        assertTrue(result.isHeating());
        assertEquals(35, result.getTemperaute());
    }

    @Test
    void getImmerRestData_outOfRangeTemperatureFallsBackToPrevious() {
        // No digit segments lit → both digits decode to 1000 → number = 10000 > 500
        // → falls back to previousTempValue which starts at 0.
        // heating=true so temperature is not forced to 0, but fallback value is 0.
        BufferedImage img = createBlackImage();
        int[] xs = defaultXs();
        int[] ys = defaultYs();
        setRegionWhite(img, xs[ImmerManagerData.HEATING], ys[ImmerManagerData.HEATING]);

        ImmerRest result = service.getImmerRestData(img, xs, ys);
        assertEquals(0, result.getTemperaute());
    }

    @Test
    void getImmerRestData_withShiftedPoints() {
        // Verify that individually configurable point coordinates shift the sampling correctly.
        int shiftX = 10;
        int shiftY = 5;
        BufferedImage img = createBlackImage();

        int[] xs = defaultXs();
        int[] ys = defaultYs();
        for (int i = 0; i < ImmerManagerData.POINT_COUNT; i++) {
            xs[i] += shiftX;
            ys[i] += shiftY;
        }

        // Heating and boiler at shifted coordinates
        setRegionWhite(img, xs[ImmerManagerData.HEATING], ys[ImmerManagerData.HEATING]);
        setRegionWhite(img, xs[ImmerManagerData.BOILER], ys[ImmerManagerData.BOILER]);

        ImmerRest result = service.getImmerRestData(img, xs, ys);

        assertTrue(result.isHeating());
        assertTrue(result.isBoilerOn());
    }
}
