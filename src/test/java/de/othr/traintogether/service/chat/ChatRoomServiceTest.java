package de.othr.traintogether.service.chat;

import de.othr.traintogether.mapper.ChatRoomMapper;
import de.othr.traintogether.model.User;
import de.othr.traintogether.model.chat.*;
import de.othr.traintogether.repository.UserRepository;
import de.othr.traintogether.repository.chat.ChatMessageRepository;
import de.othr.traintogether.repository.chat.ChatRoomMemberRepository;
import de.othr.traintogether.repository.chat.ChatRoomRepository;
import de.othr.traintogether.dto.chat.ChatRoomListingDto;
import de.othr.traintogether.dto.chat.ChatSettingsDto;
import de.othr.traintogether.service.MinioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ChatRoomMapper chatRoomMapper;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock
    private ChatAuthService chatAuthService;
    @Mock
    private MinioService minioService;

    @InjectMocks
    private ChatRoomService chatRoomService;

    private final AtomicLong idCounter = new AtomicLong(1);

    @BeforeEach
    void setupSaveStubs() {
        lenient().when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> {
            ChatRoom r = invocation.getArgument(0);
            if (r.getId() == null) r.setId(idCounter.getAndIncrement());
            return r;
        });
        lenient().when(chatRoomMemberRepository.save(any(ChatRoomMember.class))).thenAnswer(invocation -> {
            ChatRoomMember m = invocation.getArgument(0);
            if (m.getId() == null) m.setId(idCounter.getAndIncrement());
            return m;
        });
    }

    private User makeUser(Long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        return u;
    }

    private ChatRoomMember makeMember(ChatRoom room, User user, ChatRole role, boolean removed) {
        ChatRoomMember m = new ChatRoomMember(room, user, role);
        m.setRemoved(removed);
        return m;
    }

    @Test
    void createDm_shouldReturnExistingDmId_whenDmExists() {
        User u1 = makeUser(1L, "a@example.com");
        User u2 = makeUser(2L, "b@example.com");

        when(chatAuthService.getUser("a@example.com")).thenReturn(u1);
        when(chatAuthService.getUser("b@example.com")).thenReturn(u2);

        ChatRoom existing = new ChatRoom(ChatRoomType.DM);
        existing.setId(10L);
        existing.addMember(new ChatRoomMember(existing, u1, ChatRole.MEMBER));
        existing.addMember(new ChatRoomMember(existing, u2, ChatRole.MEMBER));

        when(chatRoomRepository.findDmBetweenUsers(1L, 2L)).thenReturn(Optional.of(existing));

        Long id = chatRoomService.createDm("a@example.com", "b@example.com");
        assertEquals(10L, id);
        verify(chatRoomRepository, never()).save(argThat(r -> r.getType() == ChatRoomType.DM && r.getId() != null && r.getId() != 10L));
    }

    @Test
    void createDm_shouldReactivateRemovedMemberAndReturnId_whenMemberWasRemoved() {
        User u1 = makeUser(3L, "c@example.com");
        User u2 = makeUser(4L, "d@example.com");
        when(chatAuthService.getUser("c@example.com")).thenReturn(u1);
        when(chatAuthService.getUser("d@example.com")).thenReturn(u2);

        ChatRoom existing = new ChatRoom(ChatRoomType.DM);
        existing.setId(11L);
        ChatRoomMember m1 = new ChatRoomMember(existing, u1, ChatRole.MEMBER);
        ChatRoomMember m2 = new ChatRoomMember(existing, u2, ChatRole.MEMBER);
        m2.setRemoved(true);
        existing.addMember(m1);
        existing.addMember(m2);

        when(chatRoomRepository.findDmBetweenUsers(3L, 4L)).thenReturn(Optional.of(existing));

        Long id = chatRoomService.createDm("c@example.com", "d@example.com");
        assertEquals(11L, id);
        assertFalse(m2.isRemoved());
        verify(chatRoomMemberRepository, times(1)).save(m2);
    }

    @Test
    void createDm_shouldCreateNewDmAndReturnGeneratedId_whenNoExistingDm() {
        User u1 = makeUser(5L, "e@example.com");
        User u2 = makeUser(6L, "f@example.com");
        when(chatAuthService.getUser("e@example.com")).thenReturn(u1);
        when(chatAuthService.getUser("f@example.com")).thenReturn(u2);

        when(chatRoomRepository.findDmBetweenUsers(5L, 6L)).thenReturn(Optional.empty());

        Long id = chatRoomService.createDm("e@example.com", "f@example.com");
        assertNotNull(id);
        verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
    }

    @Test
    void addUserToGroup_shouldThrow_whenChatRoomIsDm() {
        ChatRoom dm = new ChatRoom(ChatRoomType.DM);
        dm.setId(20L);
        when(chatRoomRepository.findById(20L)).thenReturn(Optional.of(dm));

        assertThrows(IllegalArgumentException.class, () -> chatRoomService.addUserToGroup(20L, "x@example.com", ChatRole.MEMBER));
        verify(chatRoomRepository, never()).save(any());
    }

    @Test
    void addUserToGroup_shouldAddNewMemberAndSaveRoom() {
        ChatRoom group = new ChatRoom(ChatRoomType.GROUP);
        group.setId(30L);
        when(chatRoomRepository.findById(30L)).thenReturn(Optional.of(group));

        User newUser = makeUser(7L, "new@example.com");
        when(chatAuthService.getUser("new@example.com")).thenReturn(newUser);

        chatRoomService.addUserToGroup(30L, "new@example.com", ChatRole.MEMBER);

        verify(chatRoomRepository, times(1)).save(group);
        assertTrue(group.getMembers().stream().anyMatch(m -> m.getUser().getId().equals(7L)));
    }

    @Test
    void removeUserFromRoom_shouldMarkRemovedAndSave() {
        ChatRoom room = new ChatRoom(ChatRoomType.GROUP);
        room.setId(40L);
        User u = makeUser(8L, "r@example.com");
        ChatRoomMember member = new ChatRoomMember(room, u, ChatRole.MEMBER);
        member.setId(77L);

        when(chatAuthService.getMember("r@example.com", 40L)).thenReturn(member);

        chatRoomService.removeUserFromRoom(40L, "r@example.com");

        assertTrue(member.isRemoved());
        verify(chatRoomMemberRepository, times(1)).save(member);
    }

    @Test
    void deleteChatRoom_shouldDeleteMessagesAndRoom_whenIdNotNull() {
        chatRoomService.deleteChatRoom(50L);
        verify(chatMessageRepository, times(1)).deleteByChatRoomId(50L);
        verify(chatRoomRepository, times(1)).deleteById(50L);
    }

    @Test
    void isUserAdmin_shouldReturnTrueFalseAndHandleExceptions() {
        ChatRoom room = new ChatRoom(ChatRoomType.GROUP);
        room.setId(60L);
        User u = makeUser(9L, "a9@example.com");
        ChatRoomMember admin = new ChatRoomMember(room, u, ChatRole.ADMIN);
        ChatRoomMember member = new ChatRoomMember(room, u, ChatRole.MEMBER);

        when(chatAuthService.getActiveMember("a9@example.com", 60L)).thenReturn(admin);
        assertTrue(chatRoomService.isUserAdmin("a9@example.com", 60L));

        when(chatAuthService.getActiveMember("a9@example.com", 60L)).thenReturn(member);
        assertFalse(chatRoomService.isUserAdmin("a9@example.com", 60L));

        when(chatAuthService.getActiveMember("a9@example.com", 60L)).thenThrow(new IllegalArgumentException("nope"));
        assertFalse(chatRoomService.isUserAdmin("a9@example.com", 60L));
    }

    @Test
    void findDmByUser_shouldPass99PlusToMapperForLargeUnreadCounts() {
        User user = makeUser(10L, "z@example.com");
        when(userRepository.findByEmail("z@example.com")).thenReturn(Optional.of(user));

        ChatRoom room = new ChatRoom(ChatRoomType.DM);
        room.setId(70L);
        ChatRoomMember member = new ChatRoomMember(room, user, ChatRole.MEMBER);
        room.addMember(member);

        when(chatRoomRepository.findByUserAndType(eq(user), eq(ChatRoomType.DM))).thenReturn(Collections.singletonList(room));
        when(chatMessageRepository.countByChatRoomIdAndSentAtAfter(eq(70L), any())).thenReturn(150L);
        when(chatMessageRepository.findFirstByChatRoomIdOrderBySentAtDesc(70L)).thenReturn(Optional.empty());

        ChatRoomListingDto fakeDto = mock(ChatRoomListingDto.class);
        when(chatRoomMapper.toDtoDm(any(), anyString(), any(), any())).thenReturn(fakeDto);

        chatRoomService.findDmByUser("z@example.com");

        ArgumentCaptor<String> unreadCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatRoomMapper, times(1)).toDtoDm(eq(room), unreadCaptor.capture(), any(), any());
        assertEquals("99+", unreadCaptor.getValue());
    }

    @Test
    void createGroup_shouldCreateGroupWithAdminAndMembers() {
        User owner = makeUser(100L, "owner@example.com");
        User u1 = makeUser(101L, "u1@example.com");
        User u2 = makeUser(102L, "u2@example.com");

        when(chatAuthService.getUser("owner@example.com")).thenReturn(owner);
        when(chatAuthService.getUser("u1@example.com")).thenReturn(u1);
        when(chatAuthService.getUser("u2@example.com")).thenReturn(u2);

        Set<String> members = new HashSet<>(Arrays.asList("u1@example.com", "u2@example.com"));

        Long id = chatRoomService.createGroup("GroupName", "picUrl", members, "owner@example.com");

        assertNotNull(id);
        ArgumentCaptor<ChatRoom> roomCaptor = ArgumentCaptor.forClass(ChatRoom.class);
        verify(chatRoomRepository, times(1)).save(roomCaptor.capture());
        ChatRoom saved = roomCaptor.getValue();
        assertEquals(ChatRoomType.GROUP, saved.getType());
        assertTrue(saved.getMembers().stream().anyMatch(m -> m.getUser().getId().equals(100L) && m.getRole() == ChatRole.ADMIN));
        assertTrue(saved.getMembers().stream().anyMatch(m -> m.getUser().getId().equals(101L) && m.getRole() == ChatRole.MEMBER));
        assertTrue(saved.getMembers().stream().anyMatch(m -> m.getUser().getId().equals(102L) && m.getRole() == ChatRole.MEMBER));
    }

    @Test
    void addUserToGroup_shouldReactivateRemovedMemberAndUpdateRole() {
        ChatRoom group = new ChatRoom(ChatRoomType.GROUP);
        group.setId(200L);
        User user = makeUser(201L, "re@example.com");
        ChatRoomMember removedMember = new ChatRoomMember(group, user, ChatRole.MEMBER);
        removedMember.setRemoved(true);
        group.addMember(removedMember);

        when(chatRoomRepository.findById(200L)).thenReturn(Optional.of(group));
        when(chatAuthService.getUser("re@example.com")).thenReturn(user);

        chatRoomService.addUserToGroup(200L, "re@example.com", ChatRole.ADMIN);

        assertFalse(removedMember.isRemoved());
        assertEquals(ChatRole.ADMIN, removedMember.getRole());
        verify(chatRoomMemberRepository, times(1)).save(removedMember);
        long count = group.getMembers().stream().filter(m -> m.getUser().getId().equals(201L)).count();
        assertEquals(1L, count);
    }

    @Test
    void addUserToGroup_shouldNotAddIfAlreadyMemberNotRemoved() {
        ChatRoom group = new ChatRoom(ChatRoomType.GROUP);
        group.setId(210L);
        User user = makeUser(211L, "present@example.com");
        ChatRoomMember member = new ChatRoomMember(group, user, ChatRole.MEMBER);
        group.addMember(member);

        when(chatRoomRepository.findById(210L)).thenReturn(Optional.of(group));
        when(chatAuthService.getUser("present@example.com")).thenReturn(user);

        chatRoomService.addUserToGroup(210L, "present@example.com", ChatRole.MEMBER);

        // should not save a new member, and members size remains 1
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
        assertEquals(1, group.getMembers().size());
    }

    @Test
    void uploadGroupPicture_shouldReplaceExistingPictureAndSave() {
        ChatRoom room = new ChatRoom(ChatRoomType.GROUP);
        room.setId(300L);
        room.setPictureUrl("oldUrl");
        User adminUser = makeUser(301L, "admin@x.com");
        ChatRoomMember admin = new ChatRoomMember(room, adminUser, ChatRole.ADMIN);
        room.addMember(admin);

        when(chatAuthService.getAdminMember("admin@x.com", 300L)).thenReturn(admin);

        org.springframework.web.multipart.MultipartFile file = mock(org.springframework.web.multipart.MultipartFile.class);
        when(minioService.uploadGroupPicture(eq(file), eq(300L))).thenReturn("newUrl");

        String returned = chatRoomService.uploadGroupPicture("admin@x.com", 300L, file);

        verify(minioService, times(1)).deleteGroupPicture("oldUrl");
        verify(minioService, times(1)).uploadGroupPicture(file, 300L);
        ArgumentCaptor<ChatRoom> captor = ArgumentCaptor.forClass(ChatRoom.class);
        verify(chatRoomRepository, times(1)).save(captor.capture());
        ChatRoom saved = captor.getValue();
        assertEquals("newUrl", saved.getPictureUrl());
        assertEquals("newUrl", returned);
    }

    @Test
    void getChatSettings_shouldFilterRemovedAndSortMembers() {
        ChatRoom room = new ChatRoom(ChatRoomType.GROUP);
        room.setId(500L);
        User adminUser = makeUser(501L, "admin@x.com");
        ChatRoomMember admin = new ChatRoomMember(room, adminUser, ChatRole.ADMIN);
        admin.getUser().setEmail("admin@x.com");
        admin.getUser().setId(501L);

        User a = makeUser(502L, "a@x.com"); a.setEmail("a@x.com");
        ChatRoomMember mA = new ChatRoomMember(room, a, ChatRole.MEMBER);
        mA.getUser().setEmail("a@x.com");
        // displayName null to test null-handling
        // simulate another member with displayName "Bob"
        User b = makeUser(503L, "b@x.com");
        b.setEmail("b@x.com");
        ChatRoomMember mB = new ChatRoomMember(room, b, ChatRole.MEMBER);
        mB.setDisplayName("Bob");
        mB.getUser().setEmail("b@x.com");

        ChatRoomMember removed = new ChatRoomMember(room, makeUser(504L, "r@x.com"), ChatRole.MEMBER);
        removed.setRemoved(true);

        room.addMember(admin);
        room.addMember(mA);
        room.addMember(mB);
        room.addMember(removed);

        when(chatAuthService.getActiveMember("admin@x.com", 500L)).thenReturn(admin);

        ChatSettingsDto dto = chatRoomService.getChatSettings("admin@x.com", 500L);

        assertTrue(dto.isCanEdit());
        // removed member filtered
        assertFalse(dto.getChatMembers().stream().anyMatch(d -> d.getDisplayName() != null && d.getDisplayName().equals("r@x.com")));
        // Admin should be first in sorted list
        assertEquals("ADMIN", dto.getChatMembers().get(0).getRole());
    }

}
