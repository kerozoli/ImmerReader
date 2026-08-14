package com.keroleap.immerreader.SharedData;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.keroleap.immerreader.ChickenRest;

@Component
public class ChickenData {

    private static final int HISTORY_SIZE = 5;
    private static final int NEST_COUNT = 3;

    private ChickenRest chickenRest = new ChickenRest();
    private final Deque<Integer>[] history = new ArrayDeque[NEST_COUNT];

    public ChickenData() {
        for (int i = 0; i < NEST_COUNT; i++) {
            history[i] = new ArrayDeque<>();
        }
    }

    public synchronized void addCounts(int[] counts) {
        for (int i = 0; i < NEST_COUNT; i++) {
            Deque<Integer> deque = history[i];
            deque.addLast(counts[i]);
            while (deque.size() > HISTORY_SIZE) {
                deque.removeFirst();
            }
        }

        int[] smoothed = new int[NEST_COUNT];
        for (int i = 0; i < NEST_COUNT; i++) {
            smoothed[i] = mode(history[i]);
        }

        ChickenRest rest = new ChickenRest();
        rest.setNest1Count(smoothed[0]);
        rest.setNest2Count(smoothed[1]);
        rest.setNest3Count(smoothed[2]);
        rest.setTotalCount(smoothed[0] + smoothed[1] + smoothed[2]);
        this.chickenRest = rest;
    }

    public synchronized ChickenRest getChickenRest() {
        return chickenRest;
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
