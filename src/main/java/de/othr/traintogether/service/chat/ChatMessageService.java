package de.othr.traintogether.service.chat;

import de.othr.traintogether.dto.chat.ChatMessageDto;
import de.othr.traintogether.dto.chat.ChatMessagePageDto;
import de.othr.traintogether.dto.chat.ChatMessagesCursorDto;
import de.othr.traintogether.dto.chat.SendChatMessageDto;
import de.othr.traintogether.mapper.ChatMessageMapper;
import de.othr.traintogether.model.chat.*;
import de.othr.traintogether.repository.chat.ChatMessageRepository;
import de.othr.traintogether.repository.chat.ChatRoomMemberRepository;
import de.othr.traintogether.service.MinioService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ChatMessageService {
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageMapper chatMessageMapper;
    private final CursorService cursorService;
    private final ChatAuthService chatAuthService;
    private final MinioService minioService;

    public ChatMessageService(ChatRoomMemberRepository chatRoomMemberRepository, ChatMessageRepository chatMessageRepository, ChatMessageMapper chatMessageMapper, CursorService cursorService, ChatAuthService chatAuthService, MinioService minioService) {
        this.chatRoomMemberRepository = chatRoomMemberRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatMessageMapper = chatMessageMapper;
        this.cursorService = cursorService;
        this.chatAuthService = chatAuthService;
        this.minioService = minioService;
    }

    @Transactional
    public void sendMessage(String userEmail, Long chatRoomID, SendChatMessageDto messageDto) {
        ChatRoomMember chatRoomMember = chatAuthService.getActiveMember(userEmail, chatRoomID);
        if (chatRoomMember.getRole() == ChatRole.READ_ONLY) {
            throw new IllegalArgumentException("User has read-only access to the chat room: " + chatRoomID);
        }

        //TODO: handle replyTo attachment later
        String trimmed = trimMessageContent(messageDto.getContent());
        ChatMessage message = new ChatMessage(chatRoomMember.getChatRoom(), chatRoomMember, trimmed);
        chatMessageRepository.save(message);

        chatRoomMember.setLastRead(message.getSentAt());
        chatRoomMemberRepository.save(chatRoomMember);

        if(!messageDto.getFile().isEmpty()){
            var file = messageDto.getFile();
             String attachmentUrl = minioService.uploadChatAttachment(file, chatRoomID);
            ChatAttachment attachment = new ChatAttachment(attachmentUrl,
                    Objects.requireNonNull(file.getOriginalFilename()),
                    Objects.requireNonNull(file.getContentType()),
                    file.getSize());
            message.setAttachment(attachment);
            chatMessageRepository.save(message);
        }

        // If DM, and a new message is sent to a removed member, un-remove them
        ChatRoom room = chatRoomMember.getChatRoom();
        if(room.getType().equals(ChatRoomType.DM)){
            ChatRoomMember otherMember = room.getMembers().stream()
                    .filter(m -> !m.getId().equals(chatRoomMember.getId()))
                    .findFirst()
                    .orElse(null);
            if(otherMember != null){
                if(otherMember.isRemoved()){
                    otherMember.setRemoved(false);
                    chatRoomMemberRepository.save(otherMember);
                }
            }
        }
    }

    @Transactional
    public void editMessageContent(String userEmail, Long chatRoomId, Long messageId, String newContent) {
        ChatMessage message = chatAuthService.getOwnedMessage(userEmail, chatRoomId, messageId);

        String trimmed = trimMessageContent(newContent);
        message.setContent(trimmed);

        chatMessageRepository.save(message);
    }

    @Transactional
    public void deleteMessage(String userEmail, Long chatRoomId, Long messageId) {
        ChatMessage message = chatAuthService.getOwnedMessage(userEmail, chatRoomId, messageId);
        chatMessageRepository.delete(message);
    }

    @Transactional
    public ChatMessagePageDto getMessageInitialCursorPage(String userEmail, Long chatRoomId, Integer pageSize) {

        ChatRoomMember member = chatAuthService.getActiveMember(userEmail, chatRoomId);
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
        ChatRoomMember member = chatAuthService.getActiveMember(userEmail, chatRoomId);

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
        ChatRoomMember member = chatAuthService.getActiveMember(userEmail, chatRoomId);

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
