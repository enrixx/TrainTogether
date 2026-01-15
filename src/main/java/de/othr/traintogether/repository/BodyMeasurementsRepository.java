package de.othr.traintogether.repository;

import de.othr.traintogether.model.trainingModel.BodyMeasurements;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BodyMeasurementsRepository extends JpaRepository<BodyMeasurements, Long> {
}