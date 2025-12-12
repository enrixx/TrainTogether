package de.othr.traintogether.service;

import de.othr.traintogether.model.User;
import lombok.Getter;

@Getter
public class UserCreatedEvent {
    private final User user;

    public UserCreatedEvent(User user) {
        this.user = user;
    }
}
