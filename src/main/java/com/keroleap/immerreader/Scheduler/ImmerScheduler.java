package com.keroleap.immerreader.Scheduler;

import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.keroleap.immerreader.ImmerRest;
import com.keroleap.immerreader.Service.ImmerAnalyzerService;
import com.keroleap.immerreader.SharedData.ImmerData;
import com.keroleap.immerreader.SharedData.ImmerOffsetData;

import jakarta.annotation.PreDestroy;

@Component
public class ImmerScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ImmerScheduler.class);

    @Autowired
    private ImmerData immerData;

    @Autowired
    private ImmerAnalyzerService immerAnalyzerService;

    @Autowired
    private ImmerOffsetData immerOffsetData;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Scheduled(fixedRate = 2000)
    public void ImmerScheduledRead() {
        int offsetX = immerOffsetData.getOffsetX();
        int offsetY = immerOffsetData.getOffsetY();
        Future<ImmerRest> future = executor.submit(() -> {
            BufferedImage cachedImage = immerAnalyzerService.getBufferedImage("http://192.168.1.196/image/jpeg.cgi");
            return immerAnalyzerService.getImmerRestData(cachedImage, offsetX, offsetY);
        });

        try {
            ImmerRest result = future.get(1500, TimeUnit.MILLISECONDS);
            immerData.setImmerRest(result);
        } catch (TimeoutException e) {
            future.cancel(true);
            logger.warn("Timeout fetching Immer data, keeping previous value.");
        } catch (Exception e) {
            logger.error("Error fetching Immer data: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void destroy() {
        logger.info("Shutting down ImmerScheduler executor.");
        executor.shutdownNow();
    }
}

