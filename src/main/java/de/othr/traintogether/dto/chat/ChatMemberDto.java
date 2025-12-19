package de.othr.traintogether.dto.chat;

import de.othr.traintogether.model.chat.ChatRoomMember;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMemberDto {

    @NotNull
    private Long id;

    @NotBlank
    @Size(max = 20)
    private String displayName;

    @NotBlank
    @Size(max = 30)
    private String role;

    @Size(max = 500)
    private String pictureUrl;


    public static ChatMemberDto fromEntity(ChatRoomMember member) {
        if (member == null) return null;

        String displayName = member.getDisplayName();
        String name = (displayName != null && !displayName.isBlank())
                ? displayName
                : member.getUser().getUsername();

        String role = member.getRole().name();

        String pic = member.getUser().getProfilePictureUrl();
        if (pic == null || pic.isBlank()) {
            pic = "/images/default-profile.png";
        }

        return ChatMemberDto.builder()
                .id(member.getId())
                .displayName(name)
                .role(role)
                .pictureUrl(pic)
                .build();
    }
}
