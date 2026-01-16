package de.othr.traintogether.repository;

import de.othr.traintogether.model.TrainingModel.StandardExercise;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StandardExerciseRepository extends JpaRepository<StandardExercise, Long> {
}
