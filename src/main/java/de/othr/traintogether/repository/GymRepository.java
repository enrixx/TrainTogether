package de.othr.traintogether.repository;

import de.othr.traintogether.model.Gym;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GymRepository extends JpaRepository<Gym, Long> {

    List<Gym> findByOwnerId(Long ownerId);

    Optional<Gym> findFirstByOwnerEmail(String email);

    List<Gym> findByCity(String city);

    List<Gym> findByCityContainingIgnoreCase(String city);

    List<Gym> findByNameContainingIgnoreCase(String name);
}
