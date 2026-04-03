package com.keroleap.immerreader.Controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.keroleap.immerreader.AristonRest;
import com.keroleap.immerreader.Service.AristonAnalyzerService;
import com.keroleap.immerreader.SharedData.AristonData;

@Controller
@RequestMapping("/Ariston")
public class AristonController {

    private static final Logger logger = LoggerFactory.getLogger(AristonController.class);

    @Autowired
    private AristonData aristonData;

    @Autowired
    private AristonAnalyzerService aristonAnalyzerService;

    @GetMapping(value = "/image", produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[] getImage() throws IOException {
        BufferedImage cachedImage = aristonAnalyzerService.getBufferedImage("http://192.168.1.191/cgi/jpg/image.cgi");
        aristonAnalyzerService.getAristonRestData(cachedImage);
        int x1 = 60;
        int y1 = 115;
        int x2 = 260;
        int y2 = 230;

        int width = x2 - x1;
        int height = y2 - y1;

        BufferedImage bufferedImage = cachedImage.getSubimage(x1, y1, width, height);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "jpg", baos);
        return baos.toByteArray();
    }

    @GetMapping(value = "/uncroppedimage", produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[] getUncroppedImage() throws IOException {
        BufferedImage cachedImage = aristonAnalyzerService.getBufferedImage("http://192.168.1.191/cgi/jpg/image.cgi");
        aristonAnalyzerService.getAristonRestData(cachedImage);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(cachedImage, "jpg", baos);
        return baos.toByteArray();
    }

    @RequestMapping(value = "/aristondata")
    public ModelAndView getAristonData() throws IOException {
        BufferedImage cachedImage = aristonAnalyzerService.getBufferedImage("http://192.168.1.191/cgi/jpg/image.cgi");
        ModelAndView modelAndView = new ModelAndView("immerdata");
        modelAndView.addObject("message", aristonAnalyzerService.getAristonRestData(cachedImage).toString());
        return modelAndView;
    }

    @RequestMapping(value = "/aristonrestdata")
    @ResponseBody
    public AristonRest getAristonRestData() {
        return aristonData.getAristonRest();
    }
}
