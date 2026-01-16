package de.othr.traintogether.service;

import de.othr.traintogether.model.Friendship;
import de.othr.traintogether.model.FriendshipStatus;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.FriendshipRepository;
import de.othr.traintogether.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public FriendshipService(FriendshipRepository friendshipRepository, UserRepository userRepository, UserService userService) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    private record LockedUserPair (User first, User second) {
    }

    private LockedUserPair lockUsersInOrder(Long userId1, Long userId2) {
        User user1, user2;

        if (userId1 < userId2) {
            user1 = userRepository.findByIdWithLock(userId1)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId1));
            user2 = userRepository.findByIdWithLock(userId2)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId2));
        } else {
            user2 = userRepository.findByIdWithLock(userId2)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId2));
            user1 = userRepository.findByIdWithLock(userId1)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId1));
        }

        return new LockedUserPair(user1, user2);
    }

    private LockedUserPair lockUsersInOrder(User user1, User user2) {
        return lockUsersInOrder(user1.getId(), user2.getId());
    }

    public Friendship sendRequest(User sender, String receiverIdentifier) {
        return sendRequest(sender, receiverIdentifier, false);
    }

    public Friendship sendRequest(User sender, String receiverIdentifier, boolean unblockIfBlocked) {
        User receiver = userService.getUserByEmail(receiverIdentifier);

        if (sender.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("Cannot send friend request to yourself");
        }

        // Lock users in consistent order to prevent race conditions
        LockedUserPair locked =
                lockUsersInOrder(sender, receiver);
        User lockedSender = locked.first.getId().equals(sender.getId()) ? locked.first : locked.second;
        User lockedReceiver = locked.first.getId().equals(receiver.getId()) ? locked.first : locked.second;

        Optional<Friendship> existing = friendshipRepository.findBetweenUsers(lockedSender, lockedReceiver);
        if (existing.isPresent()) {
            Friendship friendship = existing.get();
            if (friendship.getStatus() == FriendshipStatus.BLOCKED) {
                if (friendship.getRequester().getId().equals(lockedSender.getId())) {
                    if (unblockIfBlocked) {
                        friendship.setStatus(FriendshipStatus.PENDING);
                        friendship.setRequester(lockedSender);
                        friendship.setAddressee(lockedReceiver);
                        return friendshipRepository.save(friendship);
                    }
                    throw new IllegalStateException("You have blocked this user");
                } else {
                    // Shadow ban: If blocked by other, return fake friendship and dont save
                    return new Friendship(lockedSender, lockedReceiver, FriendshipStatus.PENDING);
                }
            }
            if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new IllegalStateException("Already friends");
            }
            if (friendship.getStatus() == FriendshipStatus.PENDING) {
                throw new IllegalStateException("Request already pending");
            }
            // If DECLINED, we can re-request.
            friendship.setRequester(lockedSender);
            friendship.setAddressee(lockedReceiver);
            friendship.setStatus(FriendshipStatus.PENDING);
            return friendshipRepository.save(friendship);
        }

        Friendship friendship = new Friendship(lockedSender, lockedReceiver, FriendshipStatus.PENDING);
        return friendshipRepository.save(friendship);
    }

    public void unblockUser(User unblocker, Long userIdToUnblock) {
        User toUnblock = userRepository.findById(userIdToUnblock)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Friendship friendship = friendshipRepository.findBetweenUsers(unblocker, toUnblock)
                .orElseThrow(() -> new IllegalArgumentException("No relationship found"));

        if (friendship.getStatus() != FriendshipStatus.BLOCKED) {
            throw new IllegalStateException("User is not blocked");
        }

        if (!friendship.getRequester().getId().equals(unblocker.getId())) {
            throw new IllegalStateException("You cannot unblock a user who has blocked you");
        }

        friendshipRepository.delete(friendship);
    }

    public List<User> getBlockedUsers(User user) {
        return friendshipRepository.findByRequesterAndStatus(user, FriendshipStatus.BLOCKED).stream()
                .map(Friendship::getAddressee)
                .collect(Collectors.toList());
    }

    public List<User> getBlockers(User user) {
        return friendshipRepository.findByAddresseeAndStatus(user, FriendshipStatus.BLOCKED).stream()
                .map(Friendship::getRequester)
                .collect(Collectors.toList());
    }

    public void acceptRequest(Long friendshipId, User user) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Friendship not found"));

        if (!friendship.getAddressee().getId().equals(user.getId())) {
            throw new IllegalStateException("Not authorized to accept this request");
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalStateException("Request is not pending");
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);
    }

    public void declineRequest(Long friendshipId, User user) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Friendship not found"));

        if (!friendship.getAddressee().getId().equals(user.getId())) {
            throw new IllegalStateException("Not authorized to decline this request");
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalStateException("Request is not pending");
        }

        friendship.setStatus(FriendshipStatus.DECLINED);
        friendshipRepository.save(friendship);
    }

    public void blockUser(User blocker, Long userIdToBlock) {
        if (blocker.getId().equals(userIdToBlock)) {
            throw new IllegalArgumentException("Cannot block yourself");
        }

        LockedUserPair locked = lockUsersInOrder(blocker.getId(), userIdToBlock);
        User lockedBlocker = locked.first.getId().equals(blocker.getId()) ? locked.first : locked.second;
        User lockedToBlock = locked.first.getId().equals(userIdToBlock) ? locked.first : locked.second;

        Optional<Friendship> existing = friendshipRepository.findBetweenUsers(lockedBlocker, lockedToBlock);
        if (existing.isPresent()) {
            Friendship friendship = existing.get();
            friendship.setRequester(lockedBlocker);
            friendship.setAddressee(lockedToBlock);
            friendship.setStatus(FriendshipStatus.BLOCKED);
            friendshipRepository.save(friendship);
        } else {
            Friendship friendship = new Friendship(lockedBlocker, lockedToBlock, FriendshipStatus.BLOCKED);
            friendshipRepository.save(friendship);
        }
    }

    public List<User> getFriends(User user) {
        return friendshipRepository.findByUserAndStatus(user, FriendshipStatus.ACCEPTED).stream()
                .map(f -> f.getRequester().getId().equals(user.getId()) ? f.getAddressee() : f.getRequester())
                .collect(Collectors.toList());
    }

    public List<Friendship> getPendingRequests(User user) {
        return friendshipRepository.findByAddresseeAndStatus(user, FriendshipStatus.PENDING);
    }

    public void removeFriend(User user, Long friendId) {
        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Friendship friendship = friendshipRepository.findBetweenUsers(user, friend)
                .orElseThrow(() -> new IllegalArgumentException("Friendship not found"));

        if (friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new IllegalStateException("You are not friends with this user");
        }

        friendshipRepository.delete(friendship);
    }

    public boolean areFriends(User user1, User user2) {
        return friendshipRepository.findBetweenUsers(user1, user2)
                .map(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
                .orElse(false);
    }

    public void createFriendship(User user1, User user2) {
        if (user1.getId().equals(user2.getId())) {
            throw new IllegalArgumentException("Cannot create friendship with yourself");
        }

        LockedUserPair locked = lockUsersInOrder(user1, user2);
        User lockedUser1 = locked.first.getId().equals(user1.getId()) ? locked.first : locked.second;
        User lockedUser2 = locked.first.getId().equals(user2.getId()) ? locked.first : locked.second;

        Optional<Friendship> existing = friendshipRepository.findBetweenUsers(lockedUser1, lockedUser2);
        if (existing.isPresent()) {
            Friendship friendship = existing.get();
            if (friendship.getStatus() == FriendshipStatus.BLOCKED) {
                throw new IllegalStateException("Cannot create friendship: Blocked");
            }
            if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
                return; // Already friends
            }
            friendship.setStatus(FriendshipStatus.ACCEPTED);
            friendshipRepository.save(friendship);
        } else {
            Friendship friendship = new Friendship(lockedUser1, lockedUser2, FriendshipStatus.ACCEPTED);
            friendshipRepository.save(friendship);
        }
    }
}
