package com.keroleap.immerreader.Service;

import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.keroleap.immerreader.EbedloRest;
import com.keroleap.immerreader.ErrorType;
import com.keroleap.immerreader.PlateRest;
import com.keroleap.immerreader.PlateRest.PlateCenter;
import com.keroleap.immerreader.SharedData.EbedloManagerData;
import com.keroleap.immerreader.SharedData.TrimMode;

@Service
public class EbedloAnalyzerService {

    private static final Logger logger = LoggerFactory.getLogger(EbedloAnalyzerService.class);

    @Autowired
    private CameraImageService cameraImageService;

    private volatile BufferedImage cachedImage;
    private volatile long cachedImageTime;
    private static final long CACHE_TTL_MS = 1000;

    public BufferedImage getBufferedImage(String imageUrl) {
        long now = System.currentTimeMillis();
        BufferedImage cached = cachedImage;
        if (cached != null && now - cachedImageTime < CACHE_TTL_MS) {
            return cached;
        }
        BufferedImage captured = cameraImageService.capture(imageUrl);
        cachedImage = captured;
        cachedImageTime = now;
        return captured;
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
        double averageValue = computeTrimmedMeanValueInPolygon(bufferedImage, area, managerData.getTrimPercentage(), managerData.getTrimMode());
        boolean on = averageValue > managerData.getThreshold();

        ebedloRest.setOn(on);
        ebedloRest.setAverageValue(Math.round(averageValue));
        drawPolygonMarkers(bufferedImage, xs, ys, count);
        return ebedloRest;
    }

    public PlateRest detectPlates(BufferedImage bufferedImage, EbedloManagerData managerData) {
        PlateRest plateRest = new PlateRest();
        if (bufferedImage == null) {
            logger.warn("No image available for plate detection");
            return plateRest;
        }

        int count = managerData.getPlatePointCount();
        int[] xs = new int[count];
        int[] ys = new int[count];
        int configuredPoints = 0;
        for (int i = 0; i < count; i++) {
            int x = managerData.getPlateX(i);
            int y = managerData.getPlateY(i);
            xs[i] = x;
            ys[i] = y;
            if (x != 0 || y != 0) {
                configuredPoints++;
            }
        }

        if (configuredPoints < 3) {
            logger.debug("Only {} plate points configured, need at least 3 to form a search area", configuredPoints);
            return plateRest;
        }

        Polygon plateArea = new Polygon(xs, ys, count);
        double background = computeTrimmedMeanValueInPolygon(bufferedImage, plateArea,
                managerData.getTrimPercentage(), managerData.getTrimMode());
        int threshold = managerData.getPlateThreshold();
        int minRadius = managerData.getPlateMinRadius();
        int maxRadius = managerData.getPlateMaxRadius();

        List<PlateCenter> plates = findPlateCenters(bufferedImage, plateArea, background, threshold, minRadius, maxRadius);
        plateRest.setDetectedPlateCount(plates.size());
        plateRest.setPlateCenters(plates);
        drawPolygonMarkers(bufferedImage, xs, ys, count);
        for (PlateCenter plate : plates) {
            drawCircle(bufferedImage, plate.getX(), plate.getY(), plate.getRadius(), 0xFF0000);
        }
        return plateRest;
    }

    private List<PlateCenter> findPlateCenters(BufferedImage image, Polygon polygon, double background,
            int threshold, int minRadius, int maxRadius) {
        int width = image.getWidth();
        int height = image.getHeight();
        int boundsX = Math.max(0, polygon.getBounds().x);
        int boundsY = Math.max(0, polygon.getBounds().y);
        int boundsW = polygon.getBounds().width;
        int boundsH = polygon.getBounds().height;
        int maxX = Math.min(width, boundsX + boundsW);
        int maxY = Math.min(height, boundsY + boundsH);

        int localW = maxX - boundsX;
        int localH = maxY - boundsY;
        if (localW <= 0 || localH <= 0) {
            return new ArrayList<>();
        }

        boolean[][] foreground = new boolean[localH][localW];
        for (int y = boundsY; y < maxY; y++) {
            for (int x = boundsX; x < maxX; x++) {
                if (polygon.contains(x, y)) {
                    int value = getHsvValue(image.getRGB(x, y));
                    foreground[y - boundsY][x - boundsX] = Math.abs(value - background) > threshold;
                }
            }
        }

        int[][] labels = new int[localH][localW];
        List<Component> components = labelComponents(foreground, labels);

        List<PlateCenter> plates = new ArrayList<>();
        for (Component component : components) {
            if (component.area < 1) {
                continue;
            }
            double radius = Math.sqrt(component.area / Math.PI);
            if (radius < minRadius || radius > maxRadius) {
                continue;
            }
            double circularity = component.perimeter > 0
                    ? (4 * Math.PI * component.area) / (component.perimeter * component.perimeter)
                    : 0;
            if (circularity < 0.5) {
                continue;
            }
            int centerX = boundsX + (component.minX + component.maxX) / 2;
            int centerY = boundsY + (component.minY + component.maxY) / 2;
            plates.add(new PlateCenter(centerX, centerY, (int) Math.round(radius)));
        }
        return plates;
    }

    private int getHsvValue(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        float[] hsv = rgbToHsv(r, g, b);
        return Math.round(hsv[2] * 255);
    }

    private List<Component> labelComponents(boolean[][] foreground, int[][] labels) {
        int height = foreground.length;
        int width = foreground[0].length;
        int nextLabel = 1;
        List<Component> components = new ArrayList<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!foreground[y][x] || labels[y][x] != 0) {
                    continue;
                }
                Component component = new Component();
                floodFill(foreground, labels, x, y, nextLabel, component);
                components.add(component);
                nextLabel++;
            }
        }

        computePerimeters(foreground, labels, components);
        return components;
    }

    private void floodFill(boolean[][] foreground, int[][] labels, int startX, int startY, int label,
            Component component) {
        int height = foreground.length;
        int width = foreground[0].length;
        int[][] stack = new int[height * width][2];
        int top = 0;
        stack[top][0] = startX;
        stack[top][1] = startY;
        top++;

        while (top > 0) {
            top--;
            int x = stack[top][0];
            int y = stack[top][1];
            if (x < 0 || x >= width || y < 0 || y >= height || !foreground[y][x] || labels[y][x] != 0) {
                continue;
            }
            labels[y][x] = label;
            component.area++;
            component.minX = Math.min(component.minX, x);
            component.maxX = Math.max(component.maxX, x);
            component.minY = Math.min(component.minY, y);
            component.maxY = Math.max(component.maxY, y);

            stack[top][0] = x + 1;
            stack[top][1] = y;
            top++;
            stack[top][0] = x - 1;
            stack[top][1] = y;
            top++;
            stack[top][0] = x;
            stack[top][1] = y + 1;
            top++;
            stack[top][0] = x;
            stack[top][1] = y - 1;
            top++;
        }
    }

    private void computePerimeters(boolean[][] foreground, int[][] labels, List<Component> components) {
        int height = foreground.length;
        int width = foreground[0].length;
        for (Component component : components) {
            component.perimeter = 0;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!foreground[y][x]) {
                    continue;
                }
                int label = labels[y][x];
                if (label < 1 || label > components.size()) {
                    continue;
                }
                Component component = components.get(label - 1);
                if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
                    component.perimeter++;
                } else {
                    if (!foreground[y - 1][x]) component.perimeter++;
                    if (!foreground[y + 1][x]) component.perimeter++;
                    if (!foreground[y][x - 1]) component.perimeter++;
                    if (!foreground[y][x + 1]) component.perimeter++;
                }
            }
        }
    }

    private static class Component {
        int area = 0;
        int perimeter = 0;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
    }

    private double computeTrimmedMeanValueInPolygon(BufferedImage image, Polygon polygon, double trimPercentage, TrimMode trimMode) {
        int width = image.getWidth();
        int height = image.getHeight();
        List<Integer> values = new ArrayList<>();

        int minX = Math.max(0, polygon.getBounds().x);
        int minY = Math.max(0, polygon.getBounds().y);
        int maxX = Math.min(width, minX + polygon.getBounds().width);
        int maxY = Math.min(height, minY + polygon.getBounds().height);

        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
                if (polygon.contains(x, y)) {
                    int rgb = image.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    float[] hsv = rgbToHsv(r, g, b);
                    values.add(Math.round(hsv[2] * 255));
                }
            }
        }

        if (values.isEmpty()) {
            return 0;
        }

        Collections.sort(values);
        int trimCount = (int) Math.floor(values.size() * trimPercentage);
        int start = 0;
        int end = values.size();
        if (trimMode == TrimMode.BOTH) {
            start = trimCount;
            end = values.size() - trimCount;
        } else if (trimMode == TrimMode.LOWER) {
            start = trimCount;
        } else if (trimMode == TrimMode.UPPER) {
            end = values.size() - trimCount;
        }
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
            if (a >= 0 && a < width && y >= 0 && y < height) {
                image.setRGB(a, y, color);
            }
        }
        for (int b = y - 5; b < y + 5; b++) {
            if (b >= 0 && b < height && x >= 0 && x < width) {
                image.setRGB(x, b, color);
            }
        }
    }

    private void drawCircle(BufferedImage image, int centerX, int centerY, int radius, int color) {
        if (radius <= 0) {
            return;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        int x = 0;
        int y = radius;
        int d = 3 - 2 * radius;
        while (x <= y) {
            drawPixel(image, centerX + x, centerY + y, color, width, height);
            drawPixel(image, centerX - x, centerY + y, color, width, height);
            drawPixel(image, centerX + x, centerY - y, color, width, height);
            drawPixel(image, centerX - x, centerY - y, color, width, height);
            drawPixel(image, centerX + y, centerY + x, color, width, height);
            drawPixel(image, centerX - y, centerY + x, color, width, height);
            drawPixel(image, centerX + y, centerY - x, color, width, height);
            drawPixel(image, centerX - y, centerY - x, color, width, height);
            if (d < 0) {
                d = d + 4 * x + 6;
            } else {
                d = d + 4 * (x - y) + 10;
                y--;
            }
            x++;
        }
    }

    private void drawPixel(BufferedImage image, int x, int y, int color, int width, int height) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            image.setRGB(x, y, color);
        }
    }

    private float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255.0f;
        float gf = g / 255.0f;
        float bf = b / 255.0f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float v = max;
        float s = max == 0 ? 0 : (max - min) / max;
        float h;
        if (max == min) {
            h = 0;
        } else if (max == rf) {
            h = (60 * ((gf - bf) / (max - min)) + 360) % 360;
        } else if (max == gf) {
            h = (60 * ((bf - rf) / (max - min)) + 120);
        } else {
            h = (60 * ((rf - gf) / (max - min)) + 240);
        }
        return new float[] { h, s, v };
    }
}
