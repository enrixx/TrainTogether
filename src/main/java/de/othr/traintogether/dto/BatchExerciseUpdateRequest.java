package de.othr.traintogether.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchExerciseUpdateRequest {
    private List<DayUpdate> updates;

    @Data
    public static class DayUpdate {
        private Long dayId;
        private List<ExerciseUpdate> exercises;
        private List<Long> exerciseIdsToDelete;
    }

    @Data
    public static class ExerciseUpdate {
        private Long exerciseId;
        private int sets;
    }
}