package com.keroleap.immerreader.SharedData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ChickenManagerData {

    private static final Logger logger = LoggerFactory.getLogger(ChickenManagerData.class);
    private static final String DATA_FILE = "/data/chicken.properties";
    private static final int MIN_NESTS = 1;
    private static final int MAX_NESTS = 5;
    private static final int POINT_COUNT = 4;

    private final List<ChickenNest> nests = new ArrayList<>();
    private int nestCount = 3;
    private boolean enabled = true;

    public ChickenManagerData() {
        load();
    }

    public List<ChickenNest> getNests() {
        return Collections.unmodifiableList(nests);
    }

    public int getNestCount() {
        return nestCount;
    }

    public void setNestCount(int nestCount) {
        this.nestCount = clamp(nestCount, MIN_NESTS, MAX_NESTS);
        resizeNests();
        save();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
    }

    public void setNestPoints(int index, int[] xs, int[] ys) {
        validateIndex(index);
        ChickenNest nest = nests.get(index);
        nest.setXs(xs);
        nest.setYs(ys);
        save();
    }

    public void setNestThreshold(int index, int threshold) {
        validateIndex(index);
        nests.get(index).setThreshold(threshold);
        save();
    }

    public void setNestFilters(int index, int minArea, int maxArea, double minCircularity) {
        validateIndex(index);
        ChickenNest nest = nests.get(index);
        nest.setMinArea(minArea);
        nest.setMaxArea(maxArea);
        nest.setMinCircularity(minCircularity);
        save();
    }

    public void save() {
        Properties properties = new Properties();
        properties.setProperty("enabled", String.valueOf(enabled));
        properties.setProperty("nestCount", String.valueOf(nestCount));
        for (int i = 0; i < nestCount; i++) {
            ChickenNest nest = nests.get(i);
            String prefix = "nest" + (i + 1) + ".";
            int[] xs = nest.getXs();
            int[] ys = nest.getYs();
            for (int p = 0; p < POINT_COUNT; p++) {
                properties.setProperty(prefix + "x" + p, String.valueOf(xs[p]));
                properties.setProperty(prefix + "y" + p, String.valueOf(ys[p]));
            }
            properties.setProperty(prefix + "threshold", String.valueOf(nest.getThreshold()));
            properties.setProperty(prefix + "minArea", String.valueOf(nest.getMinArea()));
            properties.setProperty(prefix + "maxArea", String.valueOf(nest.getMaxArea()));
            properties.setProperty(prefix + "minCircularity", String.valueOf(nest.getMinCircularity()));
        }

        File file = new File(DATA_FILE);
        file.getParentFile().mkdirs();
        try (OutputStream output = new FileOutputStream(file)) {
            properties.store(output, "Chicken nest configuration");
        } catch (IOException e) {
            logger.warn("Could not save chicken data to {}: {}", DATA_FILE, e.getMessage());
        }
    }

    private void load() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            nestCount = 3;
            resizeNests();
            return;
        }
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(file)) {
            properties.load(input);
            enabled = Boolean.parseBoolean(properties.getProperty("enabled", "true"));
            nestCount = clamp(parseInt(properties, "nestCount", 3), MIN_NESTS, MAX_NESTS);
            resizeNests();
            for (int i = 0; i < nestCount; i++) {
                ChickenNest nest = nests.get(i);
                String prefix = "nest" + (i + 1) + ".";
                int[] xs = new int[POINT_COUNT];
                int[] ys = new int[POINT_COUNT];
                for (int p = 0; p < POINT_COUNT; p++) {
                    xs[p] = parseInt(properties, prefix + "x" + p, 0);
                    ys[p] = parseInt(properties, prefix + "y" + p, 0);
                }
                nest.setXs(xs);
                nest.setYs(ys);
                nest.setThreshold(parseInt(properties, prefix + "threshold", 180));
                nest.setMinArea(parseInt(properties, prefix + "minArea", 500));
                nest.setMaxArea(parseInt(properties, prefix + "maxArea", 8000));
                nest.setMinCircularity(parseDouble(properties, prefix + "minCircularity", 0.5));
            }
        } catch (IOException e) {
            logger.warn("Could not load chicken data from {}: {}", DATA_FILE, e.getMessage());
        }
    }

    private void resizeNests() {
        while (nests.size() < nestCount) {
            nests.add(new ChickenNest());
        }
        while (nests.size() > nestCount) {
            nests.remove(nests.size() - 1);
        }
    }

    private void validateIndex(int index) {
        if (index < 0 || index >= nestCount) {
            throw new IllegalArgumentException("Invalid nest index: " + index);
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    private int parseInt(Properties properties, String key, int defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private double parseDouble(Properties properties, String key, double defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Double.parseDouble(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
