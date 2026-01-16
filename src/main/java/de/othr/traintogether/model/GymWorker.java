package de.othr.traintogether.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "gym_workers")
@Getter
@Setter
@NoArgsConstructor
public class GymWorker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne
    @JoinColumn(name = "gym_id", nullable = false)
    @JsonIgnore
    private Gym gym;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public GymWorker(User user, Gym gym, User createdBy) {
        this.user = user;
        this.gym = gym;
        this.createdBy = createdBy;
    }
}
