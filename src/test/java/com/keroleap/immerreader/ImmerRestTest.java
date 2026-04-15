package com.keroleap.immerreader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImmerRestTest {

    @Test
    void defaultValues() {
        ImmerRest immerRest = new ImmerRest();
        assertEquals(0, immerRest.getTemperaute());
        assertEquals(0, immerRest.getThrottle());
        assertFalse(immerRest.isHeating());
        assertFalse(immerRest.isBoilerOn());
    }

    @Test
    void setAndGetTemperature() {
        ImmerRest immerRest = new ImmerRest();
        immerRest.setTemperaute(42);
        assertEquals(42, immerRest.getTemperaute());
    }

    @Test
    void setAndGetThrottle() {
        ImmerRest immerRest = new ImmerRest();
        immerRest.setThrottle(3);
        assertEquals(3, immerRest.getThrottle());
    }

    @Test
    void setHeatingTrue() {
        ImmerRest immerRest = new ImmerRest();
        immerRest.setHeating(true);
        assertTrue(immerRest.isHeating());
    }

    @Test
    void setHeatingFalse() {
        ImmerRest immerRest = new ImmerRest();
        immerRest.setHeating(true);
        immerRest.setHeating(false);
        assertFalse(immerRest.isHeating());
    }

    @Test
    void setBoilerOnTrue() {
        ImmerRest immerRest = new ImmerRest();
        immerRest.setBoilerOn(true);
        assertTrue(immerRest.isBoilerOn());
    }

    @Test
    void setBoilerOnFalse() {
        ImmerRest immerRest = new ImmerRest();
        immerRest.setBoilerOn(true);
        immerRest.setBoilerOn(false);
        assertFalse(immerRest.isBoilerOn());
    }

    @Test
    void independentFields() {
        ImmerRest immerRest = new ImmerRest();
        immerRest.setTemperaute(35);
        immerRest.setThrottle(2);
        immerRest.setHeating(true);
        immerRest.setBoilerOn(true);

        assertEquals(35, immerRest.getTemperaute());
        assertEquals(2, immerRest.getThrottle());
        assertTrue(immerRest.isHeating());
        assertTrue(immerRest.isBoilerOn());
    }
}
