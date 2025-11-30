// java
package de.othr.traintogether.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterDto {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, message = "{error.password.min.length}")
    private String password;

    @Size(max = 30, message = "{error.username.max.length}")
    private String username;

    @NotBlank
    private String role;

    public RegisterDto() {}

    public RegisterDto(String email, String password, String username, String role) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.role = role;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
