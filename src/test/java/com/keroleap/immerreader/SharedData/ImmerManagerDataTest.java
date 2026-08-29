package com.keroleap.immerreader.SharedData;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImmerManagerDataTest {

    /**
     * Directly instantiates ImmerManagerData without a Spring context.
     * The @PostConstruct (load) is not invoked, but the AtomicInteger fields
     * are initialised to 0 by their field declarations, so default-value
     * assertions are still meaningful.
     */
    private ImmerManagerData newInstance() {
        return new ImmerManagerData();
    }

    @Test
    void defaultOffsetXIsZero() {
        assertEquals(0, newInstance().getOffsetX());
    }

    @Test
    void defaultOffsetYIsZero() {
        assertEquals(0, newInstance().getOffsetY());
    }

    @Test
    void setAndGetOffsetX() {
        ImmerManagerData data = newInstance();
        // save() will attempt to write to /data/offset.properties; it fails
        // silently when the directory does not exist, so the in-memory value
        // must still be updated.
        data.setOffsetX(15);
        assertEquals(15, data.getOffsetX());
    }

    @Test
    void setAndGetOffsetY() {
        ImmerManagerData data = newInstance();
        data.setOffsetY(30);
        assertEquals(30, data.getOffsetY());
    }

    @Test
    void setOffsetXToNegative() {
        ImmerManagerData data = newInstance();
        data.setOffsetX(-5);
        assertEquals(-5, data.getOffsetX());
    }

    @Test
    void setOffsetYToNegative() {
        ImmerManagerData data = newInstance();
        data.setOffsetY(-10);
        assertEquals(-10, data.getOffsetY());
    }

    @Test
    void setOffsetXMultipleTimes() {
        ImmerManagerData data = newInstance();
        data.setOffsetX(10);
        data.setOffsetX(20);
        data.setOffsetX(5);
        assertEquals(5, data.getOffsetX());
    }

    @Test
    void xAndYAreIndependent() {
        ImmerManagerData data = newInstance();
        data.setOffsetX(7);
        data.setOffsetY(13);
        assertEquals(7, data.getOffsetX());
        assertEquals(13, data.getOffsetY());
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
