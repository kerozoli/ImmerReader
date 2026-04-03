package com.keroleap.immerreader.Controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.keroleap.immerreader.ImmerRest;
import com.keroleap.immerreader.Service.ImmerAnalyzerService;
import com.keroleap.immerreader.SharedData.ImmerData;
import com.keroleap.immerreader.SharedData.ImmerOffsetData;

@Controller
@RequestMapping("/Immer")
public class ImmerController   
{  

@Autowired
private ImmerData immerData;

@Autowired
private ImmerAnalyzerService immerAnalyzerService;

@Autowired
private ImmerOffsetData immerOffsetData;

@GetMapping(value = "/image", produces = MediaType.IMAGE_JPEG_VALUE)
public @ResponseBody byte[] getImage() throws IOException {
    BufferedImage cachedImage = immerAnalyzerService.getBufferedImage("http://192.168.1.196/image/jpeg.cgi");
    immerAnalyzerService.getImmerRestData(cachedImage, immerOffsetData.getOffsetX(), immerOffsetData.getOffsetY());
    int x1 = 90; // the x-coordinate of the top-left corner of the crop area
    int y1 = 35; // the y-coordinate of the top-left corner of the crop area
    int x2 = 240; // the x-coordinate of the bottom-right corner of the crop area
    int y2 = 140; // the y-coordinate of the bottom-right corner of the crop area

    int width = x2 - x1;
    int height = y2 - y1;

    // Crop the image
    BufferedImage bufferedImage = cachedImage.getSubimage(x1, y1, width, height);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(bufferedImage, "jpg", baos);
    byte[] bytes = baos.toByteArray();

    return bytes;
}

@GetMapping(value = "/uncroppedimage", produces = MediaType.IMAGE_JPEG_VALUE)
public @ResponseBody byte[] getUncroppedImage() throws IOException {
    BufferedImage cachedImage = immerAnalyzerService.getBufferedImage("http://192.168.1.196/image/jpeg.cgi");
    immerAnalyzerService.getImmerRestData(cachedImage, immerOffsetData.getOffsetX(), immerOffsetData.getOffsetY());

    // Crop the image
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(cachedImage, "jpg", baos);
    byte[] bytes = baos.toByteArray();

    return bytes;
}

@RequestMapping(value = "/immerdata")
public ModelAndView getImmerData() throws IOException {
    BufferedImage cachedImage = immerAnalyzerService.getBufferedImage("http://192.168.1.196/image/jpeg.cgi");
    ModelAndView modelAndView = new ModelAndView("immerdata");
    modelAndView.addObject("message", immerAnalyzerService.getImmerRestData(cachedImage, immerOffsetData.getOffsetX(), immerOffsetData.getOffsetY()).toString());
    return modelAndView;
}

@RequestMapping(value = "/immerrestdata")
@ResponseBody
public ImmerRest getImmerRestData() {
    return immerData.getImmerRest();
}
}