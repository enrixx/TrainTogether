package de.othr.traintogether.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkoutDaySummaryDto {
    private LocalDate date;
    private String weekday;
    private List<WorkoutLogResponseDto> exercises;
    private boolean isRestDay;
}
