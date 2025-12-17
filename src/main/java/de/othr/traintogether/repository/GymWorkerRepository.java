package de.othr.traintogether.repository;

import de.othr.traintogether.model.GymWorker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GymWorkerRepository extends JpaRepository<GymWorker, Long> {

    List<GymWorker> findByGymId(Long gymId);

    List<GymWorker> findByGymIdAndActiveTrue(Long gymId);

    Optional<GymWorker> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    List<GymWorker> findByCreatedById(Long createdById);
}

