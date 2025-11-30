package de.othr.traintogether.repository.chat;

import de.othr.traintogether.model.chat.ChatAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatAttachmentRepository extends JpaRepository<ChatAttachment, Long> {

}
