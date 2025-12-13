package de.othr.traintogether.seed;

import de.othr.traintogether.dto.RegisterDto;
import de.othr.traintogether.dto.chat.SendChatMessageDto;
import de.othr.traintogether.repository.UserRepository;
import de.othr.traintogether.repository.chat.ChatMessageRepository;
import de.othr.traintogether.repository.chat.ChatRoomMemberRepository;
import de.othr.traintogether.repository.chat.ChatRoomRepository;
import de.othr.traintogether.service.chat.ChatMessageService;
import de.othr.traintogether.service.chat.ChatRoomService;
import de.othr.traintogether.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final UserService userService;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    public DataInitializer(UserRepository repo, UserService userService, ChatRoomRepository chatRoomRepository, ChatRoomService chatRoomService, ChatRoomMemberRepository chatRoomMemberRepository, ChatMessageRepository chatMessageRepository, ChatMessageService chatMessageService) {
        this.userRepository = repo;
        this.userService = userService;
        this.chatRoomRepository = chatRoomRepository;
        this.chatRoomService = chatRoomService;
        this.chatMessageService = chatMessageService;
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
        userService.registerUser(new RegisterDto("user@u", "user", "Normal User", "Deez", "Nuts", "USER"));
        userService.registerUser(new RegisterDto("admin@a", "admin", "Administrator", "Nick", "Gurs", "ADMIN"));
        userService.registerUser(new RegisterDto("owner@o", "owner", "Gym Owner", "Ben", "Dover", "GYM_OWNER"));
        userService.registerUser(new RegisterDto("worker@w", "worker", "Gym Worker", "Mike", "Coxlong", "GYM_WORKER"));
        userService.registerUser(new RegisterDto("Powner@o", "Powner", "Pending Gym Owner", "Mike", "Literus", "PENDING_GYM_OWNER"));
        userService.registerUser(new RegisterDto("Pworker@o", "Pworker", "Pending Gym Worker", "Hue G.", "Rection", "PENDING_GYM_WORKER"));
    }

    private void seedExampleChats() {
        if (chatRoomRepository.count() > 0) {
            return;
        }
        chatRoomService.createDm("user@u", "admin@a");
        chatRoomService.createDm("user@u", "owner@o");
        java.util.HashSet<String> members = new java.util.HashSet<>(Set.of("user@u", "owner@o", "worker@w"));
        chatRoomService.createGroup("Test Group", null, members, "admin@a");

        Long dm2 = chatRoomService.findDmByUser("user@u").getFirst().getId();
        Long group = chatRoomService.findGroupsByUser("user@u").getFirst().getId();

        chatMessageService.sendMessage("user@u", dm2, new SendChatMessageDto("Hi Admin, kurze Nachricht vom Seeder."));

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
                chatMessageService.sendMessage("user@u", group, new SendChatMessageDto("Message Nummer " + (i + 1)));
            }
        }
    }
}
