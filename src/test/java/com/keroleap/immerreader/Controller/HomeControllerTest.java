package com.keroleap.immerreader.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.keroleap.immerreader.SharedData.AristonManagerData;
import com.keroleap.immerreader.SharedData.ImmerManagerData;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImmerManagerData immerManagerData;

    @MockitoBean
    private AristonManagerData aristonManagerData;

    @Test
    void home_returnsIndexWithBothEnabled() throws Exception {
        when(immerManagerData.isEnabled()).thenReturn(true);
        when(aristonManagerData.isEnabled()).thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("immerEnabled", true))
                .andExpect(model().attribute("aristonEnabled", true));
    }

    @Test
    void home_returnsIndexWithImmerDisabled() throws Exception {
        when(immerManagerData.isEnabled()).thenReturn(false);
        when(aristonManagerData.isEnabled()).thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("immerEnabled", false))
                .andExpect(model().attribute("aristonEnabled", true));
    }

    @Test
    void home_returnsIndexWithAristonDisabled() throws Exception {
        when(immerManagerData.isEnabled()).thenReturn(true);
        when(aristonManagerData.isEnabled()).thenReturn(false);

        mockMvc.perform(MockMvcRequestBuilders.get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("immerEnabled", true))
                .andExpect(model().attribute("aristonEnabled", false));
    }
}
