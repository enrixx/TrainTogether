package de.othr.traintogether.model.TrainingModel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;

@Entity
@Table(name = "training_splits")
@Getter
@Setter
@NoArgsConstructor
public class TrainingSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String splitName;

    @OneToMany(mappedBy = "split", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<TrainingDay> days = new java.util.ArrayList<>();

    public TrainingSplit(String splitName) {
        this.splitName = splitName;
        initializeDays();
    }


    private void initializeDays() {
        for (Weekday weekday : Weekday.values()) {
            TrainingDay day = new TrainingDay();
            day.setWeekday(weekday);
            day.setSplit(this);

            day.setExercises(new ArrayList<>()); // leer = Rest Day
            this.days.add(day);
        }
    }
}
