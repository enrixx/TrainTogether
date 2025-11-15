package de.othr.traintogether.seed;

import de.othr.traintogether.model.Authority;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.AuthorityRepository;
import de.othr.traintogether.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository repo, AuthorityRepository authorityRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = repo;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User normalUser = userRepository.save(new User("user", passwordEncoder.encode("user")));
            User admin = userRepository.save(new User("admin", passwordEncoder.encode("admin")));

            if (authorityRepository.count() == 0) {
                authorityRepository.save(new Authority(normalUser,"ROLE_USER"));
                authorityRepository.save(new Authority(admin,"ROLE_ADMIN"));
            }
        }
    }
}
