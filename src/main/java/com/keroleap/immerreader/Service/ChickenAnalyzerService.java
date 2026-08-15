package com.keroleap.immerreader.Service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.keroleap.immerreader.ChickenRest;
import com.keroleap.immerreader.ErrorType;
import com.keroleap.immerreader.SharedData.ChickenManagerData;
import com.keroleap.immerreader.SharedData.ChickenNest;

@Service
public class ChickenAnalyzerService {

    private static final Logger logger = LoggerFactory.getLogger(ChickenAnalyzerService.class);
    private static final int BLUR_SIZE = 5;
    private static final int MORPH_SIZE = 3;
    private static final int[] NEST_COLORS = { 16711680, 65280, 255, 16776960, 16711935 };

    @Autowired
    private CameraImageService cameraImageService;

    public BufferedImage getBufferedImage(String cameraUrl) {
        return cameraImageService.capture(cameraUrl);
    }

    public ChickenRest getChickenRestData(BufferedImage image, ChickenManagerData managerData) {
        ChickenRest chickenRest = new ChickenRest();
        if (image == null) {
            chickenRest.setError(true);
            chickenRest.setErrorType(ErrorType.RTSP_CONNECTION_ERROR);
            return chickenRest;
        }

        List<Integer> counts = new ArrayList<>();
        List<ChickenNest> nests = managerData.getNests();
        for (ChickenNest nest : nests) {
            if (nest.isConfigured()) {
                counts.add(countEggsInNest(image, nest));
            } else {
                counts.add(0);
            }
        }

        chickenRest.setNestCounts(counts);
        chickenRest.setTotalCount(counts.stream().mapToInt(Integer::intValue).sum());
        return chickenRest;
    }

    public BufferedImage drawDebugOverlay(BufferedImage image, ChickenManagerData managerData) {
        if (image == null) {
            return createPlaceholderImage();
        }
        BufferedImage copy = deepCopy(image);
        Graphics2D graphics = copy.createGraphics();
        graphics.setStroke(new BasicStroke(3));
        List<ChickenNest> nests = managerData.getNests();
        for (int i = 0; i < nests.size(); i++) {
            ChickenNest nest = nests.get(i);
            if (!nest.isConfigured()) {
                continue;
            }
            int[] xs = nest.getXs();
            int[] ys = nest.getYs();
            graphics.setColor(new Color(NEST_COLORS[i % NEST_COLORS.length]));
            graphics.drawPolygon(xs, ys, xs.length);
            graphics.drawString("F" + (i + 1), xs[0] + 4, ys[0] + 16);
        }
        graphics.dispose();
        return copy;
    }

    private int countEggsInNest(BufferedImage image, ChickenNest nest) {
        int[] xs = nest.getXs();
        int[] ys = nest.getYs();
        Rectangle bounds = computeBounds(xs, ys, image.getWidth(), image.getHeight());
        if (bounds.width <= 0 || bounds.height <= 0) {
            return 0;
        }

        Polygon polygon = new Polygon(xs, ys, xs.length);
        Mat gray = bufferedImageToGrayMat(image, bounds);
        try {
            Mat blurred = new Mat();
            opencv_imgproc.GaussianBlur(gray, blurred, new Size(BLUR_SIZE, BLUR_SIZE), 0);

            Mat binary = new Mat();
            opencv_imgproc.threshold(blurred, binary, nest.getThreshold(), 255, opencv_imgproc.THRESH_BINARY);

            Mat kernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_ELLIPSE, new Size(MORPH_SIZE, MORPH_SIZE));
            Mat closed = new Mat();
            opencv_imgproc.morphologyEx(binary, closed, opencv_imgproc.MORPH_CLOSE, kernel);
            Mat opened = new Mat();
            opencv_imgproc.morphologyEx(closed, opened, opencv_imgproc.MORPH_OPEN, kernel);

            MatVector contours = new MatVector();
            Mat hierarchy = new Mat();
            opencv_imgproc.findContours(opened, contours, hierarchy, opencv_imgproc.RETR_EXTERNAL, opencv_imgproc.CHAIN_APPROX_SIMPLE);

            int count = 0;
            long contourCount = contours.size();
            for (int i = 0; i < contourCount; i++) {
                Mat contour = contours.get(i);
                double area = opencv_imgproc.contourArea(contour);
                if (area < nest.getMinArea() || area > nest.getMaxArea()) {
                    continue;
                }
                double perimeter = opencv_imgproc.arcLength(contour, true);
                double circularity = perimeter > 0 ? 4 * Math.PI * area / (perimeter * perimeter) : 0;
                if (circularity < nest.getMinCircularity()) {
                    continue;
                }
                int[] center = contourCenter(contour);
                int imageX = bounds.x + center[0];
                int imageY = bounds.y + center[1];
                if (polygon.contains(imageX, imageY)) {
                    count++;
                }
            }

            return count;
        } finally {
            gray.release();
        }
    }

    private Rectangle computeBounds(int[] xs, int[] ys, int maxWidth, int maxHeight) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int i = 0; i < xs.length; i++) {
            minX = Math.min(minX, xs[i]);
            minY = Math.min(minY, ys[i]);
            maxX = Math.max(maxX, xs[i]);
            maxY = Math.max(maxY, ys[i]);
        }
        minX = Math.max(0, minX);
        minY = Math.max(0, minY);
        maxX = Math.min(maxWidth, maxX);
        maxY = Math.min(maxHeight, maxY);
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    private int[] contourCenter(Mat contour) {
        Rect rect = opencv_imgproc.boundingRect(contour);
        int cx = rect.x() + rect.width() / 2;
        int cy = rect.y() + rect.height() / 2;
        return new int[] { cx, cy };
    }

    private Mat bufferedImageToGrayMat(BufferedImage image, Rectangle bounds) {
        Mat roiMat = new Mat(bounds.height, bounds.width, opencv_core.CV_8UC3);
        for (int y = 0; y < bounds.height; y++) {
            for (int x = 0; x < bounds.width; x++) {
                int rgb = image.getRGB(bounds.x + x, bounds.y + y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                byte[] data = new byte[] { (byte) b, (byte) g, (byte) r };
                roiMat.ptr(y, x).put(data);
            }
        }
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(roiMat, gray, opencv_imgproc.COLOR_BGR2GRAY);
        roiMat.release();
        return gray;
    }

    private BufferedImage deepCopy(BufferedImage source) {
        java.awt.image.ColorModel cm = source.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        java.awt.image.WritableRaster raster = source.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    private BufferedImage createPlaceholderImage() {
        BufferedImage placeholder = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = placeholder.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 320, 240);
        g.setColor(Color.WHITE);
        g.drawString("No camera image available", 20, 120);
        g.dispose();
        return placeholder;
    }
}
