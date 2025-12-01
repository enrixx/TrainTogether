package de.othr.traintogether.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
@PreAuthorize("hasAnyAuthority('ADMIN','USER','GYM_OWNER','GYM_WORKER')")
public class ChatController {

    @GetMapping("/chat")
    public String chat(Model model) {
        model.addAttribute("title", "Chat");
        return "chat";
    }
}
