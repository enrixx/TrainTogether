package de.othr.traintogether.model;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true, length=50)
    private String username;

    @Column(nullable=false, length=100)
    private String email;

    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt = Instant.now();

    // constructors, getters, setters
    public User() {}
    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }

    public Long getId() {
        return id;
    }
    // getters/setters...
}

