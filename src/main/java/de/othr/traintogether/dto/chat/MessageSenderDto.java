package de.othr.traintogether.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MessageSenderDto {
    private Long SenderId;
    private String SenderName;
    private String SenderPictureUrl;
}
