package de.othr.traintogether.model.TrainingModel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "training_days")
@Getter
@Setter
@NoArgsConstructor
public class TrainingDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Weekday weekday;

    @ManyToOne
    @JoinColumn(name = "split_id")
    private TrainingSplit split;

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<TrainingExercise> exercises = new java.util.ArrayList<>();


    public TrainingDay(Weekday weekday, TrainingSplit split) {
        this.weekday = weekday;
        this.split = split;
    }
}
