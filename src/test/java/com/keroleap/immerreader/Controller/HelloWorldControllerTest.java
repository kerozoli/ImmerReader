package com.keroleap.immerreader.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.keroleap.immerreader.ImmerRest;
import com.keroleap.immerreader.Service.ImmerAnalyzerService;
import com.keroleap.immerreader.SharedData.ImmerData;
import com.keroleap.immerreader.SharedData.ImmerManagerData;
import com.keroleap.immerreader.SharedData.ErrorStatistics;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ImmerController.class)
public class HelloWorldControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImmerData immerData;

    @MockitoBean
    private ImmerAnalyzerService immerAnalyzerService;

    @MockitoBean
    private ImmerManagerData immerManagerData;

    @MockitoBean
    private ErrorStatistics errorStatistics;

    @Test
    public void testGetImmerRestData_returnsJson() throws Exception {
        ImmerRest immerRest = new ImmerRest();
        immerRest.setTemperaute(42);
        immerRest.setThrottle(2);
        immerRest.setHeating(true);
        immerRest.setBoilerOn(true);
        when(immerData.getImmerRest()).thenReturn(immerRest);

        mockMvc.perform(MockMvcRequestBuilders.get("/Immer/immerrestdata"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.temperaute").value(42))
                .andExpect(jsonPath("$.throttle").value(2))
                .andExpect(jsonPath("$.heating").value(true))
                .andExpect(jsonPath("$.boilerOn").value(true));
    }

    @Test
    public void testGetImmerRestData_defaultValues() throws Exception {
        when(immerData.getImmerRest()).thenReturn(new ImmerRest());

        mockMvc.perform(MockMvcRequestBuilders.get("/Immer/immerrestdata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temperaute").value(0))
                .andExpect(jsonPath("$.throttle").value(0))
                .andExpect(jsonPath("$.heating").value(false))
                .andExpect(jsonPath("$.boilerOn").value(false));
    }
}