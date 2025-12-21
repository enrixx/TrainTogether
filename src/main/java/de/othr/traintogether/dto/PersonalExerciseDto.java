package de.othr.traintogether.dto;

import lombok.Data;

@Data
public class PersonalExerciseDto {
    private Long id;
    private String name;

    public PersonalExerciseDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}