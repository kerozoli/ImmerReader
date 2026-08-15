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
    private static final int DEFAULT_THRESHOLD = 100;
    private static final int DEFAULT_INTERVAL_SECONDS = 15;

    private final AtomicIntegerArray xs = new AtomicIntegerArray(POINT_COUNT);
    private final AtomicIntegerArray ys = new AtomicIntegerArray(POINT_COUNT);
    private final AtomicInteger threshold = new AtomicInteger(DEFAULT_THRESHOLD);
    private final AtomicInteger intervalSeconds = new AtomicInteger(DEFAULT_INTERVAL_SECONDS);
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
                threshold.set(Integer.parseInt(props.getProperty("threshold", String.valueOf(DEFAULT_THRESHOLD))));
                intervalSeconds.set(Integer.parseInt(props.getProperty("intervalSeconds", String.valueOf(DEFAULT_INTERVAL_SECONDS))));
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
        props.setProperty("threshold", String.valueOf(threshold.get()));
        props.setProperty("intervalSeconds", String.valueOf(intervalSeconds.get()));
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
        this.intervalSeconds.set(Math.max(5, intervalSeconds));
        save();
    }
}
