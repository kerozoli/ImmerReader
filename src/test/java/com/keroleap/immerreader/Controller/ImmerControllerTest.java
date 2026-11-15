package com.keroleap.immerreader.Controller;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.keroleap.immerreader.ImmerRest;
import com.keroleap.immerreader.Service.ImmerAnalyzerService;
import com.keroleap.immerreader.SharedData.ImmerData;
import com.keroleap.immerreader.SharedData.ImmerManagerData;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImmerController.class)
class ImmerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImmerData immerData;

    @MockitoBean
    private ImmerAnalyzerService immerAnalyzerService;

    @MockitoBean
    private ImmerManagerData immerManagerData;

    @Test
    void uncroppedImage_returnsJpegWhenCameraAvailable() throws Exception {
        when(immerAnalyzerService.getPreviewImage(anyString()))
                .thenReturn(new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB));

        mockMvc.perform(MockMvcRequestBuilders.get("/Immer/uncroppedimage"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));
    }

    @Test
    void uncroppedImage_returnsServiceUnavailableWhenCameraNeverDelivered() throws Exception {
        when(immerAnalyzerService.getPreviewImage(anyString())).thenReturn(null);

        mockMvc.perform(MockMvcRequestBuilders.get("/Immer/uncroppedimage"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void uncroppedImage_withAnalyzeDisabledSkipsAnalysis() throws Exception {
        when(immerAnalyzerService.getPreviewImage(anyString()))
                .thenReturn(new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB));

        mockMvc.perform(MockMvcRequestBuilders.get("/Immer/uncroppedimage").param("analyze", "false"))
                .andExpect(status().isOk());

        verifyNoInteractions(immerData);
        verify(immerAnalyzerService, never()).getImmerRestData(any(), any(), any());
    }

    @Test
    void croppedImage_returnsServiceUnavailableWhenCameraNeverDelivered() throws Exception {
        when(immerAnalyzerService.getPreviewImage(anyString())).thenReturn(null);

        mockMvc.perform(MockMvcRequestBuilders.get("/Immer/image"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void restData_returnsPublishedValue() throws Exception {
        ImmerRest rest = new ImmerRest();
        rest.setTemperaute(55);
        when(immerData.getImmerRest()).thenReturn(rest);

        mockMvc.perform(MockMvcRequestBuilders.get("/Immer/immerrestdata"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.temperaute").value(55));
    }
}
