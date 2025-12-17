package de.othr.traintogether.model.chat;

import de.othr.traintogether.model.User;
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
@Table(name = "chat_room_members", uniqueConstraints = @UniqueConstraint(columnNames = {"chat_room_id", "user_id"}))
public class ChatRoomMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @lombok.NonNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @lombok.NonNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    //This is optional and can be set to a nickname within the chat room
    @Column(length = 20)
    private String displayName;

    @lombok.NonNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRole role;

    @Column(name = "last_read", nullable = false)
    private Instant lastRead = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private final Instant createdAt = Instant.now();
}
