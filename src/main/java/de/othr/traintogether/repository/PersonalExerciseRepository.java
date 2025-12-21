package de.othr.traintogether.repository;

import de.othr.traintogether.model.TrainingModel.PersonalExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalExerciseRepository extends JpaRepository<PersonalExercise, Long> {
    List<PersonalExercise> findAllByUserId(Long userId);
    Optional<PersonalExercise> findByNameAndUser_Id(String name, Long userId);
}
