package com.keroleap.immerreader.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.keroleap.immerreader.SharedData.ImmerOffsetData;

@RestController
@RequestMapping("/ImmerOffset")
public class ImmerOffsetController {

    @Autowired
    private ImmerOffsetData immerOffsetData;

    @PostMapping("/set")
    public ImmerOffsetData setOffset(@RequestParam int x, @RequestParam int y) {
        immerOffsetData.setOffsetX(x);
        immerOffsetData.setOffsetY(y);
        return immerOffsetData;
    }

    @GetMapping
    public ImmerOffsetData getOffset() {
        return immerOffsetData;
    }
}
