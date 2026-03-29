package com.keroleap.immerreader.Controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.keroleap.immerreader.ImmerRest;
import com.keroleap.immerreader.SharedData.ImmerData;
import com.keroleap.immerreader.service.ImageProcessingService;
import com.keroleap.immerreader.service.ImmerParserService;

@Controller
@RequestMapping("/Immer")
public class ImmerController {
    private static final Logger logger = LoggerFactory.getLogger(ImmerController.class);

    @Autowired
    private ImmerData immerData;

    @Autowired
    private ImageProcessingService imageProcessingService;

    @Autowired
    private ImmerParserService immerParserService;

    @Value("${camera.immer.url}")
    private String immerCameraUrl;

    @GetMapping(value = "/image", produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[] getImage() throws IOException {
        BufferedImage cachedImage = imageProcessingService.getBufferedImage(immerCameraUrl);
        if (cachedImage == null) {
            logger.warn("No image available from Immer camera");
            return new byte[0];
        }
        getImmerRestData(cachedImage);
        int x1 = 90;
        int y1 = 35;
        int x2 = 240;
        int y2 = 140;

        BufferedImage bufferedImage = cachedImage.getSubimage(x1, y1, x2 - x1, y2 - y1);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "jpg", baos);
        return baos.toByteArray();
    }

    @GetMapping(value = "/uncroppedimage", produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[] getUncroppedImage() throws IOException {
        BufferedImage cachedImage = imageProcessingService.getBufferedImage(immerCameraUrl);
        if (cachedImage == null) {
            logger.warn("No image available from Immer camera");
            return new byte[0];
        }
        getImmerRestData(cachedImage);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(cachedImage, "jpg", baos);
        return baos.toByteArray();
    }

    @RequestMapping(value = "/immerdata")
    public ModelAndView getImmerData() throws IOException {
        BufferedImage cachedImage = imageProcessingService.getBufferedImage(immerCameraUrl);
        ModelAndView modelAndView = new ModelAndView("immerdata");
        modelAndView.addObject("message", getImmerRestData(cachedImage).toString());
        return modelAndView;
    }

    @RequestMapping(value = "/immerrestdata")
    @ResponseBody
    public ImmerRest getImmerRestData() {
        return immerData.getImmerRest();
    }

    private ImmerRest getImmerRestData(BufferedImage bufferedImage) {
        return immerParserService.parse(bufferedImage);
    }
}