package com.keroleap.immerreader.SharedData;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ErrorStatistics {

    private static final long TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000;

    private final Map<String, ErrorCounter> errorCounts = new ConcurrentHashMap<>();
    private final Clock clock;

    public ErrorStatistics() {
        this.clock = Clock.systemDefaultZone();
    }

    public void recordError(String system, String errorType) {
        String key = system + ":" + errorType;
        errorCounts.computeIfAbsent(key, k -> new ErrorCounter()).increment();
        cleanup();
    }

    public Map<String, Integer> getLastErrorCounts(String system) {
        cleanup();
        Map<String, Integer> result = new ConcurrentHashMap<>();
        errorCounts.entrySet().stream()
            .filter(e -> e.getKey().startsWith(system + ":"))
            .forEach(e -> {
                String errorType = e.getKey().substring(system.length() + 1);
                result.put(errorType, e.getValue().count);
            });
        return result;
    }

    private void cleanup() {
        long now = clock.millis();
        errorCounts.entrySet().removeIf(e -> now - e.getValue().lastOccurrence > TWENTY_FOUR_HOURS_MS);
    }

    private static class ErrorCounter {
        int count = 0;
        long lastOccurrence;

        void increment() {
            count++;
            lastOccurrence = Clock.systemDefaultZone().millis();
        }
    }
}
