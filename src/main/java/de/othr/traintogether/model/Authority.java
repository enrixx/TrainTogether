package de.othr.traintogether.model;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "authorities",
        uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "authority"})
})
public class Authority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String authority;

    public Authority() {}
    public Authority(User user, String authority) {
        this.user = Objects.requireNonNull(user, "User cannot be null");
        this.authority = Objects.requireNonNull(authority, "Authority cannot be null");
    }

    // getters + setters
    public String getAuthority(){
        return authority;
    }
}