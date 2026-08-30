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
public class ImmerManagerData {
    private static final Logger logger = LoggerFactory.getLogger(ImmerManagerData.class);
    private static final String DATA_FILE = "/data/offset.properties";

    // Indices for individually configurable detection points.
    // Reference point (referenceX/referenceY) stays separate and is not part of these arrays.
    public static final int HEATING = 0;
    public static final int LEVEL_ONE = 1;
    public static final int LEVEL_TWO = 2;
    public static final int LEVEL_THREE = 3;
    public static final int LEVEL_FOUR = 4;
    public static final int BOILER = 5;
    public static final int DIGIT1_SEG1 = 6;
    public static final int DIGIT1_SEG2 = 7;
    public static final int DIGIT1_SEG3 = 8;
    public static final int DIGIT1_SEG4 = 9;
    public static final int DIGIT1_SEG5 = 10;
    public static final int DIGIT1_SEG6 = 11;
    public static final int DIGIT1_SEG7 = 12;
    public static final int DIGIT2_SEG1 = 13;
    public static final int DIGIT2_SEG2 = 14;
    public static final int DIGIT2_SEG3 = 15;
    public static final int DIGIT2_SEG4 = 16;
    public static final int DIGIT2_SEG5 = 17;
    public static final int DIGIT2_SEG6 = 18;
    public static final int DIGIT2_SEG7 = 19;
    public static final int POINT_COUNT = 20;

    private static final int[] DEFAULT_X = {
        495, 305, 334, 362, 390, 490,
        306, 291, 291, 306, 324, 324, 304,
        360, 344, 344, 360, 377, 377, 360
    };
    private static final int[] DEFAULT_Y = {
        215, 150, 150, 150, 150, 120,
        178, 199, 243, 269, 243, 199, 224,
        178, 199, 243, 268, 243, 199, 224
    };

    // Individual detection point coordinates (offsetX/offsetY replaced these)
    private final AtomicIntegerArray xs = new AtomicIntegerArray(POINT_COUNT);
    private final AtomicIntegerArray ys = new AtomicIntegerArray(POINT_COUNT);

    private final AtomicBoolean enabled = new AtomicBoolean(false);

    // Reference point for automatic light/dark mode detection
    private final AtomicInteger referenceX = new AtomicInteger(150);
    private final AtomicInteger referenceY = new AtomicInteger(150);
    private final AtomicInteger referenceThreshold = new AtomicInteger(-8000000);
    private final AtomicInteger referenceHysteresis = new AtomicInteger(500000);

    // Per-mode segment detection thresholds
    private final AtomicInteger darkThreshold = new AtomicInteger(-2500000);
    private final AtomicInteger lightThreshold = new AtomicInteger(-6000000);

    // Transient state updated on every analysis so the UI can display it
    private final AtomicInteger ambientBrightness = new AtomicInteger(0);
    private final AtomicBoolean lightMode = new AtomicBoolean(false);

    public ImmerManagerData() {
        setDefaults();
    }

    @PostConstruct
    private void load() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            return;
        }
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);

            // Legacy migration: if old offset keys exist, compute individual points from them
            String legacyOffsetX = props.getProperty("offsetX");
            String legacyOffsetY = props.getProperty("offsetY");
            if (legacyOffsetX != null && legacyOffsetY != null) {
                try {
                    int offsetX = Integer.parseInt(legacyOffsetX);
                    int offsetY = Integer.parseInt(legacyOffsetY);
                    for (int i = 0; i < POINT_COUNT; i++) {
                        xs.set(i, DEFAULT_X[i] + offsetX);
                        ys.set(i, DEFAULT_Y[i] + offsetY);
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Could not parse legacy offset data from {}: {}", DATA_FILE, e.getMessage());
                    setDefaults();
                }
            } else {
                for (int i = 0; i < POINT_COUNT; i++) {
                    xs.set(i, Integer.parseInt(props.getProperty("x" + i, String.valueOf(DEFAULT_X[i]))));
                    ys.set(i, Integer.parseInt(props.getProperty("y" + i, String.valueOf(DEFAULT_Y[i]))));
                }
            }

            enabled.set(Boolean.parseBoolean(props.getProperty("enabled", "false")));
            referenceX.set(Integer.parseInt(props.getProperty("referenceX", "150")));
            referenceY.set(Integer.parseInt(props.getProperty("referenceY", "150")));
            referenceThreshold.set(Integer.parseInt(props.getProperty("referenceThreshold", "-8000000")));
            referenceHysteresis.set(Integer.parseInt(props.getProperty("referenceHysteresis", "500000")));
            darkThreshold.set(Integer.parseInt(props.getProperty("darkThreshold", "-2500000")));
            lightThreshold.set(Integer.parseInt(props.getProperty("lightThreshold", "-6000000")));
        } catch (IOException | NumberFormatException e) {
            logger.warn("Could not load offset data from {}: {}", DATA_FILE, e.getMessage());
            setDefaults();
        }
    }

    private void setDefaults() {
        for (int i = 0; i < POINT_COUNT; i++) {
            xs.set(i, DEFAULT_X[i]);
            ys.set(i, DEFAULT_Y[i]);
        }
    }

    private void save() {
        Properties props = new Properties();
        for (int i = 0; i < POINT_COUNT; i++) {
            props.setProperty("x" + i, String.valueOf(xs.get(i)));
            props.setProperty("y" + i, String.valueOf(ys.get(i)));
        }
        props.setProperty("enabled", String.valueOf(enabled.get()));
        props.setProperty("referenceX", String.valueOf(referenceX.get()));
        props.setProperty("referenceY", String.valueOf(referenceY.get()));
        props.setProperty("referenceThreshold", String.valueOf(referenceThreshold.get()));
        props.setProperty("referenceHysteresis", String.valueOf(referenceHysteresis.get()));
        props.setProperty("darkThreshold", String.valueOf(darkThreshold.get()));
        props.setProperty("lightThreshold", String.valueOf(lightThreshold.get()));
        File file = new File(DATA_FILE);
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            logger.warn("Could not create directory {}", parent.getAbsolutePath());
            return;
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            props.store(fos, null);
        } catch (IOException e) {
            logger.warn("Could not save offset data to {}: {}", DATA_FILE, e.getMessage());
        }
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

    public int getX(int index) {
        return xs.get(index);
    }

    public int getY(int index) {
        return ys.get(index);
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

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
        save();
    }

    public int getReferenceX() {
        return referenceX.get();
    }

    public void setReferenceX(int referenceX) {
        this.referenceX.set(referenceX);
        save();
    }

    public int getReferenceY() {
        return referenceY.get();
    }

    public void setReferenceY(int referenceY) {
        this.referenceY.set(referenceY);
        save();
    }

    public int getReferenceThreshold() {
        return referenceThreshold.get();
    }

    public void setReferenceThreshold(int referenceThreshold) {
        this.referenceThreshold.set(referenceThreshold);
        save();
    }

    public int getReferenceHysteresis() {
        return referenceHysteresis.get();
    }

    public void setReferenceHysteresis(int referenceHysteresis) {
        this.referenceHysteresis.set(referenceHysteresis);
        save();
    }

    public int getDarkThreshold() {
        return darkThreshold.get();
    }

    public void setDarkThreshold(int darkThreshold) {
        this.darkThreshold.set(darkThreshold);
        save();
    }

    public int getLightThreshold() {
        return lightThreshold.get();
    }

    public void setLightThreshold(int lightThreshold) {
        this.lightThreshold.set(lightThreshold);
        save();
    }

    public int getAmbientBrightness() {
        return ambientBrightness.get();
    }

    public void setAmbientBrightness(int ambientBrightness) {
        this.ambientBrightness.set(ambientBrightness);
    }

    public boolean isLightMode() {
        return lightMode.get();
    }

    public void setLightMode(boolean lightMode) {
        this.lightMode.set(lightMode);
    }
}
