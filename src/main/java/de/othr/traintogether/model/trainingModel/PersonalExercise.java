package de.othr.traintogether.model.trainingModel;

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "standard_exercise_id")
    private StandardExercise standardExercise;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "custom_exercise_id")
    private CustomExercise customExercise;

    private int sets;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public PersonalExercise(StandardExercise standardExercise, User user) {
        this.standardExercise = standardExercise;
        this.user = user;
    }

    public PersonalExercise(CustomExercise customExercise, User user) {
        this.customExercise = customExercise;
        this.user = user;
    }
    
    public String getName() {
        if (standardExercise != null) {
            // Hier könnte man noch Logik für die Sprache einbauen, aber erstmal Default (DE)
            return standardExercise.getNameDe(); 
        } else if (customExercise != null) {
            return customExercise.getName();
        }
        return "Unknown Exercise";
    }
}
