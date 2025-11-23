package de.othr.traintogether.seed;
import de.othr.traintogether.repository.UserRepository;
import de.othr.traintogether.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final UserService userService;

    public DataInitializer(UserRepository repo, UserService userService) {
        this.userRepository = repo;
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userService.registerUser("user@u", "user", "USER", "Normal User");
            userService.registerUser("admin@a", "admin", "ADMIN", "Administrator");
            userService.registerUser("owner@o", "owner", "GYM_OWNER", "Gym Owner");
            userService.registerUser("worker@w", "worker", "GYM_WORKER", "Gym Worker");
            userService.registerUser("Powner@o", "Powner", "PENDING_GYM_OWNER", "Pending Gym Owner");
            userService.registerUser("Pworker@o", "Pworker", "PENDING_GYM_WORKER", "Pending Gym Worker");
        }
    }
}
