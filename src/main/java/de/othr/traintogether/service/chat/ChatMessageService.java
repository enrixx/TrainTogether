package de.othr.traintogether.service.chat;

import de.othr.traintogether.dto.chat.ChatMessageDto;
import de.othr.traintogether.dto.chat.ChatMessagePageDto;
import de.othr.traintogether.dto.chat.ChatMessagesCursorDto;
import de.othr.traintogether.dto.chat.SendChatMessageDto;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class ChatMessageService {
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageMapper chatMessageMapper;
    private final CursorService cursorService;

    public ChatMessageService(ChatRoomMemberRepository chatRoomMemberRepository, UserRepository userRepository, ChatMessageRepository chatMessageRepository, ChatRoomRepository chatRoomRepository, ChatMessageMapper chatMessageMapper, CursorService cursorService) {
        this.chatRoomMemberRepository = chatRoomMemberRepository;
        this.userRepository = userRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.chatMessageMapper = chatMessageMapper;
        this.cursorService = cursorService;
    }

    @Transactional
    public void sendMessage(String userEmail, Long chatRoomID, SendChatMessageDto messageDto) {
        ChatRoomMember chatRoomMember = getMember(userEmail, chatRoomID);
        if (chatRoomMember.getRole() == ChatRole.READ_ONLY) {
            throw new IllegalArgumentException("User has read-only access to the chat room: " + chatRoomID);
        }

        //TODO: handle replyTo attachment later
        String trimmed = trimMessageContent(messageDto.getContent());
        ChatMessage message = new ChatMessage(chatRoomMember.getChatRoom(), chatRoomMember, trimmed);
        chatMessageRepository.save(message);

        chatRoomMember.setLastRead(message.getSentAt());
        chatRoomMemberRepository.save(chatRoomMember);
    }

    @Transactional
    public void editMessageContent(String userEmail, Long chatRoomID, Long messageId, String newContent) {
        ChatRoomMember chatRoomMember = getMember(userEmail, chatRoomID);
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));
        if (!message.getSender().getId().equals(chatRoomMember.getId())) {
            throw new IllegalArgumentException("User is not the sender of the message: " + messageId);
        }

        String trimmed = trimMessageContent(newContent);
        message.setContent(trimmed);

        chatMessageRepository.save(message);
    }

    @Transactional
    public void deleteMessage(String userEmail, Long chatRoomID, Long messageId) {
        ChatRoomMember chatRoomMember = getMember(userEmail, chatRoomID);
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));
        if (!message.getSender().getId().equals(chatRoomMember.getId())) {
            throw new IllegalArgumentException("User is not the sender of the message: " + messageId);
        }
        chatMessageRepository.delete(message);
    }

    @Transactional
    public ChatMessagePageDto getMessageInitialCursorPage(String userEmail, Long chatRoomId, Integer pageSize) {

        ChatRoomMember member = getMember(userEmail, chatRoomId);
        String topCursor = determineDefaultTopCurser(member, chatRoomId);

        // no messages at all
        if (topCursor == null) {
            return buildEmptyMessagePageDto(chatRoomId, member);
        }

        //Getting page from top cursor
        Instant topInstant = cursorService.decodeCursorInstant(topCursor);
        Long topId = cursorService.decodeCursorId(topCursor);
        Pageable pageable = PageRequest.of(0, Math.max(1, pageSize + 1), Sort.by(Sort.Order.desc("sentAt"), Sort.Order.desc("id")));
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdAfter(chatRoomId, topInstant, topId, pageable);

        if (messages.isEmpty()) {
            return buildEmptyMessagePageDto(chatRoomId, member);
        }

        boolean bottomHasMore = messages.size() > pageSize;
        if (bottomHasMore) {
            messages = messages.subList(0, pageSize);
        }

        //check if more messages on top, this way is only needed at initial load
        boolean topHasMore = false;
        Optional<ChatMessage> messageBeforeFirst = chatMessageRepository.findFirstByChatRoomIdAndIdBefore(chatRoomId, messages.getFirst().getId());
        if (messageBeforeFirst.isPresent()) {
            topHasMore = true;
        }

        ChatMessagePageDto pageDto = fillMessagePageDto(member, chatRoomId, messages, pageSize);

        //reassign topCurser even if not changed
        ChatMessage first = messages.getFirst();
        String newTopCursor = cursorService.buildCursor(first.getSentAt(), first.getId());

        ChatMessage last = messages.getLast();
        String newBottomCursor = cursorService.buildCursor(last.getSentAt(), last.getId());

        pageDto.setTopHasMore(topHasMore);
        pageDto.setTopCursor(newTopCursor);
        pageDto.setBottomHasMore(bottomHasMore);
        pageDto.setBottomCursor(newBottomCursor);
        pageDto.setCanSend(member.getRole() != ChatRole.READ_ONLY);
        return pageDto;
    }

    @Transactional
    public ChatMessagesCursorDto getMessageTopCursorPage(String userEmail, Long chatRoomId, String topCursor, Integer pageSize) {
        ChatRoomMember member = getMember(userEmail, chatRoomId);

        //Getting page up from top cursor
        Instant topInstant = cursorService.decodeCursorInstant(topCursor);
        Long topId = cursorService.decodeCursorId(topCursor);
        Pageable pageable = PageRequest.of(0, Math.max(1, pageSize + 1), Sort.by(Sort.Order.desc("sentAt"), Sort.Order.desc("id")));
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdBefore(chatRoomId, topInstant, topId, pageable);

        if (messages.isEmpty()) {
            return null;
        }

        boolean topHasMore = messages.size() > pageSize;
        if (topHasMore) {
            messages = messages.subList(0, pageSize);
        }

        messages.removeIf(m -> topId.equals(m.getId()));

        Collections.reverse(messages);

        ChatMessagesCursorDto messageDto = fillMessageCursorDto(member, chatRoomId, messages, pageSize);

        ChatMessage first = messages.getFirst();
        String newTopCursor = cursorService.buildCursor(first.getSentAt(), first.getId());

        messageDto.setHasMore(topHasMore);
        messageDto.setCursor(newTopCursor);
        return messageDto;
    }

    @Transactional
    public ChatMessagesCursorDto getMessageBottomCursorPage(String userEmail, Long chatRoomId, String bottomCursor, Integer pageSize) {
        ChatRoomMember member = getMember(userEmail, chatRoomId);

        //Getting page down from bottom cursor
        Instant bottomInstant = cursorService.decodeCursorInstant(bottomCursor);
        Long bottomId = cursorService.decodeCursorId(bottomCursor);
        Pageable pageable = PageRequest.of(0, Math.max(1, pageSize + 1), Sort.by(Sort.Order.desc("sentAt"), Sort.Order.desc("id")));
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdAfter(chatRoomId, bottomInstant, bottomId, pageable);

        if (messages.isEmpty()) {
            return null;
        }

        boolean bottomHasMore = messages.size() > pageSize;
        if (bottomHasMore) {
            messages = messages.subList(0, pageSize);
        }

        messages.removeIf(m -> bottomId.equals(m.getId()));

        ChatMessagesCursorDto messageDto = fillMessageCursorDto(member, chatRoomId, messages, pageSize);

        if (messageDto.getLastMessageId() != null) {
            messageDto.setLastMessageId(null);
            member.setLastRead(messages.getLast().getSentAt());
            chatRoomMemberRepository.save(member);
        }

        ChatMessage last = messages.getLast();
        String newBottomCursor = cursorService.buildCursor(last.getSentAt(), last.getId());

        messageDto.setHasMore(bottomHasMore);
        messageDto.setCursor(newBottomCursor);
        return messageDto;
    }

    private ChatRoomMember getMember(String userEmail, Long chatRoomID) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomID).orElseThrow(() ->
                new IllegalArgumentException("Chat room not found: " + chatRoomID));
        return chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomID, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of the chat room: " + chatRoomID));
    }

    private String trimMessageContent(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Content must not be empty");
        }
        if (trimmed.length() > 10000) {
            throw new IllegalArgumentException("Content too long");
        }
        return trimmed;
    }

    private String determineDefaultTopCurser(ChatRoomMember member, Long chatRoomId) {
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
        if (anchor == null) {
            return null;
        }

        return cursorService.buildCursor(anchor.getSentAt(), anchor.getId());
    }

    private ChatMessagePageDto buildEmptyMessagePageDto(Long chatRoomId, ChatRoomMember member) {
        ChatMessagePageDto emptyPage = new ChatMessagePageDto();
        emptyPage.setMessages(List.of());
        emptyPage.setLastMessageId(null);
        emptyPage.setTopHasMore(false);
        emptyPage.setTopCursor(null);
        emptyPage.setBottomHasMore(false);
        emptyPage.setBottomCursor(null);
        emptyPage.setRoomId(chatRoomId);
        emptyPage.setPageSize(0);
        emptyPage.setCanSend(member.getRole() != ChatRole.READ_ONLY);
        return emptyPage;
    }

    private ChatMessagePageDto fillMessagePageDto(ChatRoomMember member, Long chatRoomId, List<ChatMessage> messages, Integer pageSize) {

        List<ChatMessageDto> messagesDto = messages.stream()
                .map(m -> chatMessageMapper.toDto(m, member))
                .toList();

        Optional<ChatMessage> lastReadMessageOpt = chatMessageRepository.findFirstByChatRoomIdAndSentAtAfter(chatRoomId, member.getLastRead());
        if (lastReadMessageOpt.isPresent()) {
            member.setLastRead(messages.getLast().getSentAt());
            chatRoomMemberRepository.save(member);
        }
        Long lastReadMessageId = lastReadMessageOpt.map(ChatMessage::getId).orElse(null);
        ChatMessagePageDto pageDto = new ChatMessagePageDto();
        pageDto.setMessages(messagesDto);
        pageDto.setLastMessageId(lastReadMessageId);
        pageDto.setRoomType(member.getChatRoom().getType().name());
        pageDto.setRoomId(chatRoomId);
        pageDto.setPageSize(pageSize);
        return pageDto;
    }

    private ChatMessagesCursorDto fillMessageCursorDto(ChatRoomMember member, Long chatRoomId, List<ChatMessage> messages, Integer pageSize) {

        List<ChatMessageDto> messagesDto = messages.stream()
                .map(m -> chatMessageMapper.toDto(m, member))
                .toList();

        Optional<ChatMessage> lastReadMessageOpt = chatMessageRepository.findFirstByChatRoomIdAndSentAtAfter(chatRoomId, member.getLastRead());
        Long lastReadMessageId = lastReadMessageOpt.map(ChatMessage::getId).orElse(null);
        ChatMessagesCursorDto pageDto = new ChatMessagesCursorDto();
        pageDto.setMessages(messagesDto);
        pageDto.setLastMessageId(lastReadMessageId);
        pageDto.setRoomType(member.getChatRoom().getType().name());
        pageDto.setPageSize(pageSize);
        return pageDto;
    }
}
