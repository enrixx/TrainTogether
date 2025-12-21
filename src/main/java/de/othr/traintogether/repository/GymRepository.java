package de.othr.traintogether.repository;

import de.othr.traintogether.model.Gym;
import org.springframework.data.repository.CrudRepository;

public interface GymRepository extends CrudRepository<Gym, Long> {
}