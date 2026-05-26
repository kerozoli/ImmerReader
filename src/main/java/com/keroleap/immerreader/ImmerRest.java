package com.keroleap.immerreader;

public class ImmerRest {
    private int temperaute;
    private int throttle;
    private boolean heating;
    private boolean boilerOn;
    private boolean error;
    private ErrorType errorType;

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public void setErrorType(ErrorType errorType) {
        this.errorType = errorType;
    }

    public boolean isBoilerOn() {
        return boilerOn;
    }

    public void setBoilerOn(boolean boilerOn) {
        this.boilerOn = boilerOn;
    }

    public int getTemperaute() {
        return temperaute;
    }

    public void setTemperaute(int temperaute) {
        this.temperaute = temperaute;
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
