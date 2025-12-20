package de.othr.traintogether.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ResetPasswordDto {

    @NotBlank(message = "{error.password.required}")
    @Size(min = 6, message = "{error.password.min.length}")
    private String password;

    @NotBlank(message = "{error.password.required}")
    private String confirmPassword;
}

