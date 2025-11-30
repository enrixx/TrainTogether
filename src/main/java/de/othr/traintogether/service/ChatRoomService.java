package de.othr.traintogether.service;

import de.othr.traintogether.dto.ChatRoomDto;
import de.othr.traintogether.mapper.ChatRoomMapper;
import de.othr.traintogether.model.User;
import de.othr.traintogether.model.chat.ChatRole;
import de.othr.traintogether.model.chat.ChatRoom;
import de.othr.traintogether.model.chat.ChatRoomMember;
import de.othr.traintogether.model.chat.ChatRoomType;
import de.othr.traintogether.repository.UserRepository;
import de.othr.traintogether.repository.chat.ChatRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatRoomMapper chatRoomMapper;

    public ChatRoomService(ChatRoomRepository chatRoomRepository, UserRepository userRepository, ChatRoomMapper chatRoomMapper) {
        this.chatRoomRepository = chatRoomRepository;
        this.userRepository = userRepository;
        this.chatRoomMapper = chatRoomMapper;
    }

    @Transactional
    public void createDm(String userEmail1, String userEmail2) {
        // resolve users (throw if not found)
        User user1 = userRepository.findByEmail(userEmail1)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail1));
        User user2 = userRepository.findByEmail(userEmail2)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail2));

        ChatRoom room = new ChatRoom(ChatRoomType.DM);
        ChatRoomMember member1 = new ChatRoomMember(room, user1, ChatRole.MEMBER);
        ChatRoomMember member2 = new ChatRoomMember(room, user2, ChatRole.MEMBER);
        room.addMember(member1);
        room.addMember(member2);
        chatRoomRepository.save(room);
    }

    @Transactional
    public void createGroup(String name, String pictureUrl, Set<String> userEmails, String ownerEmail) {

        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Owner not found: " + ownerEmail));
        ChatRoom room = new ChatRoom(ChatRoomType.DM);
        ChatRoomMember admin = new ChatRoomMember(room, owner, ChatRole.ADMIN);
        admin.setUser(owner);
        for (String userEmail : userEmails) {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));
            ChatRoomMember member = new ChatRoomMember(room, user, ChatRole.MEMBER);
            room.addMember(member);
        }
    }

    @Transactional
    public void addUserToGroup(Long chatRoomId, String userEmail) {
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

        ChatRoomMember newMember = new ChatRoomMember(chatRoom, user, ChatRole.MEMBER);
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
    public List<ChatRoomDto> findDmByUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));
        List<ChatRoom> rooms = chatRoomRepository.findByUserAndType(user, ChatRoomType.DM);
        return rooms.stream().map(chatRoomMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatRoomDto> findGroupsByUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));
        List<ChatRoom> rooms = chatRoomRepository.findByUserAndType(user, ChatRoomType.GROUP);
        return rooms.stream().map(chatRoomMapper::toDto).collect(Collectors.toList());
    }
}
