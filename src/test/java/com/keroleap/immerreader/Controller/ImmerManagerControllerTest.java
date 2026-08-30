package com.keroleap.immerreader.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.keroleap.immerreader.ImmerRest;
import com.keroleap.immerreader.SharedData.ImmerData;
import com.keroleap.immerreader.SharedData.ImmerManagerData;
import com.keroleap.immerreader.SharedData.ErrorStatistics;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ImmerManagerController.class)
class ImmerManagerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImmerManagerData immerManagerData;

    @MockitoBean
    private ImmerData immerData;

    @MockitoBean
    private ErrorStatistics errorStatistics;

    @Test
    void getOffset_returnsCurrentValues() throws Exception {
        when(immerManagerData.getXs()).thenReturn(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 });
        when(immerManagerData.getYs()).thenReturn(new int[] { 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40 });

        mockMvc.perform(MockMvcRequestBuilders.get("/ImmerManager"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.xs[0]").value(1))
                .andExpect(jsonPath("$.ys[0]").value(21));
    }

    @Test
    void getOffset_defaultValues() throws Exception {
        when(immerManagerData.getXs()).thenReturn(new int[ImmerManagerData.POINT_COUNT]);
        when(immerManagerData.getYs()).thenReturn(new int[ImmerManagerData.POINT_COUNT]);

        mockMvc.perform(MockMvcRequestBuilders.get("/ImmerManager"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.xs[0]").value(0))
                .andExpect(jsonPath("$.ys[0]").value(0));
    }

    @Test
    void setPoints_updatesAndReturnsValues() throws Exception {
        String points = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40";

        mockMvc.perform(MockMvcRequestBuilders.post("/ImmerManager/setPoints")
                        .param("points", points))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(immerManagerData).setPoints(
                new int[] { 1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31, 33, 35, 37, 39 },
                new int[] { 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 36, 38, 40 });
    }

    @Test
    void setPoints_wrongCoordinateCount_returnsBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/ImmerManager/setPoints")
                        .param("points", "1,2,3,4"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setReference_updatesAllReferenceFields() throws Exception {
        when(immerManagerData.getReferenceX()).thenReturn(200);
        when(immerManagerData.getReferenceY()).thenReturn(220);
        when(immerManagerData.getReferenceThreshold()).thenReturn(-7000000);
        when(immerManagerData.getReferenceHysteresis()).thenReturn(400000);

        mockMvc.perform(MockMvcRequestBuilders.post("/ImmerManager/setReference")
                        .param("x", "200")
                        .param("y", "220")
                        .param("threshold", "-7000000")
                        .param("hysteresis", "400000"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.referenceX").value(200))
                .andExpect(jsonPath("$.referenceY").value(220))
                .andExpect(jsonPath("$.referenceThreshold").value(-7000000))
                .andExpect(jsonPath("$.referenceHysteresis").value(400000));

        verify(immerManagerData).setReferenceX(200);
        verify(immerManagerData).setReferenceY(220);
        verify(immerManagerData).setReferenceThreshold(-7000000);
        verify(immerManagerData).setReferenceHysteresis(400000);
    }

    @Test
    void setReference_missingParamReturnsBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/ImmerManager/setReference")
                        .param("x", "200")
                        .param("y", "220")
                        .param("threshold", "-7000000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setThresholds_updatesDarkAndLightThresholds() throws Exception {
        when(immerManagerData.getDarkThreshold()).thenReturn(-3000000);
        when(immerManagerData.getLightThreshold()).thenReturn(-7500000);

        mockMvc.perform(MockMvcRequestBuilders.post("/ImmerManager/setThresholds")
                        .param("dark", "-3000000")
                        .param("light", "-7500000"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.darkThreshold").value(-3000000))
                .andExpect(jsonPath("$.lightThreshold").value(-7500000));

        verify(immerManagerData).setDarkThreshold(-3000000);
        verify(immerManagerData).setLightThreshold(-7500000);
    }

    @Test
    void setThresholds_missingParamReturnsBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/ImmerManager/setThresholds")
                        .param("dark", "-3000000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void toggleEnabled_flipsAndReturnsValue() throws Exception {
        when(immerManagerData.isEnabled()).thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.post("/ImmerManager/toggle"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(immerManagerData).setEnabled(false);
    }

    @Test
    void enable_setsEnabledTrue() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/ImmerManager/enable"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(immerManagerData).setEnabled(true);
    }

    @Test
    void disable_setsEnabledFalse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/ImmerManager/disable"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(immerManagerData).setEnabled(false);
    }

    @Test
    void getOffset_returnsEnabledState() throws Exception {
        when(immerManagerData.getXs()).thenReturn(new int[ImmerManagerData.POINT_COUNT]);
        when(immerManagerData.getYs()).thenReturn(new int[ImmerManagerData.POINT_COUNT]);
        when(immerManagerData.isEnabled()).thenReturn(false);

        mockMvc.perform(MockMvcRequestBuilders.get("/ImmerManager"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void getDetectedData_returnsImmerRestData() throws Exception {
        ImmerRest immerRest = new ImmerRest();
        immerRest.setTemperaute(42);
        immerRest.setThrottle(2);
        immerRest.setHeating(true);
        immerRest.setBoilerOn(true);
        when(immerData.getImmerRest()).thenReturn(immerRest);

        mockMvc.perform(MockMvcRequestBuilders.get("/ImmerManager/data"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.temperaute").value(42))
                .andExpect(jsonPath("$.throttle").value(2))
                .andExpect(jsonPath("$.heating").value(true))
                .andExpect(jsonPath("$.boilerOn").value(true));
    }

    @Test
    void getDetectedData_defaultValues() throws Exception {
        when(immerData.getImmerRest()).thenReturn(new ImmerRest());

        mockMvc.perform(MockMvcRequestBuilders.get("/ImmerManager/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temperaute").value(0))
                .andExpect(jsonPath("$.throttle").value(0))
                .andExpect(jsonPath("$.heating").value(false))
                .andExpect(jsonPath("$.boilerOn").value(false));
    }
}
