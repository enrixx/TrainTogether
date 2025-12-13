package de.othr.traintogether.controller;

import de.othr.traintogether.dto.chat.ChatMessagePageDto;
import de.othr.traintogether.dto.chat.ChatMessagesCursorDto;
import de.othr.traintogether.dto.chat.ChatMessagesFragmentDto;
import de.othr.traintogether.dto.chat.SendChatMessageDto;
import de.othr.traintogether.service.chat.ChatMessageService;
import de.othr.traintogether.service.chat.ChatRoomService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.security.Principal;


@Controller
@RequestMapping("/chat")
@PreAuthorize("hasAnyAuthority('ADMIN','USER','GYM_OWNER','GYM_WORKER')")
public class ChatController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final SpringTemplateEngine templateEngine;

    public ChatController(ChatRoomService chatRoomService, ChatMessageService chatMessageService, SpringTemplateEngine templateEngine) {
        this.chatRoomService = chatRoomService;
        this.chatMessageService = chatMessageService;
        this.templateEngine = templateEngine;
    }

    @ModelAttribute
    public void populateCommon(Model model, Principal principal) {
        if (principal != null) {
            String userEmail = principal.getName();
            model.addAttribute("groups", chatRoomService.findGroupsByUser(userEmail));
            model.addAttribute("dms", chatRoomService.findDmByUser(userEmail));
            model.addAttribute("sendChatMessageDto", new SendChatMessageDto());
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
            ChatMessagePageDto page = chatMessageService.getMessageInitialCursorPage(principal.getName(), chatId, pageSize);
            model.addAttribute("messagePage", page);
        } catch (IllegalArgumentException e) {
            //TODO: write exception on error page
            return "error/404";
        } catch (Exception e) {
            return "error/500";
        }
        return "chat";
    }

    @PostMapping("/{chatId}")
    public String sendMessage(
            @PathVariable("chatId") Long chatId,
            @Valid @ModelAttribute("sendChatMessageDto") SendChatMessageDto SendChatMessageDto,
            RedirectAttributes redirectAttributes,
            Principal principal) {

        try {
            chatMessageService.sendMessage(principal.getName(), chatId, SendChatMessageDto);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("sendError", e.getMessage());
            return "redirect:/chat/" + chatId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("sendError", "Interner Fehler");
            return "redirect:/chat/" + chatId;
        }
        return "redirect:/chat/" + chatId;
    }

    @GetMapping("/{chatId}/messages/fragment")
    @ResponseBody
    public ResponseEntity<ChatMessagesFragmentDto> getMessagesFragment(
            @PathVariable("chatId") Long chatId,
            @RequestParam("direction") String direction,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "pageSize", defaultValue = "50") Integer pageSize,
            Principal principal) {

        try {
            ChatMessagesCursorDto fragmentDto;
            if ("up".equalsIgnoreCase(direction)) {
                // service sollte eine Cursor-basierte Methode bereitstellen
                fragmentDto = chatMessageService.getMessageTopCursorPage(principal.getName(), chatId, cursor, pageSize);
            } else {
                fragmentDto = chatMessageService.getMessageBottomCursorPage(principal.getName(), chatId, cursor, pageSize);
            }

            if (fragmentDto == null) {
                ChatMessagesFragmentDto emptyDto = new ChatMessagesFragmentDto("", false, null);
                return ResponseEntity.ok(emptyDto);
            }

            Context ctx = new Context();
            ctx.setVariable("messages", fragmentDto.getMessages());
            ctx.setVariable("lastMessageId", fragmentDto.getLastMessageId());
            // render Fragment
            String html = templateEngine.process("fragments/chat/chat-messages", ctx);

            ChatMessagesFragmentDto dto = new ChatMessagesFragmentDto(
                    html,
                    fragmentDto.isHasMore(),
                    fragmentDto.getCursor()
            );

            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
