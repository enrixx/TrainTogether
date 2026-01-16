package de.othr.traintogether.model.TrainingModel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "training_days")
@Getter
@Setter
@NoArgsConstructor
public class TrainingDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.ORDINAL)
    private DayOfWeek weekday;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "split_id")
    private TrainingSplit split;

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrainingExercise> exercises = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "training_day_personal_exercises",
            joinColumns = @JoinColumn(name = "day_id"),
            inverseJoinColumns = @JoinColumn(name = "personal_exercise_id")
    )
    private List<PersonalExercise> personalExercises = new ArrayList<>();

    public TrainingDay(DayOfWeek weekday) {
        this.weekday = weekday;
    }

    public void addPersonalExercise(PersonalExercise personalExercise) {
        personalExercises.add(personalExercise);
    }

}
