package de.othr.traintogether.mapper;

import de.othr.traintogether.dto.chat.ChatMessageDto;
import de.othr.traintogether.dto.chat.MessageSenderDto;
import de.othr.traintogether.model.chat.ChatMessage;
import de.othr.traintogether.model.chat.ChatRoomMember;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageMapper {
    public ChatMessageDto toDto(ChatMessage message, ChatRoomMember me) {
        var sender = message.getSender();

        String displayName = sender.getDisplayName();
        String name = (displayName != null && !displayName.isBlank())
                ? displayName
                : sender.getUser().getUsername();

        MessageSenderDto messageSenderDto = new MessageSenderDto(
                sender.getId(),
                name,
                sender.getUser().getProfilePictureUrl()
        );

        ChatMessageDto messageDto = new ChatMessageDto(
                message.getId(),
                messageSenderDto,
                message.getContent(),
                message.getSentAt());

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
}
