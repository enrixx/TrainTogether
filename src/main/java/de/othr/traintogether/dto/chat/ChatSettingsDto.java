package de.othr.traintogether.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatSettingsDto {
    boolean canEdit;
    ChatDetailsDto chatDetails;
    List<ChatMemberDto> chatMembers;
}
