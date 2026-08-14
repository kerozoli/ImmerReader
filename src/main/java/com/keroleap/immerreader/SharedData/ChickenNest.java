package com.keroleap.immerreader.SharedData;

import java.util.Arrays;

public class ChickenNest {

    private static final int POINT_COUNT = 4;

    private final int[] xs = new int[POINT_COUNT];
    private final int[] ys = new int[POINT_COUNT];
    private int threshold = 180;
    private int minArea = 500;
    private int maxArea = 8000;
    private double minCircularity = 0.5;

    public int[] getXs() {
        return xs.clone();
    }

    public int[] getYs() {
        return ys.clone();
    }

    public void setXs(int[] xs) {
        int copyLength = Math.min(xs.length, POINT_COUNT);
        Arrays.fill(this.xs, 0);
        System.arraycopy(xs, 0, this.xs, 0, copyLength);
    }

    public void setYs(int[] ys) {
        int copyLength = Math.min(ys.length, POINT_COUNT);
        Arrays.fill(this.ys, 0);
        System.arraycopy(ys, 0, this.ys, 0, copyLength);
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public int getMinArea() {
        return minArea;
    }

    public void setMinArea(int minArea) {
        this.minArea = minArea;
    }

    public int getMaxArea() {
        return maxArea;
    }

    public void setMaxArea(int maxArea) {
        this.maxArea = maxArea;
    }

    public double getMinCircularity() {
        return minCircularity;
    }

    public void setMinCircularity(double minCircularity) {
        this.minCircularity = minCircularity;
    }

    public boolean isConfigured() {
        for (int i = 0; i < POINT_COUNT; i++) {
            if (xs[i] != 0 || ys[i] != 0) {
                return true;
            }
        }
        return false;
    }

    public int getPointCount() {
        return POINT_COUNT;
    }
}
