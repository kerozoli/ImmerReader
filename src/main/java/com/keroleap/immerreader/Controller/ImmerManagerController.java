package com.keroleap.immerreader.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.keroleap.immerreader.ImmerRest;
import com.keroleap.immerreader.SharedData.ImmerData;
import com.keroleap.immerreader.SharedData.ImmerManagerData;
import com.keroleap.immerreader.SharedData.ImmerStatistics;
import com.keroleap.immerreader.SharedData.ImmerStatisticsSnapshot;
import com.keroleap.immerreader.SharedData.ErrorStatistics;

@Controller
@RequestMapping("/ImmerManager")
public class ImmerManagerController {

    @Autowired
    private ImmerManagerData immerManagerData;

    @Autowired
    private ImmerData immerData;

    @Autowired
    private ImmerStatistics immerStatistics;

    @Autowired
    private ErrorStatistics errorStatistics;

    @PostMapping("/setPoints")
    @ResponseBody
    public ResponseEntity<?> setPoints(@RequestParam String points) {
        String[] parts = points.split(",");
        if (parts.length != ImmerManagerData.POINT_COUNT * 2) {
            return ResponseEntity.badRequest().body("Expected " + (ImmerManagerData.POINT_COUNT * 2) + " comma-separated coordinates, got " + parts.length);
        }
        try {
            int[] xs = new int[ImmerManagerData.POINT_COUNT];
            int[] ys = new int[ImmerManagerData.POINT_COUNT];
            for (int i = 0; i < ImmerManagerData.POINT_COUNT; i++) {
                xs[i] = Integer.parseInt(parts[i * 2].trim());
                ys[i] = Integer.parseInt(parts[i * 2 + 1].trim());
            }
            immerManagerData.setPoints(xs, ys);
            return ResponseEntity.ok(immerManagerData);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid coordinate value: " + e.getMessage());
        }
    }

    @PostMapping("/setReference")
    @ResponseBody
    public ImmerManagerData setReference(@RequestParam int x,
                                         @RequestParam int y,
                                         @RequestParam int threshold,
                                         @RequestParam int hysteresis) {
        immerManagerData.setReferenceX(x);
        immerManagerData.setReferenceY(y);
        immerManagerData.setReferenceThreshold(threshold);
        immerManagerData.setReferenceHysteresis(hysteresis);
        return immerManagerData;
    }

    @PostMapping("/setThresholds")
    @ResponseBody
    public ImmerManagerData setThresholds(@RequestParam int dark, @RequestParam int light) {
        immerManagerData.setDarkThreshold(dark);
        immerManagerData.setLightThreshold(light);
        return immerManagerData;
    }

    @PostMapping("/toggle")
    @ResponseBody
    public ImmerManagerData toggleEnabled() {
        immerManagerData.setEnabled(!immerManagerData.isEnabled());
        return immerManagerData;
    }

    @PostMapping("/enable")
    @ResponseBody
    public ImmerManagerData enable() {
        immerManagerData.setEnabled(true);
        return immerManagerData;
    }

    @PostMapping("/disable")
    @ResponseBody
    public ImmerManagerData disable() {
        immerManagerData.setEnabled(false);
        return immerManagerData;
    }

    @GetMapping
    @ResponseBody
    public ImmerManagerData getOffset() {
        return immerManagerData;
    }

    @GetMapping("/data")
    @ResponseBody
    public ImmerRest getDetectedData() {
        return immerData.getImmerRest();
    }

    @GetMapping("/stats")
    @ResponseBody
    public ImmerStatisticsSnapshot getStats() {
        return immerStatistics.getLast24Hours();
    }

    @GetMapping("/adjust")
    public ModelAndView adjustOffset() {
        ModelAndView modelAndView = new ModelAndView("immer-manager");
        modelAndView.addObject("xs", immerManagerData.getXs());
        modelAndView.addObject("ys", immerManagerData.getYs());
        modelAndView.addObject("enabled", immerManagerData.isEnabled());
        modelAndView.addObject("immerRest", immerData.getImmerRest());
        modelAndView.addObject("errorStats", errorStatistics.getLastErrorCounts("Immer"));
        modelAndView.addObject("referenceX", immerManagerData.getReferenceX());
        modelAndView.addObject("referenceY", immerManagerData.getReferenceY());
        modelAndView.addObject("referenceThreshold", immerManagerData.getReferenceThreshold());
        modelAndView.addObject("referenceHysteresis", immerManagerData.getReferenceHysteresis());
        modelAndView.addObject("darkThreshold", immerManagerData.getDarkThreshold());
        modelAndView.addObject("lightThreshold", immerManagerData.getLightThreshold());
        modelAndView.addObject("ambientBrightness", immerManagerData.getAmbientBrightness());
        modelAndView.addObject("lightMode", immerManagerData.isLightMode());
        return modelAndView;
    }
}
