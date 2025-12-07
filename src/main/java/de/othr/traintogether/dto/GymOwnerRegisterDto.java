package de.othr.traintogether.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class GymOwnerRegisterDto extends RegisterDto {

    @NotBlank(message = "Gym name is required")
    @Size(max = 100, message = "Gym name cannot exceed 100 characters")
    private String gymName;

    @NotBlank(message = "Gym address is required")
    @Size(max = 200, message = "Gym address cannot exceed 200 characters")
    private String gymAddress;

    @NotBlank(message = "City is required")
    @Size(max = 50, message = "City cannot exceed 50 characters")
    private String city;

    @NotBlank(message = "Postal code is required")
    @Size(max = 10, message = "Postal code cannot exceed 10 characters")
    private String postalCode;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phoneNumber;

    @NotBlank(message = "Please describe your gym and why you want to register")
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String gymDescription;

    public GymOwnerRegisterDto() {
        super();
    }

    public String getGymName() {
        return gymName;
    }

    public void setGymName(String gymName) {
        this.gymName = gymName;
    }

    public String getGymAddress() {
        return gymAddress;
    }

    public void setGymAddress(String gymAddress) {
        this.gymAddress = gymAddress;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getGymDescription() {
        return gymDescription;
    }

    public void setGymDescription(String gymDescription) {
        this.gymDescription = gymDescription;
    }
}

