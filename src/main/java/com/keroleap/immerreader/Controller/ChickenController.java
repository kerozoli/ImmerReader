package com.keroleap.immerreader.Controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.keroleap.immerreader.ChickenRest;
import com.keroleap.immerreader.Service.ChickenAnalyzerService;
import com.keroleap.immerreader.SharedData.ChickenData;
import com.keroleap.immerreader.SharedData.ChickenManagerData;

@Controller
@RequestMapping("/Chicken")
public class ChickenController {

    @Value("${camera.chicken.url}")
    private String cameraUrl;

    @Autowired
    private ChickenData chickenData;

    @Autowired
    private ChickenAnalyzerService chickenAnalyzerService;

    @Autowired
    private ChickenManagerData chickenManagerData;

    @GetMapping(value = "/image", produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[] getImage() throws IOException {
        BufferedImage image = chickenAnalyzerService.getDebugOverlayImage(chickenManagerData);
        if (image == null) {
            image = chickenAnalyzerService.getBufferedImage(cameraUrl);
            chickenAnalyzerService.getChickenRestData(image, chickenManagerData);
            image = chickenAnalyzerService.getDebugOverlayImage(chickenManagerData);
        }
        if (image == null) {
            image = chickenAnalyzerService.createPlaceholderImage();
        }
        return writeJpeg(image);
    }

    @GetMapping(value = "/uncroppedimage", produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[] getUncroppedImage() throws IOException {
        BufferedImage image = chickenAnalyzerService.getBufferedImage(cameraUrl);
        return writeJpeg(image);
    }

    @GetMapping(value = "/debugimage", produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[] getDebugImage() throws IOException {
        BufferedImage image = chickenAnalyzerService.getDebugOverlayImage(chickenManagerData);
        if (image == null) {
            try {
                image = chickenAnalyzerService.getBufferedImage(cameraUrl);
                chickenAnalyzerService.getChickenRestData(image, chickenManagerData);
                image = chickenAnalyzerService.getDebugOverlayImage(chickenManagerData);
            } catch (Throwable t) {
                image = createErrorImage(t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }
        if (image == null) {
            image = createErrorImage("No debug overlay available");
        }
        return writeJpeg(image);
    }

    private BufferedImage createErrorImage(String message) {
        BufferedImage errorImage = new BufferedImage(640, 160, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = errorImage.createGraphics();
        g.setColor(java.awt.Color.BLACK);
        g.fillRect(0, 0, 640, 160);
        g.setColor(java.awt.Color.RED);
        g.fillRect(0, 120, 640, 40);
        g.setColor(java.awt.Color.WHITE);
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
        g.drawString("DEBUG OVERLAY ERROR", 20, 30);
        g.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 14));
        if (message != null) {
            g.drawString(message, 20, 70);
        }
        g.setColor(java.awt.Color.BLACK);
        g.drawString("Check server logs / camera configuration", 20, 145);
        g.dispose();
        return errorImage;
    }

    private byte[] writeJpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }

    @RequestMapping(value = "/chickendata")
    public ModelAndView getChickenData() throws IOException {
        BufferedImage cachedImage = chickenAnalyzerService.getBufferedImage(cameraUrl);
        ModelAndView modelAndView = new ModelAndView("chickendata");
        modelAndView.addObject("message", chickenAnalyzerService.getChickenRestData(cachedImage, chickenManagerData).toString());
        return modelAndView;
    }

    @RequestMapping(value = "/chickenrestdata")
    @ResponseBody
    public ChickenRest getChickenRestData() {
        return chickenData.getChickenRest();
    }

    @RequestMapping(value = "/contourdata")
    @ResponseBody
    public List<Map<String, Object>> getContourData() {
        return chickenAnalyzerService.getLastContourData();
    }
}
