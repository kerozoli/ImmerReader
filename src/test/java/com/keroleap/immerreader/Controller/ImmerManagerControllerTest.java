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
        when(immerManagerData.getOffsetX()).thenReturn(5);
        when(immerManagerData.getOffsetY()).thenReturn(10);

        mockMvc.perform(MockMvcRequestBuilders.get("/ImmerManager"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.offsetX").value(5))
                .andExpect(jsonPath("$.offsetY").value(10));
    }

    @Test
    void getOffset_defaultZeroValues() throws Exception {
        when(immerManagerData.getOffsetX()).thenReturn(0);
        when(immerManagerData.getOffsetY()).thenReturn(0);

        mockMvc.perform(MockMvcRequestBuilders.get("/ImmerManager"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.offsetX").value(0))
                .andExpect(jsonPath("$.offsetY").value(0));
    }

    @Test
    void setOffset_updatesAndReturnsValues() throws Exception {
        when(immerManagerData.getOffsetX()).thenReturn(7);
        when(immerManagerData.getOffsetY()).thenReturn(3);

        mockMvc.perform(MockMvcRequestBuilders.post("/ImmerManager/set")
                        .param("x", "7")
                        .param("y", "3"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.offsetX").value(7))
                .andExpect(jsonPath("$.offsetY").value(3));

        verify(immerManagerData).setOffsetX(7);
        verify(immerManagerData).setOffsetY(3);
    }

    @Test
    void setOffset_negativeValues() throws Exception {
        when(immerManagerData.getOffsetX()).thenReturn(-5);
        when(immerManagerData.getOffsetY()).thenReturn(-10);

        mockMvc.perform(MockMvcRequestBuilders.post("/ImmerManager/set")
                        .param("x", "-5")
                        .param("y", "-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offsetX").value(-5))
                .andExpect(jsonPath("$.offsetY").value(-10));

        verify(immerManagerData).setOffsetX(-5);
        verify(immerManagerData).setOffsetY(-10);
    }

    @Test
    void setOffset_missingParamReturnsBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/ImmerManager/set")
                        .param("x", "5"))
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
        when(immerManagerData.getOffsetX()).thenReturn(0);
        when(immerManagerData.getOffsetY()).thenReturn(0);
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
