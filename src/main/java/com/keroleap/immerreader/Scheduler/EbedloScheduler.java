package com.keroleap.immerreader.Scheduler;

import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.stereotype.Component;

import com.keroleap.immerreader.EbedloRest;
import com.keroleap.immerreader.ErrorType;
import com.keroleap.immerreader.Service.EbedloAnalyzerService;
import com.keroleap.immerreader.SharedData.EbedloData;
import com.keroleap.immerreader.SharedData.EbedloManagerData;
import com.keroleap.immerreader.SharedData.ErrorStatistics;
import com.keroleap.immerreader.SharedData.SchedulerHealthTracker;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class EbedloScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EbedloScheduler.class);

    @Value("${camera.ebedlo.url}")
    private String cameraUrl;

    @Autowired
    private EbedloData ebedloData;

    @Autowired
    private EbedloAnalyzerService ebedloAnalyzerService;

    @Autowired
    private EbedloManagerData ebedloManagerData;

    @Autowired
    private ErrorStatistics errorStatistics;

    @Autowired
    private SchedulerHealthTracker schedulerHealthTracker;

    @Autowired
    private TaskScheduler taskScheduler;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicInteger consecutiveErrors = new AtomicInteger(0);
    private volatile ErrorType lastErrorType = null;
    private volatile ScheduledFuture<?> scheduledFuture;
    private final AtomicLong configuredIntervalMs = new AtomicLong(15000);

    @PostConstruct
    public void init() {
        scheduleRead();
    }

    private void scheduleRead() {
        cancelExisting();
        configuredIntervalMs.set(ebedloManagerData.getIntervalSeconds() * 1000L);
        Trigger trigger = new Trigger() {
            @Override
            public Instant nextExecution(TriggerContext triggerContext) {
                Instant lastCompletion = triggerContext.lastCompletion();
                long nextMs = (lastCompletion != null ? lastCompletion.toEpochMilli() : System.currentTimeMillis()) + configuredIntervalMs.get();
                return Instant.ofEpochMilli(nextMs);
            }
        };
        scheduledFuture = taskScheduler.schedule(this::EbedloScheduledRead, trigger);
    }

    private void cancelExisting() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
    }

    public void EbedloScheduledRead() {
        if (!ebedloManagerData.isEnabled()) {
            consecutiveErrors.set(0);
            lastErrorType = null;
            return;
        }

        if (taskScheduler != null) {
            long currentIntervalMs = ebedloManagerData.getIntervalSeconds() * 1000L;
            if (currentIntervalMs != configuredIntervalMs.get()) {
                scheduleRead();
                return;
            }
        }

        Future<EbedloRest> future = executor.submit(() -> {
            BufferedImage cachedImage = ebedloAnalyzerService.getBufferedImage(cameraUrl);
            return ebedloAnalyzerService.getEbedloRestData(cachedImage, ebedloManagerData);
        });

        try {
            EbedloRest result = future.get(5000, TimeUnit.MILLISECONDS);
            if (result.isError()) {
                handleError(result.getErrorType());
                return;
            }
            result.setError(false);
            result.setErrorType(null);

            ebedloData.addOnDecision(result.isOn());
            boolean smoothedOn = ebedloData.getSmoothedOn();

            EbedloRest stored = new EbedloRest();
            stored.setOn(smoothedOn);
            stored.setAverageValue(result.getAverageValue());
            stored.setError(false);
            stored.setErrorType(null);
            ebedloData.setEbedloRest(stored);

            consecutiveErrors.set(0);
            lastErrorType = null;
            schedulerHealthTracker.recordSuccess("Ebedlo");
        } catch (TimeoutException e) {
            future.cancel(true);
            logger.warn("Timeout fetching Ebedlo data, keeping previous value.");
            handleError(ErrorType.TIMEOUT);
        } catch (Exception e) {
            logger.error("Error fetching Ebedlo data: {}", e.getMessage());
            handleError(ErrorType.FETCH_ERROR);
        }
    }

    private void handleError(ErrorType errorType) {
        errorStatistics.recordError("Ebedlo", errorType);
        schedulerHealthTracker.recordError("Ebedlo", errorType);
        if (errorType.equals(lastErrorType)) {
            consecutiveErrors.incrementAndGet();
        } else {
            lastErrorType = errorType;
            consecutiveErrors.set(1);
        }
        if (consecutiveErrors.get() >= 5) {
            ebedloData.getEbedloRest().setError(true);
            ebedloData.getEbedloRest().setErrorType(errorType);
        }
    }

    @PreDestroy
    public void destroy() {
        logger.info("Shutting down EbedloScheduler executor.");
        cancelExisting();
        executor.shutdownNow();
    }
}
