package de.othr.traintogether.dto;

import lombok.Data;

@Data
public class WorkoutLogResponse {
    private Long personalExerciseId;
    private String exerciseName;
    private int sets;
    private String reps;

    public WorkoutLogResponse(Long personalExerciseId, String exerciseName, int sets, String reps) {
        this.personalExerciseId = personalExerciseId;
        this.exerciseName = exerciseName;
        this.sets = sets;
        this.reps = reps;
    }
}