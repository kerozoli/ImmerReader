package com.keroleap.immerreader.Controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.keroleap.immerreader.AristonRest;
import com.keroleap.immerreader.SharedData.AristonData;

@Controller
@RequestMapping("/Ariston")
public class AristonController   
{  
private static final int LIGHT_THRESHOLD = -7000000;

@Autowired
private AristonData aristonData;

@GetMapping(value = "/image", produces = MediaType.IMAGE_JPEG_VALUE)
public @ResponseBody byte[] getImage() throws IOException {
    BufferedImage cachedImage = getBufferedImage("http://192.168.1.191/cgi/jpg/image.cgi");
    getAristonRestData(cachedImage);
    int x1 = 60; // the x-coordinate of the top-left corner of the crop area
    int y1 = 115; // the y-coordinate of the top-left corner of the crop area
    int x2 = 260; // the x-coordinate of the bottom-right corner of the crop area
    int y2 = 230; // the y-coordinate of the bottom-right corner of the crop area

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
    BufferedImage cachedImage = getBufferedImage("http://192.168.1.191/cgi/jpg/image.cgi");
    getAristonRestData(cachedImage);
    // Crop the image
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(cachedImage, "jpg", baos);
    byte[] bytes = baos.toByteArray();

    return bytes;
}

@RequestMapping(value = "/aristondata")
public ModelAndView getAristonData() throws IOException {
    BufferedImage cachedImage = getBufferedImage("http://192.168.1.191/cgi/jpg/image.cgi");
    ModelAndView modelAndView = new ModelAndView("immerdata");
    modelAndView.addObject( "message", getAristonRestData(cachedImage).toString());
    return modelAndView;
}


@RequestMapping(value = "/aristonrestdata")
@ResponseBody
public AristonRest getAristonRestData() {
    return aristonData.getAristonRest();
}

private AristonRest getAristonRestData(BufferedImage bufferedImage) {
    boolean percent_0 = getLightValueAnnDrawRedCross( 160, 160 ,  bufferedImage);
    boolean percent_5 = getLightValueAnnDrawRedCross( 163, 161 ,  bufferedImage);
    boolean percent_10 = getLightValueAnnDrawRedCross( 166, 162 ,  bufferedImage);
    boolean percent_15 = getLightValueAnnDrawRedCross( 169, 163 ,  bufferedImage);
    boolean percent_20 = getLightValueAnnDrawRedCross( 172, 164 ,  bufferedImage);
    boolean percent_25 = getLightValueAnnDrawRedCross( 175, 165 ,  bufferedImage);
    boolean percent_30 = getLightValueAnnDrawRedCross( 178, 166 ,  bufferedImage);
    boolean percent_35 = getLightValueAnnDrawRedCross( 181, 167 ,  bufferedImage);
    boolean percent_40 = getLightValueAnnDrawRedCross( 184, 168 ,  bufferedImage);
    boolean percent_45 = getLightValueAnnDrawRedCross( 187, 169 ,  bufferedImage);
    boolean percent_50 = getLightValueAnnDrawRedCross( 190, 170 ,  bufferedImage);
    boolean percent_55 = getLightValueAnnDrawRedCross( 193, 171 ,  bufferedImage);
    boolean percent_60 = getLightValueAnnDrawRedCross( 196, 172 ,  bufferedImage);
    boolean percent_65 = getLightValueAnnDrawRedCross( 199, 173 ,  bufferedImage);
    boolean percent_70 = getLightValueAnnDrawRedCross( 202, 174 ,  bufferedImage);
    boolean percent_75 = getLightValueAnnDrawRedCross( 205, 175 ,  bufferedImage);
    boolean percent_80 = getLightValueAnnDrawRedCross( 208, 176 ,  bufferedImage);
    boolean percent_85 = getLightValueAnnDrawRedCross( 211, 177 ,  bufferedImage);
    boolean percent_90 = getLightValueAnnDrawRedCross( 214, 178 ,  bufferedImage);
    boolean percent_95 = getLightValueAnnDrawRedCross( 217, 179 ,  bufferedImage);
    boolean percent_100 = getLightValueAnnDrawRedCross( 220, 180 ,  bufferedImage);

    int percentage = 0;
    if(percent_0) percentage = 0;
    if(percent_5) percentage = 5;
    if(percent_10) percentage = 10;
    if(percent_15) percentage = 15;
    if(percent_20) percentage = 20;
    if(percent_25) percentage = 25;
    if(percent_30) percentage = 30;
    if(percent_35) percentage = 35;
    if(percent_40) percentage = 40;
    if(percent_45) percentage = 45;
    if(percent_50) percentage = 50;
    if(percent_55) percentage = 55;
    if(percent_60) percentage = 60;
    if(percent_65) percentage = 65;
    if(percent_70) percentage = 70;
    if(percent_75) percentage = 75;
    if(percent_80) percentage = 80;
    if(percent_85) percentage = 85;
    if(percent_90) percentage = 90;
    if(percent_95) percentage = 95;
    if(percent_100) percentage = 100;

    AristonRest aristonRest = new AristonRest();
    aristonRest.setPercentage(percentage);

    return aristonRest;
}


private BufferedImage getBufferedImage(String imageUrl) throws IOException {
    try {
        URL url = new URL(imageUrl);
        try (InputStream stream = url.openStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[4096];
            int bytesRead;
            while ((bytesRead = stream.read(chunk)) > 0) {
                outputStream.write(chunk, 0, bytesRead);
            }

            try (ByteArrayInputStream input = new ByteArrayInputStream(outputStream.toByteArray())) {
                return ImageIO.read(input);
            }
        }
    } catch (IOException e) {
        e.printStackTrace();
        return null;
    }
}

private boolean getLightValueAnnDrawRedCross(int x, int y , BufferedImage image) {
    List <Integer> lightValues = new ArrayList<Integer>();
    for (int a=x-3  ; a<x+3; a++) {
            lightValues.add(image.getRGB(a, y));
    }
    for (int b=y-3  ; b<y+3; b++) {
            lightValues.add(image.getRGB(x, b));
    }   
    double lightValue = lightValues.stream().mapToInt(Integer::intValue).average().getAsDouble();
    boolean detected = (lightValue < LIGHT_THRESHOLD);
    int color = detected ? 16711680 : 16777215;
    for (int a=x-5  ; a<x+5; a++) {
            image.setRGB(a, y, color);
    }
    for (int b=y-5  ; b<y+5; b++) {
            image.setRGB(x, b, color);
    }
    return detected;
}  
}