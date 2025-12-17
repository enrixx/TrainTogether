package de.othr.traintogether.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
public class SendChatMessageDto {

    @lombok.NonNull
    @NotBlank
    private String content;
    private boolean edited;
    private String replyTo;
    private AttachmentDto attachment;
}
