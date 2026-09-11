package com.keroleap.immerreader.SharedData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class EbedloManagerData {

    private static final Logger logger = LoggerFactory.getLogger(EbedloManagerData.class);
    private static final String DATA_FILE = "/data/ebedlo.properties";
    private static final int POINT_COUNT = 4;
    private static final int PLATE_POINT_COUNT = 4;
    private static final int DEFAULT_THRESHOLD = 100;
    private static final int DEFAULT_INTERVAL_SECONDS = 15;
    private static final double DEFAULT_TRIM_PERCENTAGE = 0.10;
    private static final TrimMode DEFAULT_TRIM_MODE = TrimMode.BOTH;
    private static final int DEFAULT_PLATE_THRESHOLD = 15;
    private static final int DEFAULT_PLATE_MIN_RADIUS = 20;
    private static final int DEFAULT_PLATE_MAX_RADIUS = 80;

    private final AtomicIntegerArray xs = new AtomicIntegerArray(POINT_COUNT);
    private final AtomicIntegerArray ys = new AtomicIntegerArray(POINT_COUNT);
    private final AtomicIntegerArray plateXs = new AtomicIntegerArray(PLATE_POINT_COUNT);
    private final AtomicIntegerArray plateYs = new AtomicIntegerArray(PLATE_POINT_COUNT);
    private final AtomicInteger threshold = new AtomicInteger(DEFAULT_THRESHOLD);
    private final AtomicInteger intervalSeconds = new AtomicInteger(DEFAULT_INTERVAL_SECONDS);
    private volatile double trimPercentage = DEFAULT_TRIM_PERCENTAGE;
    private volatile TrimMode trimMode = DEFAULT_TRIM_MODE;
    private final AtomicInteger plateThreshold = new AtomicInteger(DEFAULT_PLATE_THRESHOLD);
    private final AtomicInteger plateMinRadius = new AtomicInteger(DEFAULT_PLATE_MIN_RADIUS);
    private final AtomicInteger plateMaxRadius = new AtomicInteger(DEFAULT_PLATE_MAX_RADIUS);
    private final AtomicBoolean enabled = new AtomicBoolean(false);

    @PostConstruct
    private void load() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
                for (int i = 0; i < POINT_COUNT; i++) {
                    xs.set(i, Integer.parseInt(props.getProperty("x" + i, "0")));
                    ys.set(i, Integer.parseInt(props.getProperty("y" + i, "0")));
                }
                for (int i = 0; i < PLATE_POINT_COUNT; i++) {
                    plateXs.set(i, Integer.parseInt(props.getProperty("plateX" + i, "0")));
                    plateYs.set(i, Integer.parseInt(props.getProperty("plateY" + i, "0")));
                }
                threshold.set(Integer.parseInt(props.getProperty("threshold", String.valueOf(DEFAULT_THRESHOLD))));
                intervalSeconds.set(Integer.parseInt(props.getProperty("intervalSeconds", String.valueOf(DEFAULT_INTERVAL_SECONDS))));
                trimPercentage = clampTrimPercentage(parseDouble(props.getProperty("trimPercentage"), DEFAULT_TRIM_PERCENTAGE));
                trimMode = TrimMode.fromString(props.getProperty("trimMode"));
                plateThreshold.set(Integer.parseInt(props.getProperty("plateThreshold", String.valueOf(DEFAULT_PLATE_THRESHOLD))));
                plateMinRadius.set(Integer.parseInt(props.getProperty("plateMinRadius", String.valueOf(DEFAULT_PLATE_MIN_RADIUS))));
                plateMaxRadius.set(Integer.parseInt(props.getProperty("plateMaxRadius", String.valueOf(DEFAULT_PLATE_MAX_RADIUS))));
                enabled.set(Boolean.parseBoolean(props.getProperty("enabled", "false")));
            } catch (IOException | NumberFormatException e) {
                logger.warn("Could not load Ebedlo data from {}: {}", DATA_FILE, e.getMessage());
            }
        }
    }

    private void save() {
        Properties props = new Properties();
        for (int i = 0; i < POINT_COUNT; i++) {
            props.setProperty("x" + i, String.valueOf(xs.get(i)));
            props.setProperty("y" + i, String.valueOf(ys.get(i)));
        }
        for (int i = 0; i < PLATE_POINT_COUNT; i++) {
            props.setProperty("plateX" + i, String.valueOf(plateXs.get(i)));
            props.setProperty("plateY" + i, String.valueOf(plateYs.get(i)));
        }
        props.setProperty("threshold", String.valueOf(threshold.get()));
        props.setProperty("intervalSeconds", String.valueOf(intervalSeconds.get()));
        props.setProperty("trimPercentage", String.valueOf(trimPercentage));
        props.setProperty("trimMode", String.valueOf(trimMode));
        props.setProperty("plateThreshold", String.valueOf(plateThreshold.get()));
        props.setProperty("plateMinRadius", String.valueOf(plateMinRadius.get()));
        props.setProperty("plateMaxRadius", String.valueOf(plateMaxRadius.get()));
        props.setProperty("enabled", String.valueOf(enabled.get()));
        File file = new File(DATA_FILE);
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            logger.warn("Could not create directory {}", parent.getAbsolutePath());
            return;
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            props.store(fos, null);
        } catch (IOException e) {
            logger.warn("Could not save Ebedlo data to {}: {}", DATA_FILE, e.getMessage());
        }
    }

    public int getPointCount() {
        return POINT_COUNT;
    }

    public int getPlatePointCount() {
        return PLATE_POINT_COUNT;
    }

    public int getX(int index) {
        return xs.get(index);
    }

    public void setX(int index, int x) {
        xs.set(index, x);
        save();
    }

    public int getY(int index) {
        return ys.get(index);
    }

    public void setY(int index, int y) {
        ys.set(index, y);
        save();
    }

    public int[] getXs() {
        int[] result = new int[POINT_COUNT];
        for (int i = 0; i < POINT_COUNT; i++) {
            result[i] = xs.get(i);
        }
        return result;
    }

    public int[] getYs() {
        int[] result = new int[POINT_COUNT];
        for (int i = 0; i < POINT_COUNT; i++) {
            result[i] = ys.get(i);
        }
        return result;
    }

    public int getPlateX(int index) {
        return plateXs.get(index);
    }

    public void setPlateX(int index, int x) {
        plateXs.set(index, x);
        save();
    }

    public int getPlateY(int index) {
        return plateYs.get(index);
    }

    public void setPlateY(int index, int y) {
        plateYs.set(index, y);
        save();
    }

    public int[] getPlateXs() {
        int[] result = new int[PLATE_POINT_COUNT];
        for (int i = 0; i < PLATE_POINT_COUNT; i++) {
            result[i] = plateXs.get(i);
        }
        return result;
    }

    public int[] getPlateYs() {
        int[] result = new int[PLATE_POINT_COUNT];
        for (int i = 0; i < PLATE_POINT_COUNT; i++) {
            result[i] = plateYs.get(i);
        }
        return result;
    }

    public void setPlatePoints(int[] newXs, int[] newYs) {
        if (newXs == null || newYs == null || newXs.length != PLATE_POINT_COUNT || newYs.length != PLATE_POINT_COUNT) {
            throw new IllegalArgumentException("Expected " + PLATE_POINT_COUNT + " x and y coordinates");
        }
        for (int i = 0; i < PLATE_POINT_COUNT; i++) {
            plateXs.set(i, newXs[i]);
            plateYs.set(i, newYs[i]);
        }
        save();
    }

    public void setPoints(int[] newXs, int[] newYs) {
        if (newXs == null || newYs == null || newXs.length != POINT_COUNT || newYs.length != POINT_COUNT) {
            throw new IllegalArgumentException("Expected " + POINT_COUNT + " x and y coordinates");
        }
        for (int i = 0; i < POINT_COUNT; i++) {
            xs.set(i, newXs[i]);
            ys.set(i, newYs[i]);
        }
        save();
    }

    public int getThreshold() {
        return threshold.get();
    }

    public void setThreshold(int threshold) {
        this.threshold.set(threshold);
        save();
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
        save();
    }

    public int getIntervalSeconds() {
        return intervalSeconds.get();
    }

    public void setIntervalSeconds(int intervalSeconds) {
        this.intervalSeconds.set(Math.max(1, intervalSeconds));
        save();
    }

    public double getTrimPercentage() {
        return trimPercentage;
    }

    public void setTrimPercentage(double trimPercentage) {
        this.trimPercentage = clampTrimPercentage(trimPercentage);
        save();
    }

    public TrimMode getTrimMode() {
        return trimMode;
    }

    public void setTrimMode(TrimMode trimMode) {
        this.trimMode = trimMode != null ? trimMode : DEFAULT_TRIM_MODE;
        save();
    }

    public int getPlateThreshold() {
        return plateThreshold.get();
    }

    public void setPlateThreshold(int plateThreshold) {
        this.plateThreshold.set(Math.max(0, plateThreshold));
        save();
    }

    public int getPlateMinRadius() {
        return plateMinRadius.get();
    }

    public void setPlateMinRadius(int plateMinRadius) {
        this.plateMinRadius.set(Math.max(0, plateMinRadius));
        save();
    }

    public int getPlateMaxRadius() {
        return plateMaxRadius.get();
    }

    public void setPlateMaxRadius(int plateMaxRadius) {
        this.plateMaxRadius.set(Math.max(0, plateMaxRadius));
        save();
    }

    private static double clampTrimPercentage(double value) {
        if (Double.isNaN(value) || value < 0.0) {
            return 0.0;
        }
        return Math.min(value, 0.49);
    }

    private static double parseDouble(String value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
