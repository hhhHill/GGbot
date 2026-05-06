package org.example.ggbot.service.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordHashServiceTest {

    private final PasswordHashService passwordHashService = new PasswordHashService();

    @Test
    void shouldHashAndMatchPassword() {
        String hash = passwordHashService.hash("secret-123");

        assertThat(hash).isNotBlank();
        assertThat(passwordHashService.matches("secret-123", hash)).isTrue();
        assertThat(passwordHashService.matches("wrong-password", hash)).isFalse();
    }
}
