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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CameraImageService {

    private static final Logger logger = LoggerFactory.getLogger(CameraImageService.class);

    @Autowired
    private RtspFrameGrabber rtspFrameGrabber;

    public BufferedImage capture(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            logger.warn("Camera URL is null or blank");
            return null;
        }

        try {
            if (imageUrl.startsWith("rtsp://")) {
                return rtspFrameGrabber.getLatestFrame(imageUrl);
            }
            return captureHttp(imageUrl);
        } catch (Exception e) {
            logger.error("Error fetching image from {}: {}", imageUrl, e.getMessage());
            return null;
        }
    }

    private BufferedImage captureHttp(String imageUrl) throws IOException {
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
    }
}
