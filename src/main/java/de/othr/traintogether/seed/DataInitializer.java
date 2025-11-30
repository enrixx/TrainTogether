package de.othr.traintogether.seed;

import de.othr.traintogether.dto.RegisterDto;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.UserRepository;
import de.othr.traintogether.repository.chat.ChatRoomRepository;
import de.othr.traintogether.service.ChatRoomService;
import de.othr.traintogether.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final UserService userService;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomService chatRoomService;

    public DataInitializer(UserRepository repo, UserService userService, ChatRoomRepository chatRoomRepository, ChatRoomService chatRoomService) {
        this.userRepository = repo;
        this.userService = userService;
        this.chatRoomRepository = chatRoomRepository;
        this.chatRoomService = chatRoomService;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedExampleChats();
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            return;
        }
        userService.registerUser(new RegisterDto("user@u", "user", "Normal User", "USER"));
        userService.registerUser(new RegisterDto("admin@a", "admin", "Administrator", "ADMIN"));
        userService.registerUser(new RegisterDto("owner@o", "owner", "Gym Owner", "GYM_OWNER"));
        userService.registerUser(new RegisterDto("worker@w", "worker", "Gym Worker", "GYM_WORKER"));
        userService.registerUser(new RegisterDto("Powner@o", "Powner", "Pending Gym Owner", "PENDING_GYM_OWNER"));
        userService.registerUser(new RegisterDto("Pworker@o", "Pworker", "Pending Gym Worker", "PENDING_GYM_WORKER"));
    }

    private void seedExampleChats() {
        if (chatRoomRepository.count() > 0) {
            return;
        }
        chatRoomService.createDm("user@u", "admin@a");
        java.util.HashSet<String> members = new java.util.HashSet<>(Set.of("user@u", "owner@o", "worker@w"));
        chatRoomService.createGroup("Test Group", null, members, "admin@a");
    }
}

