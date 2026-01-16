package de.othr.traintogether.model.trainingModel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "training_exercises")
@Getter
@Setter
@NoArgsConstructor
public class TrainingExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_exercise_id")
    private PersonalExercise personalExercise;

    private int sets;
    private String reps;
    private String weight;

    @ManyToOne
    @JoinColumn(name = "day_id")
    private TrainingDay day;

    private LocalDate date;

    public TrainingExercise(PersonalExercise exercise, int sets, String reps, String weight, TrainingDay day, LocalDate date) {
        this.personalExercise = exercise;
        this.sets = sets;
        this.reps = reps;
        this.weight = weight;
        this.day = day;
        this.date = date;
    }
}
