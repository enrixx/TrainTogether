package de.othr.traintogether.model.chat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "chat_rooms")
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 10, nullable = false)
    private ChatRoomType type;

    //This should be only set for group chats
    @Column(length = 50)
    private String name;

    //This should be only set for group chats
    @Column(name = "picture_url", length = 512)
    private String pictureUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ChatRoomMember> members = new HashSet<>();

    public ChatRoom(ChatRoomType type) {
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    public ChatRoom(ChatRoomType type, String name, String pictureUrl) {
        this(type);
        this.name = name;
        this.pictureUrl = pictureUrl;
    }

    public void addMember(ChatRoomMember member) {
        member.setChatRoom(this);
        members.add(member);
    }

    public void removeMember(ChatRoomMember member) {
        if (members.remove(member)) {
            //TODO: flag member as removed so the chat doesnt appear for him
        }
    }
}
