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

    @NotBlank(message = "{error.firstname.required}")
    @Size(max = 50, message = "{error.firstname.max.length}")
    private String firstName;

    @NotBlank(message = "{error.lastname.required}")
    @Size(max = 50, message = "{error.lastname.max.length}")
    private String lastName;

    public RegisterDto() {}

    public RegisterDto(String email, String password, String username, String firstName, String lastName) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

}
