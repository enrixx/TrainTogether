package de.othr.traintogether.repository;

import de.othr.traintogether.model.TrainingModel.TrainingExercise;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingExerciseRepository extends JpaRepository<TrainingExercise, Long> {
}