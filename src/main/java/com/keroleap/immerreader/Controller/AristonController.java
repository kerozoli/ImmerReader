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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.keroleap.immerreader.AristonRest;
import com.keroleap.immerreader.Service.AristonAnalyzerService;
import com.keroleap.immerreader.SharedData.AristonData;
import com.keroleap.immerreader.SharedData.AristonManagerData;

@Controller
@RequestMapping("/Ariston")
public class AristonController {

    private static final int CROP_X1 = 60;
    private static final int CROP_Y1 = 115;
    private static final int CROP_X2 = 260;
    private static final int CROP_Y2 = 230;

    @Autowired
    private AristonData aristonData;

    @Autowired
    private AristonAnalyzerService aristonAnalyzerService;

    @Autowired
    private AristonManagerData aristonManagerData;

    @Value("${camera.ariston.url:http://192.168.1.191/cgi/jpg/image.cgi}")
    private String cameraUrl = "http://192.168.1.191/cgi/jpg/image.cgi";

    /**
     * Returns a cropped preview. The preview is best-effort: when the camera is
     * unreachable the last good frame is shown instead of failing the request.
     */
    @GetMapping("/image")
    public ResponseEntity<byte[]> getImage() throws IOException {
        BufferedImage image = aristonAnalyzerService.getPreviewImage(cameraUrl);
        if (image == null) {
            return cameraUnavailable();
        }
        aristonAnalyzerService.getAristonRestData(image,
                aristonManagerData.getStartX(), aristonManagerData.getStartY(),
                aristonManagerData.getControlX(), aristonManagerData.getControlY(),
                aristonManagerData.getEndX(), aristonManagerData.getEndY());
        return jpeg(crop(image, CROP_X1, CROP_Y1, CROP_X2, CROP_Y2));
    }

    /**
     * Returns the full preview with the analyzer's detection crosses.
     */
    @GetMapping("/uncroppedimage")
    public ResponseEntity<byte[]> getUncroppedImage() throws IOException {
        BufferedImage image = aristonAnalyzerService.getPreviewImage(cameraUrl);
        if (image == null) {
            return cameraUnavailable();
        }
        aristonAnalyzerService.getAristonRestData(image,
                aristonManagerData.getStartX(), aristonManagerData.getStartY(),
                aristonManagerData.getControlX(), aristonManagerData.getControlY(),
                aristonManagerData.getEndX(), aristonManagerData.getEndY());
        return jpeg(image);
    }

    @RequestMapping("/aristondata")
    public ModelAndView getAristonData() {
        ModelAndView modelAndView = new ModelAndView("immerdata");
        BufferedImage image = aristonAnalyzerService.getPreviewImage(cameraUrl);
        String message = image != null
                ? aristonAnalyzerService.getAristonRestData(image,
                        aristonManagerData.getStartX(), aristonManagerData.getStartY(),
                        aristonManagerData.getControlX(), aristonManagerData.getControlY(),
                        aristonManagerData.getEndX(), aristonManagerData.getEndY()).toString()
                : "Camera unavailable";
        modelAndView.addObject("message", message);
        return modelAndView;
    }

    @RequestMapping("/aristonrestdata")
    @ResponseBody
    public AristonRest getAristonRestData() {
        return aristonData.getAristonRest();
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
