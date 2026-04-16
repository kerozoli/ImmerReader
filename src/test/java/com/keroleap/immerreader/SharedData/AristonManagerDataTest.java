package com.keroleap.immerreader.SharedData;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AristonManagerDataTest {

    /**
     * Directly instantiates AristonManagerData without a Spring context.
     * The @PostConstruct (load) is not invoked, but the AtomicInteger fields
     * are initialised to 0 by their field declarations, so default-value
     * assertions are still meaningful.
     */
    private AristonManagerData newInstance() {
        return new AristonManagerData();
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
        AristonManagerData data = newInstance();
        data.setOffsetX(15);
        assertEquals(15, data.getOffsetX());
    }

    @Test
    void setAndGetOffsetY() {
        AristonManagerData data = newInstance();
        data.setOffsetY(30);
        assertEquals(30, data.getOffsetY());
    }

    @Test
    void setOffsetXToNegative() {
        AristonManagerData data = newInstance();
        data.setOffsetX(-5);
        assertEquals(-5, data.getOffsetX());
    }

    @Test
    void setOffsetYToNegative() {
        AristonManagerData data = newInstance();
        data.setOffsetY(-10);
        assertEquals(-10, data.getOffsetY());
    }

    @Test
    void setOffsetXMultipleTimes() {
        AristonManagerData data = newInstance();
        data.setOffsetX(10);
        data.setOffsetX(20);
        data.setOffsetX(5);
        assertEquals(5, data.getOffsetX());
    }

    @Test
    void xAndYAreIndependent() {
        AristonManagerData data = newInstance();
        data.setOffsetX(7);
        data.setOffsetY(13);
        assertEquals(7, data.getOffsetX());
        assertEquals(13, data.getOffsetY());
    }
}
