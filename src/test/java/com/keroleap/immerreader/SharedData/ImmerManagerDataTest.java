package com.keroleap.immerreader.SharedData;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImmerManagerDataTest {

    /**
     * Directly instantiates ImmerManagerData without a Spring context.
     * The @PostConstruct (load) is not invoked, but the AtomicIntegerArray fields
     * are initialised to the default coordinates by their field declarations,
     * so default-value assertions are still meaningful.
     */
    private ImmerManagerData newInstance() {
        return new ImmerManagerData();
    }

    @Test
    void defaultXsMatchDefaults() {
        ImmerManagerData data = newInstance();
        int[] xs = data.getXs();
        assertEquals(ImmerManagerData.POINT_COUNT, xs.length);
        assertEquals(495, xs[ImmerManagerData.HEATING]);
        assertEquals(305, xs[ImmerManagerData.LEVEL_ONE]);
        assertEquals(360, xs[ImmerManagerData.DIGIT2_SEG1]);
    }

    @Test
    void defaultYsMatchDefaults() {
        ImmerManagerData data = newInstance();
        int[] ys = data.getYs();
        assertEquals(ImmerManagerData.POINT_COUNT, ys.length);
        assertEquals(215, ys[ImmerManagerData.HEATING]);
        assertEquals(150, ys[ImmerManagerData.LEVEL_ONE]);
        assertEquals(178, ys[ImmerManagerData.DIGIT2_SEG1]);
    }

    @Test
    void setAndGetPoints() {
        ImmerManagerData data = newInstance();
        int[] xs = new int[ImmerManagerData.POINT_COUNT];
        int[] ys = new int[ImmerManagerData.POINT_COUNT];
        for (int i = 0; i < ImmerManagerData.POINT_COUNT; i++) {
            xs[i] = i * 10;
            ys[i] = i * 10 + 1;
        }
        data.setPoints(xs, ys);
        assertArrayEquals(xs, data.getXs());
        assertArrayEquals(ys, data.getYs());
    }

    @Test
    void setPointsRejectsWrongLength() {
        ImmerManagerData data = newInstance();
        int[] xs = new int[ImmerManagerData.POINT_COUNT - 1];
        int[] ys = new int[ImmerManagerData.POINT_COUNT];
        assertThrows(IllegalArgumentException.class, () -> data.setPoints(xs, ys));
    }

    @Test
    void setPointsRejectsNull() {
        ImmerManagerData data = newInstance();
        int[] xs = new int[ImmerManagerData.POINT_COUNT];
        assertThrows(IllegalArgumentException.class, () -> data.setPoints(null, xs));
        assertThrows(IllegalArgumentException.class, () -> data.setPoints(xs, null));
    }

    @Test
    void getAndSetIndividualPoint() {
        ImmerManagerData data = newInstance();
        assertEquals(495, data.getX(ImmerManagerData.HEATING));
        assertEquals(215, data.getY(ImmerManagerData.HEATING));
    }

    @Test
    void defaultEnabledIsFalse() {
        assertFalse(newInstance().isEnabled());
    }

    @Test
    void setAndGetEnabled() {
        ImmerManagerData data = newInstance();
        data.setEnabled(false);
        assertFalse(data.isEnabled());
    }

    @Test
    void setEnabledMultipleTimes() {
        ImmerManagerData data = newInstance();
        data.setEnabled(false);
        data.setEnabled(true);
        data.setEnabled(false);
        assertFalse(data.isEnabled());
    }

    @Test
    void defaultReferencePointIsSet() {
        ImmerManagerData data = newInstance();
        assertEquals(150, data.getReferenceX());
        assertEquals(150, data.getReferenceY());
    }

    @Test
    void setAndGetReferenceX() {
        ImmerManagerData data = newInstance();
        data.setReferenceX(300);
        assertEquals(300, data.getReferenceX());
    }

    @Test
    void setAndGetReferenceY() {
        ImmerManagerData data = newInstance();
        data.setReferenceY(250);
        assertEquals(250, data.getReferenceY());
    }

    @Test
    void defaultReferenceThreshold() {
        assertEquals(-8000000, newInstance().getReferenceThreshold());
    }

    @Test
    void setAndGetReferenceThreshold() {
        ImmerManagerData data = newInstance();
        data.setReferenceThreshold(-5000000);
        assertEquals(-5000000, data.getReferenceThreshold());
    }

    @Test
    void defaultReferenceHysteresis() {
        assertEquals(500000, newInstance().getReferenceHysteresis());
    }

    @Test
    void setAndGetReferenceHysteresis() {
        ImmerManagerData data = newInstance();
        data.setReferenceHysteresis(250000);
        assertEquals(250000, data.getReferenceHysteresis());
    }

    @Test
    void defaultDarkThreshold() {
        assertEquals(-2500000, newInstance().getDarkThreshold());
    }

    @Test
    void setAndGetDarkThreshold() {
        ImmerManagerData data = newInstance();
        data.setDarkThreshold(-3000000);
        assertEquals(-3000000, data.getDarkThreshold());
    }

    @Test
    void defaultLightThreshold() {
        assertEquals(-6000000, newInstance().getLightThreshold());
    }

    @Test
    void setAndGetLightThreshold() {
        ImmerManagerData data = newInstance();
        data.setLightThreshold(-7000000);
        assertEquals(-7000000, data.getLightThreshold());
    }

    @Test
    void defaultAmbientBrightnessIsZero() {
        assertEquals(0, newInstance().getAmbientBrightness());
    }

    @Test
    void setAndGetAmbientBrightness() {
        ImmerManagerData data = newInstance();
        data.setAmbientBrightness(-4000000);
        assertEquals(-4000000, data.getAmbientBrightness());
    }

    @Test
    void defaultLightModeIsFalse() {
        assertFalse(newInstance().isLightMode());
    }

    @Test
    void setAndGetLightMode() {
        ImmerManagerData data = newInstance();
        data.setLightMode(true);
        assertTrue(data.isLightMode());
    }
}
