package de.othr.traintogether.repository;

import de.othr.traintogether.model.MatchingAction;
import de.othr.traintogether.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MatchingActionRepository extends JpaRepository<MatchingAction, Long> {

    Optional<MatchingAction> findByActorAndTarget(User actor, User target);

    @Query("SELECT ma.target.id FROM MatchingAction ma WHERE ma.actor = :actor AND ma.actionDate > :cutoffDate AND (ma.actionType = 'LIKE' OR ma.actionType = 'DISLIKE')")
    List<Long> findExcludedUserIds(@Param("actor") User actor, @Param("cutoffDate") LocalDateTime cutoffDate);
}
