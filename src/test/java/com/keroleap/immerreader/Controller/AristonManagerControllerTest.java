package com.keroleap.immerreader.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.keroleap.immerreader.SharedData.AristonData;
import com.keroleap.immerreader.SharedData.AristonManagerData;
import com.keroleap.immerreader.SharedData.ErrorStatistics;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AristonManagerController.class)
class AristonManagerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AristonManagerData aristonManagerData;

    @MockitoBean
    private AristonData aristonData;

    @MockitoBean
    private ErrorStatistics errorStatistics;

    @Test
    void getPoints_returnsCurrentValues() throws Exception {
        when(aristonManagerData.getStartX()).thenReturn(5);
        when(aristonManagerData.getStartY()).thenReturn(10);
        when(aristonManagerData.getEndX()).thenReturn(15);
        when(aristonManagerData.getEndY()).thenReturn(20);

        mockMvc.perform(MockMvcRequestBuilders.get("/AristonManager"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.startX").value(5))
                .andExpect(jsonPath("$.startY").value(10))
                .andExpect(jsonPath("$.endX").value(15))
                .andExpect(jsonPath("$.endY").value(20));
    }

    @Test
    void getPoints_defaultValues() throws Exception {
        when(aristonManagerData.getStartX()).thenReturn(0);
        when(aristonManagerData.getStartY()).thenReturn(0);
        when(aristonManagerData.getEndX()).thenReturn(0);
        when(aristonManagerData.getEndY()).thenReturn(0);

        mockMvc.perform(MockMvcRequestBuilders.get("/AristonManager"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startX").value(0))
                .andExpect(jsonPath("$.startY").value(0))
                .andExpect(jsonPath("$.endX").value(0))
                .andExpect(jsonPath("$.endY").value(0));
    }

    @Test
    void setPoints_updatesAndReturnsValues() throws Exception {
        when(aristonManagerData.getStartX()).thenReturn(7);
        when(aristonManagerData.getStartY()).thenReturn(3);
        when(aristonManagerData.getEndX()).thenReturn(14);
        when(aristonManagerData.getEndY()).thenReturn(6);

        mockMvc.perform(MockMvcRequestBuilders.post("/AristonManager/set")
                        .param("startX", "7")
                        .param("startY", "3")
                        .param("endX", "14")
                        .param("endY", "6"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.startX").value(7))
                .andExpect(jsonPath("$.startY").value(3))
                .andExpect(jsonPath("$.endX").value(14))
                .andExpect(jsonPath("$.endY").value(6));

        verify(aristonManagerData).setStartX(7);
        verify(aristonManagerData).setStartY(3);
        verify(aristonManagerData).setEndX(14);
        verify(aristonManagerData).setEndY(6);
    }

    @Test
    void setPoints_negativeValues() throws Exception {
        when(aristonManagerData.getStartX()).thenReturn(-5);
        when(aristonManagerData.getStartY()).thenReturn(-10);
        when(aristonManagerData.getEndX()).thenReturn(-15);
        when(aristonManagerData.getEndY()).thenReturn(-20);

        mockMvc.perform(MockMvcRequestBuilders.post("/AristonManager/set")
                        .param("startX", "-5")
                        .param("startY", "-10")
                        .param("endX", "-15")
                        .param("endY", "-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startX").value(-5))
                .andExpect(jsonPath("$.startY").value(-10))
                .andExpect(jsonPath("$.endX").value(-15))
                .andExpect(jsonPath("$.endY").value(-20));

        verify(aristonManagerData).setStartX(-5);
        verify(aristonManagerData).setStartY(-10);
        verify(aristonManagerData).setEndX(-15);
        verify(aristonManagerData).setEndY(-20);
    }

    @Test
    void setPoints_missingParamReturnsBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/AristonManager/set")
                        .param("startX", "5")
                        .param("startY", "10")
                        .param("endX", "15"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void toggleEnabled_flipsAndReturnsValue() throws Exception {
        when(aristonManagerData.isEnabled()).thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.post("/AristonManager/toggle"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(aristonManagerData).setEnabled(false);
    }

    @Test
    void enable_setsEnabledTrue() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/AristonManager/enable"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(aristonManagerData).setEnabled(true);
    }

    @Test
    void disable_setsEnabledFalse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/AristonManager/disable"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(aristonManagerData).setEnabled(false);
    }

    @Test
    void getPoints_returnsEnabledState() throws Exception {
        when(aristonManagerData.getStartX()).thenReturn(0);
        when(aristonManagerData.getStartY()).thenReturn(0);
        when(aristonManagerData.getEndX()).thenReturn(0);
        when(aristonManagerData.getEndY()).thenReturn(0);
        when(aristonManagerData.isEnabled()).thenReturn(false);

        mockMvc.perform(MockMvcRequestBuilders.get("/AristonManager"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }
}
