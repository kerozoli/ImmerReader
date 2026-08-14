package com.keroleap.immerreader.Service;

import java.awt.image.BufferedImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.keroleap.immerreader.AristonRest;

@Service
public class AristonAnalyzerService {

    private static final Logger logger = LoggerFactory.getLogger(AristonAnalyzerService.class);
    private static final int LIGHT_THRESHOLD = -7000000;
    private static final int POINT_COUNT = 50;

    @Autowired
    private CameraImageService cameraImageService;

    public AristonRest getAristonRestData(BufferedImage bufferedImage,
                                          int startX, int startY,
                                          int controlX, int controlY,
                                          int endX, int endY) {
        int[] xs = new int[POINT_COUNT];
        int[] ys = new int[POINT_COUNT];
        boolean[] detected = new boolean[POINT_COUNT];

        for (int i = 0; i < POINT_COUNT; i++) {
            double t = (double) i / (POINT_COUNT - 1);
            double oneMinusT = 1 - t;
            xs[i] = (int) Math.round(oneMinusT * oneMinusT * startX
                    + 2 * oneMinusT * t * controlX
                    + t * t * endX);
            ys[i] = (int) Math.round(oneMinusT * oneMinusT * startY
                    + 2 * oneMinusT * t * controlY
                    + t * t * endY);
            detected[i] = detectLightValue(xs[i], ys[i], bufferedImage);
        }

        for (int i = 0; i < POINT_COUNT; i++) {
            drawRedCross(xs[i], ys[i], bufferedImage, detected[i]);
        }

        int lastDetectedIndex = findLongestBrightRun(detected);
        int percentage = lastDetectedIndex >= 0
                ? (int) Math.round(lastDetectedIndex * 100.0 / (POINT_COUNT - 1))
                : 0;
        AristonRest aristonRest = new AristonRest();
        aristonRest.setPercentage(percentage);
        return aristonRest;
    }

    public BufferedImage getBufferedImage(String imageUrl) {
        return cameraImageService.capture(imageUrl);
    }

    private int findLongestBrightRun(boolean[] detected) {
        int bestStart = -1;
        int bestLength = 0;
        int currentStart = -1;
        int currentLength = 0;

        for (int i = 0; i < detected.length; i++) {
            if (detected[i]) {
                if (currentStart < 0) {
                    currentStart = i;
                    currentLength = 1;
                } else {
                    currentLength++;
                }
            } else {
                if (currentLength > bestLength) {
                    bestStart = currentStart;
                    bestLength = currentLength;
                }
                currentStart = -1;
                currentLength = 0;
            }
        }
        if (currentLength > bestLength) {
            bestStart = currentStart;
            bestLength = currentLength;
        }
        return bestLength >= 3 ? bestStart + bestLength - 1 : -1;
    }

    private boolean detectLightValue(int x, int y, BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (x < 3 || x >= width - 3 || y < 3 || y >= height - 3) {
            return false;
        }
        long sum = 0;
        for (int a = x - 3; a < x + 3; a++) {
            sum += image.getRGB(a, y);
        }
        for (int b = y - 3; b < y + 3; b++) {
            sum += image.getRGB(x, b);
        }
        double lightValue = sum / 12.0;
        return lightValue < LIGHT_THRESHOLD;
    }

    private void drawRedCross(int x, int y, BufferedImage image, boolean detected) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (x < 5 || x >= width - 5 || y < 5 || y >= height - 5) {
            return;
        }
        int color = detected ? 16711680 : 16777215;
        for (int a = x - 5; a < x + 5; a++) {
            image.setRGB(a, y, color);
        }
        for (int b = y - 5; b < y + 5; b++) {
            image.setRGB(x, b, color);
        }
    }
}
