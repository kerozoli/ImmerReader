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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/**")
public class HelloWorldController   
{  

private static int LIGHT_THRESHOLD = -1500000;

@GetMapping(value = "/image", produces = MediaType.IMAGE_JPEG_VALUE)
public @ResponseBody byte[] getImage() throws IOException {
    BufferedImage cachedImage = getBufferedImage("http://192.168.1.196/image/jpeg.cgi");
    getData(cachedImage);
    int x1 = 90; // the x-coordinate of the top-left corner of the crop area
    int y1 = 35; // the y-coordinate of the top-left corner of the crop area
    int x2 = 240; // the x-coordinate of the bottom-right corner of the crop area
    int y2 = 140; // the y-coordinate of the bottom-right corner of the crop area

    // Calculate the width and height of the crop area
    int width = x2 - x1;
    int height = y2 - y1;

    // Crop the image
    BufferedImage bufferedImage = cachedImage.getSubimage(x1, y1, width, height);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(bufferedImage, "jpg", baos);
    byte[] bytes = baos.toByteArray();

    
    return bytes;
}

@RequestMapping(value = "/immerdata")
public ModelAndView getImmerData() throws IOException {
    BufferedImage cachedImage = getBufferedImage("http://192.168.1.196/image/jpeg.cgi");
    ModelAndView modelAndView = new ModelAndView("immerdata");
    modelAndView.addObject( "message", getData(cachedImage));
    return modelAndView;
}

private String getData(BufferedImage bufferedImage) {
    boolean kazanOn = getLightValueAnnDrawRedCross( 225, 100 ,  bufferedImage);
    boolean levelOne = getLightValueAnnDrawRedCross( 136, 58 ,  bufferedImage);
    boolean levelTwo = getLightValueAnnDrawRedCross(154, 58 ,  bufferedImage);
    boolean levelThree = getLightValueAnnDrawRedCross( 172, 58 ,  bufferedImage);
    return "Kazan: " + kazanOn + " Level: " + levelOne + levelTwo + levelThree;
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

        url.openStream().close();
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
    for (int a=x-5  ; a<x+5; a++) {
            lightValues.add(image.getRGB(a, y));
    }
    for (int b=y-5  ; b<y+5; b++) {
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