package de.othr.traintogether.model.TrainingModel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "standard_exercises")
@Getter
@Setter
@NoArgsConstructor
public class StandardExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nameDe;
    private String nameEn;

    public StandardExercise(String nameDe, String nameEn) {
        this.nameDe = nameDe;
        this.nameEn = nameEn;
    }
}
