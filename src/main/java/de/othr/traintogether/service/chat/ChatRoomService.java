package de.othr.traintogether.service.chat;

import de.othr.traintogether.dto.ChatRoomListingDto;
import de.othr.traintogether.mapper.ChatRoomMapper;
import de.othr.traintogether.model.User;
import de.othr.traintogether.model.chat.*;
import de.othr.traintogether.repository.UserRepository;
import de.othr.traintogether.repository.chat.ChatMessageRepository;
import de.othr.traintogether.repository.chat.ChatRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatRoomMapper chatRoomMapper;
    private final ChatMessageRepository chatMessageRepository;

    public ChatRoomService(ChatRoomRepository chatRoomRepository, UserRepository userRepository, ChatRoomMapper chatRoomMapper, ChatMessageRepository chatMessageRepository) {
        this.chatRoomRepository = chatRoomRepository;
        this.userRepository = userRepository;
        this.chatRoomMapper = chatRoomMapper;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    public void createDm(String userEmail1, String userEmail2) {
        // resolve users (throw if not found)
        User user1 = userRepository.findByEmail(userEmail1)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail1));
        User user2 = userRepository.findByEmail(userEmail2)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail2));

        createChatRoom(user1, user2);
    }

    @Transactional
    public void createGroup(String name, String pictureUrl, Set<String> userEmails, String ownerEmail, ChatRole memberRole) {

        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Owner not found: " + ownerEmail));
        ChatRoom room = new ChatRoom(ChatRoomType.GROUP, name, pictureUrl);
        ChatRoomMember admin = new ChatRoomMember(room, owner, ChatRole.ADMIN);
        admin.setUser(owner);
        for (String userEmail : userEmails) {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));
            ChatRoomMember member = new ChatRoomMember(room, user, memberRole);
            room.addMember(member);
        }
        chatRoomRepository.save(room);
    }

    @Transactional
    public void addUserToGroup(Long chatRoomId, String userEmail, ChatRole chatRole) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("ChatRoom not found: " + chatRoomId));
        if (chatRoom.getType() != ChatRoomType.GROUP) {
            throw new IllegalArgumentException("Cannot add users to a DM chat room");
        }
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));

        boolean exists = chatRoom.getMembers().stream()
                .anyMatch(m -> m.getUser() != null && user.getId() != null && user.getId().equals(m.getUser().getId()));
        if (exists) return;

        ChatRoomMember newMember = new ChatRoomMember(chatRoom, user, chatRole);
        chatRoom.addMember(newMember);
        chatRoomRepository.save(chatRoom);
    }

    @Transactional
    public void removeUserFromRoom(Long chatRoomId, String userEmail) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("ChatRoom not found: " + chatRoomId));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));

        chatRoom.getMembers().removeIf(member -> member.getUser() != null && member.getUser().getId() != null
                && member.getUser().getId().equals(user.getId()));
        chatRoomRepository.save(chatRoom);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomListingDto> findDmByUser(String userEmail) {
        return findByUserAndType(userEmail, ChatRoomType.DM, Optional.empty(),
                (room, member, unreadCount, lastMessage) -> chatRoomMapper.toDtoDm(room, unreadCount, lastMessage, member));

    }

    @Transactional(readOnly = true)
    public List<ChatRoomListingDto> findGroupsByUser(String userEmail, Optional<Boolean> canWrite) {
        return findByUserAndType(userEmail, ChatRoomType.GROUP, canWrite,
                (room, member, unreadCount, lastMessage) -> chatRoomMapper.toDtoGroup(room, unreadCount, lastMessage));
    }

    @FunctionalInterface
    private interface RoomToDtoMapper {
        ChatRoomListingDto map(ChatRoom room, ChatRoomMember member, String unreadMessagesCount, Optional<ChatMessage> lastMessage);
    }

    private List<ChatRoomListingDto> findByUserAndType(String userEmail, ChatRoomType type, Optional<Boolean> canWrite, RoomToDtoMapper mapper) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));
        List<ChatRoom> rooms = chatRoomRepository.findByUserAndType(user, type);

        List<ChatRoomListingDto> result = new ArrayList<>();
        for (ChatRoom room : rooms) {
            ChatRoomMember member = room.getMembers().stream()
                    .filter(m -> m.getUser() != null && m.getUser().getId() != null
                            && m.getUser().getId().equals(user.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("ChatRoomMember not found for user: " + userEmail));

            long unread = chatMessageRepository.countByChatRoomIdAndSentAtAfter(room.getId(), member.getLastRead());
            String unreadMessagesCount = unread > 99 ? "99+" : Long.toString(unread);
            Optional<ChatMessage> lastMessage = chatMessageRepository.findFirstByChatRoomIdOrderBySentAtDesc(room.getId());

            if(canWrite.isEmpty()){
                result.add(mapper.map(room, member, unreadMessagesCount, lastMessage));
            }
            else if(canWrite.get() && member.getRole() != ChatRole.READ_ONLY){
                result.add(mapper.map(room, member, unreadMessagesCount, lastMessage));
            } else if (!canWrite.get() && member.getRole() == ChatRole.READ_ONLY) {
                result.add(mapper.map(room, member, unreadMessagesCount, lastMessage));
            }
        }
        return result;
    }

    @Transactional
    public Long getOrCreateDm(User user1, User user2) {
        // Lock users in consistent order to prevent deadlocks
        if (user1.getId() < user2.getId()) {
            userRepository.findByIdWithLock(user1.getId());
            userRepository.findByIdWithLock(user2.getId());
        } else {
            userRepository.findByIdWithLock(user2.getId());
            userRepository.findByIdWithLock(user1.getId());
        }

        // Check if DM exists
        List<ChatRoom> user1Dms = chatRoomRepository.findByUserAndType(user1, ChatRoomType.DM);

        for (ChatRoom room : user1Dms) {
            boolean isUser2Member = room.getMembers().stream()
                    .anyMatch(m -> m.getUser().getId().equals(user2.getId()));
            if (isUser2Member) {
                return room.getId();
            }
        }

        ChatRoom room = createChatRoom(user1, user2);

        return room.getId();
    }

    private ChatRoom createChatRoom(User user1, User user2) {
        ChatRoom room = new ChatRoom(ChatRoomType.DM);
        ChatRoomMember member1 = new ChatRoomMember(room, user1, ChatRole.MEMBER);
        ChatRoomMember member2 = new ChatRoomMember(room, user2, ChatRole.MEMBER);
        room.addMember(member1);
        room.addMember(member2);
        chatRoomRepository.save(room);
        return room;
    }
}
