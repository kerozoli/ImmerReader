package com.keroleap.immerreader.service;

import com.keroleap.immerreader.ImmerRest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;

@Service
public class ImmerParserService {
    private static final Logger logger = LoggerFactory.getLogger(ImmerParserService.class);
    private static final int LIGHT_THRESHOLD = -2500000;

    @Autowired
    private ImageProcessingService imageProcessingService;

    private int previousTempValue;

    public ImmerRest parse(BufferedImage bufferedImage) {
        if (bufferedImage == null) {
            return new ImmerRest();
        }

        boolean heating = imageProcessingService.getLightValueAndDrawCross(495, 215, bufferedImage, LIGHT_THRESHOLD, false);
        boolean levelOne = imageProcessingService.getLightValueAndDrawCross(305, 150, bufferedImage, LIGHT_THRESHOLD, false);
        boolean levelTwo = imageProcessingService.getLightValueAndDrawCross(334, 150, bufferedImage, LIGHT_THRESHOLD, false);
        boolean levelThree = imageProcessingService.getLightValueAndDrawCross(362, 150, bufferedImage, LIGHT_THRESHOLD, false);
        boolean levelFour = imageProcessingService.getLightValueAndDrawCross(390, 150, bufferedImage, LIGHT_THRESHOLD, false);

        boolean boilerOn = imageProcessingService.getLightValueAndDrawCross(490, 120, bufferedImage, LIGHT_THRESHOLD, false);

        boolean digit1_1 = imageProcessingService.getLightValueAndDrawCross(306, 178, bufferedImage, LIGHT_THRESHOLD, false);
        boolean digit1_2 = imageProcessingService.getLightValueAndDrawCross(291, 199, bufferedImage, LIGHT_THRESHOLD, false);
        boolean digit1_3 = imageProcessingService.getLightValueAndDrawCross(291, 243, bufferedImage, LIGHT_THRESHOLD, false);
        boolean digit1_4 = imageProcessingService.getLightValueAndDrawCross(306, 269, bufferedImage, LIGHT_THRESHOLD, false);
        boolean digit1_5 = imageProcessingService.getLightValueAndDrawCross(324, 243, bufferedImage, LIGHT_THRESHOLD, false);
        boolean digit1_6 = imageProcessingService.getLightValueAndDrawCross(324, 199, bufferedImage, LIGHT_THRESHOLD, false);
        boolean digit1_7 = imageProcessingService.getLightValueAndDrawCross(304, 224, bufferedImage, LIGHT_THRESHOLD, false);

        boolean digit2_1 = imageProcessingService.getLightValueAndDrawCross(360, 178, bufferedImage, LIGHT_THRESHOLD, false);
        boolean digit2_2 = imageProcessingService.getLightValueAndDrawCross(344, 199, bufferedImage, LIGHT_THRESHOLD, false);
        boolean digit2_3 = imageProcessingService.getLightValueAndDrawCross(344, 243, bufferedImage, LIGHT_THRESHOLD, false);
        boolean digit2_4 = imageProcessingService.getLightValueAndDrawCross(360, 268, bufferedImage, LIGHT_THRESHOLD, false);
        boolean digit2_5 = imageProcessingService.getLightValueAndDrawCross(377, 243, bufferedImage, LIGHT_THRESHOLD, false);
        boolean digit2_6 = imageProcessingService.getLightValueAndDrawCross(377, 199, bufferedImage, LIGHT_THRESHOLD, false);
        boolean digit2_7 = imageProcessingService.getLightValueAndDrawCross(360, 224, bufferedImage, LIGHT_THRESHOLD, false);

        int number1 = imageProcessingService.getNumber(digit1_1, digit1_2, digit1_3, digit1_4, digit1_5, digit1_6, digit1_7) * 10;
        int number2 = imageProcessingService.getNumber(digit2_1, digit2_2, digit2_3, digit2_4, digit2_5, digit2_6, digit2_7);
        int number = number1 + number2;

        if (number > 500) {
            number = previousTempValue;
        }
        if (!(20 < number && number < 56)) {
            number = previousTempValue;
        }

        previousTempValue = number;

        if (!heating) {
            number = 0;
        }

        int throttle = calculateThrottle(levelOne, levelTwo, levelThree, levelFour);

        ImmerRest immerRest = new ImmerRest();
        immerRest.setTemperature(number);
        immerRest.setThrottle(throttle);
        immerRest.setHeating(heating);
        immerRest.setBoilerOn(boilerOn);

        return immerRest;
    }

    private int calculateThrottle(boolean levelOne, boolean levelTwo, boolean levelThree, boolean levelFour) {
        if (levelFour) return 4;
        if (levelThree) return 3;
        if (levelTwo) return 2;
        if (levelOne) return 1;
        return 0;
    }
}
