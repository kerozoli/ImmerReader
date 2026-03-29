package com.keroleap.immerreader.service;

import com.keroleap.immerreader.AristonRest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;

@Service
public class AristonParserService {
    private static final Logger logger = LoggerFactory.getLogger(AristonParserService.class);
    private static final int LIGHT_THRESHOLD = -7000000;

    @Autowired
    private ImageProcessingService imageProcessingService;

    public AristonRest parse(BufferedImage bufferedImage) {
        if (bufferedImage == null) {
            return new AristonRest();
        }

        boolean[] percentLights = new boolean[21];
        for (int i = 0; i < 21; i++) {
            int x = 160 + (i * 3);
            int y = 160 + i;
            percentLights[i] = imageProcessingService.getLightValueAndDrawCross(x, y, bufferedImage, LIGHT_THRESHOLD, true);
        }

        int percentage = calculatePercentage(percentLights);

        AristonRest aristonRest = new AristonRest();
        aristonRest.setPercentage(percentage);

        return aristonRest;
    }

    private int calculatePercentage(boolean[] percentLights) {
        for (int i = 0; i < percentLights.length; i++) {
            if (percentLights[i]) {
                return i * 5;
            }
        }
        return 0;
    }
}
