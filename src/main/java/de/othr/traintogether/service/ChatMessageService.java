package de.othr.traintogether.service;

import de.othr.traintogether.dto.chat.ChatMessageDto;
import de.othr.traintogether.dto.chat.ChatMessagePageDto;
import de.othr.traintogether.mapper.ChatMessageMapper;
import de.othr.traintogether.model.User;
import de.othr.traintogether.model.chat.ChatMessage;
import de.othr.traintogether.model.chat.ChatRole;
import de.othr.traintogether.model.chat.ChatRoom;
import de.othr.traintogether.model.chat.ChatRoomMember;
import de.othr.traintogether.repository.UserRepository;
import de.othr.traintogether.repository.chat.ChatMessageRepository;
import de.othr.traintogether.repository.chat.ChatRoomMemberRepository;
import de.othr.traintogether.repository.chat.ChatRoomRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ChatMessageService {
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageMapper chatMessageMapper;

    public ChatMessageService(ChatRoomMemberRepository chatRoomMemberRepository, UserRepository userRepository, ChatMessageRepository chatMessageRepository, ChatRoomRepository chatRoomRepository, ChatMessageMapper chatMessageMapper) {
        this.chatRoomMemberRepository = chatRoomMemberRepository;
        this.userRepository = userRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.chatMessageMapper = chatMessageMapper;
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

    @Transactional(readOnly = true)
    public ChatMessagePageDto getMessageCursorPage(String userEmail, Long chatRoomId, String cursor, Integer pageSize) {

        ChatRoomMember member = GetMember(userEmail, chatRoomId);

        if (cursor == null || cursor.isBlank()) {
            // determine anchor message
            cursor = determineDefaultCurser(member, chatRoomId);
        }

        // no messages at all
        if (cursor == null) {
            ChatMessagePageDto emptyPage = new ChatMessagePageDto();
            emptyPage.setMessages(List.of());
            emptyPage.setLastMessageId(null);
            emptyPage.setHasMore(false);
            emptyPage.setNextCursor(null);
            emptyPage.setPageSize(pageSize);
            return emptyPage;
        }

        Instant before = decodeCursorInstant(cursor);
        Long beforeId = decodeCursorId(cursor);
        Pageable pageable = PageRequest.of(0, Math.max(1, pageSize + 1), Sort.by(Sort.Order.desc("sentAt"), Sort.Order.desc("id")));
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdAfter(chatRoomId, before, beforeId, pageable);

        boolean hasMore = messages.size() > pageSize;
        if (hasMore) {
            messages = messages.subList(0, pageSize);
        }

        String nextCursor = null;
        if (!messages.isEmpty()) {
            ChatMessage first = messages.getFirst();
            nextCursor = first.getSentAt().toEpochMilli() + ":" + first.getId();
        }

        List<ChatMessageDto> messagesDto = messages.stream()
                .map(m -> chatMessageMapper.toDto(m, member))
                .toList();

        Optional<ChatMessage> lastReadMessageOpt = chatMessageRepository.findFirstByChatRoomIdAndSentAtAfter(chatRoomId, member.getLastRead());
        Long lastReadMessageId = lastReadMessageOpt.map(ChatMessage::getId).orElse(null);

        ChatMessagePageDto pageDto = new ChatMessagePageDto();
        pageDto.setMessages(messagesDto);
        pageDto.setLastMessageId(lastReadMessageId);
        pageDto.setRoomType(member.getChatRoom().getType().name());
        pageDto.setHasMore(hasMore);
        pageDto.setNextCursor(nextCursor);
        pageDto.setPageSize(pageSize);
        //TODO: update last read
        return pageDto;
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

    private Instant decodeCursorInstant(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        String[] parts = cursor.split(":");
        try {
            return Instant.ofEpochMilli(Long.parseLong(parts[0]));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cursor");
        }
    }

    private Long decodeCursorId(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        String[] parts = cursor.split(":");
        if (parts.length < 2 || parts[1].isBlank()) return null;
        try {
            return Long.parseLong(parts[1]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cursor id");

        }
    }

    private String determineDefaultCurser(ChatRoomMember member, Long chatRoomId) {
        // determine anchor message
        Instant lastReadInstance = member.getLastRead();
        ChatMessage anchor = null;
        //Earliest unread
        Optional<ChatMessage> lastRead = chatMessageRepository.findFirstByChatRoomIdAndSentAtAfter(chatRoomId, lastReadInstance);
        if (lastRead.isPresent()) {
            anchor = lastRead.get();
        }

        //Latest read
        if (anchor == null) {
            Optional<ChatMessage> lastMessage = chatMessageRepository.findFirstByChatRoomIdOrderBySentAtDesc(chatRoomId);
            if (lastMessage.isPresent()) {
                anchor = lastMessage.get();
            }
        }

        //no messages at all
        if(anchor == null) {
            return null;
        }

        return anchor.getSentAt().toEpochMilli() + ":" + anchor.getId();
    }
}
