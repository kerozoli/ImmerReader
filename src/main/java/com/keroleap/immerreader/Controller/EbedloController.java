package com.keroleap.immerreader.Controller;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.keroleap.immerreader.EbedloRest;
import com.keroleap.immerreader.Service.EbedloAnalyzerService;
import com.keroleap.immerreader.SharedData.EbedloData;
import com.keroleap.immerreader.SharedData.EbedloManagerData;

@Controller
@RequestMapping("/Ebedlo")
public class EbedloController {

    @Value("${camera.ebedlo.url}")
    private String cameraUrl;

    @Autowired
    private EbedloData ebedloData;

    @Autowired
    private EbedloAnalyzerService ebedloAnalyzerService;

    @Autowired
    private EbedloManagerData ebedloManagerData;

    @GetMapping(value = "/image", produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[] getImage() throws IOException {
        BufferedImage image = ebedloAnalyzerService.getDebugOverlayImage(ebedloManagerData);
        if (image == null) {
            image = ebedloAnalyzerService.getBufferedImage(cameraUrl);
            ebedloAnalyzerService.getEbedloRestData(image, ebedloManagerData);
            image = ebedloAnalyzerService.getDebugOverlayImage(ebedloManagerData);
        }
        return writeJpeg(image);
    }

    @GetMapping(value = "/uncroppedimage", produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[] getUncroppedImage() throws IOException {
        return getImage();
    }

    private byte[] writeJpeg(BufferedImage image) throws IOException {
        if (image == null) {
            image = createPlaceholderImage();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }

    private BufferedImage createPlaceholderImage() {
        BufferedImage placeholder = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = placeholder.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 320, 240);
        g.setColor(Color.WHITE);
        g.drawString("No camera image available", 20, 120);
        g.dispose();
        return placeholder;
    }

    @RequestMapping(value = "/ebedlodata")
    public ModelAndView getEbedloData() throws IOException {
        BufferedImage cachedImage = ebedloAnalyzerService.getBufferedImage(cameraUrl);
        ModelAndView modelAndView = new ModelAndView("ebedlodata");
        modelAndView.addObject("message", ebedloAnalyzerService.getEbedloRestData(cachedImage, ebedloManagerData).toString());
        return modelAndView;
    }

    @RequestMapping(value = "/ebedlorestdata")
    @ResponseBody
    public EbedloRest getEbedloRestData() {
        return ebedloData.getEbedloRest();
    }
}
