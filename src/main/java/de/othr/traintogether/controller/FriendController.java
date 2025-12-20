package de.othr.traintogether.controller;

import de.othr.traintogether.model.Friendship;
import de.othr.traintogether.model.User;
import de.othr.traintogether.service.FriendshipService;
import de.othr.traintogether.service.UserService;
import de.othr.traintogether.service.chat.ChatRoomService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/friends")
public class FriendController {

    private final FriendshipService friendshipService;
    private final UserService userService;
    private final ChatRoomService chatRoomService;

    public FriendController(FriendshipService friendshipService, UserService userService, ChatRoomService chatRoomService) {
        this.friendshipService = friendshipService;
        this.userService = userService;
        this.chatRoomService = chatRoomService;
    }

    @GetMapping
    public String listFriends(Model model, Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        List<User> friends = friendshipService.getFriends(user);
        List<Friendship> pendingRequests = friendshipService.getPendingRequests(user);
        List<User> blockedUsers = friendshipService.getBlockedUsers(user);

        model.addAttribute("friends", friends);
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("blockedUsers", blockedUsers);
        return "friends";
    }

    @PostMapping("/unblock")
    public String unblockUser(@RequestParam("userId") Long userId,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        User user = userService.getUserByEmail(authentication.getName());
        try {
            friendshipService.unblockUser(user, userId);
            redirectAttributes.addFlashAttribute("successMessage", "User unblocked successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/friends";
    }

    @PostMapping("/request")
    public String sendRequest(@RequestParam("identifier") String identifier,
                              @RequestParam(value = "unblock", required = false, defaultValue = "false") boolean unblock,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        User user = userService.getUserByEmail(authentication.getName());
        try {
            friendshipService.sendRequest(user, identifier, unblock);
            redirectAttributes.addFlashAttribute("successMessage", "Friend request sent to " + identifier);
        } catch (IllegalStateException e) {
            if (e.getMessage().equals("You have blocked this user")) {
                redirectAttributes.addFlashAttribute("confirmUnblockIdentifier", identifier);
                redirectAttributes.addFlashAttribute("errorMessage", "You have blocked this user. Do you want to unblock and add them?");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/friends";
    }

    @PostMapping("/{id}/accept")
    public String acceptRequest(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userService.getUserByEmail(authentication.getName());
        try {
            friendshipService.acceptRequest(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "Friend request accepted");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/friends";
    }

    @PostMapping("/{id}/decline")
    public String declineRequest(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userService.getUserByEmail(authentication.getName());
        try {
            friendshipService.declineRequest(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "Friend request declined");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/friends";
    }

    @PostMapping("/block")
    public String blockUser(@RequestParam("userId") Long userId, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userService.getUserByEmail(authentication.getName());
        try {
            friendshipService.blockUser(user, userId);
            redirectAttributes.addFlashAttribute("successMessage", "User blocked");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/friends";
    }

    @PostMapping("/remove")
    public String removeFriend(@RequestParam("friendId") Long friendId, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userService.getUserByEmail(authentication.getName());
        try {
            friendshipService.removeFriend(user, friendId);
            redirectAttributes.addFlashAttribute("successMessage", "Friend removed");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/friends";
    }

    @GetMapping("/chat/{friendId}")
    public String chatWithFriend(@PathVariable Long friendId, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userService.getUserByEmail(authentication.getName());
        User friend = userService.getUserById(friendId);

        if (!friendshipService.areFriends(user, friend)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are not friends with this user.");
            return "redirect:/friends";
        }

        Long chatRoomId = chatRoomService.getOrCreateDm(user, friend);

        return "redirect:/chat/" + chatRoomId;
    }
}
