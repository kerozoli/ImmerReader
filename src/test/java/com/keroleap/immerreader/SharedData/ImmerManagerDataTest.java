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
}
