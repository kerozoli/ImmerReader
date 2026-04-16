package com.keroleap.immerreader.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.keroleap.immerreader.SharedData.ImmerOffsetData;

@Controller
@RequestMapping("/ImmerOffset")
public class ImmerOffsetController {

    @Autowired
    private ImmerOffsetData immerOffsetData;

    @PostMapping("/set")
    @ResponseBody
    public ImmerOffsetData setOffset(@RequestParam int x, @RequestParam int y) {
        immerOffsetData.setOffsetX(x);
        immerOffsetData.setOffsetY(y);
        return immerOffsetData;
    }

    @GetMapping
    @ResponseBody
    public ImmerOffsetData getOffset() {
        return immerOffsetData;
    }

    @GetMapping("/adjust")
    public ModelAndView adjustOffset() {
        ModelAndView modelAndView = new ModelAndView("offset-adjuster");
        modelAndView.addObject("offsetX", immerOffsetData.getOffsetX());
        modelAndView.addObject("offsetY", immerOffsetData.getOffsetY());
        return modelAndView;
    }
}
