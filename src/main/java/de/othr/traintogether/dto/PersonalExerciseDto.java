package de.othr.traintogether.dto;

import lombok.Data;

@Data
public class PersonalExerciseDto {
    private Long id;
    private Long standardExerciseId;
    private Long customExerciseId;
    private String name;
    private int sets;
}
