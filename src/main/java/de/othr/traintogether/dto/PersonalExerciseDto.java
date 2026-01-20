package de.othr.traintogether.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PersonalExerciseDto {
    
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "The unique ID of the personal exercise")
    private Long id;

    @Schema(description = "ID of the standard exercise (provide either this OR customExerciseId)", example = "1")
    private Long standardExerciseId;

    @Schema(description = "ID of the custom exercise (provide either this OR standardExerciseId)")
    private Long customExerciseId;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Name of the exercise (derived from standard/custom exercise)")
    private String name;

    @Schema(description = "Number of sets", example = "3")
    private int sets;
}
