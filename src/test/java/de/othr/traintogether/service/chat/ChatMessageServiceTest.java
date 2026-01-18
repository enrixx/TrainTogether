package de.othr.traintogether.service.chat;

import de.othr.traintogether.dto.chat.ChatMessagePageDto;
import de.othr.traintogether.dto.chat.SendChatMessageDto;
import de.othr.traintogether.mapper.ChatMessageMapper;
import de.othr.traintogether.model.User;
import de.othr.traintogether.model.chat.ChatMessage;
import de.othr.traintogether.model.chat.ChatRoom;
import de.othr.traintogether.model.chat.ChatRoomMember;
import de.othr.traintogether.model.chat.ChatRoomType;
import de.othr.traintogether.model.chat.ChatRole;
import de.othr.traintogether.repository.chat.ChatMessageRepository;
import de.othr.traintogether.repository.chat.ChatRoomMemberRepository;
import de.othr.traintogether.service.MinioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private ChatAuthService chatAuthService;
    @Mock
    private MinioService minioService;

    @InjectMocks
    private ChatMessageService chatMessageService;

    private final AtomicLong idCounter = new AtomicLong(1);

    @BeforeEach
    void setupSaveStubs() {
        lenient().when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage m = invocation.getArgument(0);
            if (m.getId() == null) {
                try {
                    java.lang.reflect.Field idField = ChatMessage.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(m, idCounter.getAndIncrement());
                } catch (Exception ignored) {
                }
            }
            return m;
        });
        lenient().when(chatRoomMemberRepository.save(any(ChatRoomMember.class))).thenAnswer(invocation -> {
            ChatRoomMember mm = invocation.getArgument(0);
            if (mm.getId() == null) {
                try {
                    java.lang.reflect.Field idField = ChatRoomMember.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(mm, idCounter.getAndIncrement());
                } catch (Exception ignored) {
                }
            }
            return mm;
        });
    }

    private User makeUser(Long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        return u;
    }

    private ChatRoom makeRoom(Long id, ChatRoomType type) {
        ChatRoom r = new ChatRoom(type);
        r.setId(id);
        return r;
    }

    private ChatRoomMember makeMember(ChatRoom room, User user, ChatRole role) {
        ChatRoomMember m = new ChatRoomMember(room, user, role);
        return m;
    }

    private ChatMessage makeMessage(Long id, ChatRoom room, ChatRoomMember member, String content) {
        ChatMessage msg = new ChatMessage(room, member, content);
        try {
            java.lang.reflect.Field idField = ChatMessage.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(msg, id);
        } catch (Exception ignored) {
        }
        return msg;
    }

    @Test
    void sendMessage_withAttachment_shouldUploadAndSave() {
        // Arrange
        User user = makeUser(1L, "u@x.com");
        ChatRoom room = makeRoom(10L, ChatRoomType.GROUP);
        ChatRoomMember member = makeMember(room, user, ChatRole.MEMBER);
        room.addMember(member);
        member.setLastRead(Instant.now());

        when(chatAuthService.getActiveMember("u@x.com", 10L)).thenReturn(member);

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("file.txt");
        when(file.getContentType()).thenReturn("text/plain");
        when(file.getSize()).thenReturn(123L);

        when(minioService.uploadChatAttachment(eq(file), eq(10L))).thenReturn("http://bucket/file.txt");

        SendChatMessageDto dto = new SendChatMessageDto();
        dto.setContent(" hello ");
        dto.setFile(file);

        // Act
        chatMessageService.sendMessage("u@x.com", 10L, dto);

        // Assert
        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, atLeastOnce()).save(captor.capture());
        ChatMessage saved = captor.getValue();
        assertNotNull(saved.getAttachment());
        assertEquals("http://bucket/file.txt", saved.getAttachment().getUrl());
        verify(minioService, times(1)).uploadChatAttachment(file, 10L);
        verify(chatRoomMemberRepository, times(1)).save(member);
    }

    @Test
    void sendMessage_shouldThrowWhenReadOnly() {
        User user = makeUser(2L, "r@x.com");
        ChatRoom room = makeRoom(11L, ChatRoomType.GROUP);
        ChatRoomMember member = makeMember(room, user, ChatRole.READ_ONLY);
        when(chatAuthService.getActiveMember("r@x.com", 11L)).thenReturn(member);

        SendChatMessageDto dto = new SendChatMessageDto();
        dto.setContent("hi");

        assertThrows(IllegalArgumentException.class, () -> chatMessageService.sendMessage("r@x.com", 11L, dto));

        verify(chatMessageRepository, never()).save(any());
        verify(minioService, never()).uploadChatAttachment(any(), anyLong());
    }

    @Test
    void sendMessage_dm_shouldReactivateRemovedOtherMember() {
        User s = makeUser(3L, "s@x.com");
        User o = makeUser(4L, "o@x.com");
        ChatRoom room = makeRoom(12L, ChatRoomType.DM);
        ChatRoomMember sender = makeMember(room, s, ChatRole.MEMBER);
        ChatRoomMember other = makeMember(room, o, ChatRole.MEMBER);
        room.addMember(sender);
        room.addMember(other);
        sender.setId(21L);
        other.setId(22L);
        other.setRemoved(true);

        when(chatAuthService.getActiveMember("s@x.com", 12L)).thenReturn(sender);

        SendChatMessageDto dto = new SendChatMessageDto();
        dto.setContent("hello");

        chatMessageService.sendMessage("s@x.com", 12L, dto);

        assertFalse(other.isRemoved());
        verify(chatRoomMemberRepository, times(1)).save(other);
    }

    @Test
    void editMessageContent_shouldTrimAndSave() {
        User u = makeUser(5L, "ed@x.com");
        ChatRoom room = makeRoom(13L, ChatRoomType.GROUP);
        ChatRoomMember member = makeMember(room, u, ChatRole.MEMBER);
        ChatMessage msg = makeMessage(33L, room, member, "old");

        when(chatAuthService.getOwnedMessage("ed@x.com", 13L, 33L)).thenReturn(msg);

        chatMessageService.editMessageContent("ed@x.com", 13L, 33L, "   trimmed   ");

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, times(1)).save(captor.capture());
        assertEquals("trimmed", captor.getValue().getContent());
    }

    @Test
    void deleteMessage_shouldDeleteOwnedMessage() {
        User u = makeUser(6L, "del@x.com");
        ChatRoom room = makeRoom(14L, ChatRoomType.GROUP);
        ChatRoomMember member = makeMember(room, u, ChatRole.MEMBER);
        ChatMessage msg = makeMessage(44L, room, member, "toDelete");

        when(chatAuthService.getOwnedMessage("del@x.com", 14L, 44L)).thenReturn(msg);

        chatMessageService.deleteMessage("del@x.com", 14L, 44L);

        verify(chatMessageRepository, times(1)).delete(msg);
    }

    @Test
    void getMessageInitialCursorPage_noMessages_shouldReturnEmptyDto() {
        User u = makeUser(7L, "p@x.com");
        ChatRoom room = makeRoom(15L, ChatRoomType.GROUP);
        ChatRoomMember member = makeMember(room, u, ChatRole.MEMBER);
        member.setLastRead(Instant.now());

        when(chatAuthService.getActiveMember("p@x.com", 15L)).thenReturn(member);
        when(chatMessageRepository.findFirstByChatRoomIdAndSentAtAfter(eq(15L), any())).thenReturn(Optional.empty());
        when(chatMessageRepository.findFirstByChatRoomIdOrderBySentAtDesc(15L)).thenReturn(Optional.empty());

        ChatMessagePageDto page = chatMessageService.getMessageInitialCursorPage("p@x.com", 15L, 20);

        assertNotNull(page);
        assertTrue(page.getMessages().isEmpty());
        assertEquals(15L, page.getRoomId());
        assertFalse(!page.isCanSend() && member.getRole() == ChatRole.READ_ONLY);
    }

    @Test
    void sendMessage_whenAttachmentUploadFails_shouldNotSaveAttachmentAndPropagate() {
        User user = makeUser(20L, "bad@x.com");
        ChatRoom room = makeRoom(21L, ChatRoomType.GROUP);
        ChatRoomMember member = makeMember(room, user, ChatRole.MEMBER);
        room.addMember(member);
        when(chatAuthService.getActiveMember("bad@x.com", 21L)).thenReturn(member);

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(minioService.uploadChatAttachment(eq(file), eq(21L))).thenThrow(new RuntimeException("S3 down"));

        SendChatMessageDto dto = new SendChatMessageDto();
        dto.setContent("hello");
        dto.setFile(file);

        assertThrows(RuntimeException.class, () -> chatMessageService.sendMessage("bad@x.com", 21L, dto));

        verify(chatMessageRepository, atLeastOnce()).save(any(ChatMessage.class));
        verify(minioService, times(1)).uploadChatAttachment(file, 21L);
    }

    @Test
    void sendMessage_whenContentTooLong_shouldThrowAndNotSave() {
        User u = makeUser(30L, "long@x.com");
        ChatRoom room = makeRoom(31L, ChatRoomType.GROUP);
        ChatRoomMember member = makeMember(room, u, ChatRole.MEMBER);
        when(chatAuthService.getActiveMember("long@x.com", 31L)).thenReturn(member);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10001; i++) sb.append('a');

        SendChatMessageDto dto = new SendChatMessageDto();
        dto.setContent(sb.toString());

        assertThrows(IllegalArgumentException.class, () -> chatMessageService.sendMessage("long@x.com", 31L, dto));

        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void editMessageContent_whenBlank_shouldThrowAndNotSave() {
        User u = makeUser(50L, "ed2@x.com");
        ChatRoom room = makeRoom(51L, ChatRoomType.GROUP);
        ChatRoomMember member = makeMember(room, u, ChatRole.MEMBER);
        ChatMessage msg = makeMessage(201L, room, member, "original");

        when(chatAuthService.getOwnedMessage("ed2@x.com", 51L, 201L)).thenReturn(msg);

        assertThrows(IllegalArgumentException.class, () -> chatMessageService.editMessageContent("ed2@x.com", 51L, 201L, "   "));

        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }
}
