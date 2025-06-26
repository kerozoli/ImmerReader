package com.keroleap.immerreader.Controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
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

import com.keroleap.immerreader.ImmerRest;
import com.keroleap.immerreader.SharedData.ImmerData;

@Controller
@RequestMapping("/Immer")
public class ImmerController   
{  

private static final int LIGHT_THRESHOLD = -2500000;
private int previousTempValue;
@Autowired
private ImmerData immerData;

@GetMapping(value = "/image", produces = MediaType.IMAGE_JPEG_VALUE)
public @ResponseBody byte[] getImage() throws IOException {
    BufferedImage cachedImage = getBufferedImage("http://192.168.1.196/image/jpeg.cgi");
    getImmerRestData(cachedImage);
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
    BufferedImage cachedImage = getBufferedImage("http://192.168.1.196/image/jpeg.cgi");
    getImmerRestData(cachedImage);

    // Crop the image
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(cachedImage, "jpg", baos);
    byte[] bytes = baos.toByteArray();

    return bytes;
}

@RequestMapping(value = "/immerdata")
public ModelAndView getImmerData() throws IOException {
    BufferedImage cachedImage = getBufferedImage("http://192.168.1.196/image/jpeg.cgi");
    ModelAndView modelAndView = new ModelAndView("immerdata");
    modelAndView.addObject( "message", getImmerRestData(cachedImage).toString());
    return modelAndView;
}

@RequestMapping(value = "/immerrestdata")
@ResponseBody
public ImmerRest getImmerRestData() {
    return immerData.getAristonRest();
}

private ImmerRest getImmerRestData(BufferedImage bufferedImage) {
    boolean heating = getLightValueAnnDrawRedCross( 212, 97 ,  bufferedImage);
    boolean levelOne = getLightValueAnnDrawRedCross( 110, 58 ,  bufferedImage);
    boolean levelTwo = getLightValueAnnDrawRedCross( 120, 58 ,  bufferedImage);
    boolean levelThree = getLightValueAnnDrawRedCross(140, 58 ,  bufferedImage);
    boolean levelFour = getLightValueAnnDrawRedCross( 160, 58 ,  bufferedImage);

    boolean boilerOn = getLightValueAnnDrawRedCross( 212, 40 ,  bufferedImage);

    boolean digit1_1 = getLightValueAnnDrawRedCross(110, 124, bufferedImage);
    boolean digit1_2 = getLightValueAnnDrawRedCross(119, 112, bufferedImage);
    boolean digit1_3 = getLightValueAnnDrawRedCross(119, 86, bufferedImage);
    boolean digit1_4 = getLightValueAnnDrawRedCross(111, 74, bufferedImage);
    boolean digit1_5 = getLightValueAnnDrawRedCross(102, 86, bufferedImage);
    boolean digit1_6 = getLightValueAnnDrawRedCross(102, 112, bufferedImage);
    boolean digit1_7 = getLightValueAnnDrawRedCross(109, 99, bufferedImage);

    boolean digit2_1 = getLightValueAnnDrawRedCross(140, 124, bufferedImage);
    boolean digit2_2 = getLightValueAnnDrawRedCross(149, 112, bufferedImage);
    boolean digit2_3 = getLightValueAnnDrawRedCross(149, 86, bufferedImage);
    boolean digit2_4 = getLightValueAnnDrawRedCross(141, 74, bufferedImage);
    boolean digit2_5 = getLightValueAnnDrawRedCross(132, 86, bufferedImage);
    boolean digit2_6 = getLightValueAnnDrawRedCross(132, 112, bufferedImage);
    boolean digit2_7 = getLightValueAnnDrawRedCross(139, 99, bufferedImage);

    int number1 = getNumber(digit1_1, digit1_2, digit1_3, digit1_4, digit1_5, digit1_6, digit1_7) * 10;
    int number2 = getNumber(digit2_1, digit2_2, digit2_3, digit2_4, digit2_5, digit2_6, digit2_7);
    int number = number1 + number2;

    if (number > 500) {
        number = previousTempValue;
    }
    if (!(20 < number && number < 56)) {
        number = previousTempValue;
    }

    previousTempValue = number;

    if(!heating){
        number = 0;
    }

    int throttle = 0;
    if (levelOne) {
        throttle = 1;
    } 
    if (levelTwo) {
        throttle = 2;
    } 
    if (levelThree) {
        throttle = 3;
    } 
    if (levelFour) {
        throttle = 4;
    }

    ImmerRest immerRest = new ImmerRest();
    immerRest.setTemperaute(number);
    immerRest.setThrottle(throttle);
    immerRest.setHeating(heating);
    immerRest.setBoilerOn(boilerOn);

    return immerRest;
}

public int getNumber(boolean digit1_1, boolean digit1_2, boolean digit1_3, boolean digit1_4, boolean digit1_5, boolean digit1_6, boolean digit1_7) {
    int number = 1000;
    if (digit1_1 && digit1_2 && digit1_3 && digit1_4 && digit1_5 && digit1_6 && !digit1_7) {
        number = 0;
    }
    if (!digit1_1 && digit1_2 && digit1_3 && !digit1_4 && !digit1_5 && !digit1_6 && !digit1_7) {
        number = 1;
    }
    if (digit1_1 && !digit1_2 && digit1_3 && digit1_4 && !digit1_5 && digit1_6 && digit1_7) {
        number = 2;
    }
    if (digit1_1 && digit1_2 && digit1_3 && digit1_4 && !digit1_5 && !digit1_6 && digit1_7) {
        number = 3;
    }
    if (!digit1_1 && digit1_2 && digit1_3 && !digit1_4 && digit1_5 && !digit1_6 && digit1_7) {
        number = 4;
    }
    if (digit1_1 && digit1_2 && !digit1_3 && digit1_4 && digit1_5 && !digit1_6 && digit1_7) {
        number = 5;
    }
    if (digit1_1 && digit1_2 && !digit1_3 && digit1_4 && digit1_5 && digit1_6 && digit1_7) {
        number = 6;
    }
    if (!digit1_1 && digit1_2 && digit1_3 && digit1_4 && !digit1_5 && !digit1_6 && !digit1_7) {
        number = 7;
    }
    if (digit1_1 && digit1_2 && digit1_3 && digit1_4 && digit1_5 && digit1_6 && digit1_7) {
        number = 8;
    }
    if (digit1_1 && digit1_2 && digit1_3 && digit1_4 && digit1_5 && !digit1_6 && digit1_7) {
        number = 9;
    }
    return number;
}

private BufferedImage getBufferedImage(String imageUrl) throws IOException {

    URL url = null;
    try {
        url = new URL(imageUrl);
    } catch (MalformedURLException e) {
        e.printStackTrace();
    }
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    try {
        byte[] chunk = new byte[4096];
        int bytesRead;
        InputStream stream = url.openStream();

        while ((bytesRead = stream.read(chunk)) > 0) {
            outputStream.write(chunk, 0, bytesRead);
        }

        stream.close();
        byte[] data = outputStream.toByteArray();
        ByteArrayInputStream input = new ByteArrayInputStream(data);
        BufferedImage image = ImageIO.read(input);
        outputStream.close();
        return image;

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
    boolean detected = (lightValue > LIGHT_THRESHOLD);
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