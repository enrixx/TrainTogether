package de.othr.traintogether.dto;

import lombok.Data;

@Data
public class WorkoutLogResponse {
    private Long personalExerciseId;
    private String exerciseName;
    private int sets;
    private String reps;
    private String weight;

    public WorkoutLogResponse(Long personalExerciseId, String exerciseName, int sets, String reps, String weight) {
        this.personalExerciseId = personalExerciseId;
        this.exerciseName = exerciseName;
        this.sets = sets;
        this.reps = reps;
        this.weight = weight;
    }
}