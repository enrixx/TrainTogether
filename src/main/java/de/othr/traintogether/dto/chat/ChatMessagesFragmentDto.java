package de.othr.traintogether.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ChatMessagesFragmentDto {

    private String html;
    private boolean hasMore;
    private String cursor;
}

