package de.othr.traintogether.repository;

import de.othr.traintogether.model.Authority;
import de.othr.traintogether.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorityRepository extends JpaRepository<Authority, Long> {
    Authority findByUserAndAuthority(User user, String authority);
}
