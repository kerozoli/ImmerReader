package com.keroleap.immerreader.SharedData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ChickenManagerData {

    private static final Logger logger = LoggerFactory.getLogger(ChickenManagerData.class);
    private static final String DATA_FILE = "/data/chicken.properties";
    private static final int NEST_COUNT = 3;

    private final ChickenNest[] nests = new ChickenNest[NEST_COUNT];
    private boolean enabled = true;

    public ChickenManagerData() {
        for (int i = 0; i < NEST_COUNT; i++) {
            nests[i] = new ChickenNest();
        }
        load();
    }

    public ChickenNest[] getNests() {
        return nests;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
    }

    public void setNest(int index, int x, int y, int width, int height) {
        if (index < 0 || index >= NEST_COUNT) {
            throw new IllegalArgumentException("Invalid nest index: " + index);
        }
        ChickenNest nest = nests[index];
        nest.setX(x);
        nest.setY(y);
        nest.setWidth(width);
        nest.setHeight(height);
        save();
    }

    public void setNestThreshold(int index, int threshold) {
        if (index < 0 || index >= NEST_COUNT) {
            throw new IllegalArgumentException("Invalid nest index: " + index);
        }
        nests[index].setThreshold(threshold);
        save();
    }

    public void setNestFilters(int index, int minArea, int maxArea, double minCircularity) {
        if (index < 0 || index >= NEST_COUNT) {
            throw new IllegalArgumentException("Invalid nest index: " + index);
        }
        ChickenNest nest = nests[index];
        nest.setMinArea(minArea);
        nest.setMaxArea(maxArea);
        nest.setMinCircularity(minCircularity);
        save();
    }

    public void save() {
        Properties properties = new Properties();
        properties.setProperty("enabled", String.valueOf(enabled));
        for (int i = 0; i < NEST_COUNT; i++) {
            ChickenNest nest = nests[i];
            String prefix = "nest" + (i + 1) + ".";
            properties.setProperty(prefix + "x", String.valueOf(nest.getX()));
            properties.setProperty(prefix + "y", String.valueOf(nest.getY()));
            properties.setProperty(prefix + "width", String.valueOf(nest.getWidth()));
            properties.setProperty(prefix + "height", String.valueOf(nest.getHeight()));
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
            return;
        }
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(file)) {
            properties.load(input);
            enabled = Boolean.parseBoolean(properties.getProperty("enabled", "true"));
            for (int i = 0; i < NEST_COUNT; i++) {
                ChickenNest nest = nests[i];
                String prefix = "nest" + (i + 1) + ".";
                nest.setX(parseInt(properties, prefix + "x", 0));
                nest.setY(parseInt(properties, prefix + "y", 0));
                nest.setWidth(parseInt(properties, prefix + "width", 0));
                nest.setHeight(parseInt(properties, prefix + "height", 0));
                nest.setThreshold(parseInt(properties, prefix + "threshold", 180));
                nest.setMinArea(parseInt(properties, prefix + "minArea", 500));
                nest.setMaxArea(parseInt(properties, prefix + "maxArea", 8000));
                nest.setMinCircularity(parseDouble(properties, prefix + "minCircularity", 0.5));
            }
        } catch (IOException e) {
            logger.warn("Could not load chicken data from {}: {}", DATA_FILE, e.getMessage());
        }
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
