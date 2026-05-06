package org.example.ggbot.adapter.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.example.ggbot.persistence.entity.OrganizationEntity;
import org.example.ggbot.persistence.entity.UserEntity;
import org.example.ggbot.service.auth.LocalAuthService;
import org.example.ggbot.service.dto.ResolvedWebUser;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {

    @Test
    void shouldLoginAndSetAuthCookie() throws Exception {
        LocalAuthService localAuthService = mock(LocalAuthService.class);
        when(localAuthService.login("alice", "secret-123"))
                .thenReturn(new LocalAuthService.AuthenticatedSession(
                        "token-1",
                        "alice",
                        new ResolvedWebUser(UserEntity.builder().id(3001L).build(), OrganizationEntity.builder().id(1001L).build())
                ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(localAuthService)).build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "password": "secret-123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(cookie().value("web_auth_token", "token-1"))
                .andExpect(jsonPath("$.data.userId").value(3001L))
                .andExpect(jsonPath("$.data.personalOrgId").value(1001L))
                .andExpect(jsonPath("$.data.authenticated").value(true))
                .andExpect(jsonPath("$.data.loginName").value("alice"));
    }
}
