package de.othr.traintogether.dto.chat;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChatMessagePageDto {
    private List<ChatMessageDto> messages;
    private Long lastMessageId;
    private String roomType;
    private boolean hasMore;
    private String nextCursor;
    private int pageSize;
}

