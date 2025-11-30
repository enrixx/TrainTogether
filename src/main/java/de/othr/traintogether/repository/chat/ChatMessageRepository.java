package de.othr.traintogether.repository.chat;

import de.othr.traintogether.model.chat.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    Page<ChatMessage> findByChatRoomIdOrderBySentAtDesc(Long chatRoomId, Pageable pageable);
}
