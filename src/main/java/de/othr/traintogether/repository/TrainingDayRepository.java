package de.othr.traintogether.repository;

import de.othr.traintogether.model.TrainingModel.TrainingDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.Optional;

public interface TrainingDayRepository extends JpaRepository<TrainingDay, Long> {

    Optional<TrainingDay> findBySplitIdAndWeekday(Long splitId, DayOfWeek weekday);

}