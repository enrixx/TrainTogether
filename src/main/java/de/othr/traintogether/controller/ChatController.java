package de.othr.traintogether.controller;

import de.othr.traintogether.service.ChatRoomService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;


@Controller
@PreAuthorize("hasAnyAuthority('ADMIN','USER','GYM_OWNER','GYM_WORKER')")
public class ChatController {

    private final ChatRoomService chatRoomService;

    public ChatController(ChatRoomService chatRoomService) {
        this.chatRoomService = chatRoomService;
    }

    @GetMapping("/chat")
    public String chat(Model model, Principal principal) {
        String userEmail = principal != null ? principal.getName() : null;
        if (userEmail != null) {
            model.addAttribute("groups", chatRoomService.findGroupsByUser(userEmail));
            model.addAttribute("dms", chatRoomService.findDmByUser(userEmail));
        }
        model.addAttribute("title", "Chat");
        return "chat";
    }
}
