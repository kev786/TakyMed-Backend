package com.takymed.auth_service.repositories;

import com.takymed.auth_service.entities.PasswordResetToken;
import com.takymed.auth_service.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByToken(String token);

    void deleteByUser(User user);
}
