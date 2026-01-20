package de.othr.traintogether.dto;

import lombok.Data;

@Data
public class WorkoutLogResponseDto {
    private Long personalExerciseId;
    private String exerciseName;
    private String exerciseValue; // Added field
    private int sets;
    private String reps;
    private String weight;

    public WorkoutLogResponseDto(Long personalExerciseId, String exerciseName, String exerciseValue, int sets, String reps, String weight) {
        this.personalExerciseId = personalExerciseId;
        this.exerciseName = exerciseName;
        this.exerciseValue = exerciseValue;
        this.sets = sets;
        this.reps = reps;
        this.weight = weight;
    }
}