package de.othr.traintogether.dto;

import de.othr.traintogether.model.chat.ChatRoomType;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class ChatRoomDto {

    private Long id;
    private ChatRoomType type;
    private String name;
    private String pictureUrl;
    private Instant createdAt;
    private Set<Long> memberIds = new HashSet<>();

    public ChatRoomDto() {}

    public ChatRoomDto(Long id, ChatRoomType type, String name, String pictureUrl, Instant createdAt, Set<Long> memberIds) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.pictureUrl = pictureUrl;
        this.createdAt = createdAt;
        this.memberIds = memberIds != null ? memberIds : new HashSet<>();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ChatRoomType getType() { return type; }
    public void setType(ChatRoomType type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPictureUrl() { return pictureUrl; }
    public void setPictureUrl(String pictureUrl) { this.pictureUrl = pictureUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Set<Long> getMemberIds() { return memberIds; }
    public void setMemberIds(Set<Long> memberIds) { this.memberIds = memberIds != null ? memberIds : new HashSet<>(); }
}
