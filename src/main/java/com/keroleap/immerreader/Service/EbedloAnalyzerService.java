package com.keroleap.immerreader.Service;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.keroleap.immerreader.EbedloRest;
import com.keroleap.immerreader.ErrorType;
import com.keroleap.immerreader.SharedData.EbedloManagerData;

@Service
public class EbedloAnalyzerService {

    private static final Logger logger = LoggerFactory.getLogger(EbedloAnalyzerService.class);
    private static final double TRIM_PERCENTAGE = 0.10;

    private boolean[] lastPolygonMask;
    private int lastMaskWidth;
    private int lastMaskHeight;
    private int[] lastMaskXs;
    private int[] lastMaskYs;

    private volatile BufferedImage lastAnalyzedImage;
    private volatile EbedloRest lastAnalyzedRest;
    private volatile long lastAnalyzedTimestamp;

    @Autowired
    private CameraImageService cameraImageService;

    public BufferedImage getBufferedImage(String imageUrl) {
        return cameraImageService.capture(imageUrl);
    }

    public BufferedImage getDebugOverlayImage(EbedloManagerData managerData) {
        BufferedImage source;
        synchronized (this) {
            source = lastAnalyzedImage;
        }
        if (source == null) {
            return null;
        }
        BufferedImage copy = deepCopy(source);
        int count = managerData.getPointCount();
        int[] xs = new int[count];
        int[] ys = new int[count];
        for (int i = 0; i < count; i++) {
            xs[i] = managerData.getX(i);
            ys[i] = managerData.getY(i);
        }
        drawPolygonMarkers(copy, xs, ys, count);
        return copy;
    }

    private BufferedImage deepCopy(BufferedImage source) {
        java.awt.image.ColorModel cm = source.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        java.awt.image.WritableRaster raster = source.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    public EbedloRest getEbedloRestData(BufferedImage bufferedImage, EbedloManagerData managerData) {
        EbedloRest ebedloRest = new EbedloRest();
        if (bufferedImage == null) {
            logger.warn("No image available for Ebedlo analysis");
            ebedloRest.setOn(false);
            ebedloRest.setError(true);
            ebedloRest.setErrorType(ErrorType.RTSP_CONNECTION_ERROR);
            return ebedloRest;
        }

        int count = managerData.getPointCount();
        int[] xs = new int[count];
        int[] ys = new int[count];
        int configuredPoints = 0;
        for (int i = 0; i < count; i++) {
            int x = managerData.getX(i);
            int y = managerData.getY(i);
            xs[i] = x;
            ys[i] = y;
            if (x != 0 || y != 0) {
                configuredPoints++;
            }
        }

        if (configuredPoints == 0) {
            logger.debug("No Ebedlo points configured yet, defaulting to OFF");
            ebedloRest.setOn(false);
            return ebedloRest;
        }

        if (configuredPoints < 3) {
            logger.warn("Only {} Ebedlo points configured, need at least 3 to form an area", configuredPoints);
            ebedloRest.setOn(false);
            return ebedloRest;
        }

        Polygon area = new Polygon(xs, ys, count);
        boolean[] mask = getOrCreatePolygonMask(area, bufferedImage.getWidth(), bufferedImage.getHeight(), xs, ys);
        double averageValue = computeTrimmedMeanValueInPolygon(bufferedImage, area, mask);
        boolean on = averageValue > managerData.getThreshold();

        ebedloRest.setOn(on);
        ebedloRest.setAverageValue(Math.round(averageValue));

        synchronized (this) {
            lastAnalyzedImage = bufferedImage;
            lastAnalyzedRest = ebedloRest;
            lastAnalyzedTimestamp = System.currentTimeMillis();
        }

        return ebedloRest;
    }

    private double computeTrimmedMeanValueInPolygon(BufferedImage image, Polygon polygon, boolean[] mask) {
        int width = image.getWidth();
        int height = image.getHeight();
        List<Integer> values = new ArrayList<>();

        Rectangle bounds = polygon.getBounds();
        int minX = Math.max(0, bounds.x);
        int minY = Math.max(0, bounds.y);
        int maxX = Math.min(width, minX + bounds.width);
        int maxY = Math.min(height, minY + bounds.height);
        int boxW = maxX - minX;
        int boxH = maxY - minY;
        if (boxW <= 0 || boxH <= 0) {
            return 0;
        }

        int[] rgb = image.getRGB(minX, minY, boxW, boxH, null, 0, boxW);

        for (int y = minY; y < maxY; y++) {
            int rowOffset = (y - minY) * boxW;
            int maskRowOffset = y * width;
            for (int x = minX; x < maxX; x++) {
                if (mask[maskRowOffset + x]) {
                    int pixel = rgb[rowOffset + (x - minX)];
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;
                    values.add(Math.max(r, Math.max(g, b)));
                }
            }
        }

        if (values.isEmpty()) {
            return 0;
        }

        Collections.sort(values);
        int trimCount = (int) Math.floor(values.size() * TRIM_PERCENTAGE);
        int start = trimCount;
        int end = values.size() - trimCount;
        if (end <= start) {
            start = 0;
            end = values.size();
        }

        long total = 0;
        for (int i = start; i < end; i++) {
            total += values.get(i);
        }
        return (double) total / (end - start);
    }

    private boolean[] getOrCreatePolygonMask(Polygon polygon, int width, int height, int[] xs, int[] ys) {
        if (lastPolygonMask != null
                && lastMaskWidth == width
                && lastMaskHeight == height
                && Arrays.equals(lastMaskXs, xs)
                && Arrays.equals(lastMaskYs, ys)) {
            return lastPolygonMask;
        }
        lastPolygonMask = computePolygonMask(polygon, width, height);
        lastMaskWidth = width;
        lastMaskHeight = height;
        lastMaskXs = Arrays.copyOf(xs, xs.length);
        lastMaskYs = Arrays.copyOf(ys, ys.length);
        return lastPolygonMask;
    }

    private boolean[] computePolygonMask(Polygon polygon, int width, int height) {
        boolean[] mask = new boolean[width * height];
        Rectangle bounds = polygon.getBounds();
        int minX = Math.max(0, bounds.x);
        int minY = Math.max(0, bounds.y);
        int maxX = Math.min(width, bounds.x + bounds.width);
        int maxY = Math.min(height, bounds.y + bounds.height);
        for (int y = minY; y < maxY; y++) {
            int rowOffset = y * width;
            for (int x = minX; x < maxX; x++) {
                if (polygon.contains(x, y)) {
                    mask[rowOffset + x] = true;
                }
            }
        }
        return mask;
    }

    private void drawPolygonMarkers(BufferedImage image, int[] xs, int[] ys, int count) {
        for (int i = 0; i < count; i++) {
            if (xs[i] == 0 && ys[i] == 0) {
                continue;
            }
            int next = (i + 1) % count;
            while (xs[next] == 0 && ys[next] == 0 && next != i) {
                next = (next + 1) % count;
            }
            if (next != i) {
                drawLine(image, xs[i], ys[i], xs[next], ys[next], 16777215);
            }
            drawCross(image, xs[i], ys[i], i == 0 ? 16711680 : 65280);
        }
    }

    private void drawLine(BufferedImage image, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        int width = image.getWidth();
        int height = image.getHeight();

        while (true) {
            if (x1 >= 0 && x1 < width && y1 >= 0 && y1 < height) {
                image.setRGB(x1, y1, color);
            }
            if (x1 == x2 && y1 == y2) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    private void drawCross(BufferedImage image, int x, int y, int color) {
        int width = image.getWidth();
        int height = image.getHeight();
        for (int a = x - 5; a < x + 5; a++) {
            if (a >= 0 && a < width) {
                image.setRGB(a, y, color);
            }
        }
        for (int b = y - 5; b < y + 5; b++) {
            if (b >= 0 && b < height) {
                image.setRGB(x, b, color);
            }
        }
    }
}
