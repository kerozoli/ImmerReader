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

import com.keroleap.immerreader.SharedData.EbedloData;
import com.keroleap.immerreader.SharedData.EbedloManagerData;
import com.keroleap.immerreader.SharedData.ErrorStatistics;

@Controller
@RequestMapping("/EbedloManager")
public class EbedloManagerController {

    private static final int POINT_COUNT = 4;

    @Autowired
    private EbedloManagerData ebedloManagerData;

    @Autowired
    private EbedloData ebedloData;

    @Autowired
    private ErrorStatistics errorStatistics;

    @PostMapping("/set")
    @ResponseBody
    public ResponseEntity<?> setPoints(@RequestParam String points, @RequestParam int threshold) {
        String[] parts = points.split(",");
        if (parts.length != POINT_COUNT * 2) {
            return ResponseEntity.badRequest().body("Expected " + (POINT_COUNT * 2) + " comma-separated coordinates, got " + parts.length);
        }
        try {
            int[] xs = new int[POINT_COUNT];
            int[] ys = new int[POINT_COUNT];
            for (int i = 0; i < POINT_COUNT; i++) {
                xs[i] = Integer.parseInt(parts[i * 2].trim());
                ys[i] = Integer.parseInt(parts[i * 2 + 1].trim());
            }
            ebedloManagerData.setPoints(xs, ys);
            ebedloManagerData.setThreshold(threshold);
            return ResponseEntity.ok(ebedloManagerData);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid coordinate value: " + e.getMessage());
        }
    }

    @PostMapping("/toggle")
    @ResponseBody
    public EbedloManagerData toggleEnabled() {
        ebedloManagerData.setEnabled(!ebedloManagerData.isEnabled());
        return ebedloManagerData;
    }

    @PostMapping("/enable")
    @ResponseBody
    public EbedloManagerData enable() {
        ebedloManagerData.setEnabled(true);
        return ebedloManagerData;
    }

    @PostMapping("/disable")
    @ResponseBody
    public EbedloManagerData disable() {
        ebedloManagerData.setEnabled(false);
        return ebedloManagerData;
    }

    @GetMapping
    @ResponseBody
    public EbedloManagerData getPoints() {
        return ebedloManagerData;
    }

    @GetMapping("/adjust")
    public ModelAndView adjustPoints() {
        ModelAndView modelAndView = new ModelAndView("ebedlo-manager");
        modelAndView.addObject("xs", ebedloManagerData.getXs());
        modelAndView.addObject("ys", ebedloManagerData.getYs());
        modelAndView.addObject("threshold", ebedloManagerData.getThreshold());
        modelAndView.addObject("enabled", ebedloManagerData.isEnabled());
        modelAndView.addObject("ebedloRest", ebedloData.getEbedloRest());
        modelAndView.addObject("errorStats", errorStatistics.getLastErrorCounts("Ebedlo"));
        return modelAndView;
    }
}
