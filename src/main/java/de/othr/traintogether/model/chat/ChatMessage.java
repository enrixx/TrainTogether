package de.othr.traintogether.model.chat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @lombok.NonNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false, updatable = false)
    private ChatRoom chatRoom;

    @lombok.NonNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_member_id", nullable = false, updatable = false)
    private ChatRoomMember sender;

    @lombok.NonNull
    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private final Instant sentAt = Instant.now();

    @Column(name = "edited", nullable = false)
    private boolean edited = false;

    @Column(name = "edited_at")
    private Instant editedAt;

    // Self-reference for replies
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_id")
    private ChatMessage replyTo;

    // Attachments metadata
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id", unique = true)
    private ChatAttachment attachment;

    public void setAttachment(ChatAttachment attachment) {
        this.attachment = attachment;
        if (attachment != null) {
            attachment.setMessage(this);
        }
    }

}
