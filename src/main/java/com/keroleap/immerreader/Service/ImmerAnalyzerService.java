package com.keroleap.immerreader.Service;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.keroleap.immerreader.ErrorType;
import com.keroleap.immerreader.ImmerRest;
import com.keroleap.immerreader.SharedData.ErrorStatistics;
import com.keroleap.immerreader.SharedData.ImmerManagerData;

@Service
public class ImmerAnalyzerService {

    private static final Logger logger = LoggerFactory.getLogger(ImmerAnalyzerService.class);
    private static final int LIGHT_THRESHOLD = -2500000;
    private static final int DEFAULT_REFERENCE_X = 150;
    private static final int DEFAULT_REFERENCE_Y = 150;

    private final AtomicInteger previousTempValue = new AtomicInteger(0);

    @Autowired(required = false)
    private ErrorStatistics errorStatistics;

    @Autowired
    private CameraImageService cameraImageService;

    @Autowired(required = false)
    private ImmerManagerData immerManagerData;

    public ImmerRest getImmerRestData(BufferedImage bufferedImage, int[] xs, int[] ys) {
        if (bufferedImage == null) {
            logger.warn("No image available for Immer analysis");
            ImmerRest errorRest = new ImmerRest();
            errorRest.setError(true);
            errorRest.setErrorType(ErrorType.FETCH_ERROR);
            return errorRest;
        }

        if (xs == null || ys == null || xs.length != ImmerManagerData.POINT_COUNT || ys.length != ImmerManagerData.POINT_COUNT) {
            throw new IllegalArgumentException("Expected " + ImmerManagerData.POINT_COUNT + " x and y coordinates");
        }

        boolean lightMode = false;
        int activeThreshold = LIGHT_THRESHOLD;
        int ambientBrightness = 0;

        if (immerManagerData != null) {
            int refX = immerManagerData.getReferenceX();
            int refY = immerManagerData.getReferenceY();
            ambientBrightness = measureBrightnessAt(refX, refY, bufferedImage);
            int referenceThreshold = immerManagerData.getReferenceThreshold();
            int hysteresis = immerManagerData.getReferenceHysteresis();
            boolean currentlyLight = immerManagerData.isLightMode();

            if (currentlyLight) {
                lightMode = ambientBrightness > (referenceThreshold - hysteresis);
            } else {
                lightMode = ambientBrightness > (referenceThreshold + hysteresis);
            }

            activeThreshold = lightMode ? immerManagerData.getLightThreshold() : immerManagerData.getDarkThreshold();
            immerManagerData.setAmbientBrightness(ambientBrightness);
            immerManagerData.setLightMode(lightMode);

            drawReferenceCross(refX, refY, bufferedImage, lightMode);
        }

        boolean heating = getLightValueAnnDrawRedCross(xs[ImmerManagerData.HEATING], ys[ImmerManagerData.HEATING], bufferedImage, activeThreshold);
        boolean levelOne = getLightValueAnnDrawRedCross(xs[ImmerManagerData.LEVEL_ONE], ys[ImmerManagerData.LEVEL_ONE], bufferedImage, activeThreshold);
        boolean levelTwo = getLightValueAnnDrawRedCross(xs[ImmerManagerData.LEVEL_TWO], ys[ImmerManagerData.LEVEL_TWO], bufferedImage, activeThreshold);
        boolean levelThree = getLightValueAnnDrawRedCross(xs[ImmerManagerData.LEVEL_THREE], ys[ImmerManagerData.LEVEL_THREE], bufferedImage, activeThreshold);
        boolean levelFour = getLightValueAnnDrawRedCross(xs[ImmerManagerData.LEVEL_FOUR], ys[ImmerManagerData.LEVEL_FOUR], bufferedImage, activeThreshold);

        boolean boilerOn = getLightValueAnnDrawRedCross(xs[ImmerManagerData.BOILER], ys[ImmerManagerData.BOILER], bufferedImage, activeThreshold);

        boolean digit1_1 = getLightValueAnnDrawRedCross(xs[ImmerManagerData.DIGIT1_SEG1], ys[ImmerManagerData.DIGIT1_SEG1], bufferedImage, activeThreshold);
        boolean digit1_2 = getLightValueAnnDrawRedCross(xs[ImmerManagerData.DIGIT1_SEG2], ys[ImmerManagerData.DIGIT1_SEG2], bufferedImage, activeThreshold);
        boolean digit1_3 = getLightValueAnnDrawRedCross(xs[ImmerManagerData.DIGIT1_SEG3], ys[ImmerManagerData.DIGIT1_SEG3], bufferedImage, activeThreshold);
        boolean digit1_4 = getLightValueAnnDrawRedCross(xs[ImmerManagerData.DIGIT1_SEG4], ys[ImmerManagerData.DIGIT1_SEG4], bufferedImage, activeThreshold);
        boolean digit1_5 = getLightValueAnnDrawRedCross(xs[ImmerManagerData.DIGIT1_SEG5], ys[ImmerManagerData.DIGIT1_SEG5], bufferedImage, activeThreshold);
        boolean digit1_6 = getLightValueAnnDrawRedCross(xs[ImmerManagerData.DIGIT1_SEG6], ys[ImmerManagerData.DIGIT1_SEG6], bufferedImage, activeThreshold);
        boolean digit1_7 = getLightValueAnnDrawRedCross(xs[ImmerManagerData.DIGIT1_SEG7], ys[ImmerManagerData.DIGIT1_SEG7], bufferedImage, activeThreshold);

        boolean digit2_1 = getLightValueAnnDrawRedCross(xs[ImmerManagerData.DIGIT2_SEG1], ys[ImmerManagerData.DIGIT2_SEG1], bufferedImage, activeThreshold);
        boolean digit2_2 = getLightValueAnnDrawRedCross(xs[ImmerManagerData.DIGIT2_SEG2], ys[ImmerManagerData.DIGIT2_SEG2], bufferedImage, activeThreshold);
        boolean digit2_3 = getLightValueAnnDrawRedCross(xs[ImmerManagerData.DIGIT2_SEG3], ys[ImmerManagerData.DIGIT2_SEG3], bufferedImage, activeThreshold);
        boolean digit2_4 = getLightValueAnnDrawRedCross(xs[ImmerManagerData.DIGIT2_SEG4], ys[ImmerManagerData.DIGIT2_SEG4], bufferedImage, activeThreshold);
        boolean digit2_5 = getLightValueAnnDrawRedCross(xs[ImmerManagerData.DIGIT2_SEG5], ys[ImmerManagerData.DIGIT2_SEG5], bufferedImage, activeThreshold);
        boolean digit2_6 = getLightValueAnnDrawRedCross(xs[ImmerManagerData.DIGIT2_SEG6], ys[ImmerManagerData.DIGIT2_SEG6], bufferedImage, activeThreshold);
        boolean digit2_7 = getLightValueAnnDrawRedCross(xs[ImmerManagerData.DIGIT2_SEG7], ys[ImmerManagerData.DIGIT2_SEG7], bufferedImage, activeThreshold);

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

        if (lightMode) {
            logger.debug("Immer analysis in LIGHT mode (ambient={}, threshold={})", ambientBrightness, activeThreshold);
        }

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
                errorStatistics.recordError("Immer", ErrorType.UNKNOWN_DIGIT);
            }
        }
        return number;
    }

    public BufferedImage getBufferedImage(String imageUrl) {
        return cameraImageService.capture(imageUrl);
    }

    private int measureBrightnessAt(int x, int y, BufferedImage image) {
        if (image == null || x < 3 || y < 3 || x >= image.getWidth() - 3 || y >= image.getHeight() - 3) {
            return 0;
        }
        long sum = 0;
        for (int a = x - 3; a < x + 3; a++) {
            sum += image.getRGB(a, y);
        }
        for (int b = y - 3; b < y + 3; b++) {
            sum += image.getRGB(x, b);
        }
        return (int) Math.round(sum / 12.0);
    }

    private boolean getLightValueAnnDrawRedCross(int x, int y, BufferedImage image, int threshold) {
        long sum = 0;
        for (int a = x - 3; a < x + 3; a++) {
            sum += image.getRGB(a, y);
        }
        for (int b = y - 3; b < y + 3; b++) {
            sum += image.getRGB(x, b);
        }
        double lightValue = sum / 12.0;
        boolean detected = lightValue > threshold;
        int color = detected ? 16711680 : 16777215;
        for (int a = x - 5; a < x + 5; a++) {
            image.setRGB(a, y, color);
        }
        for (int b = y - 5; b < y + 5; b++) {
            image.setRGB(x, b, color);
        }
        return detected;
    }

    private void drawReferenceCross(int x, int y, BufferedImage image, boolean lightMode) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (x < 5 || x >= width - 5 || y < 5 || y >= height - 5) {
            return;
        }
        // Green when light mode is active, blue when dark mode is active
        int color = lightMode ? 65280 : 255;
        for (int a = x - 5; a < x + 5; a++) {
            image.setRGB(a, y, color);
        }
        for (int b = y - 5; b < y + 5; b++) {
            image.setRGB(x, b, color);
        }
    }
}
