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
        User receiver = userService.getUserByEmailOrUsername(receiverIdentifier);

        if (sender.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("Cannot send friend request to yourself");
        }

        Optional<Friendship> existing = friendshipRepository.findBetweenUsers(sender, receiver);
        if (existing.isPresent()) {
            Friendship friendship = existing.get();
            if (friendship.getStatus() == FriendshipStatus.BLOCKED) {
                // If the sender is the one who blocked, they can unblock by sending request?
                // Or we just say "Cannot send request".
                // If sender is the one blocked (requester != sender), then definitely cannot.
                if (!friendship.getRequester().getId().equals(sender.getId())) {
                     throw new IllegalStateException("You are blocked by this user");
                }
                // If sender is the blocker, maybe we allow them to re-friend?
                // For now, let's just say "Cannot send request" to keep it simple as per requirements "cannot if you are blocked".
                throw new IllegalStateException("Cannot send request");
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
}
