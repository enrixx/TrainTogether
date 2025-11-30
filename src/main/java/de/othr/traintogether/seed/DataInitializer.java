package de.othr.traintogether.seed;
import de.othr.traintogether.dto.RegisterDto;
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
            userService.registerUser(new RegisterDto("user@u", "user", "Normal User", "Deez", "Nuts", "USER"));
            userService.registerUser(new RegisterDto("admin@a", "admin", "Administrator", "Nick", "Gurs", "ADMIN"));
            userService.registerUser(new RegisterDto("owner@o", "owner", "Gym Owner", "Ben", "Dover", "GYM_OWNER"));
            userService.registerUser(new RegisterDto("worker@w", "worker", "Gym Worker", "Mike", "Coxlong", "GYM_WORKER"));
            userService.registerUser(new RegisterDto("Powner@o", "Powner", "Pending Gym Owner", "Mike", "Literus", "PENDING_GYM_OWNER"));
            userService.registerUser(new RegisterDto("Pworker@o", "Pworker", "Pending Gym Worker", "Hue G.", "Rection", "PENDING_GYM_WORKER"));
        }
    }
}
