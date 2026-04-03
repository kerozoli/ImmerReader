package com.keroleap.immerreader.SharedData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class ImmerOffsetData {
    private static final Logger logger = LoggerFactory.getLogger(ImmerOffsetData.class);
    private static final String DATA_FILE = "/data/offset.properties";

    private final AtomicInteger offsetX = new AtomicInteger(0);
    private final AtomicInteger offsetY = new AtomicInteger(0);

    @PostConstruct
    private void load() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
                offsetX.set(Integer.parseInt(props.getProperty("offsetX", "0")));
                offsetY.set(Integer.parseInt(props.getProperty("offsetY", "0")));
            } catch (IOException | NumberFormatException e) {
                logger.warn("Could not load offset data from {}: {}", DATA_FILE, e.getMessage());
            }
        }
    }

    private void save() {
        Properties props = new Properties();
        props.setProperty("offsetX", String.valueOf(offsetX.get()));
        props.setProperty("offsetY", String.valueOf(offsetY.get()));
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
}
