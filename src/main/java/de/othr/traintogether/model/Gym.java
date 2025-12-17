package de.othr.traintogether.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "gyms")
@Getter
@Setter
@NoArgsConstructor
public class Gym {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 200)
    private String address;

    @Column(length = 50)
    private String city;

    @Column(length = 10)
    private String postalCode;

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 1000)
    private String description;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "gym", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<GymWorker> gymWorkers = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Gym(String name, User owner) {
        this.name = name;
        this.owner = owner;
    }
}