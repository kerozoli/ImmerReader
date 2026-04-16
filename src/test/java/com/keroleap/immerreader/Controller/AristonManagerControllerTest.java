package com.keroleap.immerreader.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.keroleap.immerreader.SharedData.AristonManagerData;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AristonManagerController.class)
class AristonManagerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AristonManagerData aristonManagerData;

    @Test
    void getOffset_returnsCurrentValues() throws Exception {
        when(aristonManagerData.getOffsetX()).thenReturn(5);
        when(aristonManagerData.getOffsetY()).thenReturn(10);

        mockMvc.perform(MockMvcRequestBuilders.get("/AristonManager"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.offsetX").value(5))
                .andExpect(jsonPath("$.offsetY").value(10));
    }

    @Test
    void getOffset_defaultZeroValues() throws Exception {
        when(aristonManagerData.getOffsetX()).thenReturn(0);
        when(aristonManagerData.getOffsetY()).thenReturn(0);

        mockMvc.perform(MockMvcRequestBuilders.get("/AristonManager"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offsetX").value(0))
                .andExpect(jsonPath("$.offsetY").value(0));
    }

    @Test
    void setOffset_updatesAndReturnsValues() throws Exception {
        when(aristonManagerData.getOffsetX()).thenReturn(7);
        when(aristonManagerData.getOffsetY()).thenReturn(3);

        mockMvc.perform(MockMvcRequestBuilders.post("/AristonManager/set")
                        .param("x", "7")
                        .param("y", "3"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.offsetX").value(7))
                .andExpect(jsonPath("$.offsetY").value(3));

        verify(aristonManagerData).setOffsetX(7);
        verify(aristonManagerData).setOffsetY(3);
    }

    @Test
    void setOffset_negativeValues() throws Exception {
        when(aristonManagerData.getOffsetX()).thenReturn(-5);
        when(aristonManagerData.getOffsetY()).thenReturn(-10);

        mockMvc.perform(MockMvcRequestBuilders.post("/AristonManager/set")
                        .param("x", "-5")
                        .param("y", "-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offsetX").value(-5))
                .andExpect(jsonPath("$.offsetY").value(-10));

        verify(aristonManagerData).setOffsetX(-5);
        verify(aristonManagerData).setOffsetY(-10);
    }

    @Test
    void setOffset_missingParamReturnsBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/AristonManager/set")
                        .param("x", "5"))
                .andExpect(status().isBadRequest());
    }
}
