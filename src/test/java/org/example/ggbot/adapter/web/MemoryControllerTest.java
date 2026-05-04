package org.example.ggbot.adapter.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.List;
import org.example.ggbot.enums.MemoryScope;
import org.example.ggbot.enums.MemoryType;
import org.example.ggbot.persistence.entity.MemoryEntity;
import org.example.ggbot.persistence.entity.OrganizationEntity;
import org.example.ggbot.persistence.entity.UserEntity;
import org.example.ggbot.service.access.AccessControlService;
import org.example.ggbot.service.dto.ResolvedWebUser;
import org.example.ggbot.service.identity.IdentityService;
import org.example.ggbot.service.memory.MemoryService;
import org.example.ggbot.service.organization.OrganizationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MemoryControllerTest {

    @Test
    void shouldListAccessibleMemoryInCurrentOrg() throws Exception {
        IdentityService identityService = mock(IdentityService.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        MemoryService memoryService = mock(MemoryService.class);
        AccessControlService accessControlService = mock(AccessControlService.class);
        when(identityService.getOrCreateUserByWebSession("web-user-key-1"))
                .thenReturn(new ResolvedWebUser(UserEntity.builder().id(3001L).build(), OrganizationEntity.builder().id(1001L).build()));
        when(memoryService.listAccessibleMemory(3001L, 1001L)).thenReturn(List.of(
                MemoryEntity.builder()
                        .id(9001L)
                        .orgId(1001L)
                        .subjectId(5001L)
                        .memoryType(MemoryType.LONG_TERM)
                        .scope(MemoryScope.GLOBAL)
                        .content("prefers concise answers")
                        .updatedAt(LocalDateTime.of(2026, 5, 1, 12, 0))
                        .build()
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new MemoryController(identityService, organizationService, accessControlService, memoryService)
        ).build();

        mockMvc.perform(get("/api/memory")
                        .cookie(new Cookie("web_user_key", "web-user-key-1"))
                        .param("orgId", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].memoryId").value(9001L))
                .andExpect(jsonPath("$.data[0].scope").value("GLOBAL"));
    }

    @Test
    void shouldListSubjectMemoryAfterSubjectAccessCheck() throws Exception {
        IdentityService identityService = mock(IdentityService.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        MemoryService memoryService = mock(MemoryService.class);
        AccessControlService accessControlService = mock(AccessControlService.class);
        when(identityService.getOrCreateUserByWebSession("web-user-key-1"))
                .thenReturn(new ResolvedWebUser(UserEntity.builder().id(3001L).build(), OrganizationEntity.builder().id(1001L).build()));
        when(memoryService.listSubjectMemory(1001L, 5001L)).thenReturn(List.of(
                MemoryEntity.builder()
                        .id(9002L)
                        .orgId(1001L)
                        .subjectId(5001L)
                        .content("shared group memory")
                        .updatedAt(LocalDateTime.of(2026, 5, 1, 12, 1))
                        .build()
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new MemoryController(identityService, organizationService, accessControlService, memoryService)
        ).build();

        mockMvc.perform(get("/api/subjects/5001/memory")
                        .cookie(new Cookie("web_user_key", "web-user-key-1"))
                        .param("orgId", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].memoryId").value(9002L))
                .andExpect(jsonPath("$.data[0].content").value("shared group memory"));
    }
}
