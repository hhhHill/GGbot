package org.example.ggbot.adapter.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.List;
import org.example.ggbot.persistence.entity.OrganizationEntity;
import org.example.ggbot.persistence.entity.UserEntity;
import org.example.ggbot.service.auth.WebUserContext;
import org.example.ggbot.service.auth.WebUserContextResolver;
import org.example.ggbot.service.dto.ResolvedWebUser;
import org.example.ggbot.service.organization.OrganizationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OrganizationControllerTest {

    @Test
    void shouldReturnAccessibleOrganizations() throws Exception {
        WebUserContextResolver resolver = mock(WebUserContextResolver.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        when(resolver.resolve(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(new WebUserContext("web-user-key-1",
                        new ResolvedWebUser(UserEntity.builder().id(3001L).build(), OrganizationEntity.builder().id(1001L).build()),
                        false, null));
        when(organizationService.listActiveOrganizations(3001L)).thenReturn(List.of(
                OrganizationEntity.builder().id(1001L).name("Personal Workspace").tenantKey("personal:3001").build()
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new OrganizationController(resolver, organizationService)).build();

        mockMvc.perform(get("/api/orgs").cookie(new Cookie("web_user_key", "web-user-key-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].orgId").value(1001L))
                .andExpect(jsonPath("$.data[0].tenantKey").value("personal:3001"));
    }

    @Test
    void shouldSwitchOrganizationWhenUserHasAccess() throws Exception {
        WebUserContextResolver resolver = mock(WebUserContextResolver.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        when(resolver.resolve(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(new WebUserContext("web-user-key-1",
                        new ResolvedWebUser(UserEntity.builder().id(3001L).build(), OrganizationEntity.builder().id(1001L).build()),
                        false, null));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new OrganizationController(resolver, organizationService)).build();

        mockMvc.perform(post("/api/orgs/switch")
                        .cookie(new Cookie("web_user_key", "web-user-key-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orgId": 2001
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("switched"));
    }
}
