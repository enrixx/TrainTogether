package de.othr.traintogether.repository.chat;

import de.othr.traintogether.model.chat.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    Optional<ChatRoomMember> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);
}
