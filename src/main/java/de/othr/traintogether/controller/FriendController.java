package de.othr.traintogether.controller;

import de.othr.traintogether.model.Friendship;
import de.othr.traintogether.model.User;
import de.othr.traintogether.service.FriendshipService;
import de.othr.traintogether.service.UserService;
import de.othr.traintogether.service.chat.ChatRoomService;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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
    private final MessageSource messageSource;

    public FriendController(FriendshipService friendshipService, UserService userService, ChatRoomService chatRoomService, MessageSource messageSource) {
        this.friendshipService = friendshipService;
        this.userService = userService;
        this.chatRoomService = chatRoomService;
        this.messageSource = messageSource;
    }

    @GetMapping
    public String listFriends(Model model, Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        List<User> friends = friendshipService.getFriends(user);
        List<Friendship> pendingRequests = friendshipService.getPendingRequests(user);
        List<Friendship> sentRequests = friendshipService.getSentPendingRequests(user);
        List<User> blockedUsers = friendshipService.getBlockedUsers(user);

        model.addAttribute("friends", friends);
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("sentRequests", sentRequests);
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
            redirectAttributes.addFlashAttribute("success", messageSource.getMessage("friends.action.unblock.success", null, LocaleContextHolder.getLocale()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
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
            redirectAttributes.addFlashAttribute("success", messageSource.getMessage("friends.action.request.success", new Object[]{identifier}, LocaleContextHolder.getLocale()));
        } catch (IllegalStateException e) {
            if (e.getMessage().equals("You have blocked this user")) {
                redirectAttributes.addFlashAttribute("confirmUnblockIdentifier", identifier);
                redirectAttributes.addFlashAttribute("errorMessage", messageSource.getMessage("friends.error.blocked", null, LocaleContextHolder.getLocale()));
            } else {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/friends";
    }

    @PostMapping("/{id}/accept")
    public String acceptRequest(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userService.getUserByEmail(authentication.getName());
        try {
            friendshipService.acceptRequest(id, user);
            redirectAttributes.addFlashAttribute("success", messageSource.getMessage("friends.action.accept.success", null, LocaleContextHolder.getLocale()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/friends";
    }

    @PostMapping("/{id}/decline")
    public String declineRequest(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userService.getUserByEmail(authentication.getName());
        try {
            friendshipService.declineRequest(id, user);
            redirectAttributes.addFlashAttribute("success", messageSource.getMessage("friends.action.decline.success", null, LocaleContextHolder.getLocale()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/friends";
    }

    @PostMapping("/block")
    public String blockUser(@RequestParam("userId") Long userId, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userService.getUserByEmail(authentication.getName());
        try {
            friendshipService.blockUser(user, userId);
            redirectAttributes.addFlashAttribute("success", messageSource.getMessage("friends.action.block.success", null, LocaleContextHolder.getLocale()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/friends";
    }

    @PostMapping("/remove")
    public String removeFriend(@RequestParam("friendId") Long friendId, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userService.getUserByEmail(authentication.getName());
        try {
            friendshipService.removeFriend(user, friendId);
            redirectAttributes.addFlashAttribute("success", messageSource.getMessage("friends.action.remove.success", null, LocaleContextHolder.getLocale()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/friends";
    }

    @GetMapping("/chat/{friendId}")
    public String chatWithFriend(@PathVariable Long friendId, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userService.getUserByEmail(authentication.getName());
        User friend = userService.getUserById(friendId);

        if (!friendshipService.areFriends(user, friend)) {
            redirectAttributes.addFlashAttribute("error", messageSource.getMessage("friends.error.not.friends", null, LocaleContextHolder.getLocale()));
            return "redirect:/friends";
        }

        Long chatRoomId = chatRoomService.createDm(user.getEmail(), friend.getEmail());

        return "redirect:/chat/" + chatRoomId;
    }

    @PostMapping("/{id}/cancel")
    public String cancelRequest(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userService.getUserByEmail(authentication.getName());
        try {
            friendshipService.cancelRequest(id, user);
            redirectAttributes.addFlashAttribute("success", messageSource.getMessage("friends.action.cancel.success", null, LocaleContextHolder.getLocale()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/friends";
    }
}
