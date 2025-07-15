package com.keroleap.immerreader.Scheduler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.imageio.ImageIO;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.keroleap.immerreader.AristonRest;
import com.keroleap.immerreader.SharedData.AristonData;
import java.awt.image.BufferedImage;


@Component
public class AristonScheduler {
    @Autowired
    private AristonData aristonData;
    private AristonRest aristonRest;
    private static final int LIGHT_THRESHOLD = -7000000;


    @Scheduled(fixedRate = 15000)
    public void AristonScheduledRead() {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<AristonRest> future = executor.submit(() -> {
        BufferedImage cachedImage = getBufferedImage("http://192.168.1.191/cgi/jpg/image.cgi");
        return getAristonRestData( cachedImage);
    });

    try {
        // Set timeout to 3 seconds (adjust as needed)
        aristonRest = future.get(20, TimeUnit.SECONDS);
        executor.shutdown();
        aristonData.setAristonRest(aristonRest);
    } catch (TimeoutException e) {
        future.cancel(true);
        executor.shutdownNow();
        System.out.println("Timeout fetching Ariston data, returning default.");
        aristonData.setAristonRest(aristonRest);
    } catch (Exception e) {
        executor.shutdownNow();
        System.out.println("Error fetching Ariston data: " + e.getMessage());
        aristonData.setAristonRest(aristonRest);
    }
    }


    private BufferedImage getBufferedImage(String imageUrl) throws IOException {
        try {
            URL url = new URL(imageUrl);
            try (InputStream stream = url.openStream();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                byte[] chunk = new byte[4096];
                int bytesRead;
                while ((bytesRead = stream.read(chunk)) > 0) {
                    outputStream.write(chunk, 0, bytesRead);
                }

                try (ByteArrayInputStream input = new ByteArrayInputStream(outputStream.toByteArray())) {
                    return ImageIO.read(input);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

private AristonRest getAristonRestData(BufferedImage bufferedImage) {
    boolean percent_0 = getLightValueAnnDrawRedCross( 160, 160 ,  bufferedImage);
    boolean percent_5 = getLightValueAnnDrawRedCross( 163, 161 ,  bufferedImage);
    boolean percent_10 = getLightValueAnnDrawRedCross( 166, 162 ,  bufferedImage);
    boolean percent_15 = getLightValueAnnDrawRedCross( 169, 163 ,  bufferedImage);
    boolean percent_20 = getLightValueAnnDrawRedCross( 172, 164 ,  bufferedImage);
    boolean percent_25 = getLightValueAnnDrawRedCross( 175, 165 ,  bufferedImage);
    boolean percent_30 = getLightValueAnnDrawRedCross( 178, 166 ,  bufferedImage);
    boolean percent_35 = getLightValueAnnDrawRedCross( 181, 167 ,  bufferedImage);
    boolean percent_40 = getLightValueAnnDrawRedCross( 184, 168 ,  bufferedImage);
    boolean percent_45 = getLightValueAnnDrawRedCross( 187, 169 ,  bufferedImage);
    boolean percent_50 = getLightValueAnnDrawRedCross( 190, 170 ,  bufferedImage);
    boolean percent_55 = getLightValueAnnDrawRedCross( 193, 171 ,  bufferedImage);
    boolean percent_60 = getLightValueAnnDrawRedCross( 196, 172 ,  bufferedImage);
    boolean percent_65 = getLightValueAnnDrawRedCross( 199, 173 ,  bufferedImage);
    boolean percent_70 = getLightValueAnnDrawRedCross( 202, 174 ,  bufferedImage);
    boolean percent_75 = getLightValueAnnDrawRedCross( 205, 175 ,  bufferedImage);
    boolean percent_80 = getLightValueAnnDrawRedCross( 208, 176 ,  bufferedImage);
    boolean percent_85 = getLightValueAnnDrawRedCross( 211, 177 ,  bufferedImage);
    boolean percent_90 = getLightValueAnnDrawRedCross( 214, 178 ,  bufferedImage);
    boolean percent_95 = getLightValueAnnDrawRedCross( 217, 179 ,  bufferedImage);
    boolean percent_100 = getLightValueAnnDrawRedCross( 220, 180 ,  bufferedImage);

    int percentage = 0;
    if(percent_0) percentage = 0;
    if(percent_5) percentage = 5;
    if(percent_10) percentage = 10;
    if(percent_15) percentage = 15;
    if(percent_20) percentage = 20;
    if(percent_25) percentage = 25;
    if(percent_30) percentage = 30;
    if(percent_35) percentage = 35;
    if(percent_40) percentage = 40;
    if(percent_45) percentage = 45;
    if(percent_50) percentage = 50;
    if(percent_55) percentage = 55;
    if(percent_60) percentage = 60;
    if(percent_65) percentage = 65;
    if(percent_70) percentage = 70;
    if(percent_75) percentage = 75;
    if(percent_80) percentage = 80;
    if(percent_85) percentage = 85;
    if(percent_90) percentage = 90;
    if(percent_95) percentage = 95;
    if(percent_100) percentage = 100;

    AristonRest aristonRest = new AristonRest();
    aristonRest.setPercentage(percentage);

    return aristonRest;
}

private boolean getLightValueAnnDrawRedCross(int x, int y , BufferedImage image) {
    List <Integer> lightValues = new ArrayList<Integer>();
    for (int a=x-3  ; a<x+3; a++) {
            lightValues.add(image.getRGB(a, y));
    }
    for (int b=y-3  ; b<y+3; b++) {
            lightValues.add(image.getRGB(x, b));
    }   
    double lightValue = lightValues.stream().mapToInt(Integer::intValue).average().getAsDouble();
    boolean detected = (lightValue < LIGHT_THRESHOLD);
    int color = detected ? 16711680 : 16777215;
    for (int a=x-5  ; a<x+5; a++) {
            image.setRGB(a, y, color);
    }
    for (int b=y-5  ; b<y+5; b++) {
            image.setRGB(x, b, color);
    }
    return detected;
}  

}
