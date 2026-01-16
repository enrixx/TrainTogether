package de.othr.traintogether.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExerciseOptionDto {
    private String value; // e.g. "S-1" or "C-5"
    private String name;
    private String type; // "STANDARD" or "CUSTOM"
}
