package de.othr.traintogether.repository;

import de.othr.traintogether.model.GymOwnerRequest;
import de.othr.traintogether.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GymOwnerRequestRepository extends JpaRepository<GymOwnerRequest, Long> {

    List<GymOwnerRequest> findByStatusOrderByRequestedAtDesc(RequestStatus status);

    List<GymOwnerRequest> findAllByOrderByRequestedAtDesc();

    Optional<GymOwnerRequest> findByUserIdAndStatus(Long userId, RequestStatus status);

    boolean existsByUserIdAndStatus(Long userId, RequestStatus status);
}

