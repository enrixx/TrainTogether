package de.othr.traintogether.service;

import de.othr.traintogether.model.Friendship;
import de.othr.traintogether.model.FriendshipStatus;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.FriendshipRepository;
import de.othr.traintogether.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;

    @InjectMocks
    private FriendshipService friendshipService;

    // --- areFriends Tests ---

    @Test
    void areFriends_shouldReturnTrue_whenStatusIsAccepted() {
        User u1 = new User(); u1.setId(1L);
        User u2 = new User(); u2.setId(2L);
        Friendship friendship = new Friendship(u1, u2, FriendshipStatus.ACCEPTED);

        when(friendshipRepository.findBetweenUsers(u1, u2)).thenReturn(Optional.of(friendship));

        assertTrue(friendshipService.areFriends(u1, u2));
    }

    @Test
    void areFriends_shouldReturnFalse_whenStatusIsNotAccepted() {
        User u1 = new User(); u1.setId(1L);
        User u2 = new User(); u2.setId(2L);
        Friendship friendship = new Friendship(u1, u2, FriendshipStatus.PENDING);

        when(friendshipRepository.findBetweenUsers(u1, u2)).thenReturn(Optional.of(friendship));

        assertFalse(friendshipService.areFriends(u1, u2));
    }

    @Test
    void areFriends_shouldReturnFalse_whenNoFriendshipExists() {
        User u1 = new User(); u1.setId(1L);
        User u2 = new User(); u2.setId(2L);

        when(friendshipRepository.findBetweenUsers(u1, u2)).thenReturn(Optional.empty());

        assertFalse(friendshipService.areFriends(u1, u2));
    }

    // --- createFriendship Tests ---

    @Test
    void createFriendship_shouldSaveNewAcceptedFriendship_whenNoneExists() {
        User u1 = new User(); u1.setId(1L);
        User u2 = new User(); u2.setId(2L);

        // Mock lock logic
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(u1));
        when(userRepository.findByIdWithLock(2L)).thenReturn(Optional.of(u2));

        when(friendshipRepository.findBetweenUsers(any(), any())).thenReturn(Optional.empty());

        friendshipService.createFriendship(u1, u2);

        verify(friendshipRepository).save(argThat(f ->
            f.getStatus() == FriendshipStatus.ACCEPTED &&
            (f.getRequester().equals(u1) || f.getRequester().equals(u2))
        ));
    }

    @Test
    void createFriendship_shouldFail_whenBlocked() {
        User u1 = new User(); u1.setId(1L);
        User u2 = new User(); u2.setId(2L);

        System.out.println(u1.getId() + " " + u2.getId());
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(u1));
        when(userRepository.findByIdWithLock(2L)).thenReturn(Optional.of(u2));

        Friendship blocked = new Friendship(u1, u2, FriendshipStatus.BLOCKED);
        when(friendshipRepository.findBetweenUsers(any(), any())).thenReturn(Optional.of(blocked));

        assertThrows(IllegalStateException.class, () -> friendshipService.createFriendship(u1, u2));
    }

    // --- sendRequest Tests ---

    @Test
    void sendRequest_shouldCreatePendingRequest_whenNoneExists() {
        User sender = new User(); sender.setId(10L);
        User receiver = new User(); receiver.setId(20L);
        String receiverEmail = "test@test.com";

        when(userService.getUserByEmail(receiverEmail)).thenReturn(receiver);
        when(userRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sender));
        when(userRepository.findByIdWithLock(20L)).thenReturn(Optional.of(receiver));

        when(friendshipRepository.findBetweenUsers(any(), any())).thenReturn(Optional.empty());

        friendshipService.sendRequest(sender, receiverEmail);

        verify(friendshipRepository).save(any(Friendship.class));
    }

    // --- acceptRequest Tests ---

    @Test
    void acceptRequest_shouldSetStatusToAccepted_whenValid() {
        User u1 = new User(); u1.setId(1L);
        User u2 = new User(); u2.setId(2L);
        Friendship friendship = new Friendship(u1, u2, FriendshipStatus.PENDING);
        friendship.setId(100L);

        when(friendshipRepository.findById(100L)).thenReturn(Optional.of(friendship));
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(u1));
        when(userRepository.findByIdWithLock(2L)).thenReturn(Optional.of(u2));

        friendshipService.acceptRequest(100L, u2);

        assertEquals(FriendshipStatus.ACCEPTED, friendship.getStatus());
        verify(friendshipRepository).save(friendship);
    }

    @Test
    void acceptRequest_shouldThrowException_whenNotAddressee() {
        User u1 = new User(); u1.setId(1L);
        User u2 = new User(); u2.setId(2L);
        Friendship friendship = new Friendship(u1, u2, FriendshipStatus.PENDING);
        friendship.setId(100L);

        when(friendshipRepository.findById(100L)).thenReturn(Optional.of(friendship));
        // Mock locking dependencies
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(u1));
        when(userRepository.findByIdWithLock(2L)).thenReturn(Optional.of(u2));

        assertThrows(IllegalStateException.class, () -> friendshipService.acceptRequest(100L, u1));
    }

    // --- declineRequest Tests ---

    @Test
    void declineRequest_shouldSetStatusToDeclined_whenValid() {
        User u1 = new User(); u1.setId(1L);
        User u2 = new User(); u2.setId(2L);
        Friendship friendship = new Friendship(u1, u2, FriendshipStatus.PENDING);
        friendship.setId(100L);

        when(friendshipRepository.findById(100L)).thenReturn(Optional.of(friendship));
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(u1));
        when(userRepository.findByIdWithLock(2L)).thenReturn(Optional.of(u2));

        friendshipService.declineRequest(100L, u2);

        assertEquals(FriendshipStatus.DECLINED, friendship.getStatus());
        verify(friendshipRepository).save(friendship);
    }

    // --- blockUser Tests ---

    @Test
    void blockUser_shouldSetStatusToBlocked_whenRelationshipExists() {
        User u1 = new User(); u1.setId(1L);
        User u2 = new User(); u2.setId(2L);
        Friendship existing = new Friendship(u1, u2, FriendshipStatus.PENDING);

        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(u1));
        when(userRepository.findByIdWithLock(2L)).thenReturn(Optional.of(u2));
        when(friendshipRepository.findBetweenUsers(u1, u2)).thenReturn(Optional.of(existing));

        friendshipService.blockUser(u1, 2L);

        assertEquals(FriendshipStatus.BLOCKED, existing.getStatus());
        assertEquals(u1, existing.getRequester());
        verify(friendshipRepository).save(existing);
    }

    // --- removeFriend Tests ---

    @Test
    void removeFriend_shouldDeleteFriendship_whenFriends() {
        User u1 = new User(); u1.setId(1L);
        User u2 = new User(); u2.setId(2L);
        Friendship friendship = new Friendship(u1, u2, FriendshipStatus.ACCEPTED);

        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(u1));
        when(userRepository.findByIdWithLock(2L)).thenReturn(Optional.of(u2));
        when(userRepository.findById(2L)).thenReturn(Optional.of(u2));
        when(friendshipRepository.findBetweenUsers(u1, u2)).thenReturn(Optional.of(friendship));

        friendshipService.removeFriend(u1, 2L);

        verify(friendshipRepository).delete(friendship);
    }

    // --- cancelRequest Tests ---

    @Test
    void cancelRequest_shouldDeleteFriendship_whenRequesterCancelsPending() {
        User requester = new User(); requester.setId(1L);
        User addressee = new User(); addressee.setId(2L);
        Friendship friendship = new Friendship(requester, addressee, FriendshipStatus.PENDING);
        friendship.setId(100L);

        when(friendshipRepository.findById(100L)).thenReturn(Optional.of(friendship));
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(requester));
        when(userRepository.findByIdWithLock(2L)).thenReturn(Optional.of(addressee));

        friendshipService.cancelRequest(100L, requester);

        verify(friendshipRepository).delete(friendship);
    }

    @Test
    void cancelRequest_shouldThrowException_whenFriendshipNotFound() {
        User user = new User(); user.setId(1L);
        when(friendshipRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> friendshipService.cancelRequest(100L, user));
    }

    @Test
    void cancelRequest_shouldThrowException_whenNotPending() {
        User requester = new User(); requester.setId(1L);
        User addressee = new User(); addressee.setId(2L);
        Friendship friendship = new Friendship(requester, addressee, FriendshipStatus.ACCEPTED);
        friendship.setId(100L);

        when(friendshipRepository.findById(100L)).thenReturn(Optional.of(friendship));
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(requester));
        when(userRepository.findByIdWithLock(2L)).thenReturn(Optional.of(addressee));

        assertThrows(IllegalStateException.class, () -> friendshipService.cancelRequest(100L, requester));
    }

    @Test
    void cancelRequest_shouldThrowException_whenUserIsNotRequester() {
        User requester = new User(); requester.setId(1L);
        User addressee = new User(); addressee.setId(2L);
        User someoneElse = new User(); someoneElse.setId(3L);
        Friendship friendship = new Friendship(requester, addressee, FriendshipStatus.PENDING);
        friendship.setId(100L);

        when(friendshipRepository.findById(100L)).thenReturn(Optional.of(friendship));
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(requester));
        when(userRepository.findByIdWithLock(2L)).thenReturn(Optional.of(addressee));

        assertThrows(IllegalStateException.class, () -> friendshipService.cancelRequest(100L, someoneElse));
    }

    // --- getSentPendingRequests Tests ---

    @Test
    void getSentPendingRequests_shouldCallRepository() {
        User user = new User(); user.setId(1L);
        friendshipService.getSentPendingRequests(user);
        verify(friendshipRepository).findByRequesterAndStatus(user, FriendshipStatus.PENDING);
    }
}
