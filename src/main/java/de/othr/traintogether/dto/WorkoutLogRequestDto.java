package de.othr.traintogether.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class WorkoutLogRequestDto {
    private Long trainingDayId;
    private LocalDate date;
    private List<ExerciseLog> exercises;

    @Data
    public static class ExerciseLog {
        private Long personalExerciseId;
        private String exerciseValue;
        private int sets;
        private String reps;
        private String weight;
    }
}
