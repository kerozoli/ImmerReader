package com.keroleap.immerreader.SharedData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.keroleap.immerreader.ChickenRest;

@Component
public class ChickenData {

    private static final int HISTORY_SIZE = 5;

    private ChickenRest chickenRest = new ChickenRest();
    private final List<Deque<Integer>> history = new ArrayList<>();
    private int configuredCount = 0;
    private int intervalSeconds = 30;

    public ChickenData() {
        resizeHistory(3);
    }

    public synchronized void addCounts(List<Integer> counts) {
        resizeHistory(counts.size());
        for (int i = 0; i < counts.size(); i++) {
            Deque<Integer> deque = history.get(i);
            deque.addLast(counts.get(i));
            while (deque.size() > HISTORY_SIZE) {
                deque.removeFirst();
            }
        }

        List<Integer> smoothed = new ArrayList<>();
        for (Deque<Integer> deque : history) {
            smoothed.add(mode(deque));
        }

        ChickenRest rest = new ChickenRest();
        rest.setNestCounts(smoothed);
        rest.setTotalCount(smoothed.stream().mapToInt(Integer::intValue).sum());
        rest.setIntervalSeconds(this.intervalSeconds);
        this.chickenRest = rest;
    }

    public synchronized ChickenRest getChickenRest() {
        chickenRest.setIntervalSeconds(this.intervalSeconds);
        return chickenRest;
    }

    public synchronized void setConfiguredCount(int count) {
        this.configuredCount = count;
        resizeHistory(count);
    }

    public synchronized void setIntervalSeconds(int intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
        if (this.chickenRest != null) {
            this.chickenRest.setIntervalSeconds(intervalSeconds);
        }
    }

    private void resizeHistory(int count) {
        while (history.size() < count) {
            history.add(new ArrayDeque<>());
        }
        while (history.size() > count) {
            history.remove(history.size() - 1);
        }
    }

    private int mode(Deque<Integer> deque) {
        if (deque.isEmpty()) {
            return 0;
        }
        Map<Integer, Integer> frequency = new HashMap<>();
        for (int value : deque) {
            frequency.merge(value, 1, Integer::sum);
        }
        int mode = 0;
        int maxCount = 0;
        for (Map.Entry<Integer, Integer> entry : frequency.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mode = entry.getKey();
            }
        }
        return mode;
    }
}
