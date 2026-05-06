package org.example.ggbot.adapter.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.example.ggbot.persistence.entity.OrganizationEntity;
import org.example.ggbot.persistence.entity.UserEntity;
import org.example.ggbot.service.binding.AccountBindingService;
import org.example.ggbot.service.auth.WebUserContext;
import org.example.ggbot.service.auth.WebUserContextResolver;
import org.example.ggbot.service.dto.ResolvedWebUser;
import org.example.ggbot.service.organization.OrganizationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AccountBindingControllerTest {

    @Test
    void shouldCreateFeishuBindTokenForCurrentUserAndOrg() throws Exception {
        WebUserContextResolver resolver = mock(WebUserContextResolver.class);
        AccountBindingService accountBindingService = mock(AccountBindingService.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        when(resolver.resolve(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(new WebUserContext("web-user-key-1",
                        new ResolvedWebUser(UserEntity.builder().id(3001L).build(), OrganizationEntity.builder().id(1001L).build()),
                        true, "alice"));
        when(accountBindingService.createBindToken(3001L, 1001L)).thenReturn("bind-token-1");

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new AccountBindingController(resolver, accountBindingService, organizationService)
        ).build();

        mockMvc.perform(post("/api/bind/feishu/token")
                        .cookie(new Cookie("web_user_key", "web-user-key-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orgId": 1001
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("bind-token-1"));
    }
}
