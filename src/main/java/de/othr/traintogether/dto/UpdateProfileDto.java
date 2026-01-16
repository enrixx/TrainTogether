package de.othr.traintogether.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class UpdateProfileDto {

    @Email(message = "{error.email.invalid}")
    private String email;

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

    @Size(max = 500, message = "{error.bio.max.length}")
    private String bio;

    @Size(min = 6, message = "{error.password.min.length}")
    private String currentPassword;

    @Size(min = 6, message = "{error.password.min.length}")
    private String newPassword;

    @Size(min = 6, message = "{error.password.min.length}")
    private String confirmPassword;

    public UpdateProfileDto() {
    }

    public UpdateProfileDto(String email, String firstName, String lastName, String gender, LocalDate birthday, String bio) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
        this.birthday = birthday;
        this.bio = bio;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
