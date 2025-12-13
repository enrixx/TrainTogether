package de.othr.traintogether.repository.chat;

import de.othr.traintogether.model.chat.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    long countByChatRoomIdAndSentAtAfter(Long chatRoomId, Instant sentAt);
    Optional<ChatMessage> findFirstByChatRoomIdAndSentAtAfter(Long chatRoomId, Instant sentAt);
    Optional<ChatMessage> findFirstByChatRoomIdAndIdBefore(Long chatRoomId, Long id);

    // newest message in chat room
    Optional<ChatMessage> findFirstByChatRoomIdOrderBySentAtDesc(Long chatRoomId);

    @Query("""
        SELECT m FROM ChatMessage m
        WHERE m.chatRoom.id = :chatRoomId
          AND (m.sentAt < :before OR (m.sentAt = :before AND m.id < :beforeId))
        ORDER BY m.sentAt DESC, m.id DESC
        """)
    List<ChatMessage> findByChatRoomIdBefore(
            @Param("chatRoomId") Long chatRoomId,
            @Param("before") Instant before,
            @Param("beforeId") Long beforeId,
            Pageable pageable);

    @Query("""
        SELECT m FROM ChatMessage m
        WHERE m.chatRoom.id = :chatRoomId
          AND (m.sentAt > :after OR (m.sentAt = :after AND m.id > :afterId))
        ORDER BY m.sentAt ASC, m.id ASC
        """)
    List<ChatMessage> findByChatRoomIdAfter(
            @Param("chatRoomId") Long chatRoomId,
            @Param("after") Instant after,
            @Param("afterId") Long afterId,
            Pageable pageable);
}
