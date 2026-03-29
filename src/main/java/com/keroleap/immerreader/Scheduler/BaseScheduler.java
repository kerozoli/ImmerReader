package com.keroleap.immerreader.Scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Base scheduler that handles timeout-protected execution of image processing tasks.
 * @param <T> The result type
 */
public abstract class BaseScheduler<T> {
    private static final Logger logger = LoggerFactory.getLogger(BaseScheduler.class);

    /**
     * Executes a task with timeout protection.
     * @param task The task to execute
     * @param timeoutMs Timeout in milliseconds
     * @param taskName Name of the task for logging
     * @return The result, or null on failure
     */
    protected T executeWithTimeout(Supplier<T> task, long timeoutMs, String taskName) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<T> future = executor.submit(task::get);

        try {
            T result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            executor.shutdown();
            return result;
        } catch (TimeoutException e) {
            future.cancel(true);
            executor.shutdownNow();
            logger.warn("Timeout fetching {} data", taskName);
            return null;
        } catch (InterruptedException e) {
            future.cancel(true);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            logger.error("Interrupted while fetching {} data", taskName, e);
            return null;
        } catch (Exception e) {
            executor.shutdownNow();
            logger.error("Error fetching {} data: {}", taskName, e.getMessage());
            return null;
        }
    }
}
