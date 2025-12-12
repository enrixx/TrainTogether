package de.othr.traintogether.model.TrainingModel;

import de.othr.traintogether.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "personal_exercises")
@Getter
@Setter
@NoArgsConstructor
public class PersonalExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private int sets;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public PersonalExercise(String name, User user) {
        this.name = name;
        this.user = user;
    }

}
