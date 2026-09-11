package com.keroleap.immerreader.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.keroleap.immerreader.Service.EbedloAnalyzerService;
import com.keroleap.immerreader.SharedData.EbedloData;
import com.keroleap.immerreader.SharedData.EbedloManagerData;
import com.keroleap.immerreader.SharedData.ErrorStatistics;
import com.keroleap.immerreader.SharedData.TrimMode;

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

    @MockitoBean
    private EbedloAnalyzerService ebedloAnalyzerService;

    @Test
    void getPoints_returnsCurrentValues() throws Exception {
        when(ebedloManagerData.getXs()).thenReturn(new int[] { 10, 20, 30, 40 });
        when(ebedloManagerData.getYs()).thenReturn(new int[] { 50, 60, 70, 80 });
        when(ebedloManagerData.getThreshold()).thenReturn(120);
        when(ebedloManagerData.getTrimPercentage()).thenReturn(0.15);
        when(ebedloManagerData.getTrimMode()).thenReturn(TrimMode.LOWER);

        mockMvc.perform(MockMvcRequestBuilders.get("/EbedloManager"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.threshold").value(120))
                .andExpect(jsonPath("$.trimPercentage").value(0.15))
                .andExpect(jsonPath("$.trimMode").value("LOWER"));
    }

    @Test
    void setPoints_updatesAndReturnsValues() throws Exception {
        when(ebedloManagerData.getXs()).thenReturn(new int[] { 1, 2, 3, 4 });
        when(ebedloManagerData.getYs()).thenReturn(new int[] { 5, 6, 7, 8 });
        when(ebedloManagerData.getThreshold()).thenReturn(100);
        when(ebedloManagerData.getTrimPercentage()).thenReturn(0.10);
        when(ebedloManagerData.getTrimMode()).thenReturn(TrimMode.UPPER);

        mockMvc.perform(MockMvcRequestBuilders.post("/EbedloManager/set")
                        .param("points", "1,5,2,6,3,7,4,8")
                        .param("threshold", "100")
                        .param("trimPercentage", "0.10")
                        .param("trimMode", "UPPER"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(ebedloManagerData).setPoints(new int[] { 1, 2, 3, 4 }, new int[] { 5, 6, 7, 8 });
        verify(ebedloManagerData).setThreshold(100);
        verify(ebedloManagerData).setTrimPercentage(0.10);
        verify(ebedloManagerData).setTrimMode(TrimMode.UPPER);
    }

    @Test
    void setPoints_omittedTrimPercentageAndMode_usesDefaultValues() throws Exception {
        when(ebedloManagerData.getXs()).thenReturn(new int[] { 1, 2, 3, 4 });
        when(ebedloManagerData.getYs()).thenReturn(new int[] { 5, 6, 7, 8 });

        mockMvc.perform(MockMvcRequestBuilders.post("/EbedloManager/set")
                        .param("points", "1,5,2,6,3,7,4,8")
                        .param("threshold", "100"))
                .andExpect(status().isOk());

        verify(ebedloManagerData).setTrimPercentage(0.10);
        verify(ebedloManagerData).setTrimMode(TrimMode.BOTH);
    }

    @Test
    void setPoints_unknownTrimMode_defaultsToBoth() throws Exception {
        when(ebedloManagerData.getXs()).thenReturn(new int[] { 1, 2, 3, 4 });
        when(ebedloManagerData.getYs()).thenReturn(new int[] { 5, 6, 7, 8 });

        mockMvc.perform(MockMvcRequestBuilders.post("/EbedloManager/set")
                        .param("points", "1,5,2,6,3,7,4,8")
                        .param("threshold", "100")
                        .param("trimMode", "INVALID"))
                .andExpect(status().isOk());

        verify(ebedloManagerData).setTrimMode(TrimMode.BOTH);
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
        when(ebedloManagerData.getTrimPercentage()).thenReturn(0.10);
        when(ebedloManagerData.getTrimMode()).thenReturn(TrimMode.BOTH);
        when(ebedloManagerData.isEnabled()).thenReturn(false);

        mockMvc.perform(MockMvcRequestBuilders.get("/EbedloManager"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }
}
