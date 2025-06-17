package com.keroleap.immerreader.SharedData;

import org.springframework.stereotype.Component;
import com.keroleap.immerreader.AristonRest;

import jakarta.annotation.PostConstruct;

@Component
public class AristonData {
    AristonRest aristonRest;

    @PostConstruct
    public void init() {
        System.out.println("AristonRest initialized at startup.");
        this.aristonRest = new AristonRest();
    }

    public AristonRest getAristonRest() {
        return aristonRest;
    }

    public void setAristonRest(AristonRest aristonRest) {
        this.aristonRest = aristonRest;
    }
}
