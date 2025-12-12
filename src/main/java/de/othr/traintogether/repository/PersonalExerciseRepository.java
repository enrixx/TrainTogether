package de.othr.traintogether.repository;

import de.othr.traintogether.model.TrainingModel.PersonalExercise;
import de.othr.traintogether.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonalExerciseRepository extends JpaRepository<PersonalExercise, Long> {
    List<PersonalExercise> findAllByUserId(Long userId);
}
