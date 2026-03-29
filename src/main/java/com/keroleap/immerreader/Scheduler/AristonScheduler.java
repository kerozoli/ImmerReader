package com.keroleap.immerreader.Scheduler;

import java.awt.image.BufferedImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.keroleap.immerreader.AristonRest;
import com.keroleap.immerreader.SharedData.AristonData;
import com.keroleap.immerreader.service.AristonParserService;
import com.keroleap.immerreader.service.ImageProcessingService;

@Component
public class AristonScheduler extends BaseScheduler<AristonRest> {
    private static final Logger logger = LoggerFactory.getLogger(AristonScheduler.class);

    @Autowired
    private AristonData aristonData;

    @Autowired
    private ImageProcessingService imageProcessingService;

    @Autowired
    private AristonParserService aristonParserService;

    @Value("${camera.ariston.url}")
    private String aristonCameraUrl;

    @Scheduled(fixedRate = 15000)
    public void AristonScheduledRead() {
        AristonRest result = executeWithTimeout(() -> {
            BufferedImage cachedImage = imageProcessingService.getBufferedImage(aristonCameraUrl);
            return getAristonRestData(cachedImage);
        }, 10000, "Ariston");

        if (result != null) {
            aristonData.setAristonRest(result);
        }
    }

    private AristonRest getAristonRestData(BufferedImage bufferedImage) {
        return aristonParserService.parse(bufferedImage);
    }

}
