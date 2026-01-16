package de.othr.traintogether.dto;

import de.othr.traintogether.model.User;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Set;

public class UserDto {

    private Long id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String gender;
    private LocalDate birthday;
    private String bio;
    private String profilePictureUrl;
    private Instant createdAt;
    private Set<String> authorities;
    private Integer age;
    private List<String> trainingDays;

    public UserDto() {
    }

    public UserDto(Long id, String email, String username, String firstName, String lastName, String gender, LocalDate birthday, String bio, String profilePictureUrl, Instant createdAt, Set<String> authorities) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
        this.birthday = birthday;
        this.bio = bio;
        this.profilePictureUrl = profilePictureUrl;
        this.createdAt = createdAt;
        this.authorities = authorities;
        if (birthday != null) {
            this.age = Period.between(birthday, LocalDate.now()).getYears();
        }
    }

    public UserDto(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.username = user.getUsername();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.gender = user.getGender();
        this.birthday = user.getBirthday();
        this.bio = user.getBio();
        this.profilePictureUrl = user.getProfilePictureUrl();
        this.createdAt = user.getCreatedAt();
        this.authorities = user.getAuthorities();
        if (user.getBirthday() != null) {
            this.age = Period.between(user.getBirthday(), LocalDate.now()).getYears();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Set<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Set<String> authorities) {
        this.authorities = authorities;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public List<String> getTrainingDays() {
        return trainingDays;
    }

    public void setTrainingDays(List<String> trainingDays) {
        this.trainingDays = trainingDays;
    }
}
