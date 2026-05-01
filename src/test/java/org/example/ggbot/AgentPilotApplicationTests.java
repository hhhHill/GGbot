package org.example.ggbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.example.ggbot.common.ApiResponse;
import org.example.ggbot.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AgentPilotApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResourceLoader resourceLoader;

    @Test
    void shouldProvideMultitenantSchemaResource() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:schema.sql");
        String schema = resource.getContentAsString(StandardCharsets.UTF_8);

        assertThat(resource.exists()).isTrue();
        assertThat(schema)
                .contains("create table if not exists organizations")
                .contains("create table if not exists messages")
                .contains("nickname varchar(100)")
                .contains("avatar_url varchar(500)")
                .contains("extra_info text")
                .contains("role varchar(50) not null")
                .contains("message_type varchar(50) not null")
                .contains("embedding_id varchar(200)")
                .contains("constraint fk_conversations_subject foreign key (org_id, subject_id) references subjects(org_id, id)")
                .contains("constraint fk_messages_conversation foreign key (org_id, conversation_id) references conversations(org_id, id)")
                .contains("constraint fk_memory_subject foreign key (org_id, subject_id) references subjects(org_id, id)")
                .contains("constraint fk_group_members_org_subject foreign key (org_id, group_subject_id) references subjects(org_id, id)")
                .contains("create index idx_conversations_org_subject_last")
                .contains("create index idx_conversations_org_last")
                .contains("create index idx_messages_org_conversation_created")
                .contains("create index idx_memory_org_subject")
                .contains("create index idx_memory_org_type_scope")
                .doesNotContain("continue-on-error")
                .doesNotContain("display_name")
                .doesNotContain("provider_union_id")
                .doesNotContain("create index if not exists");

        Resource config = resourceLoader.getResource("classpath:application.yml");
        assertThat(config.getContentAsString(StandardCharsets.UTF_8))
                .contains("mode: embedded")
                .doesNotContain("continue-on-error");
    }

    @Test
    void shouldHideInternalMessagesForUnexpectedExceptions() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ApiResponse<Void>> response = handler.handleUnexpected(new RuntimeException("sensitive details"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Internal server error");
    }

    @Test
    void shouldReturnChallengeForFeishuWebhookVerification() throws Exception {
        mockMvc.perform(post("/feishu/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "challenge": "demo-challenge"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challenge").value("demo-challenge"));
    }

    @Test
    void shouldHandleSyncWebChatRequest() throws Exception {
        mockMvc.perform(post("/api/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "web-session-1",
                                  "userId": "user-1",
                                  "message": "你好，请总结一下这个需求"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.intentType").value("CHAT"));
    }

    @Test
    void shouldAcceptAsyncWebChatRequest() throws Exception {
        mockMvc.perform(post("/api/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "web-session-1",
                                  "userId": "user-1",
                                  "message": "帮我做一个项目方案文档和汇报PPT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accepted").value(true))
                .andExpect(jsonPath("$.data.jobId").exists())
                .andExpect(jsonPath("$.data.status").value("QUEUED"));
    }

    @Test
    void shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.app").value("GGbot"))
                .andExpect(jsonPath("$.llmConfigured").exists())
                .andExpect(jsonPath("$.llmReachable").exists())
                .andExpect(jsonPath("$.llmMessage").exists());
    }

    @Test
    void shouldServeWebHomePageWithoutMvpBranding() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("GGbot Workspace")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("web-console-session")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("MVP"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/health")));
    }
}
