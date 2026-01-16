package de.othr.traintogether.dto;

import de.othr.traintogether.model.TrainingModel.TrainingProfile;
import de.othr.traintogether.model.TrainingModel.TrainingSplit;
import lombok.Builder;
import lombok.Data;

import java.time.DayOfWeek;
import java.util.List;

@Data
@Builder
public class WorkoutPageDto {
    private TrainingProfile profile;
    private TrainingSplit activeSplit;
    private List<TrainingSplit> splits;
    private List<WorkoutLogResponseDto> todaysWorkout;
    private Long loggedDayId;
    private DayOfWeek loggedDayName;
    private List<ExerciseOptionDto> allExercises;
    private Long activeSplitId;
    private String currentDayOfWeek;
}
