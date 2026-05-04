package org.example.ggbot.service.subject;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.common.IdGenerator;
import org.example.ggbot.enums.MemberStatus;
import org.example.ggbot.enums.ProviderType;
import org.example.ggbot.enums.SubjectType;
import org.example.ggbot.enums.UserOrgStatus;
import org.example.ggbot.exception.BadRequestException;
import org.example.ggbot.exception.NotFoundException;
import org.example.ggbot.persistence.entity.GroupMemberEntity;
import org.example.ggbot.persistence.entity.SubjectEntity;
import org.example.ggbot.persistence.repository.GroupMemberRepository;
import org.example.ggbot.persistence.repository.SubjectRepository;
import org.example.ggbot.persistence.repository.UserOrgRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserOrgRepository userOrgRepository;
    private final IdGenerator idGenerator;

    @Transactional
    public SubjectEntity getOrCreateUserSubject(Long userId, Long orgId) {
        if (!userOrgRepository.existsByUserIdAndOrgIdAndStatus(userId, orgId, UserOrgStatus.ACTIVE)) {
            throw new BadRequestException("User is not active in org");
        }
        return subjectRepository.findByOrgIdAndTypeAndProviderAndRefId(
                orgId, SubjectType.USER, ProviderType.SYSTEM, String.valueOf(userId)
        ).orElseGet(() -> subjectRepository.save(SubjectEntity.builder()
                .id(idGenerator.nextLongId())
                .orgId(orgId)
                .type(SubjectType.USER)
                .provider(ProviderType.SYSTEM)
                .refId(String.valueOf(userId))
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()));
    }

    @Transactional
    public SubjectEntity getOrCreateFeishuGroupSubject(String chatId, String chatName, Long orgId) {
        return subjectRepository.findByOrgIdAndTypeAndProviderAndRefId(
                orgId, SubjectType.GROUP, ProviderType.FEISHU, chatId
        ).orElseGet(() -> subjectRepository.save(SubjectEntity.builder()
                .id(idGenerator.nextLongId())
                .orgId(orgId)
                .type(SubjectType.GROUP)
                .provider(ProviderType.FEISHU)
                .refId(chatId)
                .name(chatName)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()));
    }

    @Transactional
    public void ensureGroupMember(Long orgId, Long groupSubjectId, Long userId) {
        SubjectEntity groupSubject = subjectRepository.findByIdAndOrgId(groupSubjectId, orgId)
                .orElseThrow(() -> new NotFoundException("Group subject not found"));
        if (groupSubject.getType() != SubjectType.GROUP) {
            throw new BadRequestException("Subject is not a group");
        }
        if (!userOrgRepository.existsByUserIdAndOrgIdAndStatus(userId, orgId, UserOrgStatus.ACTIVE)) {
            throw new BadRequestException("User is not active in org");
        }
        groupMemberRepository.findByOrgIdAndGroupSubjectIdAndUserId(orgId, groupSubjectId, userId)
                .ifPresentOrElse(existing -> {
                    if (existing.getStatus() != MemberStatus.ACTIVE) {
                        existing.setStatus(MemberStatus.ACTIVE);
                        existing.setLeftAt(null);
                        existing.setUpdatedAt(LocalDateTime.now());
                        groupMemberRepository.save(existing);
                    }
                }, () -> groupMemberRepository.save(GroupMemberEntity.builder()
                        .id(idGenerator.nextLongId())
                        .orgId(orgId)
                        .groupSubjectId(groupSubjectId)
                        .userId(userId)
                        .role("member")
                        .status(MemberStatus.ACTIVE)
                        .joinedAt(LocalDateTime.now())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));
    }

    @Transactional
    public void markGroupMemberLeft(Long orgId, Long groupSubjectId, Long userId) {
        groupMemberRepository.findByOrgIdAndGroupSubjectIdAndUserId(orgId, groupSubjectId, userId)
                .ifPresent(existing -> {
                    existing.setStatus(MemberStatus.LEFT);
                    existing.setLeftAt(LocalDateTime.now());
                    existing.setUpdatedAt(LocalDateTime.now());
                    groupMemberRepository.save(existing);
                });
    }
}
