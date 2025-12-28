package de.othr.traintogether.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewExerciseDto {
    // Can be a Long (for existing days) or a String (for new days)
    private String trainingDayId;
    private Long personalExerciseId;
}