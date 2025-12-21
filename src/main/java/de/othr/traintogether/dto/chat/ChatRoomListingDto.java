package de.othr.traintogether.dto.chat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@RequiredArgsConstructor
public class ChatRoomListingDto {

    @lombok.NonNull
    private Long id;
    @lombok.NonNull
    private String roomType;
    private String name;
    private String pictureUrl;
    private String lastMessageAt;
    private String lastMessagePreview;
    private String unreadMessagesCount;
    private Boolean containsMessage;
}
