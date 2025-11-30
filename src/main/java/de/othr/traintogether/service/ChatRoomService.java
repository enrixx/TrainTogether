package de.othr.traintogether.service;

import de.othr.traintogether.model.User;
import de.othr.traintogether.model.chat.ChatRole;
import de.othr.traintogether.model.chat.ChatRoom;
import de.othr.traintogether.model.chat.ChatRoomMember;
import de.othr.traintogether.model.chat.ChatRoomType;
import de.othr.traintogether.repository.chat.ChatRoomMemberRepository;
import de.othr.traintogether.repository.chat.ChatRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;

    public ChatRoomService(ChatRoomRepository chatRoomRepository, ChatRoomMemberRepository chatRoomMemberRepository) {
        this.chatRoomRepository = chatRoomRepository;
    }

    @Transactional
    public ChatRoom createDm(User user1, User user2) {

        ChatRoom room = new ChatRoom(ChatRoomType.DM);
        ChatRoomMember member1 = new ChatRoomMember(room, user1, ChatRole.MEMBER);
        ChatRoomMember member2 = new ChatRoomMember(room, user2, ChatRole.MEMBER);
        room.addMember(member1);
        room.addMember(member2);
        chatRoomRepository.save(room);
        return room;
    }

    @Transactional
    public ChatRoom createGroup(String name, String pictureUrl, HashSet<User> users, User owner) {

        ChatRoom room = new ChatRoom(ChatRoomType.GROUP, name, pictureUrl);
        ChatRoomMember admin = new ChatRoomMember(room, owner, ChatRole.ADMIN);
        room.addMember(admin);
        for (User user : users) {
            ChatRoomMember member = new ChatRoomMember(room, user, ChatRole.MEMBER);
            room.addMember(member);
        }
        chatRoomRepository.save(room);
        return room;
    }

    @Transactional
    public void addUserToGroup(ChatRoom chatRoom, User user, ChatRole role) {
        if (chatRoom.getType() != ChatRoomType.GROUP) {
            throw new IllegalArgumentException("Cannot add users to a DM chat room");
        }
        ChatRoomMember newMember = new ChatRoomMember(chatRoom, user, role);
        chatRoom.addMember(newMember);
        chatRoomRepository.save(chatRoom);
    }

    @Transactional
    public void removeUserFromRoom(ChatRoom chatRoom, User user) {
        chatRoom.getMembers().removeIf(member -> member.getUser().equals(user));
        chatRoomRepository.save(chatRoom);
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> findDmByUser(User user) {
      return chatRoomRepository.findByUserAndType(user, ChatRoomType.DM);
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> findGroupsByUser(User user) {
        return chatRoomRepository.findByUserAndType(user, ChatRoomType.GROUP);
    }
}

