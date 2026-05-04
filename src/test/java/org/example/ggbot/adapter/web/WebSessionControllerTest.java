package org.example.ggbot.adapter.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.example.ggbot.session.WebSession;
import org.example.ggbot.session.WebSessionMessage;
import org.example.ggbot.session.WebSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class WebSessionControllerTest {

    @Test
    void shouldCreateSessionForUser() throws Exception {
        WebSessionService sessionService = mock(WebSessionService.class);
        WebSession session = new WebSession("session-1", "user-1", "新对话", List.of(), 10L, 10L);
        when(sessionService.createSession("user-1")).thenReturn(session);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebSessionController(sessionService)).build();

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").value("session-1"))
                .andExpect(jsonPath("$.data.title").value("新对话"));
    }

    @Test
    void shouldListSessionsForUser() throws Exception {
        WebSessionService sessionService = mock(WebSessionService.class);
        WebSession first = new WebSession("session-1", "user-1", "标题一", List.of(), 10L, 20L);
        WebSession second = new WebSession("session-2", "user-1", "标题二", List.of(), 20L, 30L);
        when(sessionService.listSessions("user-1")).thenReturn(List.of(second, first));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebSessionController(sessionService)).build();

        mockMvc.perform(get("/api/sessions").param("userId", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].sessionId").value("session-2"))
                .andExpect(jsonPath("$.data[1].sessionId").value("session-1"));
    }

    @Test
    void shouldReturnSessionDetailWithMessages() throws Exception {
        WebSessionService sessionService = mock(WebSessionService.class);
        WebSessionMessage userMessage = new WebSessionMessage("user", "你好", 100L);
        WebSessionMessage assistantMessage = new WebSessionMessage("assistant", "你好，请问需要什么帮助？", 200L);
        WebSession session = new WebSession(
                "session-1",
                "user-1",
                "你好",
                List.of(userMessage, assistantMessage),
                100L,
                200L
        );
        when(sessionService.getSession("user-1", "session-1")).thenReturn(session);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebSessionController(sessionService)).build();

        mockMvc.perform(get("/api/sessions/session-1").param("userId", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").value("session-1"))
                .andExpect(jsonPath("$.data.messages[0].role").value("user"))
                .andExpect(jsonPath("$.data.messages[1].role").value("assistant"));
    }
}
