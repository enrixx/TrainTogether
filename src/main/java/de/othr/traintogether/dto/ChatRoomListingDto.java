package de.othr.traintogether.dto;

import de.othr.traintogether.model.chat.ChatRoomType;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ChatRoomListingDto {

    private Long id;
    private ChatRoomType type;
    private String name;
    private String pictureUrl;
    private String lastMessageAt;
    private String lastMessagePreview;
    private String unreadMessagesCount;
    private Boolean containsMessage;

    public ChatRoomListingDto() {}

    public ChatRoomListingDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }


}
