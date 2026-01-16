package de.othr.traintogether.repository;

import de.othr.traintogether.model.TrainingModel.TrainingProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrainingProfileRepository extends JpaRepository<TrainingProfile, Long> {
    Optional<TrainingProfile> findFirstByUserId(Long userId);

    TrainingProfile findByUserId(Long id);
}
