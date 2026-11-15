package com.keroleap.immerreader.Controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.keroleap.immerreader.ImmerRest;
import com.keroleap.immerreader.Service.ImmerAnalyzerService;
import com.keroleap.immerreader.SharedData.ImmerData;
import com.keroleap.immerreader.SharedData.ImmerManagerData;

@Controller
@RequestMapping("/Immer")
public class ImmerController {

    private static final int CROP_X1 = 90;
    private static final int CROP_Y1 = 35;
    private static final int CROP_X2 = 240;
    private static final int CROP_Y2 = 140;

    @Autowired
    private ImmerData immerData;

    @Autowired
    private ImmerAnalyzerService immerAnalyzerService;

    @Autowired
    private ImmerManagerData immerManagerData;

    @Value("${camera.immer.url:http://192.168.1.196/image/jpeg.cgi}")
    private String cameraUrl = "http://192.168.1.196/image/jpeg.cgi";

    /**
     * Returns a cropped preview. The preview is best-effort: when the camera is
     * unreachable the last good frame is shown instead of failing the request.
     */
    @GetMapping("/image")
    public ResponseEntity<byte[]> getImage() throws IOException {
        BufferedImage image = immerAnalyzerService.getPreviewImage(cameraUrl);
        if (image == null) {
            return cameraUnavailable();
        }
        immerAnalyzerService.getImmerRestData(image, immerManagerData.getXs(), immerManagerData.getYs());
        return jpeg(crop(image, CROP_X1, CROP_Y1, CROP_X2, CROP_Y2));
    }

    /**
     * Returns the full preview, optionally with the analyzer's detection crosses.
     */
    @GetMapping("/uncroppedimage")
    public ResponseEntity<byte[]> getUncroppedImage(@RequestParam(required = false, defaultValue = "true") boolean analyze)
            throws IOException {
        BufferedImage image = immerAnalyzerService.getPreviewImage(cameraUrl);
        if (image == null) {
            return cameraUnavailable();
        }
        if (analyze) {
            immerAnalyzerService.getImmerRestData(image, immerManagerData.getXs(), immerManagerData.getYs());
        }
        return jpeg(image);
    }

    @RequestMapping("/immerdata")
    public ModelAndView getImmerData() {
        ModelAndView modelAndView = new ModelAndView("immerdata");
        BufferedImage image = immerAnalyzerService.getPreviewImage(cameraUrl);
        String message = image != null
                ? immerAnalyzerService.getImmerRestData(image, immerManagerData.getXs(), immerManagerData.getYs()).toString()
                : "Camera unavailable";
        modelAndView.addObject("message", message);
        return modelAndView;
    }

    @RequestMapping("/immerrestdata")
    @ResponseBody
    public ImmerRest getImmerRestData() {
        return immerData.getImmerRest();
    }

    private ResponseEntity<byte[]> jpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(baos.toByteArray());
    }

    private ResponseEntity<byte[]> cameraUnavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.TEXT_PLAIN)
                .body("Camera unavailable".getBytes(StandardCharsets.UTF_8));
    }

    private BufferedImage crop(BufferedImage image, int x1, int y1, int x2, int y2) {
        int left = Math.max(0, Math.min(x1, image.getWidth() - 1));
        int top = Math.max(0, Math.min(y1, image.getHeight() - 1));
        int right = Math.max(left + 1, Math.min(x2, image.getWidth()));
        int bottom = Math.max(top + 1, Math.min(y2, image.getHeight()));
        return image.getSubimage(left, top, right - left, bottom - top);
    }
}
