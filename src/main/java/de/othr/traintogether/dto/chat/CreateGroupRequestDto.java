package de.othr.traintogether.dto.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupRequestDto {

    @NotBlank
    @Size(max = 50)
    private String name;

    @Valid
    @Size(max = 200)
    private List<AddMemberRequestDto> members;
}

