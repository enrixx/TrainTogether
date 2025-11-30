package de.othr.traintogether.model.chat;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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

    public ChatRoom() {}
    public  ChatRoom(ChatRoomType type) {
        this.type = Objects.requireNonNull(type, "type must not be null");
    }
    public ChatRoom(ChatRoomType type, String name, String pictureUrl) {
        this(type);
        this.name = name;
        this.pictureUrl = pictureUrl;
    }

     // getters + setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ChatRoomType getType() {
        return type;
    }

    public void setType(ChatRoomType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Set<ChatRoomMember> getMembers() {
        return members;
    }

    public void addMember(ChatRoomMember member) {
        member.setChatRoom(this);
        members.add(member);
    }

    public void removeMember(ChatRoomMember member) {
        if (members.remove(member)) {
            member.setChatRoom(null);
        }
    }
}
