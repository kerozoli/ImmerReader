package com.keroleap.immerreader.Scheduler;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.keroleap.immerreader.ChickenRest;
import com.keroleap.immerreader.ErrorType;
import com.keroleap.immerreader.Service.ChickenAnalyzerService;
import com.keroleap.immerreader.SharedData.ChickenData;
import com.keroleap.immerreader.SharedData.ChickenManagerData;
import com.keroleap.immerreader.SharedData.ErrorStatistics;

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

    @Autowired
    private ErrorStatistics errorStatistics;

    private ScheduledExecutorService scheduler;
    private final AtomicInteger consecutiveErrors = new AtomicInteger(0);
    private volatile ErrorType lastErrorType = null;

    @PostConstruct
    public void init() {
        logger.info("Chicken scheduler initialized.");
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "ChickenScheduler"));
        scheduler.scheduleAtFixedRate(this::run, INITIAL_DELAY_SECONDS, PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    private void run() {
        if (!chickenManagerData.isEnabled()) {
            consecutiveErrors.set(0);
            lastErrorType = null;
            return;
        }
        try {
            BufferedImage image = chickenAnalyzerService.getBufferedImage(cameraUrl);
            ChickenRest rest = chickenAnalyzerService.getChickenRestData(image, chickenManagerData);
            if (rest.isError()) {
                handleError(rest.getErrorType());
                return;
            }
            List<Integer> counts = rest.getNestCounts();
            chickenData.setConfiguredCount(counts.size());
            chickenData.addCounts(counts);
            consecutiveErrors.set(0);
            lastErrorType = null;
        } catch (Exception e) {
            logger.error("Error fetching Chicken data: {}", e.getMessage(), e);
            handleError(ErrorType.FETCH_ERROR);
        }
    }

    private void handleError(ErrorType errorType) {
        errorStatistics.recordError("chicken", errorType);
        if (errorType.equals(lastErrorType)) {
            consecutiveErrors.incrementAndGet();
        } else {
            lastErrorType = errorType;
            consecutiveErrors.set(1);
        }
        if (consecutiveErrors.get() >= 5) {
            ChickenRest current = chickenData.getChickenRest();
            current.setError(true);
            current.setErrorType(errorType);
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
