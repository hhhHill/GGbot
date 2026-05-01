package org.example.ggbot.service.access;

import lombok.RequiredArgsConstructor;
import org.example.ggbot.enums.MemberStatus;
import org.example.ggbot.enums.ProviderType;
import org.example.ggbot.enums.SubjectType;
import org.example.ggbot.enums.UserOrgStatus;
import org.example.ggbot.exception.ForbiddenException;
import org.example.ggbot.exception.NotFoundException;
import org.example.ggbot.persistence.entity.ConversationEntity;
import org.example.ggbot.persistence.entity.SubjectEntity;
import org.example.ggbot.persistence.repository.ConversationRepository;
import org.example.ggbot.persistence.repository.GroupMemberRepository;
import org.example.ggbot.persistence.repository.SubjectRepository;
import org.example.ggbot.persistence.repository.UserOrgRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final UserOrgRepository userOrgRepository;
    private final SubjectRepository subjectRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ConversationRepository conversationRepository;

    public boolean canAccessOrg(Long userId, Long orgId) {
        return userOrgRepository.existsByUserIdAndOrgIdAndStatus(userId, orgId, UserOrgStatus.ACTIVE);
    }

    public void checkCanAccessOrg(Long userId, Long orgId) {
        if (!canAccessOrg(userId, orgId)) {
            throw new ForbiddenException("User cannot access org");
        }
    }

    public boolean canAccessSubject(Long userId, Long orgId, Long subjectId) {
        if (!canAccessOrg(userId, orgId)) {
            return false;
        }
        SubjectEntity subject = subjectRepository.findByIdAndOrgId(subjectId, orgId)
                .orElseThrow(() -> new NotFoundException("Subject not found"));
        if (subject.getType() == SubjectType.USER) {
            return subject.getProvider() == ProviderType.SYSTEM
                    && String.valueOf(userId).equals(subject.getRefId());
        }
        return groupMemberRepository.existsByOrgIdAndGroupSubjectIdAndUserIdAndStatus(
                orgId, subjectId, userId, MemberStatus.ACTIVE);
    }

    public void checkCanAccessSubject(Long userId, Long orgId, Long subjectId) {
        if (!canAccessSubject(userId, orgId, subjectId)) {
            throw new ForbiddenException("User cannot access subject");
        }
    }

    public void checkCanAccessConversation(Long userId, Long orgId, Long conversationId) {
        ConversationEntity conversation = conversationRepository.findByIdAndOrgId(conversationId, orgId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
        checkCanAccessSubject(userId, orgId, conversation.getSubjectId());
    }
}
