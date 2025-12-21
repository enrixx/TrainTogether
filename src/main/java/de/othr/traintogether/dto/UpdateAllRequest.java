package de.othr.traintogether.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAllRequest {
    private List<NewSplit> newSplits;
    private List<NewExercise> newExercises;
}