package de.othr.traintogether.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "matching_actions")
@Getter
@Setter
@NoArgsConstructor
public class MatchingAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @ManyToOne
    @JoinColumn(name = "target_id", nullable = false)
    private User target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType actionType; // LIKE, DISLIKE

    @Column(nullable = false)
    private LocalDateTime actionDate = LocalDateTime.now();

    public enum ActionType {
        LIKE, DISLIKE
    }

    public MatchingAction(User actor, User target, ActionType actionType) {
        this.actor = actor;
        this.target = target;
        this.actionType = actionType;
    }
}
