package com.keroleap.immerreader.Scheduler;

import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.keroleap.immerreader.AristonRest;
import com.keroleap.immerreader.ErrorType;
import com.keroleap.immerreader.Service.AristonAnalyzerService;
import com.keroleap.immerreader.SharedData.AristonData;
import com.keroleap.immerreader.SharedData.AristonManagerData;
import com.keroleap.immerreader.SharedData.ErrorStatistics;

import jakarta.annotation.PreDestroy;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AristonScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AristonScheduler.class);
    private static final int HISTORY_SIZE = 5;
    private static final int DEADBAND = 2;

    @Autowired
    private AristonData aristonData;

    @Autowired
    private AristonAnalyzerService aristonAnalyzerService;

    @Autowired
    private AristonManagerData aristonManagerData;

    @Autowired
    private ErrorStatistics errorStatistics;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicInteger consecutiveErrors = new AtomicInteger(0);
    private volatile ErrorType lastErrorType = null;
    private final Deque<Integer> history = new ArrayDeque<>();
    private volatile int lastPublished = -1;

    @Scheduled(fixedRate = 15000)
    public void AristonScheduledRead() {
        if (!aristonManagerData.isEnabled()) {
            consecutiveErrors.set(0);
            lastErrorType = null;
            return;
        }

        Future<AristonRest> future = executor.submit(() -> {
            BufferedImage cachedImage = aristonAnalyzerService.getBufferedImage("http://192.168.1.191/cgi/jpg/image.cgi");
            return aristonAnalyzerService.getAristonRestData(cachedImage,
                    aristonManagerData.getStartX(), aristonManagerData.getStartY(),
                    aristonManagerData.getControlX(), aristonManagerData.getControlY(),
                    aristonManagerData.getEndX(), aristonManagerData.getEndY());
        });

        try {
            AristonRest result = future.get(10000, TimeUnit.MILLISECONDS);
            int smoothed = smoothPercentage(result.getPercentage());
            result.setPercentage(smoothed);
            result.setError(false);
            result.setErrorType(null);
            aristonData.setAristonRest(result);
            consecutiveErrors.set(0);
            lastErrorType = null;
        } catch (TimeoutException e) {
            future.cancel(true);
            logger.warn("Timeout fetching Ariston data, keeping previous value.");
            handleError(ErrorType.TIMEOUT);
        } catch (Exception e) {
            logger.error("Error fetching Ariston data: {}", e.getMessage());
            handleError(ErrorType.FETCH_ERROR);
        }
    }

    private void handleError(ErrorType errorType) {
        errorStatistics.recordError("Ariston", errorType);
        if (errorType.equals(lastErrorType)) {
            consecutiveErrors.incrementAndGet();
        } else {
            lastErrorType = errorType;
            consecutiveErrors.set(1);
        }
        if (consecutiveErrors.get() >= 5) {
            aristonData.getAristonRest().setError(true);
            aristonData.getAristonRest().setErrorType(errorType);
        }
    }

    private int smoothPercentage(int raw) {
        history.addLast(raw);
        while (history.size() > HISTORY_SIZE) {
            history.removeFirst();
        }
        int median = computeMedian(history);
        if (lastPublished < 0 || Math.abs(median - lastPublished) > DEADBAND) {
            lastPublished = median;
        }
        return lastPublished;
    }

    private int computeMedian(Deque<Integer> values) {
        if (values.isEmpty()) {
            return 0;
        }
        int[] sorted = values.stream().mapToInt(Integer::intValue).sorted().toArray();
        int middle = sorted.length / 2;
        if (sorted.length % 2 == 1) {
            return sorted[middle];
        }
        return (sorted[middle - 1] + sorted[middle]) / 2;
    }

    @PreDestroy
    public void destroy() {
        logger.info("Shutting down AristonScheduler executor.");
        executor.shutdownNow();
    }
}
