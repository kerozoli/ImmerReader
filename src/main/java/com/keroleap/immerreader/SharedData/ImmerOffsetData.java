package com.keroleap.immerreader.SharedData;

import org.springframework.stereotype.Component;

@Component
public class ImmerOffsetData {
    private int offsetX = 0;
    private int offsetY = 0;

    public int getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(int offsetX) {
        this.offsetX = offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(int offsetY) {
        this.offsetY = offsetY;
    }
}
