package org.example.ggbot.persistence.repository;

import java.util.Optional;
import org.example.ggbot.persistence.entity.LocalAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalAccountRepository extends JpaRepository<LocalAccountEntity, Long> {

    Optional<LocalAccountEntity> findByUsername(String username);
}
