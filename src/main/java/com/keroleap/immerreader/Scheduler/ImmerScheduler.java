package com.keroleap.immerreader.Scheduler;

import java.awt.image.BufferedImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.keroleap.immerreader.ImmerRest;
import com.keroleap.immerreader.SharedData.ImmerData;
import com.keroleap.immerreader.service.ImageProcessingService;
import com.keroleap.immerreader.service.ImmerParserService;

@Component
public class ImmerScheduler extends BaseScheduler<ImmerRest> {
    private static final Logger logger = LoggerFactory.getLogger(ImmerScheduler.class);

    @Autowired
    private ImmerData immerData;

    @Autowired
    private ImageProcessingService imageProcessingService;

    @Autowired
    private ImmerParserService immerParserService;

    @Value("${camera.immer.url}")
    private String immerCameraUrl;

    @Scheduled(fixedRate = 2000)
    public void ImmerScheduledRead() {
        ImmerRest result = executeWithTimeout(() -> {
            BufferedImage cachedImage = imageProcessingService.getBufferedImage(immerCameraUrl);
            return getImmerRestData(cachedImage);
        }, 1500, "Immer");

        if (result != null) {
            immerData.setImmerRest(result);
        }
    }

    private ImmerRest getImmerRestData(BufferedImage bufferedImage) {
        return immerParserService.parse(bufferedImage);
    }

}
