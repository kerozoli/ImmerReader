package com.keroleap.immerreader.Controller;

import java.util.HashMap;
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

    private static final int NEST_COUNT = 3;

    @Autowired
    private ChickenManagerData chickenManagerData;

    @Autowired
    private ChickenData chickenData;

    @Autowired
    private ErrorStatistics errorStatistics;

    @PostMapping("/set")
    @ResponseBody
    public ResponseEntity<?> setNests(@RequestParam String points,
                                      @RequestParam String thresholds,
                                      @RequestParam String minAreas,
                                      @RequestParam String maxAreas,
                                      @RequestParam String minCircularities) {
        String[] pointParts = points.split(",");
        String[] thresholdParts = thresholds.split(",");
        String[] minAreaParts = minAreas.split(",");
        String[] maxAreaParts = maxAreas.split(",");
        String[] circularityParts = minCircularities.split(",");

        if (pointParts.length != NEST_COUNT * 4
                || thresholdParts.length != NEST_COUNT
                || minAreaParts.length != NEST_COUNT
                || maxAreaParts.length != NEST_COUNT
                || circularityParts.length != NEST_COUNT) {
            return ResponseEntity.badRequest().body("Expected " + NEST_COUNT + " nest definitions (4 coords + filters each).");
        }

        try {
            for (int i = 0; i < NEST_COUNT; i++) {
                int x = Integer.parseInt(pointParts[i * 4].trim());
                int y = Integer.parseInt(pointParts[i * 4 + 1].trim());
                int width = Integer.parseInt(pointParts[i * 4 + 2].trim());
                int height = Integer.parseInt(pointParts[i * 4 + 3].trim());
                int threshold = Integer.parseInt(thresholdParts[i].trim());
                int minArea = Integer.parseInt(minAreaParts[i].trim());
                int maxArea = Integer.parseInt(maxAreaParts[i].trim());
                double minCircularity = Double.parseDouble(circularityParts[i].trim());

                chickenManagerData.setNest(i, x, y, width, height);
                chickenManagerData.setNestThreshold(i, threshold);
                chickenManagerData.setNestFilters(i, minArea, maxArea, minCircularity);
            }
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
        modelAndView.addObject("chickenRest", chickenData.getChickenRest());
        ChickenNest[] nests = chickenManagerData.getNests();
        modelAndView.addObject("nests", nests);
        modelAndView.addObject("errorStats", errorStatistics.getLastErrorCounts("chicken"));
    }
}
