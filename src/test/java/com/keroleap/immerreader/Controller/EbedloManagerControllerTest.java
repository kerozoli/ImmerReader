package com.keroleap.immerreader.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.keroleap.immerreader.SharedData.EbedloData;
import com.keroleap.immerreader.SharedData.EbedloManagerData;
import com.keroleap.immerreader.SharedData.ErrorStatistics;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EbedloManagerController.class)
class EbedloManagerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EbedloManagerData ebedloManagerData;

    @MockitoBean
    private EbedloData ebedloData;

    @MockitoBean
    private ErrorStatistics errorStatistics;

    @Test
    void getPoints_returnsCurrentValues() throws Exception {
        when(ebedloManagerData.getXs()).thenReturn(new int[] { 10, 20, 30, 40 });
        when(ebedloManagerData.getYs()).thenReturn(new int[] { 50, 60, 70, 80 });
        when(ebedloManagerData.getThreshold()).thenReturn(120);

        mockMvc.perform(MockMvcRequestBuilders.get("/EbedloManager"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.threshold").value(120));
    }

    @Test
    void setPoints_updatesAndReturnsValues() throws Exception {
        when(ebedloManagerData.getXs()).thenReturn(new int[] { 1, 2, 3, 4 });
        when(ebedloManagerData.getYs()).thenReturn(new int[] { 5, 6, 7, 8 });
        when(ebedloManagerData.getThreshold()).thenReturn(100);

        mockMvc.perform(MockMvcRequestBuilders.post("/EbedloManager/set")
                        .param("points", "1,5,2,6,3,7,4,8")
                        .param("threshold", "100"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(ebedloManagerData).setPoints(new int[] { 1, 2, 3, 4 }, new int[] { 5, 6, 7, 8 });
        verify(ebedloManagerData).setThreshold(100);
    }

    @Test
    void setPoints_wrongCoordinateCount_returnsBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/EbedloManager/set")
                        .param("points", "1,2,3,4")
                        .param("threshold", "100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void toggleEnabled_flipsAndReturnsValue() throws Exception {
        when(ebedloManagerData.isEnabled()).thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.post("/EbedloManager/toggle"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(ebedloManagerData).setEnabled(false);
    }

    @Test
    void enable_setsEnabledTrue() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/EbedloManager/enable"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(ebedloManagerData).setEnabled(true);
    }

    @Test
    void disable_setsEnabledFalse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/EbedloManager/disable"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(ebedloManagerData).setEnabled(false);
    }

    @Test
    void getPoints_returnsEnabledState() throws Exception {
        when(ebedloManagerData.getXs()).thenReturn(new int[] { 0, 0, 0, 0 });
        when(ebedloManagerData.getYs()).thenReturn(new int[] { 0, 0, 0, 0 });
        when(ebedloManagerData.getThreshold()).thenReturn(100);
        when(ebedloManagerData.isEnabled()).thenReturn(false);

        mockMvc.perform(MockMvcRequestBuilders.get("/EbedloManager"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }
}
