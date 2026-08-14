package com.keroleap.immerreader;

public class EbedloRest {
    private boolean on;
    private long averageValue;
    private boolean error;
    private ErrorType errorType;

    public long getAverageValue() {
        return averageValue;
    }

    public void setAverageValue(long averageValue) {
        this.averageValue = averageValue;
    }

    public boolean isOn() {
        return on;
    }

    public void setOn(boolean on) {
        this.on = on;
    }

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

    @Override
    public String toString() {
        return "Ebedlo [on=" + on + ", error=" + error + "]";
    }
}
