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
                                          int endX, int endY) {
        int[] xs = new int[POINT_COUNT];
        int[] ys = new int[POINT_COUNT];
        boolean[] detected = new boolean[POINT_COUNT];
        int lastDetectedIndex = -1;

        for (int i = 0; i < POINT_COUNT; i++) {
            double t = (double) i / (POINT_COUNT - 1);
            xs[i] = (int) Math.round(startX + (endX - startX) * t);
            ys[i] = (int) Math.round(startY + (endY - startY) * t);
            detected[i] = detectLightValue(xs[i], ys[i], bufferedImage);
            if (detected[i]) {
                lastDetectedIndex = i;
            }
        }

        for (int i = 0; i < POINT_COUNT; i++) {
            drawRedCross(xs[i], ys[i], bufferedImage, detected[i]);
        }

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

    private boolean detectLightValue(int x, int y, BufferedImage image) {
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
        int color = detected ? 16711680 : 16777215;
        for (int a = x - 5; a < x + 5; a++) {
            image.setRGB(a, y, color);
        }
        for (int b = y - 5; b < y + 5; b++) {
            image.setRGB(x, b, color);
        }
    }
}
