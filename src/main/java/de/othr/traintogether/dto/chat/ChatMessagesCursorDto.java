package de.othr.traintogether.dto.chat;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChatMessagesCursorDto {

    private List<ChatMessageDto> messages;
    private Long lastMessageId;
    private boolean hasMore;
    private String cursor;
    private int pageSize;
    private String roomType;
}
