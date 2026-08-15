package com.keroleap.immerreader.Service;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private static final Color NEST_POLYGON_COLOR = new Color(0, 140, 255);
    private static final Color REJECTED_CONTOUR_COLOR = new Color(200, 100, 0);
    private static final Color MERGED_CONTOUR_COLOR = new Color(180, 0, 220);
    private static final Color THRESHOLD_MASK_COLOR = new Color(0, 255, 0, 255);
    private static final float THRESHOLD_MASK_ALPHA = 0.4f;
    private static final double MERGED_ASPECT_RATIO_THRESHOLD = 1.8;
    private static final double MERGED_HULL_RATIO_THRESHOLD = 1.25;
    private static final int AGGRESSIVE_SPLIT_DISTANCE_THRESHOLD = 64;

    // Automatic threshold correction constants (coverage-only iterative search)
    private static final int AUTO_CORRECTION_SEARCH_MAX_DELTA = 120;
    private static final int AUTO_CORRECTION_SEARCH_STEP = 2;
    private static final double COVERAGE_HIGH_THRESHOLD = 0.25;
    private static final double COVERAGE_TARGET = 0.20;
    private static final int COVERAGE_SEARCH_MAX_STEPS = 60;

    private final List<Map<String, Object>> accumulatedContourData = java.util.Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> lastThresholdDetails = java.util.Collections.synchronizedList(new ArrayList<>());

    @Autowired
    private CameraImageService cameraImageService;

    public BufferedImage getBufferedImage(String cameraUrl) {
        return cameraImageService.capture(cameraUrl);
    }

    public ChickenRest getChickenRestData(BufferedImage image, ChickenManagerData managerData) {
        synchronized (accumulatedContourData) {
            accumulatedContourData.clear();
        }
        synchronized (lastThresholdDetails) {
            lastThresholdDetails.clear();
        }
        ChickenRest chickenRest = new ChickenRest();
        if (image == null) {
            chickenRest.setError(true);
            chickenRest.setErrorType(ErrorType.RTSP_CONNECTION_ERROR);
            return chickenRest;
        }

        List<Integer> counts = new ArrayList<>();
        List<ChickenNest> nests = managerData.getNests();
        for (int i = 0; i < nests.size(); i++) {
            ChickenNest nest = nests.get(i);
            if (nest.isConfigured()) {
                counts.add(countEggsInNest(image, nest, null, i, false));
                recordThresholdDetail("F" + (i + 1));
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
        long start = System.currentTimeMillis();
        BufferedImage copy = deepCopy(image);
        Graphics2D graphics = copy.createGraphics();
        graphics.setStroke(new BasicStroke(3));
        graphics.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        List<ChickenNest> nests = managerData.getNests();

        synchronized (accumulatedContourData) {
            accumulatedContourData.clear();
        }

        BufferedImage thresholdMaskLayer = createThresholdMaskLayer(copy, nests, managerData.isThresholdMaskEnabled());
        if (thresholdMaskLayer != null) {
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, THRESHOLD_MASK_ALPHA));
            graphics.drawImage(thresholdMaskLayer, 0, 0, null);
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }

        for (int i = 0; i < nests.size(); i++) {
            ChickenNest nest = nests.get(i);
            if (!nest.isConfigured()) {
                continue;
            }
            int[] xs = nest.getXs();
            int[] ys = nest.getYs();
            graphics.setColor(NEST_POLYGON_COLOR);
            graphics.drawPolygon(xs, ys, xs.length);
            countEggsInNest(copy, nest, graphics, i, true);
            String label = "F" + (i + 1);
            if (nest.isAutoThreshold()) {
                int otsuThreshold = computeOtsuThresholdForPolygon(copy, nest);
                label += " Otsu:" + otsuThreshold + "+" + nest.getOtsuOffset();
            }
            graphics.drawString(label, xs[0] + 4, ys[0] + 18);
        }
        graphics.dispose();
        logger.info("Debug overlay rendered in {} ms", System.currentTimeMillis() - start);
        return copy;
    }

    private BufferedImage createThresholdMaskLayer(BufferedImage source, List<ChickenNest> nests, boolean globalEnabled) {
        if (!globalEnabled) {
            return null;
        }
        BufferedImage maskLayer = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int maskColor = THRESHOLD_MASK_COLOR.getRGB();
        for (ChickenNest nest : nests) {
            if (!nest.isConfigured()) {
                continue;
            }
            int[] xs = nest.getXs();
            int[] ys = nest.getYs();
            Rectangle bounds = computeBounds(xs, ys, source.getWidth(), source.getHeight());
            if (bounds.width <= 0 || bounds.height <= 0) {
                continue;
            }
            Polygon polygon = new Polygon(xs, ys, xs.length);
            Mat mask = computeThresholdMaskMat(source, nest, bounds, polygon);
            try {
                for (int y = 0; y < bounds.height; y++) {
                    for (int x = 0; x < bounds.width; x++) {
                        int imageX = bounds.x + x;
                        int imageY = bounds.y + y;
                        if (polygon.contains(imageX, imageY)) {
                            byte[] pixel = new byte[1];
                            mask.ptr(y, x).get(pixel);
                            if ((pixel[0] & 0xFF) == 255) {
                                maskLayer.setRGB(imageX, imageY, maskColor);
                            }
                        }
                    }
                }
            } finally {
                mask.release();
            }
        }
        return maskLayer;
    }

    private Mat computeThresholdMaskMat(BufferedImage image, ChickenNest nest, Rectangle bounds, Polygon polygon) {
        Mat gray = bufferedImageToGrayMat(image, bounds);
        Mat blurred = new Mat();
        Mat binary = new Mat();
        Mat kernel = null;
        Mat closed = new Mat();
        Mat opened = new Mat();
        try {
            opencv_imgproc.GaussianBlur(gray, blurred, new Size(BLUR_SIZE, BLUR_SIZE), 0);
            int effectiveThreshold = computeEffectiveThreshold(blurred, gray, nest, bounds, polygon);
            opencv_imgproc.threshold(blurred, binary, effectiveThreshold, 255, opencv_imgproc.THRESH_BINARY);
            kernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_ELLIPSE, new Size(MORPH_SIZE, MORPH_SIZE));
            opencv_imgproc.morphologyEx(binary, closed, opencv_imgproc.MORPH_CLOSE, kernel);
            opencv_imgproc.morphologyEx(closed, opened, opencv_imgproc.MORPH_OPEN, kernel);
            return opened.clone();
        } finally {
            gray.release();
            blurred.release();
            binary.release();
            if (kernel != null) {
                kernel.release();
            }
            closed.release();
            opened.release();
        }
    }

    private int computeOtsuThresholdForPolygon(BufferedImage image, ChickenNest nest) {
        int[] xs = nest.getXs();
        int[] ys = nest.getYs();
        Rectangle bounds = computeBounds(xs, ys, image.getWidth(), image.getHeight());
        if (bounds.width <= 0 || bounds.height <= 0) {
            return nest.getThreshold();
        }
        Mat gray = bufferedImageToGrayMat(image, bounds);
        try {
            return computeOtsuThreshold(gray);
        } finally {
            gray.release();
        }
    }

    private int countEggsInNest(BufferedImage image, ChickenNest nest, Graphics2D debugGraphics, int nestIndex, boolean countMergedAsTwo) {
        java.util.concurrent.atomic.AtomicInteger nestIndexRef = new java.util.concurrent.atomic.AtomicInteger(nestIndex);
        int[] xs = nest.getXs();
        int[] ys = nest.getYs();
        Rectangle bounds = computeBounds(xs, ys, image.getWidth(), image.getHeight());
        if (bounds.width <= 0 || bounds.height <= 0) {
            return 0;
        }

        Polygon polygon = new Polygon(xs, ys, xs.length);
        Mat gray = bufferedImageToGrayMat(image, bounds);
        Mat blurred = new Mat();
        Mat binary = new Mat();
        Mat kernel = null;
        Mat closed = new Mat();
        Mat opened = new Mat();
        MatVector contours = new MatVector();
        Mat hierarchy = new Mat();
        try {
            opencv_imgproc.GaussianBlur(gray, blurred, new Size(BLUR_SIZE, BLUR_SIZE), 0);

            int effectiveThreshold = computeEffectiveThreshold(blurred, gray, nest, bounds, polygon);
            opencv_imgproc.threshold(blurred, binary, effectiveThreshold, 255, opencv_imgproc.THRESH_BINARY);

            kernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_ELLIPSE, new Size(MORPH_SIZE, MORPH_SIZE));
            opencv_imgproc.morphologyEx(binary, closed, opencv_imgproc.MORPH_CLOSE, kernel);
            opencv_imgproc.morphologyEx(closed, opened, opencv_imgproc.MORPH_OPEN, kernel);

            opencv_imgproc.findContours(opened, contours, hierarchy, opencv_imgproc.RETR_EXTERNAL, opencv_imgproc.CHAIN_APPROX_SIMPLE);

            List<Mat> eggContours = splitEggsWithWatershed(opened, gray, contours, nest, nest.getMinArea());
            int count = 0;
            List<Map<String, Object>> contourData = new ArrayList<>();
            for (Mat contour : eggContours) {
                boolean merged = isMergedContour(contour, polygon, bounds);
                if (merged) {
                    List<Mat> split = aggressivelySplitContour(opened, contour, nest);
                    if (split.size() >= 2) {
                        logger.info("Aggressive split separated merged contour into {} parts", split.size());
                        for (Mat part : split) {
                            Map<String, Object> partInfo = analyzeContour(part, bounds, polygon, nest, false, nestIndexRef);
                            double partArea = ((Number) partInfo.get("area")).doubleValue();
                            if (partArea >= nest.getMinArea()) {
                                partInfo.put("countedAs", 1);
                                if (evaluateContourFromInfo(part, bounds, polygon, nest, debugGraphics, partInfo)) {
                                    count++;
                                }
                                contourData.add(partInfo);
                            }
                            part.release();
                        }
                        contour.release();
                        continue;
                    }
                    for (Mat part : split) {
                        part.release();
                    }
                }
                Map<String, Object> contourInfo = analyzeContour(contour, bounds, polygon, nest, merged, nestIndexRef);
                double contourArea = ((Number) contourInfo.get("area")).doubleValue();
                if (contourArea >= nest.getMinArea()) {
                    contourInfo.put("countedAs", 1);
                    boolean counted = evaluateContourFromInfo(contour, bounds, polygon, nest, debugGraphics, contourInfo);
                    contourData.add(contourInfo);
                    if (counted) {
                        count++;
                        if (countMergedAsTwo && merged) {
                            count++;
                            contourInfo.put("countedAs", 2);
                        }
                    }
                }
                contour.release();
            }
            synchronized (accumulatedContourData) {
                accumulatedContourData.addAll(contourData);
            }

            return count;
        } finally {
            gray.release();
            blurred.release();
            binary.release();
            if (kernel != null) {
                kernel.release();
            }
            closed.release();
            opened.release();
            hierarchy.release();
        }
    }

    public List<Map<String, Object>> getLastContourData() {
        synchronized (accumulatedContourData) {
            return new ArrayList<>(accumulatedContourData);
        }
    }

    private List<Mat> splitEggsWithWatershed(Mat binary, Mat gray, MatVector initialContours, ChickenNest nest, double minArea) {
        List<Mat> result = new ArrayList<>();
        long count = initialContours.size();
        if (count == 0) {
            return result;
        }

        Mat distance = new Mat();
        Mat distance8u = new Mat();
        Mat sureFg = new Mat();
        MatVector fgContours = new MatVector();
        Mat fgHierarchy = new Mat();
        try {
            // Distance transform to find peaks inside foreground blobs
            opencv_imgproc.distanceTransform(binary, distance, opencv_imgproc.DIST_L2, 5);
            opencv_core.normalize(distance, distance, 0.0, 255.0, opencv_core.NORM_MINMAX, -1, new Mat());
            distance.convertTo(distance8u, opencv_core.CV_8U, 1.0, 0.0);

            // Foreground = distance peaks above 50% of normalized range
            opencv_imgproc.threshold(distance8u, sureFg, 127, 255, opencv_imgproc.THRESH_BINARY);

            opencv_imgproc.findContours(sureFg, fgContours, fgHierarchy, opencv_imgproc.RETR_EXTERNAL, opencv_imgproc.CHAIN_APPROX_SIMPLE);

            long splitCount = fgContours.size();
            List<Mat> filtered = new ArrayList<>();
            for (int i = 0; i < splitCount; i++) {
                Mat c = fgContours.get(i);
                if (opencv_imgproc.contourArea(c) >= minArea) {
                    filtered.add(c.clone());
                }
                c.release();
            }
            if (!filtered.isEmpty() && filtered.size() > count) {
                logger.info("Split {} original egg contours into {} distance-peak contours", count, filtered.size());
                result.addAll(filtered);
            } else {
                for (Mat m : filtered) {
                    m.release();
                }
                for (int i = 0; i < count; i++) {
                    result.add(initialContours.get(i).clone());
                }
            }
            return result;
        } catch (Throwable t) {
            logger.warn("Egg splitting failed, using original contours: {}", t.getMessage());
            result.clear();
            for (int i = 0; i < count; i++) {
                result.add(initialContours.get(i).clone());
            }
            return result;
        } finally {
            distance.release();
            distance8u.release();
            sureFg.release();
            fgHierarchy.release();
        }
    }

    private Map<String, Object> analyzeContour(Mat contour, Rectangle bounds, Polygon polygon, ChickenNest nest, boolean merged, java.util.concurrent.atomic.AtomicInteger nestIndexRef) {
        Map<String, Object> info = new LinkedHashMap<>();
        Rect rect = opencv_imgproc.boundingRect(contour);
        double area = opencv_imgproc.contourArea(contour);
        double perimeter = opencv_imgproc.arcLength(contour, true);
        double circularity = perimeter > 0 ? 4 * Math.PI * area / (perimeter * perimeter) : 0;
        double insideRatio = contourInsidePolygonRatio(contour, bounds, polygon);
        int w = rect.width();
        int h = rect.height();
        double aspectRatio = (w > 0 && h > 0) ? (double) Math.max(w, h) / Math.min(w, h) : 0;
        Mat hull = new Mat();
        double hullRatio = 1.0;
        try {
            opencv_imgproc.convexHull(contour, hull, false, true);
            double hullArea = opencv_imgproc.contourArea(hull);
            if (hullArea > 0 && area > 0) {
                hullRatio = hullArea / area;
            }
        } finally {
            hull.release();
        }
        info.put("nest", nestIndexRef != null ? "F" + (nestIndexRef.get() + 1) : "?");
        info.put("x", bounds.x + rect.x());
        info.put("y", bounds.y + rect.y());
        info.put("width", w);
        info.put("height", h);
        info.put("area", (int) area);
        info.put("perimeter", (int) perimeter);
        info.put("circularity", Math.round(circularity * 1000.0) / 1000.0);
        info.put("insideRatio", Math.round(insideRatio * 1000.0) / 1000.0);
        info.put("aspectRatio", Math.round(aspectRatio * 1000.0) / 1000.0);
        info.put("hullRatio", Math.round(hullRatio * 1000.0) / 1000.0);
        info.put("minArea", nest.getMinArea());
        info.put("maxArea", nest.getMaxArea());
        info.put("minCircularity", nest.getMinCircularity());
        info.put("otsuOffset", nest.getOtsuOffset());
        info.put("merged", merged);
        info.put("counted", false);
        return info;
    }

    private boolean evaluateContourFromInfo(Mat contour, Rectangle bounds, Polygon polygon, ChickenNest nest, Graphics2D debugGraphics, Map<String, Object> info) {
        boolean merged = Boolean.TRUE.equals(info.get("merged"));
        double area = ((Number) info.get("area")).doubleValue();
        double circularity = ((Number) info.get("circularity")).doubleValue();
        double insideRatio = ((Number) info.get("insideRatio")).doubleValue();
        if (area < nest.getMinArea() || area > nest.getMaxArea()) {
            if (area >= nest.getMinArea()) {
                drawDebugContour(debugGraphics, contour, bounds, merged ? MERGED_CONTOUR_COLOR : REJECTED_CONTOUR_COLOR);
            }
            return false;
        }
        if (circularity < nest.getMinCircularity()) {
            drawDebugContour(debugGraphics, contour, bounds, merged ? MERGED_CONTOUR_COLOR : REJECTED_CONTOUR_COLOR);
            return false;
        }
        if (insideRatio >= 0.5) {
            Color color = merged ? MERGED_CONTOUR_COLOR : Color.GREEN;
            drawDebugContour(debugGraphics, contour, bounds, color);
            if (merged) {
                drawDebugMergedLabel(debugGraphics, contour, bounds, "Merged?");
            }
            int[] center = contourInsideCentroid(contour, bounds, polygon);
            drawDebugCenter(debugGraphics, center[0], center[1], color);
            info.put("counted", true);
            return true;
        }
        if (debugGraphics != null) {
            drawDebugContour(debugGraphics, contour, bounds, merged ? MERGED_CONTOUR_COLOR : REJECTED_CONTOUR_COLOR);
        }
        return false;
    }


    private boolean isMergedContour(Mat contour, Polygon polygon, Rectangle bounds) {
        double area = opencv_imgproc.contourArea(contour);
        if (area <= 0) {
            return false;
        }
        Rect rect = opencv_imgproc.boundingRect(contour);
        int w = rect.width();
        int h = rect.height();
        if (w <= 0 || h <= 0) {
            return false;
        }
        double aspectRatio = (double) Math.max(w, h) / Math.min(w, h);
        if (aspectRatio >= MERGED_ASPECT_RATIO_THRESHOLD) {
            return true;
        }
        Mat hull = new Mat();
        try {
            opencv_imgproc.convexHull(contour, hull, false, true);
            double hullArea = opencv_imgproc.contourArea(hull);
            if (hullArea > 0 && hullArea / area >= MERGED_HULL_RATIO_THRESHOLD) {
                return true;
            }
        } finally {
            hull.release();
        }
        return false;
    }

    private List<Mat> aggressivelySplitContour(Mat opened, Mat contour, ChickenNest nest) {
        List<Mat> result = new ArrayList<>();
        Rect rect = opencv_imgproc.boundingRect(contour);
        int x = rect.x();
        int y = rect.y();
        int w = rect.width();
        int h = rect.height();
        if (w <= 0 || h <= 0) {
            result.add(contour.clone());
            return result;
        }

        int imgW = opened.cols();
        int imgH = opened.rows();
        int x0 = Math.max(0, x);
        int y0 = Math.max(0, y);
        int x1 = Math.min(imgW, x + w);
        int y1 = Math.min(imgH, y + h);
        int roiW = x1 - x0;
        int roiH = y1 - y0;
        if (roiW <= 0 || roiH <= 0) {
            result.add(contour.clone());
            return result;
        }

        org.bytedeco.opencv.opencv_core.Rect roiRect = new org.bytedeco.opencv.opencv_core.Rect(x0, y0, roiW, roiH);
        Mat roi = new Mat(opened, roiRect);
        Mat distance = new Mat();
        Mat distance8u = new Mat();
        Mat sureFg = new Mat();
        MatVector fgContours = new MatVector();
        Mat fgHierarchy = new Mat();
        try {
            opencv_imgproc.distanceTransform(roi, distance, opencv_imgproc.DIST_L2, 5);
            opencv_core.normalize(distance, distance, 0.0, 255.0, opencv_core.NORM_MINMAX, -1, new Mat());
            distance.convertTo(distance8u, opencv_core.CV_8U, 1.0, 0.0);
            opencv_imgproc.threshold(distance8u, sureFg, AGGRESSIVE_SPLIT_DISTANCE_THRESHOLD, 255, opencv_imgproc.THRESH_BINARY);
            opencv_imgproc.findContours(sureFg, fgContours, fgHierarchy, opencv_imgproc.RETR_EXTERNAL, opencv_imgproc.CHAIN_APPROX_SIMPLE);

            long splitCount = fgContours.size();
            if (splitCount >= 2) {
                for (int i = 0; i < splitCount; i++) {
                    Mat part = fgContours.get(i);
                    Mat shifted = shiftContour(part, x0, y0);
                    result.add(shifted);
                    part.release();
                }
            } else {
                if (splitCount == 1) {
                    fgContours.get(0).release();
                }
                result.add(contour.clone());
            }
        } catch (Throwable t) {
            logger.warn("Aggressive contour split failed: {}", t.getMessage());
            result.clear();
            result.add(contour.clone());
        } finally {
            distance.release();
            distance8u.release();
            sureFg.release();
            fgHierarchy.release();
        }
        return result;
    }

    private Mat shiftContour(Mat contour, int dx, int dy) {
        Mat shifted = new Mat(contour.rows(), contour.cols(), contour.type());
        IntBuffer src = contour.createBuffer();
        IntBuffer dst = shifted.createBuffer();
        int total = src.remaining();
        for (int i = 0; i < total; i += 2) {
            dst.put(i, src.get(i) + dx);
            dst.put(i + 1, src.get(i + 1) + dy);
        }
        return shifted;
    }

    private void drawDebugMergedLabel(Graphics2D graphics, Mat contour, Rectangle bounds, String text) {
        if (graphics == null) {
            return;
        }
        Rect rect = opencv_imgproc.boundingRect(contour);
        int x = bounds.x + rect.x();
        int y = bounds.y + rect.y();
        graphics.setColor(Color.WHITE);
        graphics.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
        graphics.drawString(text, x, Math.max(12, y - 4));
    }

    private int computeOtsuThreshold(Mat gray) {
        try {
            double otsu = opencv_imgproc.threshold(gray, new Mat(), 0, 255, opencv_imgproc.THRESH_BINARY + opencv_imgproc.THRESH_OTSU);
            int result = (int) otsu;
            return Math.max(1, Math.min(254, result));
        } catch (Throwable t) {
            logger.warn("Otsu threshold computation failed: {}", t.getMessage());
            return 180;
        }
    }

    /**
     * Computes the effective threshold for a nest, applying per-nest offset and,
     * when enabled, automatic correction based on image brightness, mask coverage
     * and contour quality.
     */
    private ThresholdDetail pendingThresholdDetail;

    private int computeEffectiveThreshold(Mat blurred, Mat gray, ChickenNest nest, Rectangle bounds, Polygon polygon) {
        if (!nest.isAutoThreshold()) {
            return clamp(nest.getThreshold(), 1, 254);
        }

        int baseOtsu = computeOtsuThreshold(blurred);
        int base = clamp(baseOtsu + nest.getOtsuOffset(), 1, 254);

        if (!nest.isAutoCorrection()) {
            logger.info("Nest threshold (no correction): Otsu={} offset={} -> {}", baseOtsu, nest.getOtsuOffset(), base);
            return base;
        }

        // Coverage-only iterative search. Starts from Otsu and raises threshold until
        // mask coverage drops below COVERAGE_HIGH_THRESHOLD (25%), then picks the best
        // candidate with coverage under the target.
        int current = base;
        ThresholdScore best = null;
        int bestThreshold = current;
        int searchAdjustment = 0;
        boolean searched = false;

        if (polygon != null) {
            best = evaluateThreshold(blurred, gray, nest, bounds, polygon, current);
            logger.info("Threshold candidate evaluation at {}: counted={}, merged={}, rejected={}, coverage={}",
                    current, best.countedCount, best.mergedCount, best.rejectedCount, String.format("%.2f", best.coverageRatio));

            if (best.coverageRatio > COVERAGE_HIGH_THRESHOLD) {
                searched = true;
                for (int step = 0; step < COVERAGE_SEARCH_MAX_STEPS; step++) {
                    int candidate = clamp(current + step * AUTO_CORRECTION_SEARCH_STEP, 1, 254);
                    ThresholdScore candidateScore = evaluateThreshold(blurred, gray, nest, bounds, polygon, candidate);
                    logger.debug("Threshold candidate {}: counted={}, merged={}, rejected={}, coverage={}",
                            candidate, candidateScore.countedCount, candidateScore.mergedCount, candidateScore.rejectedCount, String.format("%.2f", candidateScore.coverageRatio));
                    if (candidateScore.coverageRatio <= COVERAGE_HIGH_THRESHOLD) {
                        bestThreshold = candidate;
                        best = candidateScore;
                        searchAdjustment = candidate - current;
                        logger.info("Auto-corrected threshold from {} to {} (coverage {} under {})", current, bestThreshold, String.format("%.2f", candidateScore.coverageRatio), COVERAGE_HIGH_THRESHOLD);
                        break;
                    }
                    if (candidateScore.coverageRatio < best.coverageRatio) {
                        bestThreshold = candidate;
                        best = candidateScore;
                        searchAdjustment = candidate - current;
                    }
                }
                if (bestThreshold == current) {
                    logger.warn("Could not reduce coverage below {} with max threshold {}, keeping best found", COVERAGE_HIGH_THRESHOLD, current + (COVERAGE_SEARCH_MAX_STEPS - 1) * AUTO_CORRECTION_SEARCH_STEP);
                }
            }
        }

        int finalThreshold = bestThreshold;
        pendingThresholdDetail = new ThresholdDetail("", baseOtsu, nest.getOtsuOffset(), searchAdjustment, finalThreshold, searched, best != null ? best.coverageRatio : 0.0, best != null ? best.mergedCount : 0, best != null ? best.rejectedCount : 0);
        logger.info("Nest threshold (auto-corrected): Otsu={} final={} (searchAdj={})", baseOtsu, finalThreshold, searchAdjustment);
        return finalThreshold;
    }

    private void recordThresholdDetail(String nestLabel) {
        if (pendingThresholdDetail == null) {
            return;
        }
        Map<String, Object> detail = new LinkedHashMap<>(pendingThresholdDetail.toMap());
        detail.put("nest", nestLabel);
        synchronized (lastThresholdDetails) {
            lastThresholdDetails.add(detail);
        }
        pendingThresholdDetail = null;
    }

    public List<Map<String, Object>> getLastThresholdDetails() {
        synchronized (lastThresholdDetails) {
            return new ArrayList<>(lastThresholdDetails);
        }
    }

    private static class ThresholdDetail {
        final String nest;
        final int baseOtsu;
        final int otsuOffset;
        final int searchAdjustment;
        final int finalThreshold;
        final boolean autoCorrected;
        final double coverageRatio;
        final int mergedCount;
        final int rejectedCount;

        ThresholdDetail(String nest, int baseOtsu, int otsuOffset, int searchAdjustment, int finalThreshold, boolean autoCorrected, double coverageRatio, int mergedCount, int rejectedCount) {
            this.nest = nest;
            this.baseOtsu = baseOtsu;
            this.otsuOffset = otsuOffset;
            this.searchAdjustment = searchAdjustment;
            this.finalThreshold = finalThreshold;
            this.autoCorrected = autoCorrected;
            this.coverageRatio = coverageRatio;
            this.mergedCount = mergedCount;
            this.rejectedCount = rejectedCount;
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("baseOtsu", baseOtsu);
            map.put("otsuOffset", otsuOffset);
            map.put("searchAdjustment", searchAdjustment);
            map.put("finalThreshold", finalThreshold);
            map.put("autoCorrected", autoCorrected);
            map.put("coverageRatio", Math.round(coverageRatio * 1000.0) / 1000.0);
            map.put("mergedCount", mergedCount);
            map.put("rejectedCount", rejectedCount);
            return map;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class ThresholdScore {
        final int threshold;
        final int countedCount;
        final int mergedCount;
        final int rejectedCount;
        final double coverageRatio;

        ThresholdScore(int threshold, int countedCount, int mergedCount, int rejectedCount, double coverageRatio) {
            this.threshold = threshold;
            this.countedCount = countedCount;
            this.mergedCount = mergedCount;
            this.rejectedCount = rejectedCount;
            this.coverageRatio = coverageRatio;
        }

        double score() {
            return countedCount * 10.0 - mergedCount * 25.0 - rejectedCount * 3.0 - coverageRatio * 250.0;
        }
    }

    private ThresholdScore evaluateThreshold(Mat blurred, Mat gray, ChickenNest nest, Rectangle bounds, Polygon polygon, int threshold) {
        Mat binary = new Mat();
        Mat kernel = null;
        Mat closed = new Mat();
        Mat opened = new Mat();
        MatVector contours = new MatVector();
        Mat hierarchy = new Mat();
        try {
            opencv_imgproc.threshold(blurred, binary, threshold, 255, opencv_imgproc.THRESH_BINARY);
            kernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_ELLIPSE, new Size(MORPH_SIZE, MORPH_SIZE));
            opencv_imgproc.morphologyEx(binary, closed, opencv_imgproc.MORPH_CLOSE, kernel);
            opencv_imgproc.morphologyEx(closed, opened, opencv_imgproc.MORPH_OPEN, kernel);
            opencv_imgproc.findContours(opened, contours, hierarchy, opencv_imgproc.RETR_EXTERNAL, opencv_imgproc.CHAIN_APPROX_SIMPLE);

            int counted = 0;
            int merged = 0;
            int rejected = 0;
            long maskPixels = 0;
            for (int i = 0; i < contours.size(); i++) {
                Mat contour = contours.get(i);
                double area = opencv_imgproc.contourArea(contour);
                double insideRatio = contourInsidePolygonRatio(contour, bounds, polygon);
                boolean isInside = insideRatio >= 0.5;
                if (!isInside || area < nest.getMinArea()) {
                    if (isInside) {
                        rejected++;
                    }
                    continue;
                }
                if (area > nest.getMaxArea() || isMergedContour(contour, polygon, bounds)) {
                    merged++;
                } else {
                    counted++;
                }
            }

            // Real coverage: count white pixels inside the polygon in the opened binary mask
            for (int y = 0; y < opened.rows(); y++) {
                byte[] row = new byte[opened.cols()];
                opened.ptr(y).get(row);
                for (int x = 0; x < opened.cols(); x++) {
                    if ((row[x] & 0xFF) == 255) {
                        int imageX = bounds.x + x;
                        int imageY = bounds.y + y;
                        if (polygon.contains(imageX, imageY)) {
                            maskPixels++;
                        }
                    }
                }
            }

            double polygonArea = computePolygonAreaPixels(polygon);
            double coverage = polygonArea > 0 ? maskPixels / polygonArea : 0.0;
            return new ThresholdScore(threshold, counted, merged, rejected, coverage);
        } finally {
            binary.release();
            if (kernel != null) {
                kernel.release();
            }
            closed.release();
            opened.release();
            hierarchy.release();
        }
    }

    private double computePolygonAreaPixels(Polygon polygon) {
        if (polygon == null) {
            return 0.0;
        }
        Rectangle bounds = polygon.getBounds();
        if (bounds.width <= 0 || bounds.height <= 0) {
            return 0.0;
        }
        int inside = 0;
        for (int y = bounds.y; y < bounds.y + bounds.height; y++) {
            for (int x = bounds.x; x < bounds.x + bounds.width; x++) {
                if (polygon.contains(x, y)) {
                    inside++;
                }
            }
        }
        return (double) inside;
    }

    private void drawDebugContour(Graphics2D graphics, Mat contour, Rectangle bounds, Color color) {
        if (graphics == null) {
            return;
        }
        java.awt.Polygon poly = new java.awt.Polygon();
        Rect rect = opencv_imgproc.boundingRect(contour);
        int cx = rect.x();
        int cy = rect.y();
        int cw = rect.width();
        int ch = rect.height();
        poly.addPoint(bounds.x + cx, bounds.y + cy);
        poly.addPoint(bounds.x + cx + cw, bounds.y + cy);
        poly.addPoint(bounds.x + cx + cw, bounds.y + cy + ch);
        poly.addPoint(bounds.x + cx, bounds.y + cy + ch);
        graphics.setColor(color);
        graphics.setStroke(new BasicStroke(2));
        graphics.drawPolygon(poly);
    }

    private void drawDebugCenter(Graphics2D graphics, int x, int y, Color color) {
        if (graphics == null) {
            return;
        }
        graphics.setColor(color);
        graphics.fillOval(x - 4, y - 4, 8, 8);
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

    private double contourInsidePolygonRatio(Mat contour, Rectangle bounds, Polygon polygon) {
        Rect rect = opencv_imgproc.boundingRect(contour);
        int x0 = rect.x();
        int y0 = rect.y();
        int w = rect.width();
        int h = rect.height();
        if (w <= 0 || h <= 0) {
            return 0;
        }

        int insidePixels = 0;
        int totalPixels = 0;
        int step = Math.max(1, Math.min(w, h) / 20);
        for (int dy = 0; dy < h; dy += step) {
            for (int dx = 0; dx < w; dx += step) {
                int imageX = bounds.x + x0 + dx;
                int imageY = bounds.y + y0 + dy;
                if (polygon.contains(imageX, imageY)) {
                    insidePixels++;
                }
                totalPixels++;
            }
        }
        return totalPixels > 0 ? (double) insidePixels / totalPixels : 0;
    }

    private int[] contourInsideCentroid(Mat contour, Rectangle bounds, Polygon polygon) {
        Rect rect = opencv_imgproc.boundingRect(contour);
        int x0 = rect.x();
        int y0 = rect.y();
        int w = rect.width();
        int h = rect.height();

        long sumX = 0;
        long sumY = 0;
        int count = 0;
        int step = Math.max(1, Math.min(w, h) / 20);
        for (int dy = 0; dy < h; dy += step) {
            for (int dx = 0; dx < w; dx += step) {
                int imageX = bounds.x + x0 + dx;
                int imageY = bounds.y + y0 + dy;
                if (polygon.contains(imageX, imageY)) {
                    sumX += imageX;
                    sumY += imageY;
                    count++;
                }
            }
        }
        if (count == 0) {
            return new int[] { bounds.x + x0 + w / 2, bounds.y + y0 + h / 2 };
        }
        return new int[] { (int) (sumX / count), (int) (sumY / count) };
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
