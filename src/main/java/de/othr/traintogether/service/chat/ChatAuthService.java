package de.othr.traintogether.service.chat;

import de.othr.traintogether.model.User;
import de.othr.traintogether.model.chat.ChatMessage;
import de.othr.traintogether.model.chat.ChatRole;
import de.othr.traintogether.model.chat.ChatRoomMember;
import de.othr.traintogether.repository.UserRepository;
import de.othr.traintogether.repository.chat.ChatMessageRepository;
import de.othr.traintogether.repository.chat.ChatRoomMemberRepository;
import de.othr.traintogether.repository.chat.ChatRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(readOnly = true)
public class ChatAuthService {

    private final UserRepository userRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatAuthService(UserRepository userRepository, ChatRoomMemberRepository chatRoomMemberRepository, ChatRoomRepository chatRoomRepository, ChatMessageRepository chatMessageRepository) {
        this.userRepository = userRepository;
        this.chatRoomMemberRepository = chatRoomMemberRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    public ChatRoomMember getAdminMember(String userEmail, Long chatRoomId) {

        ChatRoomMember member = getActiveMember(userEmail, chatRoomId);
        if (!ChatRole.ADMIN.equals(member.getRole())) {
            throw new IllegalArgumentException("User is not admin of the chat room: " + chatRoomId);
        }
        return member;
    }


    public ChatRoomMember getActiveMember(String userEmail, Long chatRoomID) {

        ChatRoomMember member = getMember(userEmail, chatRoomID);
        if (member.isRemoved()) {
            throw new IllegalArgumentException("User is removed from the chat room: " + chatRoomID);
        }
        return member;
    }

    public ChatRoomMember getMember(String userEmail, Long chatRoomID) {
        User user = getUser(userEmail);
        chatRoomRepository.findById(chatRoomID).orElseThrow(() ->
                new IllegalArgumentException("Chat room not found: " + chatRoomID));
        return chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomID, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of the chat room: " + chatRoomID));
    }

    public User getUser(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));
    }

    public ChatMessage getOwnedMessage(String userEmail, Long chatRoomId, Long messageId) {
        ChatRoomMember chatRoomMember = getActiveMember(userEmail, chatRoomId);
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));
        if (!message.getSender().getId().equals(chatRoomMember.getId())) {
            throw new IllegalArgumentException("User is not the sender of the message: " + messageId);
        }
        return message;
    }
}
