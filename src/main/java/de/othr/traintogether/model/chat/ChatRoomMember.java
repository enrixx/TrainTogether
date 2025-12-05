package de.othr.traintogether.model.chat;

import de.othr.traintogether.model.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "chat_room_members", uniqueConstraints = @UniqueConstraint(columnNames = {"chat_room_id", "user_id"}))
public class ChatRoomMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    //This is optional and can be set to a nickname within the chat room
    @Column(length = 20)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRole role;

    @Column(name = "last_read", nullable = false)
    private final Instant lastRead = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private final Instant createdAt = Instant.now();

    public ChatRoomMember() {}
    public ChatRoomMember(ChatRoom chatRoom, User user, ChatRole role) {
        this.chatRoom = chatRoom;
        this.user = user;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ChatRoom getChatRoom() {
        return chatRoom;
    }

    public void setChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public ChatRole getRole() {
        return role;
    }

    public void setRole(ChatRole role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastRead() { return lastRead; }

    public void setLastRead(Instant lastRead) {
    }
}
