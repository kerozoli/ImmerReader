package com.keroleap.immerreader.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.keroleap.immerreader.SharedData.ChickenData;
import com.keroleap.immerreader.SharedData.ChickenManagerData;
import com.keroleap.immerreader.SharedData.ChickenNest;
import com.keroleap.immerreader.SharedData.ErrorStatistics;

@Controller
@RequestMapping("/ChickenManager")
public class ChickenManagerController {

    private static final int POINT_COUNT = 4;

    @Autowired
    private ChickenManagerData chickenManagerData;

    @Autowired
    private ChickenData chickenData;

    @Autowired
    private ErrorStatistics errorStatistics;

    @PostMapping("/set")
    @ResponseBody
    public ResponseEntity<?> setNests(@RequestParam int count,
                                      @RequestParam String points,
                                      @RequestParam String thresholds,
                                      @RequestParam String minAreas,
                                      @RequestParam String maxAreas,
                                      @RequestParam String minCircularities) {
        String[] pointParts = points.split(",");
        String[] thresholdParts = thresholds.split(",");
        String[] minAreaParts = minAreas.split(",");
        String[] maxAreaParts = maxAreas.split(",");
        String[] circularityParts = minCircularities.split(",");

        if (count < 1 || count > 5) {
            return ResponseEntity.badRequest().body("Nest count must be between 1 and 5.");
        }
        int expectedPoints = count * POINT_COUNT * 2;
        if (pointParts.length != expectedPoints
                || thresholdParts.length != count
                || minAreaParts.length != count
                || maxAreaParts.length != count
                || circularityParts.length != count) {
            return ResponseEntity.badRequest().body("Expected " + count + " nests with " + POINT_COUNT + " points each and per-nest filters.");
        }

        try {
            chickenManagerData.setNestCount(count);
            for (int i = 0; i < count; i++) {
                int[] xs = new int[POINT_COUNT];
                int[] ys = new int[POINT_COUNT];
                for (int p = 0; p < POINT_COUNT; p++) {
                    xs[p] = Integer.parseInt(pointParts[i * POINT_COUNT * 2 + p * 2].trim());
                    ys[p] = Integer.parseInt(pointParts[i * POINT_COUNT * 2 + p * 2 + 1].trim());
                }
                int threshold = Integer.parseInt(thresholdParts[i].trim());
                int minArea = Integer.parseInt(minAreaParts[i].trim());
                int maxArea = Integer.parseInt(maxAreaParts[i].trim());
                double minCircularity = Double.parseDouble(circularityParts[i].trim());

                chickenManagerData.setNestPoints(i, xs, ys);
                chickenManagerData.setNestThreshold(i, threshold);
                chickenManagerData.setNestFilters(i, minArea, maxArea, minCircularity);
            }
            chickenData.setConfiguredCount(count);
            return ResponseEntity.ok(chickenManagerData.getNests());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid number format: " + e.getMessage());
        }
    }

    @PostMapping("/toggle")
    @ResponseBody
    public Map<String, Object> toggle() {
        chickenManagerData.setEnabled(!chickenManagerData.isEnabled());
        Map<String, Object> response = new HashMap<>();
        response.put("enabled", chickenManagerData.isEnabled());
        return response;
    }

    @PostMapping("/enable")
    @ResponseBody
    public Map<String, Object> enable() {
        chickenManagerData.setEnabled(true);
        Map<String, Object> response = new HashMap<>();
        response.put("enabled", true);
        return response;
    }

    @PostMapping("/disable")
    @ResponseBody
    public Map<String, Object> disable() {
        chickenManagerData.setEnabled(false);
        Map<String, Object> response = new HashMap<>();
        response.put("enabled", false);
        return response;
    }

    @GetMapping
    public ModelAndView adjust() {
        ModelAndView modelAndView = new ModelAndView("chicken-manager");
        addCommonAttributes(modelAndView);
        return modelAndView;
    }

    @GetMapping("/adjust")
    public ModelAndView adjustPath() {
        return adjust();
    }

    private void addCommonAttributes(ModelAndView modelAndView) {
        modelAndView.addObject("enabled", chickenManagerData.isEnabled());
        modelAndView.addObject("nestCount", chickenManagerData.getNestCount());
        modelAndView.addObject("chickenRest", chickenData.getChickenRest());
        List<ChickenNest> nests = chickenManagerData.getNests();
        modelAndView.addObject("nests", nests);
        modelAndView.addObject("errorStats", errorStatistics.getLastErrorCounts("Chicken"));
    }
}
