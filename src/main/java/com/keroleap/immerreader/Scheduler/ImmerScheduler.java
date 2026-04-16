package com.keroleap.immerreader.Scheduler;

import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.keroleap.immerreader.ImmerRest;
import com.keroleap.immerreader.Service.ImmerAnalyzerService;
import com.keroleap.immerreader.SharedData.ImmerData;
import com.keroleap.immerreader.SharedData.ImmerManagerData;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class ImmerScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ImmerScheduler.class);
    private static final int DEFAULT_DELAY_MS = 2000;
    private static final int MAX_DELAY_MS = 60000;
    private static final int DELAY_INCREMENT_MS = 1000;

    @Autowired
    private ImmerData immerData;

    @Autowired
    private ImmerAnalyzerService immerAnalyzerService;

    @Autowired
    private ImmerManagerData immerManagerData;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicInteger currentDelayMs = new AtomicInteger(DEFAULT_DELAY_MS);

    @PostConstruct
    public void init() {
        scheduleNextRead();
    }

    private void scheduleNextRead() {
        scheduler.schedule(this::ImmerScheduledRead, currentDelayMs.get(), TimeUnit.MILLISECONDS);
    }

    private void ImmerScheduledRead() {
        int offsetX = immerManagerData.getOffsetX();
        int offsetY = immerManagerData.getOffsetY();
        Future<ImmerRest> future = Executors.newSingleThreadExecutor().submit(() -> {
            BufferedImage cachedImage = immerAnalyzerService.getBufferedImage("http://192.168.1.196/image/jpeg.cgi");
            return immerAnalyzerService.getImmerRestData(cachedImage, offsetX, offsetY);
        });

        try {
            ImmerRest result = future.get(1500, TimeUnit.MILLISECONDS);
            immerData.setImmerRest(result);
            onReadSuccess();
        } catch (TimeoutException e) {
            future.cancel(true);
            logger.warn("Timeout fetching Immer data, keeping previous value.");
            onReadError();
        } catch (Exception e) {
            logger.error("Error fetching Immer data: {}", e.getMessage());
            onReadError();
        }
    }

    private void onReadSuccess() {
        currentDelayMs.set(DEFAULT_DELAY_MS);
        scheduleNextRead();
    }

    private void onReadError() {
        int newDelay = Math.min(currentDelayMs.get() + DELAY_INCREMENT_MS, MAX_DELAY_MS);
        currentDelayMs.set(newDelay);
        logger.info("Increased delay to {} ms due to connection error", newDelay);
        scheduleNextRead();
    }

    @PreDestroy
    public void destroy() {
        logger.info("Shutting down ImmerScheduler.");
        scheduler.shutdownNow();
    }
}

