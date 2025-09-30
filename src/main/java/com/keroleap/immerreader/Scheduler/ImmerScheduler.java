package com.keroleap.immerreader.Scheduler;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
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

import com.keroleap.immerreader.ImmerRest;
import com.keroleap.immerreader.SharedData.ImmerData;

@Component
public class ImmerScheduler {
    @Autowired
    private ImmerData immerData;
    private ImmerRest immerRest;

    private int previousTempValue;
    private static final int LIGHT_THRESHOLD = -2500000;

    @Scheduled(fixedRate = 2000)
    public void ImmerScheduledRead() {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<ImmerRest> future = executor.submit(() -> {
        BufferedImage cachedImage = getBufferedImage("http://192.168.1.196/image/jpeg.cgi");
        return getImmerRestData(cachedImage);
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


    private ImmerRest getImmerRestData(BufferedImage bufferedImage) {
    boolean heating = getLightValueAnnDrawRedCross( 495, 215 ,  bufferedImage);
    boolean levelOne = getLightValueAnnDrawRedCross( 305, 150 ,  bufferedImage);
    boolean levelTwo = getLightValueAnnDrawRedCross( 334, 150 ,  bufferedImage);
    boolean levelThree = getLightValueAnnDrawRedCross(362, 150 ,  bufferedImage);
    boolean levelFour = getLightValueAnnDrawRedCross( 390, 150 ,  bufferedImage);

    boolean boilerOn = getLightValueAnnDrawRedCross( 490, 120 ,  bufferedImage);

    boolean digit1_1 = getLightValueAnnDrawRedCross(306, 178, bufferedImage);
    boolean digit1_2 = getLightValueAnnDrawRedCross(291, 199, bufferedImage);
    boolean digit1_3 = getLightValueAnnDrawRedCross(291, 243, bufferedImage);
    boolean digit1_4 = getLightValueAnnDrawRedCross(306, 269, bufferedImage);
    boolean digit1_5 = getLightValueAnnDrawRedCross(324, 243, bufferedImage);
    boolean digit1_6 = getLightValueAnnDrawRedCross(324, 199, bufferedImage);
    boolean digit1_7 = getLightValueAnnDrawRedCross(304, 224, bufferedImage);

    boolean digit2_1 = getLightValueAnnDrawRedCross(360, 178, bufferedImage);
    boolean digit2_2 = getLightValueAnnDrawRedCross(344, 199, bufferedImage);
    boolean digit2_3 = getLightValueAnnDrawRedCross(344, 243, bufferedImage);
    boolean digit2_4 = getLightValueAnnDrawRedCross(360, 268, bufferedImage);
    boolean digit2_5 = getLightValueAnnDrawRedCross(377, 243, bufferedImage);
    boolean digit2_6 = getLightValueAnnDrawRedCross(377, 199, bufferedImage);
    boolean digit2_7 = getLightValueAnnDrawRedCross(360, 224, bufferedImage);
    
    int number1 = getNumber(digit1_1, digit1_2, digit1_3, digit1_4, digit1_5, digit1_6, digit1_7) * 10;
    int number2 = getNumber(digit2_1, digit2_2, digit2_3, digit2_4, digit2_5, digit2_6, digit2_7);
    int number = number1 + number2;

    if (number > 500) {
        number = previousTempValue;
    }
    if (!(20 < number && number < 56)) {
        number = previousTempValue;
    }

    previousTempValue = number;

    if(!heating){
        number = 0;
    }

    int throttle = 0;
    if (levelOne) {
        throttle = 1;
    } 
    if (levelTwo) {
        throttle = 2;
    } 
    if (levelThree) {
        throttle = 3;
    } 
    if (levelFour) {
        throttle = 4;
    }

    ImmerRest immerRest = new ImmerRest();
    immerRest.setTemperaute(number);
    immerRest.setThrottle(throttle);
    immerRest.setHeating(heating);
    immerRest.setBoilerOn(boilerOn);

    return immerRest;
}
        
public int getNumber(boolean digit1_1, boolean digit1_2, boolean digit1_3, boolean digit1_4, boolean digit1_5, boolean digit1_6, boolean digit1_7) {
    int number = 1000;
    if (digit1_1 && digit1_2 && digit1_3 && digit1_4 && digit1_5 && digit1_6 && !digit1_7) {
        number = 0;
    }
    if (!digit1_1 && digit1_2 && digit1_3 && !digit1_4 && !digit1_5 && !digit1_6 && !digit1_7) {
        number = 1;
    }
    if (digit1_1 && !digit1_2 && digit1_3 && digit1_4 && !digit1_5 && digit1_6 && digit1_7) {
        number = 2;
    }
    if (digit1_1 && digit1_2 && digit1_3 && digit1_4 && !digit1_5 && !digit1_6 && digit1_7) {
        number = 3;
    }
    if (!digit1_1 && digit1_2 && digit1_3 && !digit1_4 && digit1_5 && !digit1_6 && digit1_7) {
        number = 4;
    }
    if (digit1_1 && digit1_2 && !digit1_3 && digit1_4 && digit1_5 && !digit1_6 && digit1_7) {
        number = 5;
    }
    if (digit1_1 && digit1_2 && !digit1_3 && digit1_4 && digit1_5 && digit1_6 && digit1_7) {
        number = 6;
    }
    if (!digit1_1 && digit1_2 && digit1_3 && digit1_4 && !digit1_5 && !digit1_6 && !digit1_7) {
        number = 7;
    }
    if (digit1_1 && digit1_2 && digit1_3 && digit1_4 && digit1_5 && digit1_6 && digit1_7) {
        number = 8;
    }
    if (digit1_1 && digit1_2 && digit1_3 && digit1_4 && digit1_5 && !digit1_6 && digit1_7) {
        number = 9;
    }
    if(number == 1000) {
        System.out.println("Undetected digit pattern" + digit1_1 + digit1_2 + digit1_3 + digit1_4 + digit1_5 + digit1_6 + digit1_7);
    }
    return number;
}

private BufferedImage getBufferedImage(String imageUrl) throws IOException {

    URL url = null;
    try {
        url = new URL(imageUrl);
    } catch (MalformedURLException e) {
        System.out.println("Malformed URL: " + imageUrl);
    }
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    try {
        byte[] chunk = new byte[4096];
        int bytesRead;
        InputStream stream = url.openStream();

        while ((bytesRead = stream.read(chunk)) > 0) {
            outputStream.write(chunk, 0, bytesRead);
        }

        url.openStream().close();
        byte[] data = outputStream.toByteArray();
        ByteArrayInputStream input = new ByteArrayInputStream(data);
        BufferedImage image = ImageIO.read(input);
        outputStream.close();
        return image;

    } catch (IOException e) {
        System.out.println("Error fetching image: " + e.getMessage());
        return null;
    }
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
    boolean detected = (lightValue > LIGHT_THRESHOLD);
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
