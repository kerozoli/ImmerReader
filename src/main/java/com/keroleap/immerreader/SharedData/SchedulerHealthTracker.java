package com.keroleap.immerreader.SharedData;

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
        Health.Builder builder = Health.up();
        for (Map.Entry<String, ConsecutiveErrorState> entry : states.entrySet()) {
            String system = entry.getKey();
            int count = entry.getValue().getCount();
            builder.withDetail(system + "ConsecutiveTimeouts", count);
            boolean thresholdExceeded = count >= CONSECUTIVE_TIMEOUT_THRESHOLD;
            if (thresholdExceeded) {
                healthy = false;
                builder.down()
                        .withDetail(system + "Status", "DOWN: " + count + " consecutive timeouts");
            } else {
                builder.withDetail(system + "Status", "UP: " + count + " consecutive timeouts");
            }
        }
        return healthy ? builder.build() : builder.build();
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
