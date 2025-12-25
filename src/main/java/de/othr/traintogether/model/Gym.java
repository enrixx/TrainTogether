package de.othr.traintogether.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "gyms")
@Getter
@Setter
@NoArgsConstructor
public class Gym {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false, length = 10)
    private String postalCode;

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 1000)
    private String description;

    @Column(length = 500)
    private String bannerImageUrl;

    @Column(length = 100)
    private String bannerText;

    @Column(length = 20)
    private String bannerTextColor;

    private double lat;
    private double lon;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private User owner;

    @OneToMany(mappedBy = "gym", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dateTime ASC")
    private List<Course> courses = new ArrayList<>();

    @OneToMany(mappedBy = "gym", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GymWorker> workers = new ArrayList<>();

//    @OneToMany(mappedBy = "gym", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<GymRating> ratings;
}
