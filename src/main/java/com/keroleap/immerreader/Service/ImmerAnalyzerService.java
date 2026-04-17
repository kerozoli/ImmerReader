package com.keroleap.immerreader.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.keroleap.immerreader.ImmerRest;
import com.keroleap.immerreader.SharedData.ErrorStatistics;

@Service
public class ImmerAnalyzerService {

    private static final Logger logger = LoggerFactory.getLogger(ImmerAnalyzerService.class);
    private static final int LIGHT_THRESHOLD = -2500000;
    private final AtomicInteger previousTempValue = new AtomicInteger(0);

    @Autowired(required = false)
    private ErrorStatistics errorStatistics;

    public ImmerRest getImmerRestData(BufferedImage bufferedImage, int offsetX, int offsetY) {
        boolean heating = getLightValueAnnDrawRedCross(495 + offsetX, 215 + offsetY, bufferedImage);
        boolean levelOne = getLightValueAnnDrawRedCross(305 + offsetX, 150 + offsetY, bufferedImage);
        boolean levelTwo = getLightValueAnnDrawRedCross(334 + offsetX, 150 + offsetY, bufferedImage);
        boolean levelThree = getLightValueAnnDrawRedCross(362 + offsetX, 150 + offsetY, bufferedImage);
        boolean levelFour = getLightValueAnnDrawRedCross(390 + offsetX, 150 + offsetY, bufferedImage);

        boolean boilerOn = getLightValueAnnDrawRedCross(490 + offsetX, 120 + offsetY, bufferedImage);

        boolean digit1_1 = getLightValueAnnDrawRedCross(306 + offsetX, 178 + offsetY, bufferedImage);
        boolean digit1_2 = getLightValueAnnDrawRedCross(291 + offsetX, 199 + offsetY, bufferedImage);
        boolean digit1_3 = getLightValueAnnDrawRedCross(291 + offsetX, 243 + offsetY, bufferedImage);
        boolean digit1_4 = getLightValueAnnDrawRedCross(306 + offsetX, 269 + offsetY, bufferedImage);
        boolean digit1_5 = getLightValueAnnDrawRedCross(324 + offsetX, 243 + offsetY, bufferedImage);
        boolean digit1_6 = getLightValueAnnDrawRedCross(324 + offsetX, 199 + offsetY, bufferedImage);
        boolean digit1_7 = getLightValueAnnDrawRedCross(304 + offsetX, 224 + offsetY, bufferedImage);

        boolean digit2_1 = getLightValueAnnDrawRedCross(360 + offsetX, 178 + offsetY, bufferedImage);
        boolean digit2_2 = getLightValueAnnDrawRedCross(344 + offsetX, 199 + offsetY, bufferedImage);
        boolean digit2_3 = getLightValueAnnDrawRedCross(344 + offsetX, 243 + offsetY, bufferedImage);
        boolean digit2_4 = getLightValueAnnDrawRedCross(360 + offsetX, 268 + offsetY, bufferedImage);
        boolean digit2_5 = getLightValueAnnDrawRedCross(377 + offsetX, 243 + offsetY, bufferedImage);
        boolean digit2_6 = getLightValueAnnDrawRedCross(377 + offsetX, 199 + offsetY, bufferedImage);
        boolean digit2_7 = getLightValueAnnDrawRedCross(360 + offsetX, 224 + offsetY, bufferedImage);

        int number1 = getNumber(digit1_1, digit1_2, digit1_3, digit1_4, digit1_5, digit1_6, digit1_7) * 10;
        int number2 = getNumber(digit2_1, digit2_2, digit2_3, digit2_4, digit2_5, digit2_6, digit2_7);
        int number = number1 + number2;

        if (number > 500) {
            number = previousTempValue.get();
        }
        if (!(20 < number && number < 56)) {
            number = previousTempValue.get();
        }

        previousTempValue.set(number);

        if (!heating) {
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
        if (!digit1_1 && !digit1_2 && !digit1_3 && !digit1_4 && digit1_5 && digit1_6 && !digit1_7) {
            number = 1;
        }
        if (digit1_1 && !digit1_2 && digit1_3 && digit1_4 && !digit1_5 && digit1_6 && digit1_7) {
            number = 2;
        }
        if (digit1_1 && !digit1_2 && !digit1_3 && digit1_4 && digit1_5 && digit1_6 && digit1_7) {
            number = 3;
        }
        if (!digit1_1 && digit1_2 && !digit1_3 && !digit1_4 && digit1_5 && digit1_6 && digit1_7) {
            number = 4;
        }
        if (digit1_1 && digit1_2 && !digit1_3 && digit1_4 && digit1_5 && !digit1_6 && digit1_7) {
            number = 5;
        }
        if (digit1_1 && digit1_2 && digit1_3 && digit1_4 && digit1_5 && !digit1_6 && digit1_7) {
            number = 6;
        }
        if (digit1_1 && !digit1_2 && !digit1_3 && !digit1_4 && digit1_5 && digit1_6 && !digit1_7) {
            number = 7;
        }
        if (digit1_1 && digit1_2 && digit1_3 && digit1_4 && digit1_5 && digit1_6 && digit1_7) {
            number = 8;
        }
        if (digit1_1 && digit1_2 && !digit1_3 && digit1_4 && digit1_5 && digit1_6 && digit1_7) {
            number = 9;
        }
        if (number == 1000) {
            logger.warn("Unknown digit detected: {}{}{}{}{}{}{}", digit1_1, digit1_2, digit1_3, digit1_4, digit1_5, digit1_6, digit1_7);
            if (errorStatistics != null) {
                errorStatistics.recordError("Immer", "unknown_digit");
            }
        }
        return number;
    }

    public BufferedImage getBufferedImage(String imageUrl) throws IOException {
        try {
            URL url = URI.create(imageUrl).toURL();
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
            logger.error("Error fetching image from {}: {}", imageUrl, e.getMessage());
            return null;
        }
    }

    private boolean getLightValueAnnDrawRedCross(int x, int y, BufferedImage image) {
        long sum = 0;
        for (int a = x - 3; a < x + 3; a++) {
            sum += image.getRGB(a, y);
        }
        for (int b = y - 3; b < y + 3; b++) {
            sum += image.getRGB(x, b);
        }
        double lightValue = sum / 12.0;
        boolean detected = lightValue > LIGHT_THRESHOLD;
        int color = detected ? 16711680 : 16777215;
        for (int a = x - 5; a < x + 5; a++) {
            image.setRGB(a, y, color);
        }
        for (int b = y - 5; b < y + 5; b++) {
            image.setRGB(x, b, color);
        }
        return detected;
    }
}
