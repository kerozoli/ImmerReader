package com.keroleap.immerreader.SharedData;

import java.util.ArrayDeque;
import java.util.Deque;

import org.springframework.stereotype.Component;

import com.keroleap.immerreader.EbedloRest;

import jakarta.annotation.PostConstruct;

@Component
public class EbedloData {

    private static final int SMOOTHING_WINDOW = 3;

    private EbedloRest ebedloRest;
    private final Deque<Boolean> recentOnDecisions = new ArrayDeque<>(SMOOTHING_WINDOW);

    @PostConstruct
    public void init() {
        System.out.println("EbedloRest initialized at startup.");
        this.ebedloRest = new EbedloRest();
    }

    public EbedloRest getEbedloRest() {
        return ebedloRest;
    }

    public void setEbedloRest(EbedloRest ebedloRest) {
        this.ebedloRest = ebedloRest;
    }

    public synchronized void addOnDecision(boolean on) {
        if (recentOnDecisions.size() >= SMOOTHING_WINDOW) {
            recentOnDecisions.pollFirst();
        }
        recentOnDecisions.offerLast(on);
    }

    public synchronized boolean getSmoothedOn() {
        if (recentOnDecisions.isEmpty()) {
            return false;
        }
        if (recentOnDecisions.size() < SMOOTHING_WINDOW) {
            return recentOnDecisions.peekLast();
        }
        int trueCount = 0;
        for (boolean decision : recentOnDecisions) {
            if (decision) {
                trueCount++;
            }
        }
        return trueCount >= 2;
    }
}
