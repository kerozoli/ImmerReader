package com.keroleap.immerreader.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.keroleap.immerreader.SharedData.ImmerData;
import com.keroleap.immerreader.SharedData.ImmerManagerData;
import com.keroleap.immerreader.SharedData.ErrorStatistics;

@Controller
@RequestMapping("/ImmerManager")
public class ImmerManagerController {

    @Autowired
    private ImmerManagerData immerManagerData;

    @Autowired
    private ImmerData immerData;

    @Autowired
    private ErrorStatistics errorStatistics;

    @PostMapping("/set")
    @ResponseBody
    public ImmerManagerData setOffset(@RequestParam int x, @RequestParam int y) {
        immerManagerData.setOffsetX(x);
        immerManagerData.setOffsetY(y);
        return immerManagerData;
    }

    @GetMapping
    @ResponseBody
    public ImmerManagerData getOffset() {
        return immerManagerData;
    }

    @GetMapping("/adjust")
    public ModelAndView adjustOffset() {
        ModelAndView modelAndView = new ModelAndView("immer-manager");
        modelAndView.addObject("offsetX", immerManagerData.getOffsetX());
        modelAndView.addObject("offsetY", immerManagerData.getOffsetY());
        modelAndView.addObject("immerRest", immerData.getImmerRest());
        modelAndView.addObject("errorStats", errorStatistics.getLastErrorCounts("Immer"));
        return modelAndView;
    }
}
