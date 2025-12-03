package de.othr.traintogether.mapper;

import de.othr.traintogether.dto.ChatRoomDto;
import de.othr.traintogether.model.chat.ChatRoom;
import de.othr.traintogether.model.chat.ChatRoomMember;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ChatRoomMapper {

    public ChatRoomDto toDtoGroup(ChatRoom room) {
        if (room == null) return null;
        ChatRoomDto dto = new ChatRoomDto();
        dto.setId(room.getId());
        dto.setType(room.getType());
        dto.setName(room.getName());
        dto.setCreatedAt(room.getCreatedAt());

        String pic = room.getPictureUrl();
        if (pic == null || pic.isBlank()) {
            dto.setPictureUrl("images/default-profile.png");
        } else {
            dto.setPictureUrl(pic);
        }

        if (room.getMembers() == null || room.getMembers().isEmpty()) {
            dto.setMemberIds(Collections.emptySet());
        } else {
            dto.setMemberIds(membersToUserIds(room.getMembers()));
        }
        return dto;
    }
    public ChatRoomDto toDtoDm(ChatRoom room, Long currentUserId) {
        ChatRoomDto dto = toDtoGroup(room);
        if((long) room.getMembers().size() != 2) {
            return dto; // fallback to normal group mapping
        }
        ChatRoomMember otherMember = room.getMembers().stream()
                .filter(m -> m.getUser() != null && !m.getUser().getId().equals(currentUserId))
                .findFirst()
                .orElse(null);

        if (otherMember != null) {

            String pic = otherMember.getUser().getProfilePictureUrl();
            if (pic == null || pic.isBlank()) {
                dto.setPictureUrl("images/default-profile.png");
            } else {
                dto.setPictureUrl(pic);
            }
            dto.setName(otherMember.getUser().getUsername());
        }
        return dto;
    }

    private Set<Long> membersToUserIds(Set<ChatRoomMember> members) {
        if (members == null) return Collections.emptySet();
        return members.stream()
                .filter(Objects::nonNull)
                .map(m -> {
                    if (m.getUser() != null) return m.getUser().getId();
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
    }
}
