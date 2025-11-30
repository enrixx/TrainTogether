package de.othr.traintogether.dto;

import de.othr.traintogether.model.User;

import java.time.Instant;
import java.util.Set;

public class UserDto {

    private Long id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String profilePictureUrl;
    private Instant createdAt;
    private Set<String> authorities;

    public UserDto() {
    }

    public UserDto(Long id, String email, String username, String firstName, String lastName, String profilePictureUrl, Instant createdAt, Set<String> authorities) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.profilePictureUrl = profilePictureUrl;
        this.createdAt = createdAt;
        this.authorities = authorities;
    }

    public UserDto(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.username = user.getUsername();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.profilePictureUrl = user.getProfilePictureUrl();
        this.createdAt = user.getCreatedAt();
        this.authorities = user.getAuthorities();
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
}

