package de.othr.traintogether.repository;

import de.othr.traintogether.model.Gym;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GymRepository extends JpaRepository<Gym, Long> {

    List<Gym> findByOwnerId(Long ownerId);

    List<Gym> findByCity(String city);

    List<Gym> findByCityContainingIgnoreCase(String city);

    List<Gym> findByNameContainingIgnoreCase(String name);
}