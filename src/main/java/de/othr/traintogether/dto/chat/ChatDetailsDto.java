package de.othr.traintogether.dto.chat;

import de.othr.traintogether.model.chat.ChatRoom;
import de.othr.traintogether.model.chat.ChatRoomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatDetailsDto {

    private Long chatId;

    private ChatRoomType type;

    @NotBlank
    @Size(max = 50)
    private String name;

    private String pictureUrl;

    private String createdAt;

    @Size(max = 255)
    private String description;

    private int memberCount;

    private boolean hasPicture;

    public static ChatDetailsDto fromEntity(@NotNull ChatRoom room) {
        if (room == null) throw new IllegalArgumentException("room must not be null");

        int count;
        try {
            count = (int) room.getMembers().stream().filter(m -> !m.isRemoved()).count();
        } catch (Exception ex) {
            count = 0;
        }

        String pic = room.getPictureUrl();
        boolean hasPicture = true;
        if (pic == null || pic.isBlank()) {
            pic = "/images/default-profile.png";
            hasPicture = false;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                .withZone(ZoneId.systemDefault());
        String created = room.getCreatedAt() != null ? formatter.format(room.getCreatedAt()) : "";

        return ChatDetailsDto.builder()
                .chatId(room.getId())
                .type(room.getType())
                .name(room.getName())
                .pictureUrl(pic)
                .hasPicture(hasPicture)
                .createdAt(created)
                .description(room.getDescription())
                .memberCount(count)
                .build();
    }
}
