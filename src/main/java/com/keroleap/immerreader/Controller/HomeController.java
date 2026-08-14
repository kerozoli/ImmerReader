package com.keroleap.immerreader.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.keroleap.immerreader.SharedData.AristonManagerData;
import com.keroleap.immerreader.SharedData.ChickenManagerData;
import com.keroleap.immerreader.SharedData.EbedloManagerData;
import com.keroleap.immerreader.SharedData.ImmerManagerData;

@Controller
public class HomeController {

    @Autowired
    private ImmerManagerData immerManagerData;

    @Autowired
    private AristonManagerData aristonManagerData;

    @Autowired
    private EbedloManagerData ebedloManagerData;

    @Autowired
    private ChickenManagerData chickenManagerData;

    @GetMapping("/")
    public ModelAndView home() {
        ModelAndView modelAndView = new ModelAndView("index");
        modelAndView.addObject("immerEnabled", immerManagerData.isEnabled());
        modelAndView.addObject("aristonEnabled", aristonManagerData.isEnabled());
        modelAndView.addObject("ebedloEnabled", ebedloManagerData.isEnabled());
        modelAndView.addObject("chickenEnabled", chickenManagerData.isEnabled());
        return modelAndView;
    }
}
