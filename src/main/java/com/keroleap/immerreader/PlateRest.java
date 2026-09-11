package com.keroleap.immerreader;

import java.util.ArrayList;
import java.util.List;

public class PlateRest {
    private int detectedPlateCount;
    private List<PlateCenter> plateCenters = new ArrayList<>();

    public int getDetectedPlateCount() {
        return detectedPlateCount;
    }

    public void setDetectedPlateCount(int detectedPlateCount) {
        this.detectedPlateCount = detectedPlateCount;
    }

    public List<PlateCenter> getPlateCenters() {
        return plateCenters;
    }

    public void setPlateCenters(List<PlateCenter> plateCenters) {
        this.plateCenters = plateCenters != null ? plateCenters : new ArrayList<>();
    }

    public static class PlateCenter {
        private int x;
        private int y;
        private int radius;

        public PlateCenter() {
        }

        public PlateCenter(int x, int y, int radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }

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

        public int getRadius() {
            return radius;
        }

        public void setRadius(int radius) {
            this.radius = radius;
        }
    }
}
