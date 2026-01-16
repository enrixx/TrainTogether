package de.othr.traintogether.model.trainingModel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    @OneToMany(
            mappedBy = "split",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )

    private List<TrainingDay> days = new ArrayList<>();

    public TrainingSplit(String name) {
        this.splitName = name;

        for (DayOfWeek weekday : DayOfWeek.values()) {
            TrainingDay day = new TrainingDay(weekday);
            addDay(day);
        }
    }

    public void addDay(TrainingDay day) {
        days.add(day);
        day.setSplit(this);
    }


    @PostLoad
    private void ensureAllDaysExist() {
        Set<DayOfWeek> existing = days.stream()
                .map(TrainingDay::getWeekday)
                .collect(Collectors.toSet());

        for (DayOfWeek weekday : DayOfWeek.values()) {
            if (!existing.contains(weekday)) {
                TrainingDay d = new TrainingDay(weekday);
                addDay(d);
            }
        }
    }
}
