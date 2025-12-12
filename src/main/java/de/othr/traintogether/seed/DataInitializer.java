package de.othr.traintogether.seed;

import de.othr.traintogether.dto.GymOwnerRegisterDto;
import de.othr.traintogether.dto.RegisterDto;
import de.othr.traintogether.model.Role;
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
            // Regular users
            userService.registerUser(new RegisterDto("user@u", "user", "Normal User", "Deez", "Nuts"), Role.USER);
            userService.registerUser(new RegisterDto("admin@a", "admin", "Administrator", "Nick", "Gurs"), Role.ADMIN);
            userService.registerUser(new RegisterDto("owner@o", "owner", "Gym Owner", "Ben", "Dover"), Role.GYM_OWNER);
            userService.registerUser(new RegisterDto("worker@w", "worker", "Gym Worker", "Mike", "Coxlong"), Role.GYM_WORKER);

            // Pending gym owner
            GymOwnerRegisterDto pendingOwnerDto = new GymOwnerRegisterDto();
            pendingOwnerDto.setEmail("enricomc11@gmail.com");
            pendingOwnerDto.setPassword("Powner");
            pendingOwnerDto.setUsername("Pending Gym Owner");
            pendingOwnerDto.setFirstName("Mike");
            pendingOwnerDto.setLastName("Literus");
            pendingOwnerDto.setGymName("FitCenter");
            pendingOwnerDto.setGymAddress("123 Fitness Street");
            pendingOwnerDto.setCity("Munich");
            pendingOwnerDto.setPostalCode("80331");
            pendingOwnerDto.setPhoneNumber("+49 89 123456");
            pendingOwnerDto.setGymDescription("A modern fitness center with state-of-the-art equipment.");
            userService.registerGymOwner(pendingOwnerDto);

            userService.registerUser(new RegisterDto("Pworker@o", "Pworker", "Pending Gym Worker", "Hue G.", "Rection"), Role.PENDING_GYM_WORKER);
        }
    }
}
