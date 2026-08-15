package com.keroleap.immerreader;

import java.util.ArrayList;
import java.util.List;

public class ChickenRest {

    private List<Integer> nestCounts = new ArrayList<>();
    private int totalCount;
    private boolean error;
    private ErrorType errorType;
    private int intervalSeconds;
    private boolean enabled;
    private boolean thresholdMaskEnabled;

    public List<Integer> getNestCounts() {
        return nestCounts;
    }

    public void setNestCounts(List<Integer> nestCounts) {
        this.nestCounts = nestCounts;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
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

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(int intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isThresholdMaskEnabled() {
        return thresholdMaskEnabled;
    }

    public void setThresholdMaskEnabled(boolean thresholdMaskEnabled) {
        this.thresholdMaskEnabled = thresholdMaskEnabled;
    }

    @Override
    public String toString() {
        return "Chicken [nestCounts=" + nestCounts + ", total=" + totalCount + ", error=" + error + "]";
    }
}
