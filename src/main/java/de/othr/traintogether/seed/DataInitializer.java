package de.othr.traintogether.seed;

import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository repo;
    public DataInitializer(UserRepository repo) { this.repo = repo; }

    @Override
    public void run(String... args) {
        if (repo.count() == 0) {
            repo.save(new User("alice", "alice@example.com"));
            repo.save(new User("bob", "bob@example.com"));
        }
    }
}
