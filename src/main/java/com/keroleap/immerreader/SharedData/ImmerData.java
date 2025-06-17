package com.keroleap.immerreader.SharedData;

import org.springframework.stereotype.Component;

import com.keroleap.immerreader.ImmerRest;

import jakarta.annotation.PostConstruct;

@Component
public class ImmerData {
    ImmerRest immerRest;

    @PostConstruct
    public void init() {
        System.out.println("ImmerRest initialized at startup.");
        this.immerRest = new ImmerRest();
    }

    public ImmerRest getAristonRest() {
        return immerRest;
    }

    public void setAristonRest(ImmerRest immerRest) {
        this.immerRest = immerRest;
    }
}
