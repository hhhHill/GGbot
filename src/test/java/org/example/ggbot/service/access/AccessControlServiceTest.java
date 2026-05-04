package org.example.ggbot.service.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.example.ggbot.enums.ConversationStatus;
import org.example.ggbot.enums.MemberStatus;
import org.example.ggbot.enums.ProviderType;
import org.example.ggbot.enums.SubjectType;
import org.example.ggbot.enums.UserOrgStatus;
import org.example.ggbot.exception.ForbiddenException;
import org.example.ggbot.persistence.entity.ConversationEntity;
import org.example.ggbot.persistence.entity.SubjectEntity;
import org.example.ggbot.persistence.repository.ConversationRepository;
import org.example.ggbot.persistence.repository.GroupMemberRepository;
import org.example.ggbot.persistence.repository.SubjectRepository;
import org.example.ggbot.persistence.repository.UserOrgRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {

    @Mock
    private UserOrgRepository userOrgRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @InjectMocks
    private AccessControlService accessControlService;

    @Test
    void shouldAllowAccessToOwnUserSubjectOnlyInsideCurrentOrg() {
        when(userOrgRepository.existsByUserIdAndOrgIdAndStatus(3001L, 1001L, UserOrgStatus.ACTIVE)).thenReturn(true);
        when(subjectRepository.findByIdAndOrgId(5001L, 1001L)).thenReturn(Optional.of(
                SubjectEntity.builder()
                        .id(5001L)
                        .orgId(1001L)
                        .type(SubjectType.USER)
                        .provider(ProviderType.SYSTEM)
                        .refId("3001")
                        .status("ACTIVE")
                        .build()));

        assertThat(accessControlService.canAccessSubject(3001L, 1001L, 5001L)).isTrue();
    }

    @Test
    void shouldDenyAccessToOtherUsersSubject() {
        when(userOrgRepository.existsByUserIdAndOrgIdAndStatus(3001L, 1001L, UserOrgStatus.ACTIVE)).thenReturn(true);
        when(subjectRepository.findByIdAndOrgId(5002L, 1001L)).thenReturn(Optional.of(
                SubjectEntity.builder()
                        .id(5002L)
                        .orgId(1001L)
                        .type(SubjectType.USER)
                        .provider(ProviderType.SYSTEM)
                        .refId("9999")
                        .status("ACTIVE")
                        .build()));

        assertThat(accessControlService.canAccessSubject(3001L, 1001L, 5002L)).isFalse();
    }

    @Test
    void shouldAllowGroupSubjectOnlyForActiveMember() {
        when(userOrgRepository.existsByUserIdAndOrgIdAndStatus(3001L, 1001L, UserOrgStatus.ACTIVE)).thenReturn(true);
        when(subjectRepository.findByIdAndOrgId(6001L, 1001L)).thenReturn(Optional.of(
                SubjectEntity.builder()
                        .id(6001L)
                        .orgId(1001L)
                        .type(SubjectType.GROUP)
                        .provider(ProviderType.FEISHU)
                        .refId("oc_group")
                        .status("ACTIVE")
                        .build()));
        when(groupMemberRepository.existsByOrgIdAndGroupSubjectIdAndUserIdAndStatus(
                1001L, 6001L, 3001L, MemberStatus.ACTIVE)).thenReturn(true);

        assertThat(accessControlService.canAccessSubject(3001L, 1001L, 6001L)).isTrue();
    }

    @Test
    void shouldResolveConversationAccessThroughSubject() {
        when(conversationRepository.findByIdAndOrgId(7001L, 1001L)).thenReturn(Optional.of(
                ConversationEntity.builder()
                        .id(7001L)
                        .orgId(1001L)
                        .subjectId(5001L)
                        .status(ConversationStatus.ACTIVE)
                        .build()));
        when(userOrgRepository.existsByUserIdAndOrgIdAndStatus(3001L, 1001L, UserOrgStatus.ACTIVE)).thenReturn(true);
        when(subjectRepository.findByIdAndOrgId(5001L, 1001L)).thenReturn(Optional.of(
                SubjectEntity.builder()
                        .id(5001L)
                        .orgId(1001L)
                        .type(SubjectType.USER)
                        .provider(ProviderType.SYSTEM)
                        .refId("3001")
                        .status("ACTIVE")
                        .build()));

        accessControlService.checkCanAccessConversation(3001L, 1001L, 7001L);
    }

    @Test
    void shouldThrowForbiddenWhenUserCannotAccessConversation() {
        when(conversationRepository.findByIdAndOrgId(7002L, 1001L)).thenReturn(Optional.of(
                ConversationEntity.builder()
                        .id(7002L)
                        .orgId(1001L)
                        .subjectId(5002L)
                        .status(ConversationStatus.ACTIVE)
                        .build()));
        when(userOrgRepository.existsByUserIdAndOrgIdAndStatus(3001L, 1001L, UserOrgStatus.ACTIVE)).thenReturn(true);
        when(subjectRepository.findByIdAndOrgId(5002L, 1001L)).thenReturn(Optional.of(
                SubjectEntity.builder()
                        .id(5002L)
                        .orgId(1001L)
                        .type(SubjectType.USER)
                        .provider(ProviderType.SYSTEM)
                        .refId("4001")
                        .status("ACTIVE")
                        .build()));

        assertThatThrownBy(() -> accessControlService.checkCanAccessConversation(3001L, 1001L, 7002L))
                .isInstanceOf(ForbiddenException.class);
    }
}
