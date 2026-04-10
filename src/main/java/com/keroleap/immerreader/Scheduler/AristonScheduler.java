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

import com.keroleap.immerreader.AristonRest;
import com.keroleap.immerreader.Service.AristonAnalyzerService;
import com.keroleap.immerreader.SharedData.AristonData;

import jakarta.annotation.PreDestroy;

@Component
public class AristonScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AristonScheduler.class);

    @Autowired
    private AristonData aristonData;

    @Autowired
    private AristonAnalyzerService aristonAnalyzerService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Scheduled(fixedRate = 15000)
    public void AristonScheduledRead() {
        Future<AristonRest> future = executor.submit(() -> {
            BufferedImage cachedImage = aristonAnalyzerService.getBufferedImage("http://192.168.1.191/cgi/jpg/image.cgi");
            return aristonAnalyzerService.getAristonRestData(cachedImage);
        });

        try {
            AristonRest result = future.get(10000, TimeUnit.MILLISECONDS);
            aristonData.setAristonRest(result);
        } catch (TimeoutException e) {
            future.cancel(true);
            logger.warn("Timeout fetching Ariston data, keeping previous value.");
        } catch (Exception e) {
            logger.error("Error fetching Ariston data: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void destroy() {
        logger.info("Shutting down AristonScheduler executor.");
        executor.shutdownNow();
    }
}

