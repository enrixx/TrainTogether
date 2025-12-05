package de.othr.traintogether.repository;

import de.othr.traintogether.model.TrainingModel.TrainingProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingProfileRepository extends JpaRepository<TrainingProfile, Long> {
}
