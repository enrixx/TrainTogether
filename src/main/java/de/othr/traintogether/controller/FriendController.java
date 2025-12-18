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

        model.addAttribute("friends", friends);
        model.addAttribute("pendingRequests", pendingRequests);
        return "friends";
    }

    @PostMapping("/request")
    public String sendRequest(@RequestParam("identifier") String identifier,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        User user = userService.getUserByEmail(authentication.getName());
        try {
            friendshipService.sendRequest(user, identifier);
            redirectAttributes.addFlashAttribute("successMessage", "Friend request sent to " + identifier);
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
    public String chatWithFriend(@PathVariable Long friendId, Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        User friend = userService.getUserById(friendId);

        Long chatRoomId = chatRoomService.getOrCreateDm(user, friend);

        return "redirect:/chat/" + chatRoomId;
    }
}
