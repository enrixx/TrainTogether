package de.othr.traintogether.repository.chat;

import de.othr.traintogether.model.User;
import de.othr.traintogether.model.chat.ChatRoom;
import de.othr.traintogether.model.chat.ChatRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("select r from ChatRoom r join r.members m where m.user = :user and r.type = :type")
    List<ChatRoom> findByUserAndType(@Param("user") User user, @Param("type") ChatRoomType type);

    @Query("SELECT r FROM ChatRoom r JOIN r.members m1 JOIN r.members m2 WHERE r.type = 'DM' AND m1.user = :user1 AND m2.user = :user2")
    Optional<ChatRoom> findDmBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);
}
