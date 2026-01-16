package de.othr.traintogether.dto.chat;

import de.othr.traintogether.model.chat.ChatRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddMemberRequestDto {

    @NotBlank
    @Email
    private String email;

    private ChatRole role = ChatRole.MEMBER;
}

