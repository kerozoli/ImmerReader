package com.keroleap.immerreader.Scheduler;

import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.keroleap.immerreader.ErrorType;
import com.keroleap.immerreader.ImmerRest;
import com.keroleap.immerreader.Service.CameraUnavailableException;
import com.keroleap.immerreader.Service.ImmerAnalyzerService;
import com.keroleap.immerreader.SharedData.ImmerData;
import com.keroleap.immerreader.SharedData.ImmerManagerData;
import com.keroleap.immerreader.SharedData.ImmerStatistics;
import com.keroleap.immerreader.SharedData.ErrorStatistics;
import com.keroleap.immerreader.SharedData.SchedulerHealthTracker;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class ImmerScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ImmerScheduler.class);
    private static final int DEFAULT_DELAY_MS = 2000;
    private static final int MAX_DELAY_MS = 60000;
    private static final int DELAY_INCREMENT_MS = 1000;
    private static final int ERROR_THRESHOLD = 5;

    @Autowired
    private ImmerData immerData;

    @Autowired
    private ImmerAnalyzerService immerAnalyzerService;

    @Autowired
    private ImmerManagerData immerManagerData;

    @Autowired
    private ImmerStatistics immerStatistics;

    @Autowired
    private ErrorStatistics errorStatistics;

    @Autowired
    private SchedulerHealthTracker schedulerHealthTracker;

    @Value("${camera.immer.url:http://192.168.1.196/image/jpeg.cgi}")
    private String cameraUrl = "http://192.168.1.196/image/jpeg.cgi";

    /**
     * Single scheduler thread. Reads run on this thread and the next read is
     * scheduled only after the current one completes, so a slow camera delays the
     * next read instead of piling work up behind it.
     */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "immer-scheduler");
        thread.setDaemon(true);
        return thread;
    });

    private final AtomicInteger currentDelayMs = new AtomicInteger(DEFAULT_DELAY_MS);
    private final AtomicInteger consecutiveErrors = new AtomicInteger(0);
    private volatile ErrorType lastErrorType = null;

    @PostConstruct
    public void init() {
        scheduleNextRead();
    }

    private void scheduleNextRead() {
        if (scheduler.isShutdown()) {
            return;
        }
        try {
            scheduler.schedule(this::scheduledRead, currentDelayMs.get(), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            logger.debug("Immer scheduler is shutting down, not scheduling another read.");
        }
    }

    private void scheduledRead() {
        try {
            readOnce();
        } catch (RuntimeException e) {
            logger.error("Unexpected error during Immer read: {}", e.getMessage(), e);
            handleError(ErrorType.FETCH_ERROR);
        } finally {
            scheduleNextRead();
        }
    }

    /**
     * Performs one read cycle. Package visible so it can be exercised directly by
     * tests without waiting for the scheduled delay.
     */
    void readOnce() {
        if (!immerManagerData.isEnabled()) {
            currentDelayMs.set(DEFAULT_DELAY_MS);
            consecutiveErrors.set(0);
            lastErrorType = null;
            return;
        }

        int[] xs = immerManagerData.getXs();
        int[] ys = immerManagerData.getYs();

        try {
            BufferedImage cachedImage = immerAnalyzerService.getBufferedImage(cameraUrl);
            ImmerRest result = immerAnalyzerService.getImmerRestData(cachedImage, xs, ys);
            result.setError(false);
            result.setErrorType(null);
            result.setLastSuccessMs(System.currentTimeMillis());
            immerData.setImmerRest(result);
            immerStatistics.record(result);
            onReadSuccess();
        } catch (CameraUnavailableException e) {
            logger.warn("Immer camera unavailable: {}", e.getMessage());
            handleError(e.isTimedOut() ? ErrorType.TIMEOUT : ErrorType.FETCH_ERROR);
        } catch (Exception e) {
            logger.error("Error fetching Immer data: {}", e.getMessage());
            handleError(ErrorType.FETCH_ERROR);
        }
    }

    private void onReadSuccess() {
        consecutiveErrors.set(0);
        lastErrorType = null;
        currentDelayMs.set(DEFAULT_DELAY_MS);
        schedulerHealthTracker.recordSuccess("Immer");
    }

    private void onReadError() {
        int newDelay = Math.min(currentDelayMs.get() + DELAY_INCREMENT_MS, MAX_DELAY_MS);
        currentDelayMs.set(newDelay);
        logger.info("Increased delay to {} ms due to connection error", newDelay);
    }

    private void handleError(ErrorType errorType) {
        errorStatistics.recordError("Immer", errorType);
        schedulerHealthTracker.recordError("Immer", errorType);
        lastErrorType = errorType;
        if (consecutiveErrors.incrementAndGet() >= ERROR_THRESHOLD) {
            ImmerRest current = immerData.getImmerRest();
            if (current != null) {
                current.setError(true);
                current.setErrorType(errorType);
            }
        }
        onReadError();
    }

    @PreDestroy
    public void destroy() {
        logger.info("Shutting down ImmerScheduler.");
        scheduler.shutdownNow();
    }
}
