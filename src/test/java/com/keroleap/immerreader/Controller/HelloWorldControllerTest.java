package com.keroleap.immerreader.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImmerController.class)
public class HelloWorldControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /*@Test
    public void testGetImage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/image"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("image/jpeg"));
    }

    @Test
    public void testGetImmerData() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/immerdata"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("immerdata"))
                .andExpect(MockMvcResultMatchers.model().attributeExists("message"));
    }*/
}