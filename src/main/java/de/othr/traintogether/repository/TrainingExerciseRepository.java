package de.othr.traintogether.repository;

import de.othr.traintogether.model.TrainingModel.TrainingExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface TrainingExerciseRepository extends JpaRepository<TrainingExercise, Long> {
    List<TrainingExercise> findByDateAndPersonalExercise_User_Id(LocalDate date, Long userId);
    List<TrainingExercise> findAllByPersonalExercise_IdAndPersonalExercise_User_IdOrderByDateAsc(Long personalExerciseId, Long userId);
}
