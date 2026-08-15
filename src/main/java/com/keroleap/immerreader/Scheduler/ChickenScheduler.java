package com.keroleap.immerreader.Scheduler;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import com.keroleap.immerreader.SharedData.SchedulerHealthTracker;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class ChickenScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ChickenScheduler.class);
    private static final long INITIAL_DELAY_SECONDS = 5;
    private static final long TIMEOUT_MS = 20000;

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

    @Autowired
    private SchedulerHealthTracker schedulerHealthTracker;

    private ScheduledExecutorService scheduler;
    private ExecutorService executor;
    private final AtomicInteger consecutiveErrors = new AtomicInteger(0);
    private volatile ErrorType lastErrorType = null;

    @PostConstruct
    public void init() {
        logger.info("Chicken scheduler initialized.");
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "ChickenScheduler"));
        executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "ChickenAnalyzer"));
        scheduleRun();
    }

    private void scheduleRun() {
        long periodSeconds = chickenManagerData.getIntervalSeconds();
        scheduler.scheduleAtFixedRate(this::run, INITIAL_DELAY_SECONDS, periodSeconds, TimeUnit.SECONDS);
    }

    private void run() {
        if (!chickenManagerData.isEnabled()) {
            consecutiveErrors.set(0);
            lastErrorType = null;
            chickenData.addCounts(java.util.Collections.nCopies(chickenManagerData.getNestCount(), 0));
            return;
        }

        Future<ChickenRest> future = executor.submit(() -> {
            BufferedImage image = chickenAnalyzerService.getBufferedImage(cameraUrl);
            return chickenAnalyzerService.getChickenRestData(image, chickenManagerData);
        });

        try {
            ChickenRest rest = future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (rest.isError()) {
                handleError(rest.getErrorType());
                return;
            }
            List<Integer> counts = rest.getNestCounts();
            chickenData.setConfiguredCount(counts.size());
            chickenData.setIntervalSeconds(chickenManagerData.getIntervalSeconds());
            chickenData.addCounts(counts);
            consecutiveErrors.set(0);
            lastErrorType = null;
            schedulerHealthTracker.recordSuccess("Chicken");
        } catch (TimeoutException e) {
            future.cancel(true);
            logger.warn("Timeout fetching Chicken data, keeping previous value.");
            handleError(ErrorType.TIMEOUT);
        } catch (Exception e) {
            logger.error("Error fetching Chicken data: {}", e.getMessage(), e);
            handleError(ErrorType.FETCH_ERROR);
        }
    }

    private void handleError(ErrorType errorType) {
        errorStatistics.recordError("Chicken", errorType);
        schedulerHealthTracker.recordError("Chicken", errorType);
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
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
