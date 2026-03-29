package com.keroleap.immerreader;

public class ImmerRest {
    private int temperature;
    private int throttle;
    private boolean heating;
    private boolean boilerOn;

    public boolean isBoilerOn() {
        return boilerOn;
    }

    public void setBoilerOn(boolean boilerOn) {
        this.boilerOn = boilerOn;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public int getThrottle() {
        return throttle;
    }

    public void setThrottle(int throttle) {
        this.throttle = throttle;
    }

    public boolean isHeating() {
        return heating;
    }

    public void setHeating(boolean heating) {
        this.heating  = heating;
    }
}
