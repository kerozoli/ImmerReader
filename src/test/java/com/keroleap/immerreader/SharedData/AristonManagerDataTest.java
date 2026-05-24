package com.keroleap.immerreader.SharedData;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AristonManagerDataTest {

    /**
     * Directly instantiates AristonManagerData without a Spring context.
     * The @PostConstruct (load) is not invoked, but the AtomicInteger fields
     * are initialised to their declared defaults, so default-value
     * assertions are still meaningful.
     */
    private AristonManagerData newInstance() {
        return new AristonManagerData();
    }

    @Test
    void defaultStartXIs160() {
        assertEquals(160, newInstance().getStartX());
    }

    @Test
    void defaultStartYIs160() {
        assertEquals(160, newInstance().getStartY());
    }

    @Test
    void defaultEndXIs220() {
        assertEquals(220, newInstance().getEndX());
    }

    @Test
    void defaultEndYIs180() {
        assertEquals(180, newInstance().getEndY());
    }

    @Test
    void setAndGetStartX() {
        AristonManagerData data = newInstance();
        data.setStartX(150);
        assertEquals(150, data.getStartX());
    }

    @Test
    void setAndGetStartY() {
        AristonManagerData data = newInstance();
        data.setStartY(155);
        assertEquals(155, data.getStartY());
    }

    @Test
    void setAndGetEndX() {
        AristonManagerData data = newInstance();
        data.setEndX(230);
        assertEquals(230, data.getEndX());
    }

    @Test
    void setAndGetEndY() {
        AristonManagerData data = newInstance();
        data.setEndY(190);
        assertEquals(190, data.getEndY());
    }

    @Test
    void setStartXToNegative() {
        AristonManagerData data = newInstance();
        data.setStartX(-5);
        assertEquals(-5, data.getStartX());
    }

    @Test
    void setEndYToNegative() {
        AristonManagerData data = newInstance();
        data.setEndY(-10);
        assertEquals(-10, data.getEndY());
    }

    @Test
    void setStartXMultipleTimes() {
        AristonManagerData data = newInstance();
        data.setStartX(10);
        data.setStartX(20);
        data.setStartX(5);
        assertEquals(5, data.getStartX());
    }

    @Test
    void coordinatesAreIndependent() {
        AristonManagerData data = newInstance();
        data.setStartX(7);
        data.setStartY(13);
        data.setEndX(25);
        data.setEndY(30);
        assertEquals(7, data.getStartX());
        assertEquals(13, data.getStartY());
        assertEquals(25, data.getEndX());
        assertEquals(30, data.getEndY());
    }
}
