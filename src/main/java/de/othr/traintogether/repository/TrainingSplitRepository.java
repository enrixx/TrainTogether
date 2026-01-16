package de.othr.traintogether.repository;

import de.othr.traintogether.model.trainingModel.TrainingSplit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingSplitRepository extends JpaRepository<TrainingSplit, Long> {
}