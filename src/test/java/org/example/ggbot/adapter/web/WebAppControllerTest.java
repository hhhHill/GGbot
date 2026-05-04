package org.example.ggbot.adapter.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class WebAppControllerTest {

    @Test
    void shouldForwardRootAndChatRoutesToIndexHtml() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebAppController()).build();

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/chat/new"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/chat/session-1"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }
}
