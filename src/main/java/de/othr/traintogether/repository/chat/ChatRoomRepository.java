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

    // Find a DM room that contains both users
    @Query("select r from ChatRoom r join r.members m1 join r.members m2 " +
            "where r.type = de.othr.traintogether.model.chat.ChatRoomType.DM " +
            "and m1.user.id = :userId1 and m2.user.id = :userId2")
    Optional<ChatRoom> findDmBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
