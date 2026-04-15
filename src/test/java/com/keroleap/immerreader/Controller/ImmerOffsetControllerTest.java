package com.keroleap.immerreader.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.keroleap.immerreader.SharedData.ImmerOffsetData;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ImmerOffsetController.class)
class ImmerOffsetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImmerOffsetData immerOffsetData;

    @Test
    void getOffset_returnsCurrentValues() throws Exception {
        when(immerOffsetData.getOffsetX()).thenReturn(5);
        when(immerOffsetData.getOffsetY()).thenReturn(10);

        mockMvc.perform(MockMvcRequestBuilders.get("/ImmerOffset"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.offsetX").value(5))
                .andExpect(jsonPath("$.offsetY").value(10));
    }

    @Test
    void getOffset_defaultZeroValues() throws Exception {
        when(immerOffsetData.getOffsetX()).thenReturn(0);
        when(immerOffsetData.getOffsetY()).thenReturn(0);

        mockMvc.perform(MockMvcRequestBuilders.get("/ImmerOffset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offsetX").value(0))
                .andExpect(jsonPath("$.offsetY").value(0));
    }

    @Test
    void setOffset_updatesAndReturnsValues() throws Exception {
        when(immerOffsetData.getOffsetX()).thenReturn(7);
        when(immerOffsetData.getOffsetY()).thenReturn(3);

        mockMvc.perform(MockMvcRequestBuilders.post("/ImmerOffset/set")
                        .param("x", "7")
                        .param("y", "3"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.offsetX").value(7))
                .andExpect(jsonPath("$.offsetY").value(3));

        verify(immerOffsetData).setOffsetX(7);
        verify(immerOffsetData).setOffsetY(3);
    }

    @Test
    void setOffset_negativeValues() throws Exception {
        when(immerOffsetData.getOffsetX()).thenReturn(-5);
        when(immerOffsetData.getOffsetY()).thenReturn(-10);

        mockMvc.perform(MockMvcRequestBuilders.post("/ImmerOffset/set")
                        .param("x", "-5")
                        .param("y", "-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offsetX").value(-5))
                .andExpect(jsonPath("$.offsetY").value(-10));

        verify(immerOffsetData).setOffsetX(-5);
        verify(immerOffsetData).setOffsetY(-10);
    }

    @Test
    void setOffset_missingParamReturnsBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/ImmerOffset/set")
                        .param("x", "5"))
                .andExpect(status().isBadRequest());
    }
}
