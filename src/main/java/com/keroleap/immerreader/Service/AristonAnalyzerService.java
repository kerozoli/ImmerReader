package com.keroleap.immerreader.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.keroleap.immerreader.AristonRest;

@Service
public class AristonAnalyzerService {

    private static final Logger logger = LoggerFactory.getLogger(AristonAnalyzerService.class);
    private static final int LIGHT_THRESHOLD = -7000000;
    private static final int POINT_COUNT = 50;

    public AristonRest getAristonRestData(BufferedImage bufferedImage,
                                          int startX, int startY,
                                          int endX, int endY) {
        int percentage = 0;
        int lastDetectedIndex = -1;
        for (int i = 0; i < POINT_COUNT; i++) {
            double t = (double) i / (POINT_COUNT - 1);
            int x = (int) Math.round(startX + (endX - startX) * t);
            int y = (int) Math.round(startY + (endY - startY) * t);
            if (getLightValueAndDrawRedCross(x, y, bufferedImage)) {
                lastDetectedIndex = i;
            }
        }
        if (lastDetectedIndex >= 0) {
            percentage = (int) Math.round(lastDetectedIndex * 100.0 / (POINT_COUNT - 1));
        }
        AristonRest aristonRest = new AristonRest();
        aristonRest.setPercentage(percentage);
        return aristonRest;
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

    private boolean getLightValueAndDrawRedCross(int x, int y, BufferedImage image) {
        long sum = 0;
        for (int a = x - 3; a < x + 3; a++) {
            sum += image.getRGB(a, y);
        }
        for (int b = y - 3; b < y + 3; b++) {
            sum += image.getRGB(x, b);
        }
        double lightValue = sum / 12.0;
        boolean detected = lightValue < LIGHT_THRESHOLD;
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
