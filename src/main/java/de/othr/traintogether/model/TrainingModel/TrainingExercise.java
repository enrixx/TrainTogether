package de.othr.traintogether.model.TrainingModel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "training_exercises")
@Getter
@Setter
@NoArgsConstructor
public class TrainingExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ExerciseName exercise;

    private int sets;
    private int reps;

    @ManyToOne
    @JoinColumn(name = "day_id")
    private TrainingDay day;

    public TrainingExercise(ExerciseName exercise, int sets, int reps, TrainingDay day) {
        this.exercise = exercise;
        this.sets = sets;
        this.reps = reps;
        this.day = day;
    }
}
