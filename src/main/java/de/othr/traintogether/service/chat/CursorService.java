package de.othr.traintogether.service.chat;

import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CursorService {

    public String buildCursor(Instant instant, Long id) {
        return instant.toEpochMilli() + ":" + id;
    }

    public Instant decodeCursorInstant(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        String[] parts = cursor.split(":");
        try {
            return Instant.ofEpochMilli(Long.parseLong(parts[0]));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cursor");
        }
    }

    public Long decodeCursorId(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        String[] parts = cursor.split(":");
        if (parts.length < 2 || parts[1].isBlank()) return null;
        try {
            return Long.parseLong(parts[1]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cursor id");

        }
    }
}
