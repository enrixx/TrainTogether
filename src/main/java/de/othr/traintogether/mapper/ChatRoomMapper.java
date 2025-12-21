package de.othr.traintogether.mapper;

import de.othr.traintogether.dto.chat.ChatRoomListingDto;
import de.othr.traintogether.model.chat.ChatMessage;
import de.othr.traintogether.model.chat.ChatRoom;
import de.othr.traintogether.model.chat.ChatRoomMember;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class ChatRoomMapper {

    public ChatRoomListingDto toDtoGroup(ChatRoom room, String unreadMessagesCount, Optional<ChatMessage> lastMessage) {
        if (room == null) return null;
        ChatRoomListingDto dto = new ChatRoomListingDto(room.getId(), room.getType().name());
        dto.setUnreadMessagesCount(unreadMessagesCount);
        if (lastMessage.isPresent()) {
            dto.setContainsMessage(true);
            ChatMessage message = lastMessage.get();
            dto.setLastMessageAt(formatSentAt(message.getSentAt()));
            dto.setLastMessagePreview(message.getContent());
        }
        dto.setName(room.getName());

        String pic = room.getPictureUrl();
        if (pic == null || pic.isBlank()) {
            dto.setPictureUrl("/images/default-profile.png");
        } else {
            dto.setPictureUrl(pic);
        }
        return dto;
    }

    public ChatRoomListingDto toDtoDm(ChatRoom room, String unreadMessagesCount, Optional<ChatMessage> lastMessage, ChatRoomMember member) {
        ChatRoomListingDto dto = toDtoGroup(room, unreadMessagesCount, lastMessage);
        if ((long) room.getMembers().size() != 2) {
            return dto; // fallback to normal group mapping
        }
        ChatRoomMember otherMember = room.getMembers().stream()
                .filter(m -> !m.getUser().getId().equals(member.getUser().getId()))
                .findFirst()
                .orElse(null);

        if (otherMember != null) {

            String pic = otherMember.getUser().getProfilePictureUrl();
            if (pic == null || pic.isBlank()) {
                dto.setPictureUrl("/images/default-profile.png");
            } else {
                dto.setPictureUrl(pic);
            }
            dto.setName(otherMember.getUser().getUsername());
        }
        return dto;
    }

    private String formatSentAt(Instant sentAt) {
        if (sentAt == null) return null;
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime zdt = sentAt.atZone(zone);
        LocalDate sentDate = zdt.toLocalDate();
        LocalDate today = LocalDate.now(zone);

        if (sentDate.equals(today)) {
            return zdt.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else if (sentDate.getYear() == today.getYear()) {
            return zdt.format(DateTimeFormatter.ofPattern("dd.MM"));
        } else {
            return zdt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }
    }
}
