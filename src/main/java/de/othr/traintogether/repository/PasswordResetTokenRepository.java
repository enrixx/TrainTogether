package de.othr.traintogether.repository;

import de.othr.traintogether.model.PasswordResetToken;
import de.othr.traintogether.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByExpiryDateBefore(LocalDateTime dateTime);
    void deleteByUser(User user);
}

