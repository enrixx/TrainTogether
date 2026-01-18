package de.othr.traintogether.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExerciseOptionDto {
    /**
     * A composite key representing the exercise type and ID.
     * Format: "S-{id}" for Standard Exercises (e.g., "S-1")
     *         "C-{id}" for Custom Exercises (e.g., "C-5")
     * This is used as the value in HTML select options to uniquely identify
     * exercises across different tables/entities.
     */
    private String value;
    private String name;
    private String type; // "STANDARD" or "CUSTOM"
}
