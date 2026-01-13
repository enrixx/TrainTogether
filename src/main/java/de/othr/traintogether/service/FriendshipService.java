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

    public Friendship sendRequest(User sender, String receiverIdentifier) {
        return sendRequest(sender, receiverIdentifier, false);
    }

    public Friendship sendRequest(User sender, String receiverIdentifier, boolean unblockIfBlocked) {
        User receiver = userService.getUserByEmailOrUsername(receiverIdentifier);

        if (sender.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("Cannot send friend request to yourself");
        }

        // Lock users in consistent order to prevent race conditions
        if (sender.getId() < receiver.getId()) {
            userRepository.findByIdWithLock(sender.getId());
            userRepository.findByIdWithLock(receiver.getId());
        } else {
            userRepository.findByIdWithLock(receiver.getId());
            userRepository.findByIdWithLock(sender.getId());
        }

        Optional<Friendship> existing = friendshipRepository.findBetweenUsers(sender, receiver);
        if (existing.isPresent()) {
            Friendship friendship = existing.get();
            if (friendship.getStatus() == FriendshipStatus.BLOCKED) {
                if (friendship.getRequester().getId().equals(sender.getId())) {
                    if (unblockIfBlocked) {
                        friendship.setStatus(FriendshipStatus.PENDING);
                        friendship.setRequester(sender);
                        friendship.setAddressee(receiver);
                        return friendshipRepository.save(friendship);
                    }
                    throw new IllegalStateException("You have blocked this user");
                } else {
                    // Shadow ban: If blocked by other, return fake friendship and dont save
                    return new Friendship(sender, receiver, FriendshipStatus.PENDING);
                }
            }
            if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new IllegalStateException("Already friends");
            }
            if (friendship.getStatus() == FriendshipStatus.PENDING) {
                throw new IllegalStateException("Request already pending");
            }
            // If DECLINED, we can re-request.
            friendship.setRequester(sender);
            friendship.setAddressee(receiver);
            friendship.setStatus(FriendshipStatus.PENDING);
            return friendshipRepository.save(friendship);
        }

        Friendship friendship = new Friendship(sender, receiver, FriendshipStatus.PENDING);
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

        // Lock users in consistent order to prevent race conditions
        if (blocker.getId() < userIdToBlock) {
            userRepository.findByIdWithLock(blocker.getId());
            userRepository.findByIdWithLock(userIdToBlock);
        } else {
            userRepository.findByIdWithLock(userIdToBlock);
            userRepository.findByIdWithLock(blocker.getId());
        }

        User toBlock = userRepository.findById(userIdToBlock)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Optional<Friendship> existing = friendshipRepository.findBetweenUsers(blocker, toBlock);
        if (existing.isPresent()) {
            Friendship friendship = existing.get();
            friendship.setRequester(blocker);
            friendship.setAddressee(toBlock);
            friendship.setStatus(FriendshipStatus.BLOCKED);
            friendshipRepository.save(friendship);
        } else {
            Friendship friendship = new Friendship(blocker, toBlock, FriendshipStatus.BLOCKED);
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

        // Lock users in consistent order to prevent race conditions
        if (user1.getId() < user2.getId()) {
            userRepository.findByIdWithLock(user1.getId());
            userRepository.findByIdWithLock(user2.getId());
        } else {
            userRepository.findByIdWithLock(user2.getId());
            userRepository.findByIdWithLock(user1.getId());
        }

        Optional<Friendship> existing = friendshipRepository.findBetweenUsers(user1, user2);
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
            Friendship friendship = new Friendship(user1, user2, FriendshipStatus.ACCEPTED);
            friendshipRepository.save(friendship);
        }
    }
}
