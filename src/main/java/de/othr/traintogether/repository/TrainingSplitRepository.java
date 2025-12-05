package de.othr.traintogether.repository;

import de.othr.traintogether.model.TrainingModel.TrainingSplit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingSplitRepository extends JpaRepository<TrainingSplit, Long> {
}