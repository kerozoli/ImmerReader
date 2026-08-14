package com.keroleap.immerreader.Scheduler;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.keroleap.immerreader.ChickenRest;
import com.keroleap.immerreader.Service.ChickenAnalyzerService;
import com.keroleap.immerreader.SharedData.ChickenData;
import com.keroleap.immerreader.SharedData.ChickenManagerData;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class ChickenScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ChickenScheduler.class);
    private static final long INITIAL_DELAY_SECONDS = 5;
    private static final long PERIOD_SECONDS = 30;

    @Value("${camera.chicken.url}")
    private String cameraUrl;

    @Autowired
    private ChickenAnalyzerService chickenAnalyzerService;

    @Autowired
    private ChickenData chickenData;

    @Autowired
    private ChickenManagerData chickenManagerData;

    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void init() {
        logger.info("Chicken scheduler initialized.");
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "ChickenScheduler"));
        scheduler.scheduleAtFixedRate(this::run, INITIAL_DELAY_SECONDS, PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    private void run() {
        if (!chickenManagerData.isEnabled()) {
            return;
        }
        try {
            BufferedImage image = chickenAnalyzerService.getBufferedImage(cameraUrl);
            ChickenRest rest = chickenAnalyzerService.getChickenRestData(image, chickenManagerData);
            List<Integer> counts = rest.getNestCounts();
            chickenData.setConfiguredCount(counts.size());
            chickenData.addCounts(counts);
        } catch (Exception e) {
            logger.error("Error fetching Chicken data: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down ChickenScheduler.");
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
