package org.example.ggbot.persistence.repository;

import org.example.ggbot.persistence.entity.WebAuthSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebAuthSessionRepository extends JpaRepository<WebAuthSessionEntity, String> {
}
