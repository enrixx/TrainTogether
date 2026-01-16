package de.othr.traintogether.repository;

import de.othr.traintogether.model.TrainingModel.PersonalExercise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonalExerciseRepository extends JpaRepository<PersonalExercise, Long> {
    List<PersonalExercise> findAllByUserId(Long userId);

    // Find by Standard Exercise - returning List to be safe against duplicates
    List<PersonalExercise> findByStandardExercise_IdAndUser_Id(Long standardExerciseId, Long userId);

    // Find by Custom Exercise - returning List to be safe against duplicates
    List<PersonalExercise> findByCustomExercise_IdAndUser_Id(Long customExerciseId, Long userId);
}
