package de.othr.traintogether.api;

import de.othr.traintogether.dto.chat.*;
import de.othr.traintogether.model.chat.ChatRoomType;
import de.othr.traintogether.service.chat.ChatRoomService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Chat Rooms", description = "APIs to create, manage and list chat rooms (DMs, groups)")
@RestController
@RequestMapping("/api/chatrooms")
@PreAuthorize("hasAnyAuthority('ADMIN','USER','GYM_OWNER','GYM_WORKER')")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    public ChatRoomController(ChatRoomService chatRoomService) {
        this.chatRoomService = chatRoomService;
    }

    /**
     * Create or return a DM (Direct Message) between the authenticated user and the provided email.
     * Request body: { "email": "target@example.com" }
     * Returns the chatId of the existing or newly created DM.
     */
    @Operation(summary = "Create/get DM", description = "Creates a direct chat (DM) between the caller and the provided email or returns the existing chatId.")
    @PostMapping("/dm")
    public ResponseEntity<ChatIdResponseDto> createDm(@RequestParam("email") String otherUserEmail, Principal principal) {
        if (otherUserEmail == null || otherUserEmail.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Long roomId = chatRoomService.createDm(principal.getName(), otherUserEmail);
            return ResponseEntity.ok(new ChatIdResponseDto(roomId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Delete/leave a DM by one of the participants. Only valid for DM rooms.
     */
    @Operation(summary = "Delete DM (participant)", description = "Allows an active DM participant to leave/delete the DM. Only valid for DM rooms.")
    @DeleteMapping("/{chatId}/dm")
    public ResponseEntity<Void> deleteDm(@PathVariable("chatId") Long chatId, Principal principal) {
        try {
            var settings = chatRoomService.getChatSettings(principal.getName(), chatId);
            if(settings.getChatDetails().getType() != ChatRoomType.DM){
                return ResponseEntity.notFound().build();
            }
            chatRoomService.removeUserFromRoom(chatId, principal.getName());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * A member leaves the group (only for GROUP or READ_ONLY). Admins are not allowed to leave via this endpoint.
     */
    @Operation(summary = "Leave group", description = "Allows a group member (non-admin) to leave the group.")
    @DeleteMapping("/{chatId}/members/me")
    public ResponseEntity<Void> leaveGroup(@PathVariable("chatId") Long chatId, Principal principal) {
        try {
            String email = principal.getName();
            var type = chatRoomService.getChatSettings(email, chatId).getChatDetails().getType();
            if(type != ChatRoomType.GROUP && type != ChatRoomType.READ_ONLY){
                return ResponseEntity.notFound().build();
            }
            if (chatRoomService.isUserAdmin(email, chatId)) {
                // Admins must delete the room or transfer admin — disallow leaving
                return ResponseEntity.status(403).build();
            }
            chatRoomService.removeUserFromRoom(chatId, email);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Deletes a chat room (admins only for groups/read-only).
     */
    @Operation(summary = "Delete chat room (admin)", description = "Deletes a chat room (admins only for groups/read-only).")
    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteChatRoom(@PathVariable("chatId") Long chatId, Principal principal) {
        try {
            String email = principal.getName();
            if (!chatRoomService.isUserAdmin(email, chatId)) {
                return ResponseEntity.status(403).build();
            }
            chatRoomService.deleteChatRoom(chatId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Create a new group. Request body: { "name": "Group name", "members": ["a@b","c@d"] }
     * Note: Controller does not handle images; uploads are handled separately.
     */
    @Operation(summary = "Create group", description = "Creates a new group with optional members (controller does not handle images).")
    @PostMapping("/group")
    public ResponseEntity<ChatIdResponseDto> createGroup(@RequestBody @Valid CreateGroupRequestDto body, Principal principal) {
        String name = body.getName();

        Set<String> members = Collections.emptySet();
        if (body.getMembers() != null && !body.getMembers().isEmpty()) {
            members = body.getMembers().stream().map(AddMemberRequestDto::getEmail).collect(Collectors.toSet());
        }

        try {
            // Controller does not handle picture upload/URL — pass null so service keeps default
            Long chatId = chatRoomService.createGroup(name, null, members, principal.getName());
            return ResponseEntity.ok(new ChatIdResponseDto(chatId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Adds a member to the group. Admin rights required.
     */
    @Operation(summary = "Add member", description = "Adds a member to the group (admin rights required). Body: { 'email': '...', 'role': 'MEMBER' }")
    @PostMapping("/{chatId}/members")
    public ResponseEntity<Void> addMember(@PathVariable("chatId") Long chatId, @RequestBody @Valid AddMemberRequestDto body, Principal principal) {
        String email = body.getEmail();
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            // only admins may add members
            if (!chatRoomService.isUserAdmin(principal.getName(), chatId)) {
                return ResponseEntity.status(403).build();
            }
            chatRoomService.addUserToGroup(chatId, email, body.getRole());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Removes a member from the group. Admin rights required.
     */
    @Operation(summary = "Remove member", description = "Removes a member from the group (admin rights required).")
    @DeleteMapping("/{chatId}/members/{memberEmail}")
    public ResponseEntity<Void> removeMember(@PathVariable("chatId") Long chatId, @PathVariable("memberEmail") String memberEmail, Principal principal) {
        try {
            if (!chatRoomService.isUserAdmin(principal.getName(), chatId)) {
                return ResponseEntity.status(403).build();
            }
            chatRoomService.removeUserFromRoom(chatId, memberEmail);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Lists DM, Group and ReadOnly rooms for the calling user (without messages).
     */
    @Operation(summary = "List chat rooms (no messages)", description = "Lists DMs, groups and read-only rooms for the calling user.")
    @GetMapping("")
    public ResponseEntity<List<ChatRoomListingDto>> listRooms(Principal principal) {
        try {
            List<ChatRoomListingDto> dms = chatRoomService.findDmByUser(principal.getName());
            List<ChatRoomListingDto> groups = chatRoomService.findGroupsByUser(principal.getName());
            List<ChatRoomListingDto> readOnly = chatRoomService.findRedOnlyGroupsByUser(principal.getName());
            List<ChatRoomListingDto> all = new ArrayList<>();
            all.addAll(dms);
            all.addAll(groups);
            all.addAll(readOnly);
            return ResponseEntity.ok(all);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Returns metadata and members of a chat room (no messages).
     */
    @Operation(summary = "Get chat settings", description = "Returns metadata and members of the chat room (no messages).")
    @GetMapping("/{chatId}/settings")
    public ResponseEntity<ChatSettingsDto> getChatSettingsApi(@PathVariable("chatId") Long chatId, Principal principal) {
        try {
            ChatSettingsDto settings = chatRoomService.getChatSettings(principal.getName(), chatId);
            return ResponseEntity.ok(settings);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

}
