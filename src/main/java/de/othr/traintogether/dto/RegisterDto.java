package de.othr.traintogether.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class RegisterDto {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, message = "{error.password.min.length}")
    private String password;

    @NotBlank(message = "{error.firstname.required}")
    @Size(max = 50, message = "{error.firstname.max.length}")
    private String firstName;

    @NotBlank(message = "{error.lastname.required}")
    @Size(max = 50, message = "{error.lastname.max.length}")
    private String lastName;

    @NotBlank(message = "{error.gender.required}")
    private String gender;

    @NotNull(message = "{error.birthday.required}")
    @Past(message = "{error.birthday.past}")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    public RegisterDto() {}

    public RegisterDto(String email, String password, String firstName, String lastName, String gender, LocalDate birthday) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
        this.birthday = birthday;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }

}
