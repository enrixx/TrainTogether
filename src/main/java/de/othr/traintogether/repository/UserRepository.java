package de.othr.traintogether.repository;

import de.othr.traintogether.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = {"authorities"})
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);
}
