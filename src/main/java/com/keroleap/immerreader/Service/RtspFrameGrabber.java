package com.keroleap.immerreader.Service;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

@Service
public class RtspFrameGrabber {

    private static final Logger logger = LoggerFactory.getLogger(RtspFrameGrabber.class);
    private static final long FRAME_TIMEOUT_MS = 10000;
    private static final long GRABBER_ERROR_DELAY_MS = 2000;

    private final Map<String, StreamHolder> holders = new ConcurrentHashMap<>();
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public BufferedImage getLatestFrame(String rtspUrl) {
        StreamHolder holder = holders.computeIfAbsent(rtspUrl, StreamHolder::new);
        return holder.getFrame();
    }

    public void stopStream(String rtspUrl) {
        StreamHolder holder = holders.remove(rtspUrl);
        if (holder != null) {
            logger.info("Stopping RTSP stream for {}", rtspUrl);
            holder.stop();
        }
    }

    @PreDestroy
    public void destroy() {
        shutdown.set(true);
        holders.values().forEach(StreamHolder::stop);
    }

    private class StreamHolder {
        private final String rtspUrl;
        private final ReentrantLock lock = new ReentrantLock();
        private final AtomicLong lastFrameTime = new AtomicLong(0);
        private volatile BufferedImage latestFrame;
        private volatile Thread workerThread;

        StreamHolder(String rtspUrl) {
            this.rtspUrl = rtspUrl;
            startWorker();
        }

        void startWorker() {
            lock.lock();
            try {
                if (workerThread != null && workerThread.isAlive()) {
                    return;
                }
                Thread thread = new Thread(this::runGrabber, "rtsp-grabber-" + rtspUrl.replaceAll("[^a-zA-Z0-9]", "_"));
                thread.setDaemon(true);
                workerThread = thread;
                thread.start();
            } finally {
                lock.unlock();
            }
        }

        BufferedImage getFrame() {
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < FRAME_TIMEOUT_MS) {
                BufferedImage frame = latestFrame;
                if (frame != null && System.currentTimeMillis() - lastFrameTime.get() < FRAME_TIMEOUT_MS) {
                    return copyImage(frame);
                }
                startWorker();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            logger.warn("No fresh frame available for {} after {} ms", rtspUrl, FRAME_TIMEOUT_MS);
            return copyImage(latestFrame);
        }

        private BufferedImage copyImage(BufferedImage source) {
            if (source == null) {
                return null;
            }
            BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
            Graphics2D g = copy.createGraphics();
            g.drawImage(source, 0, 0, null);
            g.dispose();
            return copy;
        }

        void runGrabber() {
            logger.info("Starting RTSP grabber for {}", rtspUrl);
            while (!shutdown.get() && !Thread.currentThread().isInterrupted()) {
                try (FFmpegFrameGrabber grabber = createGrabber();
                     Java2DFrameConverter converter = new Java2DFrameConverter()) {
                    grabber.start();
                    while (!shutdown.get() && !Thread.currentThread().isInterrupted()) {
                        Frame frame = grabber.grabImage();
                        if (frame == null) {
                            break;
                        }
                        BufferedImage image = converter.convert(frame);
                        if (image != null) {
                            latestFrame = image;
                            lastFrameTime.set(System.currentTimeMillis());
                        }
                    }
                } catch (Exception e) {
                    if (!shutdown.get()) {
                        logger.warn("RTSP grabber error for {}: {}", rtspUrl, e.getMessage());
                    }
                }
                if (!shutdown.get()) {
                    try {
                        Thread.sleep(GRABBER_ERROR_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            logger.info("RTSP grabber stopped for {}", rtspUrl);
        }

        FFmpegFrameGrabber createGrabber() {
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(rtspUrl);
            grabber.setOption("rtsp_transport", "tcp");
            grabber.setOption("stimeout", "5000000");
            grabber.setImageWidth(0);
            grabber.setImageHeight(0);
            grabber.setFrameRate(0);
            return grabber;
        }

        void stop() {
            Thread thread;
            lock.lock();
            try {
                thread = workerThread;
                workerThread = null;
            } finally {
                lock.unlock();
            }
            if (thread != null) {
                thread.interrupt();
            }
        }
    }
}
