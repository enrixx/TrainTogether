package de.othr.traintogether.dto.chat;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
public class ChatMessageDto {
    @lombok.NonNull
    private Long id;
    @lombok.NonNull
    private MessageSenderDto sender;
    @lombok.NonNull
    private String content;
    @lombok.NonNull
    private String sentAt;
    @lombok.NonNull
    private String cursor;
    private boolean edited;
    private boolean mine;
    private String replyTo;
    private AttachmentDto attachment;
}

