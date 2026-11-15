package com.keroleap.immerreader.Service;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLConnection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Fetches camera frames.
 *
 * <p>Every fetch is bounded by a connect and a read timeout so a hung camera can
 * never block a calling thread forever. Successfully fetched frames are cached
 * for a short TTL, which means two consumers asking for the same camera at the
 * same time (for example the scheduler and the manager preview) share a single
 * upstream request instead of doubling the load.
 *
 * <p>Two entry points exist on purpose:
 * <ul>
 *   <li>{@link #capture(String)} is strict and throws when no fresh frame can be
 *       obtained. Schedulers use it so a camera outage is never silently
 *       reported as a valid reading.</li>
 *   <li>{@link #captureWithFallback(String)} never throws and returns the last
 *       good frame when the camera is unavailable. Controllers use it so a
 *       preview keeps showing the last image instead of failing.</li>
 * </ul>
 */
@Service
public class CameraImageService {

    private static final Logger logger = LoggerFactory.getLogger(CameraImageService.class);

    private static final int MAX_IMAGE_BYTES = 20 * 1024 * 1024;
    private static final int READ_BUFFER_SIZE = 8192;

    @Value("${camera.http.connect-timeout-ms:2000}")
    private int connectTimeoutMs = 2000;

    @Value("${camera.http.read-timeout-ms:5000}")
    private int readTimeoutMs = 5000;

    @Value("${camera.http.cache-ttl-ms:1000}")
    private long cacheTtlMs = 1000;

    @Autowired
    private RtspFrameGrabber rtspFrameGrabber;

    private final Map<String, FrameCache> caches = new ConcurrentHashMap<>();

    /**
     * Returns a copy of a frame no older than the configured TTL.
     *
     * @throws CameraUnavailableException when no fresh frame can be obtained
     */
    public BufferedImage capture(String imageUrl) {
        return captureInternal(imageUrl, false);
    }

    /**
     * Returns a copy of the freshest available frame, falling back to the last
     * good frame when the camera cannot be reached.
     *
     * @return the image, or {@code null} when the camera has never produced one
     */
    public BufferedImage captureWithFallback(String imageUrl) {
        return captureInternal(imageUrl, true);
    }

    private BufferedImage captureInternal(String imageUrl, boolean fallback) {
        if (imageUrl == null || imageUrl.isBlank()) {
            logger.warn("Camera URL is null or blank");
            if (fallback) {
                return null;
            }
            throw new CameraUnavailableException("Camera URL is null or blank", false);
        }

        if (imageUrl.startsWith("rtsp://")) {
            // RtspFrameGrabber already returns a copy and keeps the last frame itself.
            BufferedImage frame = rtspFrameGrabber.getLatestFrame(imageUrl);
            if (frame == null && !fallback) {
                throw new CameraUnavailableException("No RTSP frame available for " + imageUrl, true);
            }
            return frame;
        }

        FrameCache cache = caches.computeIfAbsent(imageUrl, key -> new FrameCache());
        synchronized (cache) {
            long now = System.currentTimeMillis();
            if (cache.image != null && now - cache.timestamp < cacheTtlMs) {
                return copyImage(cache.image);
            }
            if (cache.lastAttempt > 0 && now - cache.lastAttempt < cacheTtlMs) {
                // A fetch was attempted moments ago. Do not hammer the camera with
                // a second attempt while the first result is still fresh.
                return recentFailure(cache, imageUrl, fallback);
            }

            cache.lastAttempt = now;
            try {
                BufferedImage fresh = captureHttp(imageUrl);
                if (fresh == null) {
                    throw new IOException("Payload from " + imageUrl + " is not a decodable image");
                }
                cache.image = fresh;
                cache.timestamp = System.currentTimeMillis();
                cache.lastFailureTimedOut = false;
                return copyImage(fresh);
            } catch (SocketTimeoutException e) {
                logger.warn("Camera timeout for {}: {}", imageUrl, e.getMessage());
                cache.lastFailureTimedOut = true;
                return recentFailure(cache, imageUrl, fallback);
            } catch (IOException | RuntimeException e) {
                logger.warn("Camera fetch failed for {}: {}", imageUrl, e.getMessage());
                cache.lastFailureTimedOut = false;
                return recentFailure(cache, imageUrl, fallback);
            }
        }
    }

    private BufferedImage recentFailure(FrameCache cache, String imageUrl, boolean fallback) {
        if (fallback) {
            return copyImage(cache.image);
        }
        throw new CameraUnavailableException("Camera unavailable: " + imageUrl, cache.lastFailureTimedOut);
    }

    private BufferedImage captureHttp(String imageUrl) throws IOException {
        URLConnection connection = URI.create(imageUrl).toURL().openConnection();
        connection.setConnectTimeout(connectTimeoutMs);
        connection.setReadTimeout(readTimeoutMs);
        connection.setRequestProperty("Connection", "close");
        try (InputStream stream = connection.getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[READ_BUFFER_SIZE];
            int bytesRead;
            int totalBytes = 0;
            while ((bytesRead = stream.read(chunk)) > 0) {
                totalBytes += bytesRead;
                if (totalBytes > MAX_IMAGE_BYTES) {
                    throw new IOException("Image from " + imageUrl + " exceeds " + MAX_IMAGE_BYTES + " bytes");
                }
                outputStream.write(chunk, 0, bytesRead);
            }

            try (ByteArrayInputStream input = new ByteArrayInputStream(outputStream.toByteArray())) {
                return ImageIO.read(input);
            }
        } finally {
            if (connection instanceof HttpURLConnection httpConnection) {
                httpConnection.disconnect();
            }
        }
    }

    private BufferedImage copyImage(BufferedImage source) {
        if (source == null) {
            return null;
        }
        int type = source.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_RGB : source.getType();
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), type);
        Graphics2D graphics = copy.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return copy;
    }

    private static final class FrameCache {
        private BufferedImage image;
        private long timestamp;
        private long lastAttempt;
        private boolean lastFailureTimedOut;
    }
}
