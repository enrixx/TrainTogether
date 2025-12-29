package de.othr.traintogether.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAllRequestDto {
    private List<NewSplitDto> newSplits;
    private List<NewExerciseDto> newExercises;
}