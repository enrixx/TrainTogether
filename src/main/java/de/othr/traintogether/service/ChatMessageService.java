package de.othr.traintogether.service;

import de.othr.traintogether.model.User;
import de.othr.traintogether.model.chat.ChatMessage;
import de.othr.traintogether.model.chat.ChatRole;
import de.othr.traintogether.model.chat.ChatRoom;
import de.othr.traintogether.model.chat.ChatRoomMember;
import de.othr.traintogether.repository.UserRepository;
import de.othr.traintogether.repository.chat.ChatMessageRepository;
import de.othr.traintogether.repository.chat.ChatRoomMemberRepository;
import de.othr.traintogether.repository.chat.ChatRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatMessageService {
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;

    public ChatMessageService(ChatRoomMemberRepository chatRoomMemberRepository, UserRepository userRepository, ChatMessageRepository chatMessageRepository, ChatRoomRepository chatRoomRepository) {
        this.chatRoomMemberRepository = chatRoomMemberRepository;
        this.userRepository = userRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatRoomRepository = chatRoomRepository;
    }

    @Transactional
    public void sendMessage(String userEmail, Long chatRoomID, String content) {
        ChatRoomMember chatRoomMember = GetMember(userEmail, chatRoomID);
        if (chatRoomMember.getRole() == ChatRole.READ_ONLY) {
            throw new IllegalArgumentException("User has read-only access to the chat room: " + chatRoomID);
        }

        String trimmed = TrimMessageContent(content);
        chatMessageRepository.save(new ChatMessage(chatRoomMember.getChatRoom(), chatRoomMember, trimmed));
    }

    @Transactional
    public void editMessageContent(String userEmail, Long chatRoomID, Long messageId, String newContent) {
        ChatRoomMember chatRoomMember = GetMember(userEmail, chatRoomID);
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));
        if (!message.getSender().getId().equals(chatRoomMember.getId())) {
            throw new IllegalArgumentException("User is not the sender of the message: " + messageId);
        }

        String trimmed = TrimMessageContent(newContent);
        message.setContent(trimmed);

        chatMessageRepository.save(message);
    }

    @Transactional
    public void DeleteMessage(String userEmail, Long chatRoomID, Long messageId) {
        ChatRoomMember chatRoomMember = GetMember(userEmail, chatRoomID);
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));
        if (!message.getSender().getId().equals(chatRoomMember.getId())) {
            throw new IllegalArgumentException("User is not the sender of the message: " + messageId);
        }
        chatMessageRepository.delete(message);
    }

    public void getMessagePage() {
        //TODO: Add pagination and DTO
    }
    public void getRecentMessages() {
        //TODO: add Chatroommember last read timestamp update and DTO
    }

    private ChatRoomMember GetMember(String userEmail, Long chatRoomID) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomID).orElseThrow(() ->
                new IllegalArgumentException("Chat room not found: " + chatRoomID));
        return chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomID, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of the chat room: " + chatRoomID));
    }

    private String TrimMessageContent(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Content must not be empty");
        }
        if (trimmed.length() > 10000) {
            throw new IllegalArgumentException("Content too long");
        }
        return trimmed;
    }
}
