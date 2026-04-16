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

@Controller
@RequestMapping("/AristonManager")
public class AristonManagerController {

    @Autowired
    private AristonManagerData aristonManagerData;

    @Autowired
    private AristonData aristonData;

    @PostMapping("/set")
    @ResponseBody
    public AristonManagerData setOffset(@RequestParam int x, @RequestParam int y) {
        aristonManagerData.setOffsetX(x);
        aristonManagerData.setOffsetY(y);
        return aristonManagerData;
    }

    @GetMapping
    @ResponseBody
    public AristonManagerData getOffset() {
        return aristonManagerData;
    }

    @GetMapping("/adjust")
    public ModelAndView adjustOffset() {
        ModelAndView modelAndView = new ModelAndView("ariston-manager");
        modelAndView.addObject("offsetX", aristonManagerData.getOffsetX());
        modelAndView.addObject("offsetY", aristonManagerData.getOffsetY());
        modelAndView.addObject("aristonRest", aristonData.getAristonRest());
        return modelAndView;
    }
}
