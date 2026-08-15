package com.keroleap.immerreader.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.keroleap.immerreader.SharedData.AristonData;
import com.keroleap.immerreader.SharedData.AristonManagerData;
import com.keroleap.immerreader.SharedData.ErrorStatistics;

@Controller
@RequestMapping("/AristonManager")
public class AristonManagerController {

    @Autowired
    private AristonManagerData aristonManagerData;

    @Autowired
    private AristonData aristonData;

    @Autowired
    private ErrorStatistics errorStatistics;

    @PostMapping("/set")
    @ResponseBody
    public AristonManagerData setPoints(@RequestParam int startX,
                                         @RequestParam int startY,
                                         @RequestParam(required = false, defaultValue = "0") int controlX,
                                         @RequestParam(required = false, defaultValue = "0") int controlY,
                                         @RequestParam int endX,
                                         @RequestParam int endY) {
        aristonManagerData.setStartX(startX);
        aristonManagerData.setStartY(startY);
        if (controlX != 0 || controlY != 0) {
            aristonManagerData.setControlX(controlX);
            aristonManagerData.setControlY(controlY);
        }
        aristonManagerData.setEndX(endX);
        aristonManagerData.setEndY(endY);
        return aristonManagerData;
    }

    @PostMapping("/toggle")
    @ResponseBody
    public AristonManagerData toggleEnabled() {
        aristonManagerData.setEnabled(!aristonManagerData.isEnabled());
        return aristonManagerData;
    }

    @PostMapping("/enable")
    @ResponseBody
    public AristonManagerData enable() {
        aristonManagerData.setEnabled(true);
        return aristonManagerData;
    }

    @PostMapping("/disable")
    @ResponseBody
    public AristonManagerData disable() {
        aristonManagerData.setEnabled(false);
        return aristonManagerData;
    }

    @GetMapping
    @ResponseBody
    public AristonManagerData getPoints() {
        return aristonManagerData;
    }

    @GetMapping("/adjust")
    public ModelAndView adjustPoints() {
        ModelAndView modelAndView = new ModelAndView("ariston-manager");
        modelAndView.addObject("startX", aristonManagerData.getStartX());
        modelAndView.addObject("startY", aristonManagerData.getStartY());
        modelAndView.addObject("controlX", aristonManagerData.getControlX());
        modelAndView.addObject("controlY", aristonManagerData.getControlY());
        modelAndView.addObject("endX", aristonManagerData.getEndX());
        modelAndView.addObject("endY", aristonManagerData.getEndY());
        modelAndView.addObject("enabled", aristonManagerData.isEnabled());
        modelAndView.addObject("aristonRest", aristonData.getAristonRest());
        modelAndView.addObject("errorStats", errorStatistics.getLastErrorCounts("Ariston"));
        return modelAndView;
    }
}
