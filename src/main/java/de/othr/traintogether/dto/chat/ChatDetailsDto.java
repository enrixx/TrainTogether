package de.othr.traintogether.dto.chat;

import de.othr.traintogether.model.chat.ChatRoom;
import de.othr.traintogether.model.chat.ChatRoomType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatDetailsDto {

    @NotNull
    private Long chatId;

    @NotNull
    private ChatRoomType type;

    @NotBlank
    @Size(max = 50)
    private String name;

    @Size(max = 512)
    private String pictureUrl;

    @NotNull
    private Instant createdAt;

    @NotBlank
    @Size(max = 255)
    private String description;

    @Min(0)
    private int memberCount;

    public static ChatDetailsDto fromEntity(@NotNull ChatRoom room) {
        if (room == null) throw new IllegalArgumentException("room must not be null");

        int count;
        try {
            count = (int) room.getMembers().stream().filter(m -> !m.isRemoved()).count();
        } catch (Exception ex) {
            count = 0;
        }

        return ChatDetailsDto.builder()
                .chatId(room.getId())
                .type(room.getType())
                .name(room.getName())
                .pictureUrl(room.getPictureUrl())
                .createdAt(room.getCreatedAt())
                .description(room.getDescription())
                .memberCount(count)
                .build();
    }
}
