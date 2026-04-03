package com.keroleap.immerreader.SharedData;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

@Component
public class ImmerOffsetData {
    private final AtomicInteger offsetX = new AtomicInteger(0);
    private final AtomicInteger offsetY = new AtomicInteger(0);

    public int getOffsetX() {
        return offsetX.get();
    }

    public void setOffsetX(int offsetX) {
        this.offsetX.set(offsetX);
    }

    public int getOffsetY() {
        return offsetY.get();
    }

    public void setOffsetY(int offsetY) {
        this.offsetY.set(offsetY);
    }
}
