package de.othr.traintogether.service.chat;

import de.othr.traintogether.dto.chat.ChatDetailsDto;
import de.othr.traintogether.dto.chat.ChatMemberDto;
import de.othr.traintogether.dto.chat.ChatRoomListingDto;
import de.othr.traintogether.dto.chat.ChatSettingsDto;
import de.othr.traintogether.mapper.ChatRoomMapper;
import de.othr.traintogether.model.User;
import de.othr.traintogether.model.chat.*;
import de.othr.traintogether.repository.UserRepository;
import de.othr.traintogether.repository.chat.ChatMessageRepository;
import de.othr.traintogether.repository.chat.ChatRoomMemberRepository;
import de.othr.traintogether.repository.chat.ChatRoomRepository;
import de.othr.traintogether.service.MinioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatRoomMapper chatRoomMapper;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatAuthService chatAuthService;
    private final MinioService minioService;

    public ChatRoomService(ChatRoomRepository chatRoomRepository, UserRepository userRepository, ChatRoomMapper chatRoomMapper, ChatMessageRepository chatMessageRepository, ChatRoomMemberRepository chatRoomMemberRepository, ChatAuthService chatAuthService, MinioService minioService) {
        this.chatRoomRepository = chatRoomRepository;
        this.userRepository = userRepository;
        this.chatRoomMapper = chatRoomMapper;
        this.chatMessageRepository = chatMessageRepository;
        this.chatRoomMemberRepository = chatRoomMemberRepository;
        this.chatAuthService = chatAuthService;
        this.minioService = minioService;
    }

    @Transactional
    public void createDm(String userEmail1, String userEmail2) {
        // resolve users (throw if not found)
        User user1 = chatAuthService.getUser(userEmail1);
        User user2 = chatAuthService.getUser(userEmail2);

        ChatRoom room = new ChatRoom(ChatRoomType.DM);
        ChatRoomMember member1 = new ChatRoomMember(room, user1, ChatRole.MEMBER);
        ChatRoomMember member2 = new ChatRoomMember(room, user2, ChatRole.MEMBER);
        room.addMember(member1);
        room.addMember(member2);
        chatRoomRepository.save(room);
    }

    @Transactional
    public void createGroup(String name, String pictureUrl, Set<String> userEmails, String ownerEmail) {

        User owner = chatAuthService.getUser(ownerEmail);
        ChatRoom room = new ChatRoom(ChatRoomType.GROUP, name, pictureUrl);
        ChatRoomMember admin = new ChatRoomMember(room, owner, ChatRole.ADMIN);
        room.addMember(admin);
        for (String userEmail : userEmails) {
            User user = chatAuthService.getUser(userEmail);
            ChatRoomMember member = new ChatRoomMember(room, user, ChatRole.MEMBER);
            room.addMember(member);
        }
        chatRoomRepository.save(room);
    }

    @Transactional
    public void createReadOnlyGroup(String name, String pictureUrl, Set<String> userEmails, String ownerEmail) {

        User owner = chatAuthService.getUser(ownerEmail);
        ChatRoom room = new ChatRoom(ChatRoomType.READ_ONLY, name, pictureUrl);
        ChatRoomMember admin = new ChatRoomMember(room, owner, ChatRole.ADMIN);
        room.addMember(admin);
        for (String userEmail : userEmails) {
            User user = chatAuthService.getUser(userEmail);
            ChatRoomMember member = new ChatRoomMember(room, user, ChatRole.READ_ONLY);
            room.addMember(member);
        }
        chatRoomRepository.save(room);
    }

    @Transactional
    public void addUserToGroup(Long chatRoomId, String userEmail, ChatRole chatRole) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("ChatRoom not found: " + chatRoomId));
        if (chatRoom.getType() == ChatRoomType.DM) {
            throw new IllegalArgumentException("Cannot add users to a DM chat room");
        }
        User user = chatAuthService.getUser(userEmail);

        boolean exists = chatRoom.getMembers().stream()
                .anyMatch(m -> user.getId() != null && user.getId().equals(m.getUser().getId()));
        if (exists) return;

        ChatRoomMember newMember = new ChatRoomMember(chatRoom, user, chatRole);
        chatRoom.addMember(newMember);
        chatRoomRepository.save(chatRoom);
    }

    @Transactional
    public void removeUserFromRoom(Long chatRoomId, String userEmail) {
        ChatRoomMember member = chatAuthService.getMember(userEmail, chatRoomId);
        member.setRemoved(true);
        chatRoomMemberRepository.save(member);
    }

    @Transactional
    public String uploadGroupPicture(String email, Long chatRoomId, MultipartFile file) {

        ChatRoomMember member = chatAuthService.getAdminMember(email, chatRoomId);
        ChatRoom room = member.getChatRoom();
        String presentPictureUrl = room.getPictureUrl();

        // Delete old profile picture if exists
        if (presentPictureUrl != null && !presentPictureUrl.isEmpty()) {
            minioService.deleteGroupPicture(presentPictureUrl);
        }

        // Upload new profile picture
        String pictureUrl = minioService.uploadGroupPicture(file, room.getId());
        room.setPictureUrl(pictureUrl);
        chatRoomRepository.save(room);

        return pictureUrl;
    }

    @Transactional
    public void deleteGroupPicture(String email, Long chatRoomId) {
        ChatRoomMember member = chatAuthService.getAdminMember(email, chatRoomId);
        ChatRoom room = member.getChatRoom();
        String pictureUrl = room.getPictureUrl();

        if (pictureUrl != null && !pictureUrl.isEmpty()) {
            minioService.deleteGroupPicture(pictureUrl);
            room.setPictureUrl(null);
            chatRoomRepository.save(room);
        }
    }

    @Transactional
    public void updateChatDetails(String email, Long chatRoomId, ChatDetailsDto chatDetailsDto) {
        ChatRoomMember member = chatAuthService.getAdminMember(email, chatRoomId);
        ChatRoom room = member.getChatRoom();
        room.setName(chatDetailsDto.getName());
        room.setDescription(chatDetailsDto.getDescription());
        chatRoomRepository.save(room);
    }

    @Transactional(readOnly = true)
    public boolean isUserAdmin(String email, Long chatRoomId) {
        try {
            ChatRoomMember member = chatAuthService.getAdminMember(email, chatRoomId);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public List<ChatRoomListingDto> findDmByUser(String userEmail) {
        return findByUserAndType(userEmail, ChatRoomType.DM,
                (room, member, unreadCount, lastMessage) -> chatRoomMapper.toDtoDm(room, unreadCount, lastMessage, member));

    }

    @Transactional(readOnly = true)
    public List<ChatRoomListingDto> findGroupsByUser(String userEmail) {
        return findByUserAndType(userEmail, ChatRoomType.GROUP,
                (room, member, unreadCount, lastMessage) -> chatRoomMapper.toDtoGroup(room, unreadCount, lastMessage));
    }

    @Transactional(readOnly = true)
    public List<ChatRoomListingDto> findRedOnlyGroupsByUser(String userEmail) {
        return findByUserAndType(userEmail, ChatRoomType.READ_ONLY,
                (room, member, unreadCount, lastMessage) -> chatRoomMapper.toDtoGroup(room, unreadCount, lastMessage));
    }

    @Transactional(readOnly = true)
    public ChatSettingsDto getChatSettings(String userEmail, Long chatRoomId) {
        ChatRoomMember member = chatAuthService.getActiveMember(userEmail, chatRoomId);
        ChatDetailsDto chatDetailsDto = ChatDetailsDto.fromEntity(member.getChatRoom());
        List<ChatMemberDto> chatMemberDtos = member.getChatRoom().getMembers().stream()
                .filter(m -> !m.isRemoved())
                .map(ChatMemberDto::fromEntity)
                .sorted(java.util.Comparator
                        .comparingInt((ChatMemberDto d) -> d.getRole().equals("ADMIN") ? 0 : 1)
                        .thenComparing(d -> d.getDisplayName() == null ? "" : d.getDisplayName(), String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
        return new ChatSettingsDto(
                member.getRole() == ChatRole.ADMIN,
                chatDetailsDto,
                chatMemberDtos);
    }

    @FunctionalInterface
    private interface RoomToDtoMapper {
        ChatRoomListingDto map(ChatRoom room, ChatRoomMember member, String unreadMessagesCount, Optional<ChatMessage> lastMessage);
    }

    private List<ChatRoomListingDto> findByUserAndType(String userEmail, ChatRoomType type, RoomToDtoMapper mapper) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));
        List<ChatRoom> rooms = chatRoomRepository.findByUserAndType(user, type);

        List<ChatRoomListingDto> result = new ArrayList<>();
        for (ChatRoom room : rooms) {
            ChatRoomMember member = room.getMembers().stream()
                    .filter(m -> m.getUser().getId() != null
                            && m.getUser().getId().equals(user.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("ChatRoomMember not found for user: " + userEmail));

            if (member.isRemoved()) {
                continue;
            }

            long unread = chatMessageRepository.countByChatRoomIdAndSentAtAfter(room.getId(), member.getLastRead());
            String unreadMessagesCount = unread > 99 ? "99+" : Long.toString(unread);
            Optional<ChatMessage> lastMessage = chatMessageRepository.findFirstByChatRoomIdOrderBySentAtDesc(room.getId());

            result.add(mapper.map(room, member, unreadMessagesCount, lastMessage));
        }
        return result;
    }
}
