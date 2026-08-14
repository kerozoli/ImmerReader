package com.keroleap.immerreader;

public class ChickenRest {
    private int nest1Count;
    private int nest2Count;
    private int nest3Count;
    private int totalCount;
    private boolean error;
    private ErrorType errorType;

    public int getNest1Count() {
        return nest1Count;
    }

    public void setNest1Count(int nest1Count) {
        this.nest1Count = nest1Count;
    }

    public int getNest2Count() {
        return nest2Count;
    }

    public void setNest2Count(int nest2Count) {
        this.nest2Count = nest2Count;
    }

    public int getNest3Count() {
        return nest3Count;
    }

    public void setNest3Count(int nest3Count) {
        this.nest3Count = nest3Count;
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

    @Override
    public String toString() {
        return "Chicken [nest1=" + nest1Count + ", nest2=" + nest2Count + ", nest3=" + nest3Count + ", total=" + totalCount + ", error=" + error + "]";
    }
}
