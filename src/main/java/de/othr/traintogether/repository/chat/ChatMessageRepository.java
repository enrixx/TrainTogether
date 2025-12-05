package de.othr.traintogether.repository.chat;

import de.othr.traintogether.model.chat.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    Page<ChatMessage> findByChatRoomIdOrderBySentAtDesc(Long chatRoomId, Pageable pageable);
    long countByChatRoomIdAndSentAtAfter(Long chatRoomId, Instant sentAt);
    Optional<ChatMessage> findFirstByChatRoomIdOrderBySentAtDesc(Long chatRoomId);
}
