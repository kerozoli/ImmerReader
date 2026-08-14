package com.keroleap.immerreader.SharedData;

public class ChickenNest {
    private int x;
    private int y;
    private int width;
    private int height;
    private int threshold = 180;
    private int minArea = 500;
    private int maxArea = 8000;
    private double minCircularity = 0.5;

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
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
        return width > 0 && height > 0;
    }
}
