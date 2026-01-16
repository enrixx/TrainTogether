package de.othr.traintogether.model.TrainingModel;

import de.othr.traintogether.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "custom_exercises")
@Getter
@Setter
@NoArgsConstructor
public class CustomExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User createdBy;

    public CustomExercise(String name, User createdBy) {
        this.name = name;
        this.createdBy = createdBy;
    }
}
