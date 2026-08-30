package com.keroleap.immerreader.SharedData;

import java.util.Collections;
import java.util.List;

public class ImmerStatisticsSnapshot {

    private final List<String> labels;
    private final List<Double> temperatures;
    private final List<Double> throttleLevels;
    private final List<Long> heatingMsPerMinute;
    private final long totalHeatingMs;

    public ImmerStatisticsSnapshot(List<String> labels,
                                     List<Double> temperatures,
                                     List<Double> throttleLevels,
                                     List<Long> heatingMsPerMinute,
                                     long totalHeatingMs) {
        this.labels = labels != null ? Collections.unmodifiableList(labels) : Collections.emptyList();
        this.temperatures = temperatures != null ? Collections.unmodifiableList(temperatures) : Collections.emptyList();
        this.throttleLevels = throttleLevels != null ? Collections.unmodifiableList(throttleLevels) : Collections.emptyList();
        this.heatingMsPerMinute = heatingMsPerMinute != null ? Collections.unmodifiableList(heatingMsPerMinute) : Collections.emptyList();
        this.totalHeatingMs = totalHeatingMs;
    }

    public List<String> getLabels() {
        return labels;
    }

    public List<Double> getTemperatures() {
        return temperatures;
    }

    public List<Double> getThrottleLevels() {
        return throttleLevels;
    }

    public List<Long> getHeatingMsPerMinute() {
        return heatingMsPerMinute;
    }

    public long getTotalHeatingMs() {
        return totalHeatingMs;
    }
}
