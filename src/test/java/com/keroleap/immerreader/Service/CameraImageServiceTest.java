package com.keroleap.immerreader.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sun.net.httpserver.HttpServer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CameraImageServiceTest {

    private HttpServer server;
    private String imageUrl;
    private CameraImageService cameraImageService;

    private final AtomicInteger requestCount = new AtomicInteger();
    private volatile int responseDelayMs;
    private volatile int responseStatus = 200;
    private volatile byte[] payload;

    @BeforeEach
    void setUp() throws IOException {
        payload = pngBytes(20, 20);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/image", exchange -> {
            requestCount.incrementAndGet();
            if (responseDelayMs > 0) {
                try {
                    Thread.sleep(responseDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            exchange.sendResponseHeaders(responseStatus, payload.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(payload);
            } catch (IOException ignored) {
                // Client timed out and closed the connection.
            }
            exchange.close();
        });
        server.start();
        imageUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/image";

        cameraImageService = new CameraImageService();
        ReflectionTestUtils.setField(cameraImageService, "rtspFrameGrabber", mock(RtspFrameGrabber.class));
        ReflectionTestUtils.setField(cameraImageService, "connectTimeoutMs", 500);
        ReflectionTestUtils.setField(cameraImageService, "readTimeoutMs", 250);
        ReflectionTestUtils.setField(cameraImageService, "cacheTtlMs", 1000L);
    }

    @AfterEach
    void tearDown() {
        stopServer();
    }

    @Test
    void capture_returnsDecodedImage() {
        BufferedImage image = cameraImageService.capture(imageUrl);

        assertNotNull(image);
        assertEquals(20, image.getWidth());
        assertEquals(20, image.getHeight());
        assertEquals(1, requestCount.get());
    }

    @Test
    void capture_reusesFrameWithinCacheTtl() {
        cameraImageService.capture(imageUrl);
        cameraImageService.capture(imageUrl);
        cameraImageService.capture(imageUrl);

        assertEquals(1, requestCount.get());
    }

    @Test
    void capture_refetchesAfterCacheTtlExpires() {
        ReflectionTestUtils.setField(cameraImageService, "cacheTtlMs", 0L);

        cameraImageService.capture(imageUrl);
        cameraImageService.capture(imageUrl);

        assertEquals(2, requestCount.get());
    }

    @Test
    void capture_returnsCopyOfCachedFrame() {
        BufferedImage first = cameraImageService.capture(imageUrl);
        first.setRGB(0, 0, 0x00FF00);
        BufferedImage second = cameraImageService.capture(imageUrl);

        assertNotSame(first, second);
        assertNotEquals(0x00FF00, second.getRGB(0, 0));
        assertEquals(1, requestCount.get());
    }

    @Test
    void capture_throwsWhenCameraReturnsError() {
        responseStatus = 400;

        CameraUnavailableException exception = assertThrows(CameraUnavailableException.class,
                () -> cameraImageService.capture(imageUrl));

        assertFalse(exception.isTimedOut());
    }

    @Test
    void capture_reportsTimeout() {
        responseDelayMs = 600;

        CameraUnavailableException exception = assertThrows(CameraUnavailableException.class,
                () -> cameraImageService.capture(imageUrl));

        assertTrue(exception.isTimedOut());
    }

    @Test
    void captureWithFallback_returnsLastGoodFrame() {
        BufferedImage first = cameraImageService.captureWithFallback(imageUrl);
        assertNotNull(first);

        stopServer();
        ReflectionTestUtils.setField(cameraImageService, "cacheTtlMs", 0L);

        BufferedImage fallback = cameraImageService.captureWithFallback(imageUrl);

        assertNotNull(fallback);
        assertEquals(20, fallback.getWidth());
    }

    @Test
    void captureWithFallback_returnsNullWhenNeverAvailable() {
        stopServer();

        assertNull(cameraImageService.captureWithFallback(imageUrl));
    }

    @Test
    void capture_returnsNullForBlankUrlOnlyViaFallback() {
        assertNull(cameraImageService.captureWithFallback(" "));
        assertThrows(CameraUnavailableException.class, () -> cameraImageService.capture(null));
    }

    private void stopServer() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private static byte[] pngBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, 0x102030);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
