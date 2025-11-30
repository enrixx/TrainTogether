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

    public ChatRoomDto toDto(ChatRoom room) {
        if (room == null) return null;
        ChatRoomDto dto = new ChatRoomDto();
        dto.setId(room.getId());
        dto.setType(room.getType());
        dto.setName(room.getName());
        dto.setPictureUrl(room.getPictureUrl());
        dto.setCreatedAt(room.getCreatedAt());

        if (room.getMembers() == null || room.getMembers().isEmpty()) {
            dto.setMemberIds(Collections.emptySet());
        } else {
            dto.setMemberIds(membersToUserIds(room.getMembers()));
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
