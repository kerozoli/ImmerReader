package com.keroleap.immerreader.SharedData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class ImmerManagerData {
    private static final Logger logger = LoggerFactory.getLogger(ImmerManagerData.class);
    private static final String DATA_FILE = "/data/offset.properties";

    // Offset for the sampling grid
    private final AtomicInteger offsetX = new AtomicInteger(0);
    private final AtomicInteger offsetY = new AtomicInteger(0);
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

    @PostConstruct
    private void load() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
                offsetX.set(Integer.parseInt(props.getProperty("offsetX", "0")));
                offsetY.set(Integer.parseInt(props.getProperty("offsetY", "0")));
                enabled.set(Boolean.parseBoolean(props.getProperty("enabled", "false")));
                referenceX.set(Integer.parseInt(props.getProperty("referenceX", "150")));
                referenceY.set(Integer.parseInt(props.getProperty("referenceY", "150")));
                referenceThreshold.set(Integer.parseInt(props.getProperty("referenceThreshold", "-8000000")));
                referenceHysteresis.set(Integer.parseInt(props.getProperty("referenceHysteresis", "500000")));
                darkThreshold.set(Integer.parseInt(props.getProperty("darkThreshold", "-2500000")));
                lightThreshold.set(Integer.parseInt(props.getProperty("lightThreshold", "-6000000")));
            } catch (IOException | NumberFormatException e) {
                logger.warn("Could not load offset data from {}: {}", DATA_FILE, e.getMessage());
            }
        }
    }

    private void save() {
        Properties props = new Properties();
        props.setProperty("offsetX", String.valueOf(offsetX.get()));
        props.setProperty("offsetY", String.valueOf(offsetY.get()));
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

    public int getOffsetX() {
        return offsetX.get();
    }

    public void setOffsetX(int offsetX) {
        this.offsetX.set(offsetX);
        save();
    }

    public int getOffsetY() {
        return offsetY.get();
    }

    public void setOffsetY(int offsetY) {
        this.offsetY.set(offsetY);
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
