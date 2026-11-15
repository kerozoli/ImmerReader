package com.keroleap.immerreader.Scheduler;

import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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

import com.keroleap.immerreader.AristonRest;
import com.keroleap.immerreader.ErrorType;
import com.keroleap.immerreader.Service.AristonAnalyzerService;
import com.keroleap.immerreader.Service.CameraUnavailableException;
import com.keroleap.immerreader.SharedData.AristonData;
import com.keroleap.immerreader.SharedData.AristonManagerData;
import com.keroleap.immerreader.SharedData.ErrorStatistics;
import com.keroleap.immerreader.SharedData.SchedulerHealthTracker;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class AristonScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AristonScheduler.class);
    private static final int HISTORY_SIZE = 5;
    private static final int DEADBAND = 2;
    private static final int ERROR_THRESHOLD = 5;
    private static final long DEFAULT_INTERVAL_MS = 15000L;
    private static final long MIN_INTERVAL_MS = 1000L;

    @Autowired
    private AristonData aristonData;

    @Autowired
    private AristonAnalyzerService aristonAnalyzerService;

    @Autowired
    private AristonManagerData aristonManagerData;

    @Autowired
    private ErrorStatistics errorStatistics;

    @Autowired
    private SchedulerHealthTracker schedulerHealthTracker;

    @Autowired
    private TaskScheduler taskScheduler;

    @Value("${camera.ariston.url:http://192.168.1.191/cgi/jpg/image.cgi}")
    private String cameraUrl = "http://192.168.1.191/cgi/jpg/image.cgi";

    /**
     * Dedicated thread for the blocking camera read. It is deliberately separate
     * from the Spring {@link TaskScheduler}, which is shared with other schedulers.
     * A SynchronousQueue rejects a read when the previous one is still running, so
     * slow cameras cannot build up a backlog of queued reads.
     */
    private final ThreadPoolExecutor readExecutor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS, new SynchronousQueue<>(),
            runnable -> {
                Thread thread = new Thread(runnable, "ariston-reader");
                thread.setDaemon(true);
                return thread;
            });

    private final AtomicInteger consecutiveErrors = new AtomicInteger(0);
    private volatile ErrorType lastErrorType = null;
    private final Deque<Integer> history = new ArrayDeque<>();
    private volatile int lastPublished = -1;
    private volatile ScheduledFuture<?> scheduledFuture;
    private final AtomicLong configuredIntervalMs = new AtomicLong(DEFAULT_INTERVAL_MS);

    @PostConstruct
    public void init() {
        scheduleRead();
    }

    private void scheduleRead() {
        cancelExisting();
        configuredIntervalMs.set(resolveIntervalMs());
        Trigger trigger = new Trigger() {
            @Override
            public Instant nextExecution(TriggerContext triggerContext) {
                Instant lastCompletion = triggerContext.lastCompletion();
                long nextMs = (lastCompletion != null ? lastCompletion.toEpochMilli() : System.currentTimeMillis())
                        + configuredIntervalMs.get();
                return Instant.ofEpochMilli(nextMs);
            }
        };
        scheduledFuture = taskScheduler.schedule(this::dispatchRead, trigger);
    }

    private void dispatchRead() {
        try {
            readExecutor.execute(this::AristonScheduledRead);
        } catch (RejectedExecutionException e) {
            logger.debug("Skipping Ariston read, previous read is still running.");
        }
    }

    private void cancelExisting() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
    }

    private long resolveIntervalMs() {
        int seconds = aristonManagerData.getIntervalSeconds();
        long intervalMs = seconds > 0 ? seconds * 1000L : DEFAULT_INTERVAL_MS;
        return Math.max(MIN_INTERVAL_MS, intervalMs);
    }

    public void AristonScheduledRead() {
        if (!aristonManagerData.isEnabled()) {
            consecutiveErrors.set(0);
            lastErrorType = null;
            return;
        }

        if (taskScheduler != null) {
            long currentIntervalMs = resolveIntervalMs();
            if (currentIntervalMs != configuredIntervalMs.get()) {
                scheduleRead();
                return;
            }
        }

        try {
            BufferedImage image = aristonAnalyzerService.getBufferedImage(cameraUrl);
            AristonRest result = aristonAnalyzerService.getAristonRestData(image,
                    aristonManagerData.getStartX(), aristonManagerData.getStartY(),
                    aristonManagerData.getControlX(), aristonManagerData.getControlY(),
                    aristonManagerData.getEndX(), aristonManagerData.getEndY());

            int smoothed = smoothPercentage(result.getPercentage());
            result.setPercentage(smoothed);
            result.setError(false);
            result.setErrorType(null);
            result.setLastSuccessMs(System.currentTimeMillis());
            aristonData.setAristonRest(result);
            consecutiveErrors.set(0);
            lastErrorType = null;
            schedulerHealthTracker.recordSuccess("Ariston");
        } catch (CameraUnavailableException e) {
            logger.warn("Ariston camera unavailable: {}", e.getMessage());
            handleError(e.isTimedOut() ? ErrorType.TIMEOUT : ErrorType.FETCH_ERROR);
        } catch (Exception e) {
            logger.error("Error fetching Ariston data: {}", e.getMessage());
            handleError(ErrorType.FETCH_ERROR);
        }
    }

    private void handleError(ErrorType errorType) {
        errorStatistics.recordError("Ariston", errorType);
        schedulerHealthTracker.recordError("Ariston", errorType);
        lastErrorType = errorType;
        int streak = consecutiveErrors.incrementAndGet();
        if (streak >= ERROR_THRESHOLD) {
            AristonRest current = aristonData.getAristonRest();
            if (current != null) {
                current.setError(true);
                current.setErrorType(errorType);
            }
        }
    }

    private synchronized int smoothPercentage(int raw) {
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
        cancelExisting();
        readExecutor.shutdownNow();
    }
}
