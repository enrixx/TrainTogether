package de.othr.traintogether.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class WorkoutLogRequest {
    private Long trainingDayId;
    private LocalDate date;
    private List<ExerciseLog> exercises;

    @Data
    public static class ExerciseLog {
        private Long personalExerciseId;
        private int sets;
        private String reps; // Changed to String
    }
}