package com.keroleap.immerreader.SharedData;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import com.keroleap.immerreader.ImmerRest;

@Component
public class ImmerStatistics {

    private static final long ONE_MINUTE_MS = 60_000L;
    private static final int TWENTY_FOUR_HOURS_MINUTES = 24 * 60;
    private static final long MAX_INTERVAL_MS = ONE_MINUTE_MS * 2;
    private static final DateTimeFormatter LABEL_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final Clock clock;
    private final ConcurrentHashMap<Long, ImmerMinuteStats> buckets = new ConcurrentHashMap<>();
    private final Object intervalLock = new Object();

    private volatile boolean lastHeating = false;
    private volatile long lastTimestamp = 0;

    public ImmerStatistics() {
        this.clock = Clock.systemDefaultZone();
    }

    ImmerStatistics(Clock clock) {
        this.clock = clock;
    }

    public void record(ImmerRest rest) {
        long now = clock.millis();
        synchronized (intervalLock) {
            if (lastTimestamp > 0) {
                long elapsed = now - lastTimestamp;
                if (elapsed > 0 && elapsed < MAX_INTERVAL_MS) {
                    addHeatingInterval(lastTimestamp, now, lastHeating);
                }
            }
            lastHeating = rest != null && rest.isHeating();
            lastTimestamp = now;
        }

        if (rest != null) {
            addSample(now, rest.getTemperaute(), rest.getThrottle());
        }
        cleanup();
    }

    private void addHeatingInterval(long start, long end, boolean heating) {
        if (!heating || start >= end) {
            return;
        }
        long startMinute = start / ONE_MINUTE_MS;
        long endMinute = end / ONE_MINUTE_MS;
        if (startMinute == endMinute) {
            getBucket(startMinute).addHeatingMs(end - start);
        } else {
            long endOfStartMinute = (startMinute + 1) * ONE_MINUTE_MS;
            getBucket(startMinute).addHeatingMs(endOfStartMinute - start);
            for (long minute = startMinute + 1; minute < endMinute; minute++) {
                getBucket(minute).addHeatingMs(ONE_MINUTE_MS);
            }
            getBucket(endMinute).addHeatingMs(end - endMinute * ONE_MINUTE_MS);
        }
    }

    private void addSample(long timestamp, int temperature, int throttle) {
        long minute = timestamp / ONE_MINUTE_MS;
        ImmerMinuteStats bucket = getBucket(minute);
        if (temperature > 0) {
            bucket.addTemperature(temperature);
        }
        if (throttle >= 0) {
            bucket.addThrottle(throttle);
        }
    }

    private ImmerMinuteStats getBucket(long minute) {
        return buckets.computeIfAbsent(minute, ImmerMinuteStats::new);
    }

    private void cleanup() {
        long cutoff = (clock.millis() / ONE_MINUTE_MS) - TWENTY_FOUR_HOURS_MINUTES;
        buckets.keySet().removeIf(minute -> minute <= cutoff);
    }

    public ImmerStatisticsSnapshot getLast24Hours() {
        cleanup();
        long currentMinute = clock.millis() / ONE_MINUTE_MS;
        long startMinute = currentMinute - TWENTY_FOUR_HOURS_MINUTES + 1;

        List<String> labels = new ArrayList<>(TWENTY_FOUR_HOURS_MINUTES);
        List<Double> temperatures = new ArrayList<>(TWENTY_FOUR_HOURS_MINUTES);
        List<Double> throttleLevels = new ArrayList<>(TWENTY_FOUR_HOURS_MINUTES);
        List<Long> heatingMsPerMinute = new ArrayList<>(TWENTY_FOUR_HOURS_MINUTES);
        long totalHeatingMs = 0;

        ZoneId zone = clock.getZone();
        for (long minute = startMinute; minute <= currentMinute; minute++) {
            ImmerMinuteStats bucket = buckets.get(minute);
            labels.add(formatLabel(minute, zone));
            temperatures.add(bucket != null ? bucket.getAverageTemperature() : null);
            throttleLevels.add(bucket != null ? bucket.getAverageThrottle() : null);
            long heatingMs = bucket != null ? bucket.getHeatingMs() : 0;
            heatingMsPerMinute.add(heatingMs);
            totalHeatingMs += heatingMs;
        }

        return new ImmerStatisticsSnapshot(labels, temperatures, throttleLevels, heatingMsPerMinute, totalHeatingMs);
    }

    private String formatLabel(long minute, ZoneId zone) {
        long epochMillis = minute * ONE_MINUTE_MS;
        return Instant.ofEpochMilli(epochMillis).atZone(zone).format(LABEL_FORMATTER);
    }

    private static class ImmerMinuteStats {
        @SuppressWarnings("unused")
        private final long minute;
        private final AtomicInteger temperatureSum = new AtomicInteger(0);
        private final AtomicInteger temperatureCount = new AtomicInteger(0);
        private final AtomicInteger throttleSum = new AtomicInteger(0);
        private final AtomicInteger throttleCount = new AtomicInteger(0);
        private final AtomicLong heatingMs = new AtomicLong(0);

        ImmerMinuteStats(long minute) {
            this.minute = minute;
        }

        void addTemperature(int temperature) {
            temperatureSum.addAndGet(temperature);
            temperatureCount.incrementAndGet();
        }

        void addThrottle(int throttle) {
            throttleSum.addAndGet(throttle);
            throttleCount.incrementAndGet();
        }

        void addHeatingMs(long ms) {
            heatingMs.addAndGet(ms);
        }

        Double getAverageTemperature() {
            int count = temperatureCount.get();
            if (count == 0) {
                return null;
            }
            return temperatureSum.get() / (double) count;
        }

        Double getAverageThrottle() {
            int count = throttleCount.get();
            if (count == 0) {
                return null;
            }
            return throttleSum.get() / (double) count;
        }

        long getHeatingMs() {
            return heatingMs.get();
        }
    }
}
