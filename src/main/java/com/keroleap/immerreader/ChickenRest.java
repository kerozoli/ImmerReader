package com.keroleap.immerreader;

import java.util.ArrayList;
import java.util.List;

public class ChickenRest {

    private List<Integer> nestCounts = new ArrayList<>();
    private int totalCount;
    private boolean error;
    private ErrorType errorType;
    private int intervalSeconds;

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

    @Override
    public String toString() {
        return "Chicken [nestCounts=" + nestCounts + ", total=" + totalCount + ", error=" + error + "]";
    }
}
