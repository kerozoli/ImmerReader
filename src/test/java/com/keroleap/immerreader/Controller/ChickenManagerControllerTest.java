package com.keroleap.immerreader.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.keroleap.immerreader.SharedData.ChickenData;
import com.keroleap.immerreader.SharedData.ChickenManagerData;
import com.keroleap.immerreader.SharedData.ErrorStatistics;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChickenManagerController.class)
class ChickenManagerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChickenManagerData chickenManagerData;

    @MockitoBean
    private ChickenData chickenData;

    @MockitoBean
    private ErrorStatistics errorStatistics;

    @Test
    void toggleEnabled_flipsAndReturnsValue() throws Exception {
        when(chickenManagerData.isEnabled()).thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.post("/ChickenManager/toggle"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(chickenManagerData).setEnabled(false);
    }

    @Test
    void enable_setsEnabledTrue() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/ChickenManager/enable"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.enabled").value(true));

        verify(chickenManagerData).setEnabled(true);
    }

    @Test
    void disable_setsEnabledFalse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/ChickenManager/disable"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.enabled").value(false));

        verify(chickenManagerData).setEnabled(false);
    }
}
