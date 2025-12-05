package de.othr.traintogether.repository;

import de.othr.traintogether.model.TrainingModel.BodyMeasurements;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BodyMeasurementsRepository extends JpaRepository<BodyMeasurements, Long> {
}