package de.othr.traintogether.repository;

import de.othr.traintogether.model.TrainingModel.CustomExercise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomExerciseRepository extends JpaRepository<CustomExercise, Long> {
    List<CustomExercise> findAllByCreatedBy_Id(Long userId);
}
