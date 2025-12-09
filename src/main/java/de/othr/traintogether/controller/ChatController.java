package de.othr.traintogether.controller;

import de.othr.traintogether.dto.chat.ChatMessagePageDto;
import de.othr.traintogether.service.ChatMessageService;
import de.othr.traintogether.service.ChatRoomService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;


@Controller
@RequestMapping("/chat")
@PreAuthorize("hasAnyAuthority('ADMIN','USER','GYM_OWNER','GYM_WORKER')")
public class ChatController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    public ChatController(ChatRoomService chatRoomService, ChatMessageService chatMessageService) {
        this.chatRoomService = chatRoomService;
        this.chatMessageService = chatMessageService;
    }

    @ModelAttribute
    public void populateCommon(Model model, Principal principal) {
        if (principal != null) {
            String userEmail = principal.getName();
            model.addAttribute("groups", chatRoomService.findGroupsByUser(userEmail));
            model.addAttribute("dms", chatRoomService.findDmByUser(userEmail));
            model.addAttribute("title", "Chat");
        }
    }

    @GetMapping()
    public String chat(Model model, Principal principal) {
        return "chat";
    }

    @GetMapping("/{chatId}")
    public String getMessages(
            @PathVariable("chatId") Long chatId,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "pageSize", defaultValue = "50") Integer pageSize,
            Model model,
            Principal principal) {

        try {
            ChatMessagePageDto page = chatMessageService.getMessageCursorPage(principal.getName(), chatId, cursor, pageSize);
            model.addAttribute("messagePage", page);
        } catch (IllegalArgumentException e) {
            return "error/404";
        } catch (Exception e) {
            return "error/500";
        }
        return  "chat";
    }
}
