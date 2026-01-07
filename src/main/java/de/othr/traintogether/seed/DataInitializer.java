package de.othr.traintogether.seed;

import de.othr.traintogether.dto.GymOwnerRegisterDto;
import de.othr.traintogether.dto.RegisterDto;
import de.othr.traintogether.dto.chat.SendChatMessageDto;
import de.othr.traintogether.model.Gym;
import de.othr.traintogether.model.Role;
import de.othr.traintogether.model.User;
import de.othr.traintogether.model.chat.ChatRole;
import de.othr.traintogether.repository.UserRepository;
import de.othr.traintogether.repository.chat.ChatMessageRepository;
import de.othr.traintogether.repository.chat.ChatRoomMemberRepository;
import de.othr.traintogether.repository.chat.ChatRoomRepository;
import de.othr.traintogether.service.GymService;
import de.othr.traintogether.service.UserService;
import de.othr.traintogether.service.chat.ChatMessageService;
import de.othr.traintogether.service.chat.ChatRoomService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final UserService userService;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final GymService gymService;

    public DataInitializer(UserRepository repo, UserService userService, ChatRoomRepository chatRoomRepository, ChatRoomService chatRoomService, ChatRoomMemberRepository chatRoomMemberRepository, ChatMessageRepository chatMessageRepository, ChatMessageService chatMessageService, GymService gymService) {
        this.userRepository = repo;
        this.userService = userService;
        this.chatRoomRepository = chatRoomRepository;
        this.chatRoomService = chatRoomService;
        this.chatMessageService = chatMessageService;
        this.gymService = gymService;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedGyms();
        seedExampleChats();
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            return;
        }

        // Regular users
        userService.registerUser(new RegisterDto("user@u", "user", "Normal User", "Deez", "Nuts", "male", LocalDate.of(1990, 1, 1)), Role.USER);
        userService.registerUser(new RegisterDto("admin@a", "admin", "Administrator", "Nick", "Gurs", "male", LocalDate.of(1990, 1, 1)), Role.ADMIN);
        userService.registerUser(new RegisterDto("owner@o", "owner", "Gym Owner", "Ben", "Dover", "male", LocalDate.of(1990, 1, 1)), Role.GYM_OWNER);
        userService.registerUser(new RegisterDto("worker@w", "worker", "Gym Worker", "Mike", "Coxlong", "male", LocalDate.of(1990, 1, 1)), Role.GYM_WORKER);

        // Pending gym owner
        GymOwnerRegisterDto pendingOwnerDto = new GymOwnerRegisterDto();
        pendingOwnerDto.setEmail("traintogetherapplication@gmail.com");
        pendingOwnerDto.setPassword("Powner");
        pendingOwnerDto.setUsername("Pending Gym Owner");
        pendingOwnerDto.setFirstName("Mike");
        pendingOwnerDto.setLastName("Literus");
        pendingOwnerDto.setGender("male");
        pendingOwnerDto.setBirthday(LocalDate.of(1990, 1, 1));
        pendingOwnerDto.setGymName("FitCenter");
        pendingOwnerDto.setGymAddress("123 Fitness Street");
        pendingOwnerDto.setCity("Munich");
        pendingOwnerDto.setPostalCode("80331");
        pendingOwnerDto.setPhoneNumber("+49 89 123456");
        pendingOwnerDto.setGymDescription("A modern fitness center with state-of-the-art equipment.");
        userService.registerGymOwner(pendingOwnerDto);

        userService.registerUser(new RegisterDto("Pworker@o", "Pworker", "Pending Gym Worker", "Hue G.", "Rection", "male", LocalDate.of(1990, 1, 1)), Role.PENDING_GYM_WORKER);
    }

    private void seedGyms() {
        if (gymService.findAll().stream().anyMatch(g -> g.getName().equals("McFit Regensburg"))) {
            return;
        }

        Optional<User> ownerOpt = userRepository.findByEmail("owner@o");
        if (ownerOpt.isPresent()) {
            User owner = ownerOpt.get();
            Gym gym = new Gym();
            gym.setName("McFit Regensburg");
            gym.setAddress("Frankenstraße 2c");
            gym.setCity("Regensburg");
            gym.setPostalCode("93059");
            gym.setPhoneNumber("0941 7852345");
            gym.setDescription("McFit Regensburg - Trainieren auf 2000qm. 24h geöffnet.");
            
            // Coordinates will be set automatically by GymService
            gymService.create(gym, owner);
        }
    }

    private void seedExampleChats() {
        if (chatRoomRepository.count() > 0) {
            return;
        }
        chatRoomService.createDm("user@u", "admin@a");
        chatRoomService.createDm("user@u", "owner@o");
        java.util.HashSet<String> members = new java.util.HashSet<>(Set.of("user@u", "owner@o", "worker@w"));
        chatRoomService.createGroup("Test Group", null, members, "admin@a");
        chatRoomService.createReadOnlyGroup(" Read only Test Group", null, members, "admin@a");

        Long dm2 = chatRoomService.findDmByUser("user@u").getFirst().getId();
        Long group = chatRoomService.findGroupsByUser("user@u").getFirst().getId();

        chatMessageService.sendMessage("admin@a", dm2, new SendChatMessageDto("Hi User, kurze Nachricht vom Seeder."));

        chatMessageService.sendMessage("user@u", group, new SendChatMessageDto("Hallo zusammen!"));
        chatMessageService.sendMessage("owner@o", group, new SendChatMessageDto("Hi, freut mich dabei zu sein."));
        chatMessageService.sendMessage("worker@w", group, new SendChatMessageDto("Moin, was geht?"));
        chatMessageService.sendMessage("user@u", group, new SendChatMessageDto("Wollen wir morgen trainieren?"));
        chatMessageService.sendMessage("owner@o", group, new SendChatMessageDto("Ja, gerne."));
        chatMessageService.sendMessage("worker@w", group, new SendChatMessageDto("Passt bei mir."));

        for (long i = 0; i < 300; i++) {
            if (i % 2 == 0) {
                chatMessageService.sendMessage("owner@o", group, new SendChatMessageDto("Message Nummer " + (i + 1)));
            } else {
                chatMessageService.sendMessage("worker@w", group, new SendChatMessageDto("Message Nummer " + (i + 1)));
            }
        }
    }
}
