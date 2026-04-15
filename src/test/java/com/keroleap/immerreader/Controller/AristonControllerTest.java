package com.keroleap.immerreader.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.keroleap.immerreader.AristonRest;
import com.keroleap.immerreader.Service.AristonAnalyzerService;
import com.keroleap.immerreader.SharedData.AristonData;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AristonController.class)
class AristonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AristonData aristonData;

    @MockitoBean
    private AristonAnalyzerService aristonAnalyzerService;

    @Test
    void getAristonRestData_returnsJson() throws Exception {
        AristonRest aristonRest = new AristonRest();
        aristonRest.setPercentage(75);
        when(aristonData.getAristonRest()).thenReturn(aristonRest);

        mockMvc.perform(MockMvcRequestBuilders.get("/Ariston/aristonrestdata"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.percentage").value(75));
    }

    @Test
    void getAristonRestData_defaultPercentage() throws Exception {
        when(aristonData.getAristonRest()).thenReturn(new AristonRest());

        mockMvc.perform(MockMvcRequestBuilders.get("/Ariston/aristonrestdata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percentage").value(0));
    }

    @Test
    void getAristonRestData_zeroPercentage() throws Exception {
        AristonRest aristonRest = new AristonRest();
        aristonRest.setPercentage(0);
        when(aristonData.getAristonRest()).thenReturn(aristonRest);

        mockMvc.perform(MockMvcRequestBuilders.get("/Ariston/aristonrestdata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percentage").value(0));
    }

    @Test
    void getAristonRestData_fullPercentage() throws Exception {
        AristonRest aristonRest = new AristonRest();
        aristonRest.setPercentage(100);
        when(aristonData.getAristonRest()).thenReturn(aristonRest);

        mockMvc.perform(MockMvcRequestBuilders.get("/Ariston/aristonrestdata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percentage").value(100));
    }
}
