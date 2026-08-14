package com.keroleap.immerreader.SharedData;

import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.keroleap.immerreader.ErrorType;

@Component
public class SchedulerHealthTracker implements HealthIndicator {

    private static final int CONSECUTIVE_TIMEOUT_THRESHOLD = 50;
    private static final String[] TRACKED_SYSTEMS = { "Ariston", "Immer" };
    private static final LocalTime NIGHT_START = LocalTime.of(20, 55);
    private static final LocalTime NIGHT_END = LocalTime.of(9, 5);

    private final Map<String, ConsecutiveErrorState> states = new ConcurrentHashMap<>();

    public SchedulerHealthTracker() {
        for (String system : TRACKED_SYSTEMS) {
            states.put(system, new ConsecutiveErrorState());
        }
    }

    public void recordError(String system, ErrorType errorType) {
        ConsecutiveErrorState state = states.get(system);
        if (state == null) {
            return;
        }
        if (errorType == ErrorType.TIMEOUT) {
            state.increment();
        } else {
            state.reset();
        }
    }

    public void recordSuccess(String system) {
        ConsecutiveErrorState state = states.get(system);
        if (state != null) {
            state.reset();
        }
    }

    @Override
    public Health health() {
        boolean healthy = true;
        boolean nightWindow = isNightWindow();
        Health.Builder builder = Health.up();
        for (Map.Entry<String, ConsecutiveErrorState> entry : states.entrySet()) {
            String system = entry.getKey();
            int count = entry.getValue().getCount();
            builder.withDetail(system + "ConsecutiveTimeouts", count);
            boolean thresholdExceeded = count >= CONSECUTIVE_TIMEOUT_THRESHOLD;
            if (thresholdExceeded && !nightWindow) {
                healthy = false;
                builder.down()
                        .withDetail(system + "Status", "DOWN: " + count + " consecutive timeouts");
            } else if (thresholdExceeded) {
                builder.withDetail(system + "Status", "UP (night silence): " + count + " consecutive timeouts");
            } else {
                builder.withDetail(system + "Status", "UP: " + count + " consecutive timeouts");
            }
        }
        return healthy ? builder.build() : builder.build();
    }

    private boolean isNightWindow() {
        LocalTime now = LocalTime.now();
        if (now.isAfter(NIGHT_START) || now.isBefore(NIGHT_END)) {
            return true;
        }
        return false;
    }

    private static class ConsecutiveErrorState {
        private int count;

        synchronized void increment() {
            count++;
        }

        synchronized void reset() {
            count = 0;
        }

        synchronized int getCount() {
            return count;
        }
    }
}
