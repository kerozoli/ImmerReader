package com.keroleap.immerreader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AristonRestTest {

    @Test
    void defaultPercentageIsZero() {
        AristonRest aristonRest = new AristonRest();
        assertEquals(0, aristonRest.getPercentage());
    }

    @Test
    void setAndGetPercentage() {
        AristonRest aristonRest = new AristonRest();
        aristonRest.setPercentage(75);
        assertEquals(75, aristonRest.getPercentage());
    }

    @Test
    void setPercentageToZero() {
        AristonRest aristonRest = new AristonRest();
        aristonRest.setPercentage(50);
        aristonRest.setPercentage(0);
        assertEquals(0, aristonRest.getPercentage());
    }

    @Test
    void setPercentageToMaxValue() {
        AristonRest aristonRest = new AristonRest();
        aristonRest.setPercentage(100);
        assertEquals(100, aristonRest.getPercentage());
    }

    @Test
    void setPercentageToNegativeValue() {
        AristonRest aristonRest = new AristonRest();
        aristonRest.setPercentage(-10);
        assertEquals(-10, aristonRest.getPercentage());
    }
}
