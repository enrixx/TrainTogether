package de.othr.traintogether.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AttachmentDto {
    String fileName;
    String fileUrl;
    String mimeType;
}
