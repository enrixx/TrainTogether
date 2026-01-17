package de.othr.traintogether.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatIdResponseDto {

    @Schema(description = "Chat identifier", example = "123")
    private Long chatId;
}