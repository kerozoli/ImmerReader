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
public class AristonManagerData {
    private static final Logger logger = LoggerFactory.getLogger(AristonManagerData.class);
    private static final String DATA_FILE = "/data/ariston.properties";

    private final AtomicInteger startX = new AtomicInteger(160);
    private final AtomicInteger startY = new AtomicInteger(160);
    private final AtomicInteger controlX = new AtomicInteger(190);
    private final AtomicInteger controlY = new AtomicInteger(130);
    private final AtomicInteger endX = new AtomicInteger(220);
    private final AtomicInteger endY = new AtomicInteger(180);
    private final AtomicBoolean enabled = new AtomicBoolean(false);

    @PostConstruct
    private void load() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
                startX.set(Integer.parseInt(props.getProperty("startX", "160")));
                startY.set(Integer.parseInt(props.getProperty("startY", "160")));
                controlX.set(Integer.parseInt(props.getProperty("controlX", "190")));
                controlY.set(Integer.parseInt(props.getProperty("controlY", "130")));
                endX.set(Integer.parseInt(props.getProperty("endX", "220")));
                endY.set(Integer.parseInt(props.getProperty("endY", "180")));
                enabled.set(Boolean.parseBoolean(props.getProperty("enabled", "false")));
            } catch (IOException | NumberFormatException e) {
                logger.warn("Could not load offset data from {}: {}", DATA_FILE, e.getMessage());
            }
        }
    }

    private void save() {
        Properties props = new Properties();
        props.setProperty("startX", String.valueOf(startX.get()));
        props.setProperty("startY", String.valueOf(startY.get()));
        props.setProperty("controlX", String.valueOf(controlX.get()));
        props.setProperty("controlY", String.valueOf(controlY.get()));
        props.setProperty("endX", String.valueOf(endX.get()));
        props.setProperty("endY", String.valueOf(endY.get()));
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
            logger.warn("Could not save offset data to {}: {}", DATA_FILE, e.getMessage());
        }
    }

    public int getStartX() {
        return startX.get();
    }

    public void setStartX(int startX) {
        this.startX.set(startX);
        save();
    }

    public int getStartY() {
        return startY.get();
    }

    public void setStartY(int startY) {
        this.startY.set(startY);
        save();
    }

    public int getControlX() {
        return controlX.get();
    }

    public void setControlX(int controlX) {
        this.controlX.set(controlX);
        save();
    }

    public int getControlY() {
        return controlY.get();
    }

    public void setControlY(int controlY) {
        this.controlY.set(controlY);
        save();
    }

    public int getEndX() {
        return endX.get();
    }

    public void setEndX(int endX) {
        this.endX.set(endX);
        save();
    }

    public int getEndY() {
        return endY.get();
    }

    public void setEndY(int endY) {
        this.endY.set(endY);
        save();
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
        save();
    }
}
