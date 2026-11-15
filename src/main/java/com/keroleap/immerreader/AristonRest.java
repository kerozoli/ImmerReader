package com.keroleap.immerreader;

public class AristonRest {
    private int percentage;
    private boolean error;
    private ErrorType errorType;
    private long lastSuccessMs;

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

    public int getPercentage() {
        return percentage;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

    public long getLastSuccessMs() {
        return lastSuccessMs;
    }

    public void setLastSuccessMs(long lastSuccessMs) {
        this.lastSuccessMs = lastSuccessMs;
    }

}
