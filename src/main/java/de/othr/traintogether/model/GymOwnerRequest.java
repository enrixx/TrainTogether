package de.othr.traintogether.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "gym_owner_requests")
public class GymOwnerRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    //ToDo: add Gym

    @Column(length = 100)
    private String gymName;

    @Column(length = 200)
    private String gymAddress;

    @Column(length = 50)
    private String city;

    @Column(length = 10)
    private String postalCode;

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 1000)
    private String gymDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    private LocalDateTime reviewedAt;

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(length = 1000)
    private String requestMessage;

    public GymOwnerRequest() {
    }

    public GymOwnerRequest(User user, String requestMessage) {
        this.user = user;
        this.requestMessage = requestMessage;
    }

}

