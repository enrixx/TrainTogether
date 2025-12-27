package de.othr.traintogether.model.chat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
@Table(name = "chat_attachments")
public class ChatAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "attachment")
    private ChatMessage message;

    @lombok.NonNull
    @Column(length = 512, nullable = false)
    private String url;

    @lombok.NonNull
    @Column(length = 255)
    private String filename;

    @lombok.NonNull
    @Column(name = "content_type", length = 100)
    private String contentType;

    @lombok.NonNull
    @Column(name = "size_bytes")
    private Long size;

}



