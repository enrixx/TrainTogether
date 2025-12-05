package de.othr.traintogether.repository;

import de.othr.traintogether.model.TrainingModel.TrainingDay;
import de.othr.traintogether.model.TrainingModel.Weekday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrainingDayRepository extends JpaRepository<TrainingDay, Long> {

    Optional<TrainingDay> findBySplitIdAndWeekday(Long splitId, Weekday weekday);

}