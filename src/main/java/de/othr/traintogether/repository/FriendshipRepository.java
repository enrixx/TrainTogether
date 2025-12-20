package de.othr.traintogether.repository;

import de.othr.traintogether.model.Friendship;
import de.othr.traintogether.model.FriendshipStatus;
import de.othr.traintogether.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @EntityGraph(attributePaths = {"requester", "addressee"})
    @Query("SELECT f FROM Friendship f WHERE (f.requester = :user OR f.addressee = :user) AND f.status = :status")
    List<Friendship> findByUserAndStatus(@Param("user") User user, @Param("status") FriendshipStatus status);

    @EntityGraph(attributePaths = {"requester", "addressee"})
    @Query("SELECT f FROM Friendship f WHERE (f.requester = :user1 AND f.addressee = :user2) OR (f.requester = :user2 AND f.addressee = :user1)")
    Optional<Friendship> findBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);

    @EntityGraph(attributePaths = {"requester", "addressee"})
    List<Friendship> findByAddresseeAndStatus(User addressee, FriendshipStatus status);

    @EntityGraph(attributePaths = {"requester", "addressee"})
    List<Friendship> findByRequesterAndStatus(User requester, FriendshipStatus status);
}

