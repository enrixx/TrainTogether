package de.othr.traintogether.mapper;

import de.othr.traintogether.dto.chat.ChatMessageDto;
import de.othr.traintogether.dto.chat.MessageSenderDto;
import de.othr.traintogether.model.chat.ChatMessage;
import de.othr.traintogether.model.chat.ChatRoomMember;
import de.othr.traintogether.service.chat.CursorService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ChatMessageMapper {

    private final CursorService cursorService;

    public ChatMessageMapper(CursorService cursorService) {
        this.cursorService = cursorService;
    }

    public ChatMessageDto toDto(ChatMessage message, ChatRoomMember me) {
        var sender = message.getSender();

        String displayName = sender.getDisplayName();
        String name = (displayName != null && !displayName.isBlank())
                ? displayName
                : sender.getUser().getUsername();


        MessageSenderDto messageSenderDto = new MessageSenderDto(
                sender.getId(),
                name,
                null
        );

        var pic = sender.getUser().getProfilePictureUrl();
        if (pic == null || pic.isBlank()) {
            messageSenderDto.setSenderPictureUrl("/images/default-profile.png");
        } else {
            messageSenderDto.setSenderPictureUrl(pic);
        }

        ChatMessageDto messageDto = new ChatMessageDto(
                message.getId(),
                messageSenderDto,
                message.getContent(),
                formatSentAt(message.getSentAt()),
                cursorService.buildCursor(message.getSentAt(), message.getId()));

        messageDto.setEdited(message.isEdited());
        messageDto.setMine(message.getSender().getId().equals(me.getId()));

        if(message.getReplyTo() != null){
            //TODO: add later
            messageDto.setReplyTo("Reply");
        }

        if (message.getAttachment() != null) {
            //TODO: add AttachmentDto later
        }

        return messageDto;
    }

    private String formatSentAt(Instant sentAt) {
        if (sentAt == null) return null;
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime zdt = sentAt.atZone(zone);
        LocalDate sentDate = zdt.toLocalDate();
        LocalDate today = LocalDate.now(zone);

        DateTimeFormatter timeOnly = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dayMonthTime = DateTimeFormatter.ofPattern("dd.MM HH:mm");
        DateTimeFormatter fullDateTime = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        if (sentDate.equals(today)) {
            return zdt.format(timeOnly);
        } else if (sentDate.getYear() == today.getYear()) {
            return zdt.format(dayMonthTime);
        } else {
            return zdt.format(fullDateTime);
        }
    }
}
