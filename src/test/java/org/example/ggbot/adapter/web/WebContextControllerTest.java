package org.example.ggbot.adapter.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.Cookie;
import org.example.ggbot.persistence.entity.OrganizationEntity;
import org.example.ggbot.persistence.entity.UserEntity;
import org.example.ggbot.service.dto.ResolvedWebUser;
import org.example.ggbot.service.identity.IdentityService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class WebContextControllerTest {

    @Test
    void shouldResolveContextFromCookie() throws Exception {
        IdentityService identityService = mock(IdentityService.class);
        when(identityService.getOrCreateUserByWebSession("web-user-key-1"))
                .thenReturn(new ResolvedWebUser(
                        UserEntity.builder().id(3001L).build(),
                        OrganizationEntity.builder().id(1001L).build()
                ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebContextController(identityService)).build();

        mockMvc.perform(get("/api/web/context")
                        .cookie(new Cookie("web_user_key", "web-user-key-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(3001L))
                .andExpect(jsonPath("$.data.personalOrgId").value(1001L))
                .andExpect(cookie().value("web_user_key", "web-user-key-1"));
    }

    @Test
    void shouldResolveContextFromClientKeyAndSetCookie() throws Exception {
        IdentityService identityService = mock(IdentityService.class);
        when(identityService.getOrCreateUserByWebSession("client-key-1"))
                .thenReturn(new ResolvedWebUser(
                        UserEntity.builder().id(3002L).build(),
                        OrganizationEntity.builder().id(1002L).build()
                ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebContextController(identityService)).build();

        mockMvc.perform(get("/api/web/context").param("clientKey", "client-key-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(3002L))
                .andExpect(jsonPath("$.data.personalOrgId").value(1002L))
                .andExpect(cookie().value("web_user_key", "client-key-1"));
    }
}
