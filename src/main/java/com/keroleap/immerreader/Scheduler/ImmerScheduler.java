package com.keroleap.immerreader.Scheduler;

import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.keroleap.immerreader.ImmerRest;
import com.keroleap.immerreader.Service.ImmerAnalyzerService;
import com.keroleap.immerreader.SharedData.ImmerData;
import com.keroleap.immerreader.SharedData.ImmerOffsetData;

@Component
public class ImmerScheduler {
    @Autowired
    private ImmerData immerData;

    @Autowired
    private ImmerAnalyzerService immerAnalyzerService;

    @Autowired
    private ImmerOffsetData immerOffsetData;

    private ImmerRest immerRest;

    @Scheduled(fixedRate = 2000)
    public void ImmerScheduledRead() {
    int offsetX = immerOffsetData.getOffsetX();
    int offsetY = immerOffsetData.getOffsetY();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<ImmerRest> future = executor.submit(() -> {
        BufferedImage cachedImage = immerAnalyzerService.getBufferedImage("http://192.168.1.196/image/jpeg.cgi");
        return immerAnalyzerService.getImmerRestData(cachedImage, offsetX, offsetY);
    });

    try {
        ImmerRest result = future.get(1500, TimeUnit.MILLISECONDS);
        executor.shutdown();
        immerData.setImmerRest(result);
    } catch (TimeoutException e) {
        future.cancel(true);
        executor.shutdownNow();
        System.out.println("Timeout fetching Immer data, returning default.");
        immerData.setImmerRest(immerRest);
    } catch (Exception e) {
        executor.shutdownNow();
        System.out.println("Error fetching Immer data: " + e.getMessage());
        immerData.setImmerRest(immerRest);
    }
    }
}
